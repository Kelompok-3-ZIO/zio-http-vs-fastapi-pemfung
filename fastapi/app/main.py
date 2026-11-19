import logging

from fastapi import FastAPI

from app.errors import register_error_handlers
from app.routers import admin, auth, grades

logging.basicConfig(level=logging.INFO, format="%(message)s")

app = FastAPI(title="SIAK (FastAPI)")
app.include_router(auth.router)
app.include_router(grades.view_router)
app.include_router(grades.edit_router)
app.include_router(admin.router)
register_error_handlers(app)
