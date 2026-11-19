#!/usr/bin/env bash
# Mengirim request yang sama ke server mana pun (ZIO di :8080, FastAPI di :8000).
# Pakai: JWT_SECRET=... demo/serangan.sh http://localhost:8080
set -uo pipefail
BASE="${1:?pakai: demo/serangan.sh <base-url>}"
SECRET="${JWT_SECRET:?JWT_SECRET harus sama dengan milik server}"
DIR="$(cd "$(dirname "$0")" && pwd)"
GRADE='{"course":"PEMFUNG","letter":"A"}'
LULUS=0; GAGAL=0

token() {
  curl -s -X POST "$BASE/login" -d "username=$1&password=rahasia-$1" \
    | python3 -c "import sys, json; print(json.load(sys.stdin)['access_token'])"
}

cek() {
  local judul="$1" harap="$2"; shift 2
  local dapat
  dapat=$(curl -s -o /dev/null -w "%{http_code}" "$@")
  if [ "$dapat" = "$harap" ]; then LULUS=$((LULUS + 1)); hasil="ok"; else GAGAL=$((GAGAL + 1)); hasil="GAGAL"; fi
  printf "%-5s %-58s harap %s, dapat %s\n" "$hasil" "$judul" "$harap" "$dapat"
}

CICI=$(token cici); BUDI=$(token budi)

echo "== Authentication"
cek "login dengan password salah"                  401 -X POST "$BASE/login" -d "username=cici&password=salah"
cek "tanpa token"                                  401 "$BASE/students/3/grades"
cek "token ditandatangani secret lain"             401 -H "Authorization: Bearer $(python3 "$DIR/jwt_palsu.py" secret-lain)" "$BASE/admin/users"
cek "token tanpa exp"                              401 -H "Authorization: Bearer $(python3 "$DIR/jwt_palsu.py" tanpa-exp "$SECRET")" "$BASE/admin/users"
cek "token kedaluwarsa"                            401 -H "Authorization: Bearer $(python3 "$DIR/jwt_palsu.py" kedaluwarsa "$SECRET")" "$BASE/admin/users"
cek "token alg none (tanpa tanda tangan)"          401 -H "Authorization: Bearer $(python3 "$DIR/jwt_palsu.py" alg-none)" "$BASE/admin/users"

echo "== Vertical privilege escalation (OWASP: BFLA)"
cek "Student mengubah grade"                       403 -X PUT -H "Authorization: Bearer $CICI" -H "Content-Type: application/json" -d "$GRADE" "$BASE/students/3/grades"
cek "Student membuka daftar user"                  403 -H "Authorization: Bearer $CICI" "$BASE/admin/users"
cek "Lecturer membuka daftar user"                 403 -H "Authorization: Bearer $BUDI" "$BASE/admin/users"

echo "== Horizontal privilege escalation (OWASP: BOLA)"
cek "Student melihat grade Student lain"           403 -H "Authorization: Bearer $CICI" "$BASE/students/4/grades"

echo "== Request yang sah"
cek "Student melihat grade sendiri"                200 -H "Authorization: Bearer $CICI" "$BASE/students/3/grades"
cek "Lecturer mengubah grade"                      200 -X PUT -H "Authorization: Bearer $BUDI" -H "Content-Type: application/json" -d "$GRADE" "$BASE/students/3/grades"

echo
echo "Lulus $LULUS, gagal $GAGAL"
[ "$GAGAL" -eq 0 ]
