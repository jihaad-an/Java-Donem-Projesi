-- SQLite Veritabanı Şeması
-- Dosya: app.db

PRAGMA foreign_keys = ON;

-- Kullanıcılar tablosu
CREATE TABLE IF NOT EXISTS users (
  id TEXT PRIMARY KEY,
  username TEXT UNIQUE NOT NULL,
  password TEXT NOT NULL,
  name TEXT,
  surname TEXT,
  tcNo TEXT,
  role TEXT NOT NULL,
  phone TEXT,
  email TEXT
);

-- Username için unique index
CREATE UNIQUE INDEX IF NOT EXISTS idx_users_username ON users(username);

-- Email için unique index
CREATE UNIQUE INDEX IF NOT EXISTS idx_users_email ON users(email);

-- Doktorlar tablosu
CREATE TABLE IF NOT EXISTS doctors (
  user_id TEXT PRIMARY KEY,
  branch TEXT,
  startHour INTEGER,
  endHour INTEGER,
  FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Hastalar tablosu
CREATE TABLE IF NOT EXISTS patients (
  user_id TEXT PRIMARY KEY,
  FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Randevular tablosu
CREATE TABLE IF NOT EXISTS appointments (
  id TEXT PRIMARY KEY,
  patient_id TEXT NOT NULL,
  doctor_id TEXT NOT NULL,
  date TEXT NOT NULL,
  time TEXT,
  state TEXT DEFAULT 'SCHEDULED',
  note TEXT,
  FOREIGN KEY(patient_id) REFERENCES users(id) ON DELETE CASCADE,
  FOREIGN KEY(doctor_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Randevu tarihine göre index (sorgu performansı için)
CREATE INDEX IF NOT EXISTS idx_appointments_date ON appointments(date);

-- Randevu durumuna göre index
CREATE INDEX IF NOT EXISTS idx_appointments_state ON appointments(state);

-- Randevu hasta ID'sine göre index
CREATE INDEX IF NOT EXISTS idx_appointments_patient_id ON appointments(patient_id);

-- Randevu doktor ID'sine göre index
CREATE INDEX IF NOT EXISTS idx_appointments_doctor_id ON appointments(doctor_id);

