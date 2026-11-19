package siak

import pdi.jwt.{Jwt, JwtAlgorithm, JwtClaim}
import zio.*
import zio.http.*
import zio.schema.codec.JsonCodec.schemaBasedBinaryCodec
import zio.test.*

import TestEnv.*

object ApiSpec extends ZIOSpecDefault:
  val grade = Grade("PEMFUNG", "A")

  def withToken(token: String): Headers = Headers(Header.Authorization.Bearer(token))

  val accessMatrix: List[(String, Method, String, Status)] = List(
    ("ani", Method.GET, "/students/3/grades", Status.Ok),
    ("budi", Method.GET, "/students/3/grades", Status.Ok),
    ("cici", Method.GET, "/students/3/grades", Status.Ok),
    ("cici", Method.GET, "/students/4/grades", Status.Forbidden), // horizontal privilege escalation
    ("cici", Method.PUT, "/students/3/grades", Status.Forbidden), // vertical privilege escalation
    ("cici", Method.GET, "/admin/users", Status.Forbidden),       // vertical privilege escalation
    ("budi", Method.GET, "/admin/users", Status.Forbidden),
    ("budi", Method.PUT, "/students/3/grades", Status.Ok),
    ("ani", Method.GET, "/admin/users", Status.Ok)
  )

  val editErrors: List[(Int, Body, Status)] = List(
    (3, Body.from(Grade("PEMFUNG", "A")), Status.Ok),
    (99, Body.from(Grade("PEMFUNG", "A")), Status.NotFound),
    (3, Body.from(Grade("BASDAT", "A")), Status.NotFound),
    (3, Body.from(Grade("KALKULUS", "A")), Status.Conflict),
    (3, Body.fromString("""{"course":"PEMFUNG"}"""), Status.BadRequest)
  )

  def spec = suite("Api")(
    test("login dengan password salah") {
      login("cici", "salah").map(response => assertTrue(response.status == Status.Unauthorized))
    },
    test("login dengan user yang tidak ada") {
      login("tidakada", "x").map(response => assertTrue(response.status == Status.Unauthorized))
    },
    test("tanpa token") {
      send(Request.get(URL.root / "students" / "3" / "grades")).map(r => assertTrue(r.status == Status.Unauthorized))
    },
    test("token tanpa exp") {
      val token = Jwt.encode(JwtClaim(subject = Some("1")), secret, JwtAlgorithm.HS256)
      send(Request.get(URL.root / "admin" / "users").addHeaders(withToken(token)))
        .map(r => assertTrue(r.status == Status.Unauthorized))
    },
    test("token kedaluwarsa") {
      // Jam di ZIO Test mulai dari 1970, jadi majukan dulu ke waktu sekarang.
      val token = Jwt.encode(JwtClaim(subject = Some("1"), expiration = Some(1L)), secret, JwtAlgorithm.HS256)
      for
        _ <- TestClock.setTime(java.time.Instant.parse("2026-11-19T00:00:00Z"))
        r <- send(Request.get(URL.root / "admin" / "users").addHeaders(withToken(token)))
      yield assertTrue(r.status == Status.Unauthorized)
    },
    test("token ditandatangani secret lain") {
      val token = Jwt.encode(JwtClaim(subject = Some("1")), "secret-lain-yang-panjangnya-minimal-32-byte", JwtAlgorithm.HS256)
      send(Request.get(URL.root / "admin" / "users").addHeaders(withToken(token)))
        .map(r => assertTrue(r.status == Status.Unauthorized))
    },
    suite("matriks akses")(
      accessMatrix.map { case (username, method, path, expected) =>
        test(s"$username $method $path -> ${expected.code}") {
          for
            headers  <- bearer(username)
            body      = if method == Method.PUT then Body.from(grade) else Body.empty
            response <- send(Request(method = method, url = URL(Path.decode(path)), headers = headers, body = body))
          yield assertTrue(response.status == expected)
        }
      }*
    ),
    suite("error saat mengubah grade")(
      editErrors.map { case (studentId, body, expected) =>
        test(s"student $studentId -> ${expected.code}") {
          for
            headers  <- bearer("budi")
            response <- send(Request.put(URL.root / "students" / studentId.toString / "grades", body).addHeaders(headers))
          yield assertTrue(response.status == expected)
        }
      }*
    )
  ).provide(TestEnv.live)
