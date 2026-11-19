# SIAK: ZIO HTTP vs FastAPI

Materi subtopik 3 kelompok 3 Pemrograman Fungsional. SIAK mini (sistem informasi akademik) yang sama ditulis dua kali, dengan **ZIO HTTP** (Scala) dan dengan **FastAPI** (Python), lalu dibandingkan satu lawan satu: autentikasi, otorisasi, middleware, error handling, dan perubahan sistem.

Kedua versi ditulis dengan praktik yang dipakai di industri: login OAuth2 *password flow* yang mengeluarkan JWT, password di-hash dengan Argon2id, konfigurasi dari environment variable, database SQLite, dan test otomatis. Istilah yang dipakai ada di [GLOSSARY.md](GLOSSARY.md).

Lanjutan dari subtopik 2: [zio-http-rbac-pemfung](https://github.com/Kelompok-3-ZIO/zio-http-rbac-pemfung).

## Isi repo

| Folder | Isi |
|---|---|
| `db/` | `schema.sql` dan `seed.sql` yang dipakai bersama, serta `init.sh` untuk membuat `siak.db` |
| `zio/` | Versi ZIO HTTP (Scala 3, scala-cli) |
| `fastapi/` | Versi FastAPI (Python, uv) |
| `demo/` | `serangan.sh`, skrip yang mengirim request serangan yang sama ke server mana pun |

## Menjalankan

Yang dibutuhkan: [scala-cli](https://scala-cli.virtuslab.org/), [uv](https://docs.astral.sh/uv/), `sqlite3`, dan `openssl`.

```bash
db/init.sh                                  # membuat db/siak.db dari schema.sql dan seed.sql
export JWT_SECRET=$(openssl rand -hex 32)   # kedua server memakai secret yang sama untuk demo

cd zio && scala-cli run .                   # terminal 1: ZIO HTTP di http://localhost:8080
cd fastapi && uv run fastapi dev            # terminal 2: FastAPI di http://localhost:8000

demo/serangan.sh http://localhost:8080      # terminal 3
demo/serangan.sh http://localhost:8000
```

User contoh: `ani` (Admin), `budi` (Lecturer), `cici` dan `dedi` (Student). Password-nya `rahasia-<username>`, misalnya `rahasia-cici`.

```bash
curl -X POST localhost:8080/login -d "username=cici&password=rahasia-cici"
curl -H "Authorization: Bearer <access_token>" localhost:8080/students/3/grades
```

## Endpoint

| Method dan path | Permission | Catatan |
|---|---|---|
| `POST /login` | publik | Form `username` dan `password`, dijawab `{"access_token": ..., "token_type": "bearer"}` |
| `GET /students/{id}/grades` | ViewGrade | Student hanya boleh melihat grade miliknya sendiri (ownership rule) |
| `PUT /students/{id}/grades` | EditGrade | Body `{"course": "PEMFUNG", "letter": "A"}`, dicatat di audit log |
| `GET /admin/users` | ManageUsers | |

## Test

```bash
cd zio && scala-cli test .
cd fastapi && uv run pytest
```

GitHub Actions menjalankan keduanya setiap kali ada push.

## Pilihan library

Setiap sisi memakai library yang dianjurkan dokumentasi resminya, atau yang paling umum di ekosistemnya.

| Kebutuhan | ZIO HTTP | FastAPI |
|---|---|---|
| JWT | jwt-scala (dipakai di contoh resmi zio-http) | PyJWT (tutorial resmi FastAPI) |
| Hash password Argon2id | Password4j | pwdlib (tutorial resmi FastAPI) |
| Database | ZIO Quill (library resmi ZIO) + sqlite-jdbc | SQLModel (tutorial resmi FastAPI) |
| Konfigurasi | `zio.Config` bawaan ZIO | pydantic-settings |

## Keputusan desain

- **Token hanya berisi id user (`sub`), role dibaca ulang dari database di setiap request.** Jika role seseorang diubah, perubahan itu langsung berlaku tanpa menunggu token kedaluwarsa.
- **Endpoint menyebut permission, bukan role.** Seluruh aturan role ke permission ada di satu tempat (`Rbac.permissions` dan `ROLE_PERMISSIONS`).
- **Skema dan data contoh dipakai bersama.** Kedua versi membaca `db/schema.sql` dan `db/seed.sql` yang sama, sehingga datanya identik. Hash Argon2id di `seed.sql` dibuat dengan pwdlib dan diverifikasi oleh Password4j di sisi Scala.
- **ZIO di-compile dengan `-Werror`**, sehingga `match` yang tidak lengkap (misalnya role baru yang belum diberi permission) menghentikan compile.

## Yang disederhanakan dari sistem produksi

- Tidak ada *refresh token*. Access token berlaku 30 menit.
- Skema dibuat dari `schema.sql`. Di produksi, perubahan skema dikelola dengan tool migrasi (Flyway di JVM, Alembic di Python).
- JWT ditandatangani dengan HS256 dan satu secret. Jika token diperiksa oleh beberapa layanan berbeda, RS256 atau ES256 dengan pasangan kunci lebih tepat.
- SQLite cukup untuk demo. Di produksi biasanya dipakai PostgreSQL.
