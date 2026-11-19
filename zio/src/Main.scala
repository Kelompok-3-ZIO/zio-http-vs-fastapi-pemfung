package siak

import zio.*
import zio.http.*
import zio.logging.slf4j.bridge.Slf4jBridge

object Main extends ZIOAppDefault:
  // Log dari library Java (lewat SLF4J) diteruskan ke logger ZIO.
  override val bootstrap: ZLayer[ZIOAppArgs, Any, Any] = Slf4jBridge.init()

  val run =
    Server
      .serve(Api.routes)
      .provide(
        Server.defaultWithPort(8080),
        AppConfig.layer,
        Db.dataSource,
        Db.quill,
        QuillUserRepository.layer,
        QuillGradeRepository.layer,
        Argon2PasswordHasher.layer,
        TokenService.layer
      )
