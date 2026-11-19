import logging
from collections.abc import Callable, Coroutine
from typing import Any

from fastapi import HTTPException, Request, Response
from fastapi.routing import APIRoute

logger = logging.getLogger("siak.audit")


def _target(request: Request) -> str:
    # Path mentah yang masih ter-encode, supaya teks dari penyerang (spasi, baris baru)
    # tidak bisa menyamar sebagai isi log.
    return f"{request.method} {request.scope['raw_path'].decode('ascii', 'backslashreplace')}"


# Audit log hanya untuk router yang memasang route_class ini.
class AuditedRoute(APIRoute):
    def get_route_handler(self) -> Callable[[Request], Coroutine[Any, Any, Response]]:
        original = super().get_route_handler()

        async def handler(request: Request) -> Response:
            try:
                response = await original(request)
            except HTTPException as exc:
                logger.info("audit %s -> %s", _target(request), exc.status_code)
                raise
            except Exception as exc:
                # Exception domain baru diubah menjadi status code oleh exception_handler,
                # setelah keluar dari sini, jadi yang bisa dicatat hanya nama exception-nya.
                logger.info("audit %s -> %s", _target(request), type(exc).__name__)
                raise
            logger.info("audit %s -> %s", _target(request), response.status_code)
            return response

        return handler
