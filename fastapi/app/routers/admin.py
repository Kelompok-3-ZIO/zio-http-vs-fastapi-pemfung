from fastapi import APIRouter, Depends

from app.domain import Permission, User
from app.repository import UserRepoDep
from app.security import requires

router = APIRouter(dependencies=[Depends(requires(Permission.MANAGE_USERS))])


@router.get("/admin/users")
def list_users(users: UserRepoDep) -> list[User]:
    return users.all()
