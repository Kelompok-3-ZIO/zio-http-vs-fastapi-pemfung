package siak

import zio.*
import zio.http.*

object AuditLog:

  // Mencatat method, path (tetap ter-encode), dan status akhir untuk route yang memasangnya.
  val aspect: HandlerAspect[Any, Unit] =
    HandlerAspect.interceptHandlerStateful(
      Handler.fromFunction[Request](request => (s"${request.method} ${request.path.encode}", (request, ())))
    )(
      Handler.fromFunctionZIO[(String, Response)] { case (target, response) =>
        ZIO.logInfo(s"audit $target -> ${response.status.code}").as(response)
      }
    )
