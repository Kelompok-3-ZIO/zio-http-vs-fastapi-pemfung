package siak

import com.password4j.{Argon2Function, Password}
import com.password4j.types.Argon2
import pdi.jwt.{Jwt, JwtAlgorithm, JwtClaim}
import zio.*

import java.time.ZoneOffset

trait PasswordHasher:
  def verify(password: String, hash: String): UIO[Boolean]

object Argon2PasswordHasher extends PasswordHasher:
  // Parameter sama dengan pwdlib: m=65536, t=3, p=4, panjang hash 32 byte, Argon2id.
  private val argon2 = Argon2Function.getInstance(65536, 3, 4, 32, Argon2.ID)

  // Dipakai saat username tidak ada, supaya waktu respons sama (mencegah timing attack).
  val dummyHash: String = Password.hash("dummypassword").addRandomSalt(16).`with`(argon2).getResult

  def verify(password: String, hash: String): UIO[Boolean] =
    ZIO.attemptBlocking(Password.check(password, hash).`with`(Argon2Function.getInstanceFromHash(hash))).orDie

  val layer: ULayer[PasswordHasher] = ZLayer.succeed(Argon2PasswordHasher)

final class TokenService(config: AppConfig):
  private val secret = config.jwtSecret.stringValue

  def issue(userId: Int): UIO[String] =
    Clock.instant.map { now =>
      val claim = JwtClaim(
        subject = Some(userId.toString),
        issuedAt = Some(now.getEpochSecond),
        expiration = Some(now.plusSeconds(config.accessTokenExpireMinutes * 60L).getEpochSecond)
      )
      Jwt.encode(claim, secret, JwtAlgorithm.HS256)
    }

  // Token -> id user. Tanda tangan, exp, dan sub wajib valid; selain itu None.
  def verify(token: String): UIO[Option[Int]] =
    Clock.instant.map { now =>
      Jwt(java.time.Clock.fixed(now, ZoneOffset.UTC))
        .decode(token, secret, Seq(JwtAlgorithm.HS256))
        .toOption
        .filter(_.expiration.isDefined)
        .flatMap(_.subject)
        .flatMap(_.toIntOption)
    }

object TokenService:
  val layer: ZLayer[AppConfig, Nothing, TokenService] = ZLayer.fromFunction(TokenService(_))
