package siak

import io.getquill.SnakeCase
import io.getquill.jdbczio.Quill
import zio.*
import zio.http.*
import zio.schema.codec.JsonCodec.schemaBasedBinaryCodec

import java.nio.file.{Files, Path}
import java.sql.DriverManager

object TestEnv:
  val secret = "test-secret-yang-panjangnya-minimal-32-byte"

  // Database SQLite baru dari schema.sql dan seed.sql untuk setiap test.
  private val freshDatabase: ZIO[Any, Throwable, String] = ZIO.attemptBlocking {
    val file = Files.createTempFile("siak", ".db")
    val connection = DriverManager.getConnection(s"jdbc:sqlite:$file")
    try
      val statement = connection.createStatement()
      for name <- List("schema.sql", "seed.sql") do
        Files.readString(Path.of("../db", name)).split(";").map(_.trim).filter(_.nonEmpty).foreach(statement.execute)
    finally connection.close()
    file.toString
  }

  val config: ZLayer[Any, Throwable, AppConfig] =
    ZLayer.fromZIO(freshDatabase.map(path => AppConfig(Config.Secret(secret), path, 30)))

  type Env = UserRepository & GradeRepository & PasswordHasher & TokenService

  val live: ZLayer[Any, Throwable, Env] =
    ZLayer.make[Env](
      config,
      Db.dataSource,
      Db.quill,
      QuillUserRepository.layer,
      QuillGradeRepository.layer,
      Argon2PasswordHasher.layer,
      TokenService.layer
    )

  def send(request: Request): ZIO[Env, Nothing, Response] =
    ZIO.scoped(Api.routes.runZIO(request))

  def login(username: String, password: String): ZIO[Env, Nothing, Response] =
    send(
      Request.post(
        URL.root / "login",
        Body.fromURLEncodedForm(Form(FormField.simpleField("username", username), FormField.simpleField("password", password)))
      )
    )

  def bearer(username: String): ZIO[Env, Throwable, Headers] =
    for
      response <- login(username, s"rahasia-$username")
      token    <- response.body.to[TokenResponse]
    yield Headers(Header.Authorization.Bearer(token.access_token))
