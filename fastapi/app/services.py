from app.domain import Grade
from app.errors import CourseNotFound, GradingPeriodClosed, StudentNotFound
from app.repository import GradeRepository


def edit_grade(grades: GradeRepository, student_id: int, grade: Grade) -> Grade:
    if not grades.student_exists(student_id):
        raise StudentNotFound(student_id)
    is_open = grades.grading_open(grade.course)
    if is_open is None:
        raise CourseNotFound(grade.course)
    if not is_open:
        raise GradingPeriodClosed(grade.course)
    grades.save(student_id, grade)
    return grade
