from typing import Annotated, Protocol

from fastapi import Depends
from sqlmodel import Session, select

from app.db import CourseRow, GradeRow, SessionDep, UserRow
from app.domain import Grade, Role, User


class UserRepository(Protocol):
    def find_by_id(self, user_id: int) -> User | None: ...
    def find_with_hash(self, username: str) -> tuple[User, str] | None: ...
    def all(self) -> list[User]: ...


class GradeRepository(Protocol):
    def student_exists(self, student_id: int) -> bool: ...
    def grading_open(self, course_code: str) -> bool | None: ...
    def grades_of(self, student_id: int) -> list[Grade]: ...
    def save(self, student_id: int, grade: Grade) -> None: ...


def _to_user(row: UserRow) -> User:
    return User(id=row.id, username=row.username, name=row.name, role=Role(row.role))


class SqlUserRepository:
    def __init__(self, session: Session) -> None:
        self.session = session

    def find_by_id(self, user_id: int) -> User | None:
        row = self.session.get(UserRow, user_id)
        return _to_user(row) if row else None

    def find_with_hash(self, username: str) -> tuple[User, str] | None:
        row = self.session.exec(select(UserRow).where(UserRow.username == username)).first()
        return (_to_user(row), row.password_hash) if row else None

    def all(self) -> list[User]:
        return [_to_user(row) for row in self.session.exec(select(UserRow).order_by(UserRow.id))]


class SqlGradeRepository:
    def __init__(self, session: Session) -> None:
        self.session = session

    def student_exists(self, student_id: int) -> bool:
        row = self.session.get(UserRow, student_id)
        return row is not None and row.role == Role.STUDENT

    def grading_open(self, course_code: str) -> bool | None:
        row = self.session.get(CourseRow, course_code)
        return row.grading_open if row else None

    def grades_of(self, student_id: int) -> list[Grade]:
        rows = self.session.exec(
            select(GradeRow).where(GradeRow.student_id == student_id).order_by(GradeRow.course_code)
        )
        return [Grade(course=row.course_code, letter=row.letter) for row in rows]

    def save(self, student_id: int, grade: Grade) -> None:
        self.session.merge(GradeRow(student_id=student_id, course_code=grade.course, letter=grade.letter))
        self.session.commit()


def get_user_repository(session: SessionDep) -> UserRepository:
    return SqlUserRepository(session)


def get_grade_repository(session: SessionDep) -> GradeRepository:
    return SqlGradeRepository(session)


UserRepoDep = Annotated[UserRepository, Depends(get_user_repository)]
GradeRepoDep = Annotated[GradeRepository, Depends(get_grade_repository)]
