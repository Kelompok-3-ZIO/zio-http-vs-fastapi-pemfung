"""Membuat token JWT palsu untuk skrip serangan, hanya dengan pustaka standar Python.

Pakai: python3 jwt_palsu.py <jenis> [secret]
jenis: secret-lain | tanpa-exp | kedaluwarsa | alg-none
"""

import base64
import hashlib
import hmac
import json
import sys


def b64(data: bytes) -> str:
    return base64.urlsafe_b64encode(data).rstrip(b"=").decode()


def token(header: dict, payload: dict, secret: str | None) -> str:
    signing_input = f"{b64(json.dumps(header).encode())}.{b64(json.dumps(payload).encode())}"
    if secret is None:
        return signing_input + "."
    signature = hmac.new(secret.encode(), signing_input.encode(), hashlib.sha256).digest()
    return f"{signing_input}.{b64(signature)}"


jenis = sys.argv[1]
secret = sys.argv[2] if len(sys.argv) > 2 else ""
hs256 = {"alg": "HS256", "typ": "JWT"}
far_future = 4102444800  # 2100-01-01

if jenis == "secret-lain":
    print(token(hs256, {"sub": "1", "exp": far_future}, "secret-lain-yang-panjangnya-minimal-32-byte"))
elif jenis == "tanpa-exp":
    print(token(hs256, {"sub": "1"}, secret))
elif jenis == "kedaluwarsa":
    print(token(hs256, {"sub": "1", "exp": 1}, secret))
elif jenis == "alg-none":
    print(token({"alg": "none", "typ": "JWT"}, {"sub": "1", "exp": far_future}, None))
else:
    raise SystemExit(f"jenis tidak dikenal: {jenis}")
