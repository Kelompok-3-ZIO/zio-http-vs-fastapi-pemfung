from app.domain import Permission, Role, User, can_view_grades_of, permissions


# Aturan akses adalah fungsi biasa, jadi bisa dites tanpa server dan tanpa database.
def test_student_can_only_view_grades() -> None:
    assert permissions(Role.STUDENT) == {Permission.VIEW_GRADE}


def test_lecturer_can_edit_but_not_manage_users() -> None:
    assert Permission.EDIT_GRADE in permissions(Role.LECTURER)
    assert Permission.MANAGE_USERS not in permissions(Role.LECTURER)


def test_every_role_has_permissions() -> None:
    for role in Role:
        assert permissions(role)


def test_ownership_rule() -> None:
    cici = User(id=3, username="cici", name="Cici", role=Role.STUDENT)
    budi = User(id=2, username="budi", name="Budi", role=Role.LECTURER)
    assert can_view_grades_of(cici, 3)
    assert not can_view_grades_of(cici, 4)
    assert can_view_grades_of(budi, 4)
