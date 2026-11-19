# SIAK: ZIO HTTP vs FastAPI

SIAK mini (sistem informasi akademik) yang ditulis dua kali, dengan ZIO HTTP dan dengan FastAPI, untuk membandingkan cara keduanya menangani autentikasi, otorisasi, dan error. Istilah di sini dipakai di kode, slide, dan diskusi kelompok 3 subtopik 3.

## Pengguna dan akses

**User**:
Orang yang memakai SIAK. Setiap user punya tepat satu role.
_Avoid_: pengguna, akun, account

**Role**:
Peran user di SIAK, yaitu Admin, Lecturer, atau Student. Endpoint tidak pernah memeriksa role secara langsung.
_Avoid_: Dosen, Mahasiswa (nama lama subtopik 2), Teacher, level

**Permission**:
Tindakan yang bisa diminta ke sistem, yaitu ViewGrade, EditGrade, atau ManageUsers. Setiap endpoint menyatakan permission yang dibutuhkannya.
_Avoid_: Aksi (nama lama subtopik 2), action, privilege, hak akses

**Role permissions**:
Pemetaan dari setiap role ke kumpulan permission yang diperbolehkan. Permission yang tidak tercantum untuk sebuah role berarti ditolak.
_Avoid_: Izin (nama lama subtopik 2), ACL, matriks akses

**Ownership rule**:
Aturan akses yang bergantung pada pemilik data yang diminta, bukan hanya pada role. Contohnya, Student punya permission ViewGrade tetapi hanya untuk grade miliknya sendiri.
_Avoid_: Kepemilikan, object-level authorization

**Authentication**:
Menentukan siapa user yang mengirim request, dari access token yang dibawanya. Gagal authentication berarti 401.
_Avoid_: autentikasi, login check

**Authorization**:
Menentukan apakah user yang sudah terautentikasi boleh melakukan permission yang diminta, berdasarkan role permissions dan ownership rule. Gagal authorization berarti 403.
_Avoid_: otorisasi, access control

**Access token**:
Bukti login berumur pendek yang dikeluarkan setelah username dan password cocok, lalu dikirim di setiap request. Isinya hanya identitas user, sedangkan role selalu dibaca ulang.
_Avoid_: session, token (tanpa keterangan)

## Pelanggaran akses

**Vertical privilege escalation**:
User menjalankan permission milik role yang lebih tinggi, misalnya Student mengubah grade. OWASP menyebutnya BFLA.
_Avoid_: BFLA (kecuali dalam kurung sebagai rujukan), privilege escalation (tanpa keterangan arah)

**Horizontal privilege escalation**:
User membuka data milik user lain dengan role yang sama, misalnya Student membuka grade Student lain. Ini pelanggaran ownership rule. OWASP menyebutnya BOLA.
_Avoid_: BOLA, IDOR (kecuali dalam kurung sebagai rujukan)

## Nilai akademik

**Grade**:
Huruf mutu satu course milik seorang Student.
_Avoid_: Nilai (nama lama subtopik 2), score, mark

**Course**:
Mata kuliah yang menjadi pasangan sebuah grade.
_Avoid_: mataKuliah, subject, class

**Grading period**:
Rentang waktu ketika grade sebuah course masih boleh diubah. Di luar rentang itu, perubahan grade ditolak.
_Avoid_: periode input nilai, deadline
