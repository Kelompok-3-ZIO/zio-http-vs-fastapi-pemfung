package siak

import zio.test.*

// Aturan akses adalah fungsi biasa, jadi bisa dites tanpa server dan tanpa database.
object RbacSpec extends ZIOSpecDefault:
  val cici = User(3, "cici", "Cici", Role.Student)
  val budi = User(2, "budi", "Budi", Role.Lecturer)

  def spec = suite("Rbac")(
    test("Student hanya boleh melihat grade") {
      assertTrue(Rbac.permissions(Role.Student) == Set(Permission.ViewGrade))
    },
    test("Lecturer boleh mengubah grade, tetapi tidak boleh mengelola user") {
      assertTrue(
        Rbac.permissions(Role.Lecturer).contains(Permission.EditGrade),
        !Rbac.permissions(Role.Lecturer).contains(Permission.ManageUsers)
      )
    },
    test("ownership rule") {
      assertTrue(Rbac.canViewGradesOf(cici, 3), !Rbac.canViewGradesOf(cici, 4), Rbac.canViewGradesOf(budi, 4))
    }
  )
