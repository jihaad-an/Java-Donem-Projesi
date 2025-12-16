PRAGMA foreign_keys = ON;

CREATE TABLE IF NOT EXISTS users (
  id TEXT PRIMARY KEY,
  username TEXT,
  password TEXT,
  name TEXT,
  surname TEXT,
  tcNo TEXT,
  role TEXT,
  phone TEXT,
  email TEXT
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_users_username ON users(username);
CREATE UNIQUE INDEX IF NOT EXISTS idx_users_email ON users(email);

CREATE TABLE IF NOT EXISTS doctors (
  user_id TEXT PRIMARY KEY,
  branch TEXT,
  startHour INTEGER,
  endHour INTEGER,
  FOREIGN KEY(user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS patients (
  user_id TEXT PRIMARY KEY,
  FOREIGN KEY(user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS appointments (
  id TEXT PRIMARY KEY,
  patient_id TEXT,
  doctor_id TEXT,
  date TEXT,
  time TEXT,
  state TEXT,
  note TEXT,
  cancelled_at TEXT,  -- EKLENEN SATIR BURASI
  FOREIGN KEY(patient_id) REFERENCES users(id),
  FOREIGN KEY(doctor_id) REFERENCES users(id)
);