import jwt
import pytest
from fastapi.testclient import TestClient

from tests.conftest import bearer

GRADE = {"course": "PEMFUNG", "letter": "A"}


def test_login_wrong_password(client: TestClient) -> None:
    response = client.post("/login", data={"username": "cici", "password": "salah"})
    assert response.status_code == 401


def test_login_unknown_user(client: TestClient) -> None:
    response = client.post("/login", data={"username": "tidakada", "password": "x"})
    assert response.status_code == 401


def test_no_token(client: TestClient) -> None:
    assert client.get("/students/3/grades").status_code == 401


def test_token_without_exp(client: TestClient) -> None:
    no_exp = jwt.encode({"sub": "1"}, "test-secret-yang-panjangnya-minimal-32-byte", algorithm="HS256")
    response = client.get("/admin/users", headers={"Authorization": f"Bearer {no_exp}"})
    assert response.status_code == 401


def test_expired_token(client: TestClient) -> None:
    expired = jwt.encode({"sub": "1", "exp": 1}, "test-secret-yang-panjangnya-minimal-32-byte", algorithm="HS256")
    response = client.get("/admin/users", headers={"Authorization": f"Bearer {expired}"})
    assert response.status_code == 401


def test_token_signed_with_other_secret(client: TestClient) -> None:
    forged = jwt.encode({"sub": "1"}, "secret-lain-yang-panjangnya-minimal-32-byte", algorithm="HS256")
    response = client.get("/admin/users", headers={"Authorization": f"Bearer {forged}"})
    assert response.status_code == 401


@pytest.mark.parametrize(
    ("username", "method", "path", "expected"),
    [
        ("ani", "GET", "/students/3/grades", 200),
        ("budi", "GET", "/students/3/grades", 200),
        ("cici", "GET", "/students/3/grades", 200),
        ("cici", "GET", "/students/4/grades", 403),  # horizontal privilege escalation
        ("cici", "PUT", "/students/3/grades", 403),  # vertical privilege escalation
        ("cici", "GET", "/admin/users", 403),  # vertical privilege escalation
        ("budi", "GET", "/admin/users", 403),
        ("budi", "PUT", "/students/3/grades", 200),
        ("ani", "GET", "/admin/users", 200),
    ],
)
def test_access_matrix(client: TestClient, username: str, method: str, path: str, expected: int) -> None:
    body = GRADE if method == "PUT" else None
    response = client.request(method, path, headers=bearer(client, username), json=body)
    assert response.status_code == expected


@pytest.mark.parametrize(
    ("student_id", "grade", "expected"),
    [
        (3, {"course": "PEMFUNG", "letter": "A"}, 200),
        (99, {"course": "PEMFUNG", "letter": "A"}, 404),
        (3, {"course": "BASDAT", "letter": "A"}, 404),
        (3, {"course": "KALKULUS", "letter": "A"}, 409),
        (3, {"course": "PEMFUNG"}, 422),
    ],
)
def test_edit_grade_errors(client: TestClient, student_id: int, grade: dict[str, str], expected: int) -> None:
    response = client.put(f"/students/{student_id}/grades", headers=bearer(client, "budi"), json=grade)
    assert response.status_code == expected
