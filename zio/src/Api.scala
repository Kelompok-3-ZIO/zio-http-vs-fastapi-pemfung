package siak

import zio.*
import zio.http.*
import zio.schema.{DeriveSchema, Schema}
import zio.schema.codec.JsonCodec.schemaBasedBinaryCodec

final case class TokenResponse(access_token: String, token_type: String)

object TokenResponse:
  given Schema[TokenResponse] = DeriveSchema.gen[TokenResponse]

object Api:

  private def json[A: Schema](value: A): Response =
    Response(body = Body.from(value)).addHeader(Header.ContentType(MediaType.application.json))

  private val incorrectLogin: Response =
    ErrorBody.response(Status.Unauthorized, "Incorrect username or password")
      .addHeader(Header.WWWAuthenticate.Bearer(realm = "SIAK"))

  // OAuth2 password flow: form username dan password, dijawab access_token.
  val login: Routes[UserRepository & PasswordHasher & TokenService, Response] = Routes(
    Method.POST / "login" -> handler { (request: Request) =>
      for
        form     <- request.body.asURLEncodedForm.orElseFail(incorrectLogin)
        username <- ZIO.fromOption(form.get("username").flatMap(_.stringValue)).orElseFail(incorrectLogin)
        password <- ZIO.fromOption(form.get("password").flatMap(_.stringValue)).orElseFail(incorrectLogin)
        found    <- ZIO.serviceWithZIO[UserRepository](_.findWithHash(username))
        hasher   <- ZIO.service[PasswordHasher]
        user     <- found match
                      case Some((user, hash)) => hasher.verify(password, hash).map(ok => Option.when(ok)(user))
                      case None               => hasher.verify(password, Argon2PasswordHasher.dummyHash).as(None)
        user     <- ZIO.fromOption(user).orElseFail(incorrectLogin)
        token    <- ZIO.serviceWithZIO[TokenService](_.issue(user.id))
      yield json(TokenResponse(token, "bearer"))
    }
  )

  val viewGrades: Routes[User & GradeRepository, Response] = Routes(
    Method.GET / "students" / int("studentId") / "grades" -> handler { (studentId: Int, _: Request) =>
      for
        user   <- ZIO.service[User]
        _      <- ZIO.unless(Rbac.canViewGradesOf(user, studentId))(
                    ZIO.fail(ErrorBody.response(Status.Forbidden, "Students may only view their own grades"))
                  )
        grades <- ZIO.serviceWithZIO[GradeRepository](_.gradesOf(studentId))
      yield json(grades)
    }
  ) @@ Auth.requires(Permission.ViewGrade)

  val editGrade: Routes[User & GradeRepository, Response] = (Routes(
    Method.PUT / "students" / int("studentId") / "grades" -> handler { (studentId: Int, request: Request) =>
      (for
        grade <- request.body.to[Grade].orElseFail(GradeError.InvalidBody)
        saved <- GradeService.edit(studentId, grade)
      yield json(saved)).mapError(GradeError.toResponse)
    }
  ) @@ Auth.requires(Permission.EditGrade)) @@ AuditLog.aspect

  val listUsers: Routes[User & UserRepository, Response] = Routes(
    Method.GET / "admin" / "users" -> handler { (_: Request) =>
      ZIO.serviceWithZIO[UserRepository](_.all).map(json)
    }
  ) @@ Auth.requires(Permission.ManageUsers)

  // Route yang butuh User belum bisa dijalankan sebelum Auth.authenticate menyediakannya.
  val protectedRoutes: Routes[User & GradeRepository & UserRepository, Response] =
    viewGrades ++ editGrade ++ listUsers

  val routes: Routes[UserRepository & GradeRepository & PasswordHasher & TokenService, Response] =
    login ++ protectedRoutes.@@[GradeRepository & UserRepository & TokenService](Auth.authenticate)
