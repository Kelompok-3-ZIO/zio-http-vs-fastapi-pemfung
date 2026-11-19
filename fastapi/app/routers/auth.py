from typing import Annotated

from fastapi import APIRouter, Depends, HTTPException, status
from fastapi.security import OAuth2PasswordRequestForm
from pydantic import BaseModel

from app.config import SettingsDep
from app.repository import UserRepoDep
from app.security import authenticate_user, create_access_token

router = APIRouter()


class Token(BaseModel):
    access_token: str
    token_type: str


@router.post("/login")
def login(
    form: Annotated[OAuth2PasswordRequestForm, Depends()],
    users: UserRepoDep,
    settings: SettingsDep,
) -> Token:
    user = authenticate_user(users, form.username, form.password)
    if user is None:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Incorrect username or password",
            headers={"WWW-Authenticate": "Bearer"},
        )
    return Token(access_token=create_access_token(user.id, settings), token_type="bearer")
