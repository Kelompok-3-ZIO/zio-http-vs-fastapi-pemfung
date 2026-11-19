import sqlite3
from collections.abc import Iterator
from pathlib import Path

import pytest
from fastapi.testclient import TestClient

from app.config import get_settings
from app.db import get_engine
from app.main import app

DB_DIR = Path(__file__).resolve().parents[2] / "db"


@pytest.fixture
def client(tmp_path: Path, monkeypatch: pytest.MonkeyPatch) -> Iterator[TestClient]:
    """Server lengkap dengan database SQLite baru dari schema.sql dan seed.sql."""
    db = tmp_path / "siak.db"
    with sqlite3.connect(db) as con:
        con.executescript((DB_DIR / "schema.sql").read_text())
        con.executescript((DB_DIR / "seed.sql").read_text())
    monkeypatch.setenv("JWT_SECRET", "test-secret-yang-panjangnya-minimal-32-byte")
    monkeypatch.setenv("DATABASE_PATH", str(db))
    get_settings.cache_clear()
    get_engine.cache_clear()
    with TestClient(app) as c:
        yield c
    app.dependency_overrides.clear()
    get_settings.cache_clear()
    get_engine.cache_clear()


def bearer(client: TestClient, username: str) -> dict[str, str]:
    response = client.post("/login", data={"username": username, "password": f"rahasia-{username}"})
    assert response.status_code == 200
    return {"Authorization": f"Bearer {response.json()['access_token']}"}
