-- Data contoh. Password tiap user: rahasia-<username>, misalnya rahasia-cici.
-- Hash dibuat dengan Argon2id (pwdlib, parameter rekomendasi) dan bisa diverifikasi kedua versi.
INSERT INTO users (id, username, name, role, password_hash) VALUES
  (1, 'ani', 'Ani', 'Admin', '$argon2id$v=19$m=65536,t=3,p=4$PAaEZWDmrCe3c8lnCJe67A$RwOJWBsGJTIeV6awhoqrGKjEvh5bMITebVYX/ay/Yck'),
  (2, 'budi', 'Budi', 'Lecturer', '$argon2id$v=19$m=65536,t=3,p=4$gwknUjey9PR83GT/FLZukA$MjG+WuyQrmk8ljGjNZGTNERNQOXSbZbxpww42ArJBmQ'),
  (3, 'cici', 'Cici', 'Student', '$argon2id$v=19$m=65536,t=3,p=4$LM0y9kjuTbb4hjoiDGVDIQ$3SmyUSlotL1sIQkCCzGv7yLWEoZ1UbToGcMT6Gj3h/k'),
  (4, 'dedi', 'Dedi', 'Student', '$argon2id$v=19$m=65536,t=3,p=4$obVfyCyQAOI6PiPfccGTEA$SM+SCbLEHp/2FQpMz2dGwagcQFxkvxqLbd/sHwovHPQ');

INSERT INTO courses (code, name, grading_open) VALUES
  ('PEMFUNG', 'Pemrograman Fungsional', 1),
  ('KALKULUS', 'Kalkulus 1', 0);

INSERT INTO grades (student_id, course_code, letter) VALUES
  (3, 'PEMFUNG', 'B'),
  (3, 'KALKULUS', 'A'),
  (4, 'PEMFUNG', 'A');
