from collections.abc import Iterator
from functools import lru_cache
from typing import Annotated

from fastapi import Depends
from sqlalchemy import Engine
from sqlmodel import Field, Session, SQLModel, create_engine

from app.config import get_settings


class UserRow(SQLModel, table=True):
    __tablename__ = "users"  # type: ignore[assignment]
    id: int = Field(primary_key=True)
    username: str
    name: str
    role: str
    password_hash: str


class CourseRow(SQLModel, table=True):
    __tablename__ = "courses"  # type: ignore[assignment]
    code: str = Field(primary_key=True)
    name: str
    grading_open: bool


class GradeRow(SQLModel, table=True):
    __tablename__ = "grades"  # type: ignore[assignment]
    student_id: int = Field(primary_key=True)
    course_code: str = Field(primary_key=True)
    letter: str


@lru_cache
def get_engine() -> Engine:
    path = get_settings().database_path
    return create_engine(f"sqlite:///{path}", connect_args={"check_same_thread": False})


def get_session() -> Iterator[Session]:
    with Session(get_engine()) as session:
        yield session


SessionDep = Annotated[Session, Depends(get_session)]
