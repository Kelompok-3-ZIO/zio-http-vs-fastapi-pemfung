package siak

import zio.http.*
import zio.schema.{DeriveSchema, Schema}
import zio.schema.codec.JsonCodec.schemaBasedBinaryCodec

// Bentuk body error sama dengan FastAPI: {"detail": "..."}.
final case class ErrorBody(detail: String)

object ErrorBody:
  given Schema[ErrorBody] = DeriveSchema.gen[ErrorBody]

  def response(status: Status, detail: String): Response =
    Response(status = status, body = Body.from(ErrorBody(detail)))
      .addHeader(Header.ContentType(MediaType.application.json))

// Expected error saat mengubah grade, semuanya dalam satu tipe.
enum GradeError:
  case InvalidBody
  case StudentNotFound(studentId: Int)
  case CourseNotFound(course: String)
  case GradingPeriodClosed(course: String)

object GradeError:
  def toResponse(error: GradeError): Response = error match
    case InvalidBody                 => ErrorBody.response(Status.BadRequest, "Body must be a JSON Grade")
    case StudentNotFound(studentId)  => ErrorBody.response(Status.NotFound, s"Student $studentId not found")
    case CourseNotFound(course)      => ErrorBody.response(Status.NotFound, s"Course $course not found")
    case GradingPeriodClosed(course) => ErrorBody.response(Status.Conflict, s"Grading period for $course is closed")
