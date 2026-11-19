from functools import lru_cache
from typing import Annotated

from fastapi import Depends
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    """Konfigurasi dibaca dari environment variable (atau berkas .env)."""

    model_config = SettingsConfigDict(env_file=".env")

    jwt_secret: str
    database_path: str = "../db/siak.db"
    access_token_expire_minutes: int = 30


@lru_cache
def get_settings() -> Settings:
    return Settings()


SettingsDep = Annotated[Settings, Depends(get_settings)]
