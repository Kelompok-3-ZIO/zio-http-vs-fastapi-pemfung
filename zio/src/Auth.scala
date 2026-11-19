package siak

import zio.*
import zio.http.*

object Auth:

  // Authentication: token -> User. Role dibaca ulang dari database di setiap request.
  val authenticate: HandlerAspect[UserRepository & TokenService, User] =
    HandlerAspect.customAuthProvidingZIO(
      (request: Request) =>
        request.header(Header.Authorization) match
          case Some(Header.Authorization.Bearer(token)) =>
            for
              userId <- ZIO.serviceWithZIO[TokenService](_.verify(token.stringValue))
              user   <- ZIO.foreach(userId)(id => ZIO.serviceWithZIO[UserRepository](_.findById(id)))
            yield user.flatten
          case _ => ZIO.none,
      Headers(Header.WWWAuthenticate.Bearer(realm = "SIAK"))
    )

  // Authorization: route menyebut permission yang dibutuhkan, bukan role.
  def requires(permission: Permission): HandlerAspect[User, Unit] =
    HandlerAspect.interceptIncomingHandler(
      Handler.fromFunctionZIO[Request] { request =>
        ZIO.serviceWithZIO[User] { user =>
          if Rbac.permissions(user.role).contains(permission) then ZIO.succeed((request, ()))
          else ZIO.fail(ErrorBody.response(Status.Forbidden, s"${user.role} lacks permission $permission"))
        }
      }
    )
