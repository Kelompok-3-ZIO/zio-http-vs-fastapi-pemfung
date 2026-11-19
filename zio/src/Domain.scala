package siak

import zio.schema.{DeriveSchema, Schema}

enum Role:
  case Admin, Lecturer, Student

enum Permission:
  case ViewGrade, EditGrade, ManageUsers

final case class User(id: Int, username: String, name: String, role: Role)

final case class Grade(course: String, letter: String)

object Role:
  // Di JSON, role ditulis sebagai teks biasa: "Admin", "Lecturer", "Student".
  given Schema[Role] = Schema[String].transformOrFail(
    text => Role.values.find(_.toString == text).toRight(s"Unknown role $text"),
    role => Right(role.toString)
  )

object User:
  given Schema[User] = DeriveSchema.gen[User]

object Grade:
  given Schema[Grade] = DeriveSchema.gen[Grade]

object Rbac:

  // Role permissions: seluruh aturan role ke permission ada di sini.
  def permissions(role: Role): Set[Permission] = role match
    case Role.Admin    => Set(Permission.ViewGrade, Permission.EditGrade, Permission.ManageUsers)
    case Role.Lecturer => Set(Permission.ViewGrade, Permission.EditGrade)
    case Role.Student  => Set(Permission.ViewGrade)

  // Ownership rule: Student hanya boleh melihat grade miliknya sendiri.
  def canViewGradesOf(user: User, studentId: Int): Boolean =
    user.role != Role.Student || user.id == studentId
