#!/usr/bin/env bash
# Membuat (ulang) database SQLite dari schema.sql dan seed.sql.
# Pakai: db/init.sh [path], default db/siak.db
set -euo pipefail
DIR="$(cd "$(dirname "$0")" && pwd)"
DB="${1:-$DIR/siak.db}"
rm -f "$DB"
sqlite3 "$DB" < "$DIR/schema.sql"
sqlite3 "$DB" < "$DIR/seed.sql"
echo "Database dibuat: $DB"
