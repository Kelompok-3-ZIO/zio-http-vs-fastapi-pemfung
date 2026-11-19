package siak

import io.getquill.*
import io.getquill.jdbczio.Quill
import org.sqlite.SQLiteDataSource
import zio.*

import javax.sql.DataSource

trait UserRepository:
  def findById(id: Int): UIO[Option[User]]
  def findWithHash(username: String): UIO[Option[(User, String)]]
  def all: UIO[List[User]]

trait GradeRepository:
  def studentExists(studentId: Int): UIO[Boolean]
  def gradingOpen(course: String): UIO[Option[Boolean]]
  def gradesOf(studentId: Int): UIO[List[Grade]]
  def save(studentId: Int, grade: Grade): UIO[Unit]

// Baris tabel, terpisah dari model domain.
final case class UserRow(id: Int, username: String, name: String, role: String, passwordHash: String)
final case class CourseRow(code: String, name: String, gradingOpen: Boolean)
final case class GradeRow(studentId: Int, courseCode: String, letter: String)

object Db:
  val dataSource: ZLayer[AppConfig, Nothing, DataSource] = ZLayer.fromFunction { (config: AppConfig) =>
    val ds = SQLiteDataSource()
    ds.setUrl(s"jdbc:sqlite:${config.databasePath}")
    ds
  }

  val quill: ZLayer[DataSource, Nothing, Quill.Sqlite[SnakeCase]] = Quill.Sqlite.fromNamingStrategy(SnakeCase)

// Kegagalan database dianggap unexpected error (defect), jadi tidak muncul di tipe.
final class QuillUserRepository(quill: Quill.Sqlite[SnakeCase]) extends UserRepository:
  import quill.*

  private inline def users = quote(querySchema[UserRow]("users"))

  private def toUser(row: UserRow): User =
    User(row.id, row.username, row.name, Role.valueOf(row.role))

  def findById(id: Int): UIO[Option[User]] =
    run(users.filter(_.id == lift(id))).map(_.headOption.map(toUser)).orDie

  def findWithHash(username: String): UIO[Option[(User, String)]] =
    run(users.filter(_.username == lift(username))).map(_.headOption.map(row => (toUser(row), row.passwordHash))).orDie

  def all: UIO[List[User]] =
    run(users.sortBy(_.id)).map(_.map(toUser)).orDie

object QuillUserRepository:
  val layer: ZLayer[Quill.Sqlite[SnakeCase], Nothing, UserRepository] = ZLayer.fromFunction(QuillUserRepository(_))

final class QuillGradeRepository(quill: Quill.Sqlite[SnakeCase]) extends GradeRepository:
  import quill.*

  private inline def users   = quote(querySchema[UserRow]("users"))
  private inline def courses = quote(querySchema[CourseRow]("courses"))
  private inline def grades  = quote(querySchema[GradeRow]("grades"))

  def studentExists(studentId: Int): UIO[Boolean] =
    run(users.filter(u => u.id == lift(studentId) && u.role == "Student").nonEmpty).orDie

  def gradingOpen(course: String): UIO[Option[Boolean]] =
    run(courses.filter(_.code == lift(course)).map(_.gradingOpen)).map(_.headOption).orDie

  def gradesOf(studentId: Int): UIO[List[Grade]] =
    run(grades.filter(_.studentId == lift(studentId)).sortBy(_.courseCode))
      .map(_.map(row => Grade(row.courseCode, row.letter)))
      .orDie

  def save(studentId: Int, grade: Grade): UIO[Unit] =
    val row = GradeRow(studentId, grade.course, grade.letter)
    run(
      grades
        .insertValue(lift(row))
        .onConflictUpdate(_.studentId, _.courseCode)((table, excluded) => table.letter -> excluded.letter)
    ).unit.orDie

object QuillGradeRepository:
  val layer: ZLayer[Quill.Sqlite[SnakeCase], Nothing, GradeRepository] = ZLayer.fromFunction(QuillGradeRepository(_))
