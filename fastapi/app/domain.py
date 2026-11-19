from enum import StrEnum

from pydantic import BaseModel


class Role(StrEnum):
    ADMIN = "Admin"
    LECTURER = "Lecturer"
    STUDENT = "Student"


class Permission(StrEnum):
    VIEW_GRADE = "ViewGrade"
    EDIT_GRADE = "EditGrade"
    MANAGE_USERS = "ManageUsers"


class User(BaseModel):
    id: int
    username: str
    name: str
    role: Role


class Grade(BaseModel):
    course: str
    letter: str


# Role permissions: seluruh aturan role ke permission ada di sini.
ROLE_PERMISSIONS: dict[Role, frozenset[Permission]] = {
    Role.ADMIN: frozenset({Permission.VIEW_GRADE, Permission.EDIT_GRADE, Permission.MANAGE_USERS}),
    Role.LECTURER: frozenset({Permission.VIEW_GRADE, Permission.EDIT_GRADE}),
    Role.STUDENT: frozenset({Permission.VIEW_GRADE}),
}


def permissions(role: Role) -> frozenset[Permission]:
    return ROLE_PERMISSIONS[role]


# Ownership rule: Student hanya boleh melihat grade miliknya sendiri.
def can_view_grades_of(user: User, student_id: int) -> bool:
    return user.role != Role.STUDENT or user.id == student_id
