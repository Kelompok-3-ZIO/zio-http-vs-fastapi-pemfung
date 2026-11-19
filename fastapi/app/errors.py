from fastapi import FastAPI, Request, status
from fastapi.responses import JSONResponse


# Expected error saat mengubah grade, satu kelas per alasan gagal.
class GradeError(Exception):
    pass


class StudentNotFound(GradeError):
    def __init__(self, student_id: int) -> None:
        self.student_id = student_id


class CourseNotFound(GradeError):
    def __init__(self, course: str) -> None:
        self.course = course


class GradingPeriodClosed(GradeError):
    def __init__(self, course: str) -> None:
        self.course = course


def _json(status_code: int, detail: str) -> JSONResponse:
    return JSONResponse(status_code=status_code, content={"detail": detail})


def register_error_handlers(app: FastAPI) -> None:
    @app.exception_handler(StudentNotFound)
    async def student_not_found(request: Request, exc: StudentNotFound) -> JSONResponse:
        return _json(status.HTTP_404_NOT_FOUND, f"Student {exc.student_id} not found")

    @app.exception_handler(CourseNotFound)
    async def course_not_found(request: Request, exc: CourseNotFound) -> JSONResponse:
        return _json(status.HTTP_404_NOT_FOUND, f"Course {exc.course} not found")

    @app.exception_handler(GradingPeriodClosed)
    async def grading_period_closed(request: Request, exc: GradingPeriodClosed) -> JSONResponse:
        return _json(status.HTTP_409_CONFLICT, f"Grading period for {exc.course} is closed")
