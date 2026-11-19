from fastapi.testclient import TestClient

from app.domain import Grade
from app.repository import get_grade_repository
from tests.conftest import bearer


class InMemoryGradeRepository:
    """Pengganti database untuk test: semua periode ditutup."""

    def __init__(self) -> None:
        self.saved: list[tuple[int, Grade]] = []

    def student_exists(self, student_id: int) -> bool:
        return True

    def grading_open(self, course_code: str) -> bool | None:
        return False

    def grades_of(self, student_id: int) -> list[Grade]:
        return []

    def save(self, student_id: int, grade: Grade) -> None:
        self.saved.append((student_id, grade))


# Mengganti repository lewat dict dependency_overrides. Kuncinya fungsi, nilainya fungsi pengganti;
# isi dict ini tidak diperiksa terhadap tipe GradeRepository.
def test_closed_period_with_fake_repository(client: TestClient) -> None:
    fake = InMemoryGradeRepository()
    client.app.dependency_overrides[get_grade_repository] = lambda: fake  # type: ignore[attr-defined]
    response = client.put("/students/3/grades", headers=bearer(client, "budi"), json={"course": "PEMFUNG", "letter": "A"})
    assert response.status_code == 409
    assert fake.saved == []
