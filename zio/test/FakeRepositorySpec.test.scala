package siak

import zio.*
import zio.http.*
import zio.test.*
import zio.schema.codec.JsonCodec.schemaBasedBinaryCodec

// Pengganti database untuk test: semua periode ditutup.
final class InMemoryGradeRepository(saved: Ref[List[(Int, Grade)]]) extends GradeRepository:
  def studentExists(studentId: Int): UIO[Boolean]        = ZIO.succeed(true)
  def gradingOpen(course: String): UIO[Option[Boolean]]  = ZIO.some(false)
  def gradesOf(studentId: Int): UIO[List[Grade]]         = ZIO.succeed(Nil)
  def save(studentId: Int, grade: Grade): UIO[Unit]      = saved.update((studentId, grade) :: _)

object FakeRepositorySpec extends ZIOSpecDefault:

  // Mengganti repository cukup dengan memberi layer lain; compiler memastikan tipenya GradeRepository.
  def spec = test("periode ditutup dengan repository palsu") {
    for
      saved    <- Ref.make(List.empty[(Int, Grade)])
      response <- TestEnv.bearer("budi").flatMap { headers =>
                    TestEnv.send(Request.put(URL.root / "students" / "3" / "grades", Body.from(Grade("PEMFUNG", "A"))).addHeaders(headers))
                  }.provideSome[UserRepository & PasswordHasher & TokenService](ZLayer.succeed[GradeRepository](InMemoryGradeRepository(saved)))
      stored   <- saved.get
    yield assertTrue(response.status == Status.Conflict, stored.isEmpty)
  }.provide(TestEnv.live)
