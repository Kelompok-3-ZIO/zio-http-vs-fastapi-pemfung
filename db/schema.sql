-- Skema SIAK yang dipakai bersama oleh versi ZIO HTTP dan FastAPI.
CREATE TABLE users (
  id            INTEGER PRIMARY KEY,
  username      TEXT    NOT NULL UNIQUE,
  name          TEXT    NOT NULL,
  role          TEXT    NOT NULL CHECK (role IN ('Admin', 'Lecturer', 'Student')),
  password_hash TEXT    NOT NULL
);

CREATE TABLE courses (
  code         TEXT    PRIMARY KEY,
  name         TEXT    NOT NULL,
  grading_open INTEGER NOT NULL CHECK (grading_open IN (0, 1))
);

CREATE TABLE grades (
  student_id  INTEGER NOT NULL REFERENCES users (id),
  course_code TEXT    NOT NULL REFERENCES courses (code),
  letter      TEXT    NOT NULL,
  PRIMARY KEY (student_id, course_code)
);
