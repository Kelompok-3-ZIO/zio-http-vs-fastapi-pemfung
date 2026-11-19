from fastapi import APIRouter, Depends, HTTPException, status

from app.audit import AuditedRoute
from app.domain import Grade, Permission, can_view_grades_of
from app.repository import GradeRepoDep
from app.security import CurrentUser, requires
from app.services import edit_grade

view_router = APIRouter(dependencies=[Depends(requires(Permission.VIEW_GRADE))])
edit_router = APIRouter(dependencies=[Depends(requires(Permission.EDIT_GRADE))], route_class=AuditedRoute)


@view_router.get("/students/{student_id}/grades")
def view_grades(student_id: int, user: CurrentUser, grades: GradeRepoDep) -> list[Grade]:
    if not can_view_grades_of(user, student_id):
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Students may only view their own grades")
    return grades.grades_of(student_id)


@edit_router.put("/students/{student_id}/grades")
def update_grade(student_id: int, grade: Grade, grades: GradeRepoDep) -> Grade:
    return edit_grade(grades, student_id, grade)
