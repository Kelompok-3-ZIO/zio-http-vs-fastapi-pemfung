package siak

import zio.*

object GradeService:

  // Tipe hasilnya menyebut semua expected error yang mungkin: GradeError.
  def edit(studentId: Int, grade: Grade): ZIO[GradeRepository, GradeError, Grade] =
    for
      grades <- ZIO.service[GradeRepository]
      exists <- grades.studentExists(studentId)
      _      <- ZIO.unless(exists)(ZIO.fail(GradeError.StudentNotFound(studentId)))
      isOpen <- grades.gradingOpen(grade.course).someOrFail(GradeError.CourseNotFound(grade.course))
      _      <- ZIO.unless(isOpen)(ZIO.fail(GradeError.GradingPeriodClosed(grade.course)))
      _      <- grades.save(studentId, grade)
    yield grade
