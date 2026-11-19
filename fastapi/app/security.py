from collections.abc import Callable
from datetime import UTC, datetime, timedelta
from typing import Annotated

import jwt
from fastapi import Depends, HTTPException, status
from fastapi.security import OAuth2PasswordBearer
from jwt.exceptions import InvalidTokenError
from pwdlib import PasswordHash

from app.config import Settings, SettingsDep
from app.domain import Permission, User, permissions
from app.repository import UserRepoDep

ALGORITHM = "HS256"

password_hash = PasswordHash.recommended()

# Dipakai saat username tidak ada, supaya waktu respons sama (mencegah timing attack).
DUMMY_HASH = password_hash.hash("dummypassword")

oauth2_scheme = OAuth2PasswordBearer(tokenUrl="login")


def authenticate_user(users: UserRepoDep, username: str, password: str) -> User | None:
    found = users.find_with_hash(username)
    if found is None:
        password_hash.verify(password, DUMMY_HASH)
        return None
    user, hashed = found
    return user if password_hash.verify(password, hashed) else None


def create_access_token(user_id: int, settings: Settings) -> str:
    expire = datetime.now(UTC) + timedelta(minutes=settings.access_token_expire_minutes)
    return jwt.encode({"sub": str(user_id), "exp": expire}, settings.jwt_secret, algorithm=ALGORITHM)


# Authentication: token -> User. Role dibaca ulang dari database di setiap request.
def get_current_user(
    token: Annotated[str, Depends(oauth2_scheme)],
    settings: SettingsDep,
    users: UserRepoDep,
) -> User:
    credentials_exception = HTTPException(
        status_code=status.HTTP_401_UNAUTHORIZED,
        detail="Could not validate credentials",
        headers={"WWW-Authenticate": "Bearer"},
    )
    try:
        payload = jwt.decode(token, settings.jwt_secret, algorithms=[ALGORITHM], options={"require": ["exp", "sub"]})
        user_id = int(payload["sub"])
    except (InvalidTokenError, KeyError, ValueError):
        raise credentials_exception
    user = users.find_by_id(user_id)
    if user is None:
        raise credentials_exception
    return user


CurrentUser = Annotated[User, Depends(get_current_user)]


# Authorization: endpoint menyebut permission yang dibutuhkan, bukan role.
def requires(permission: Permission) -> Callable[[User], None]:
    def check(user: CurrentUser) -> None:
        if permission not in permissions(user.role):
            raise HTTPException(
                status_code=status.HTTP_403_FORBIDDEN,
                detail=f"{user.role} lacks permission {permission}",
            )

    return check
