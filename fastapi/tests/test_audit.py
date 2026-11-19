import logging

import pytest
from fastapi.testclient import TestClient

from tests.conftest import bearer


def test_audit_log_keeps_path_encoded(client: TestClient, caplog: pytest.LogCaptureFixture) -> None:
    headers = bearer(client, "cici")
    with caplog.at_level(logging.INFO, logger="siak.audit"):
        client.put("/students/3%0Aaudit%20PUT%20palsu/grades", headers=headers, json={"course": "PEMFUNG", "letter": "A"})
    lines = [r.getMessage() for r in caplog.records if r.name == "siak.audit"]
    assert lines == ["audit PUT /students/3%0Aaudit%20PUT%20palsu/grades -> 403"]
