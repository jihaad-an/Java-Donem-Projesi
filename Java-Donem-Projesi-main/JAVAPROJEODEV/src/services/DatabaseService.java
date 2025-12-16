package services;

import models.Appointment;
import models.Doctor;
import models.Patient;
import models.AbstractUser;
import models.states.AppointmentState;
import models.states.ScheduledState;
import models.states.CancelledState;
import models.states.CompletedState;
import models.states.NoShowState;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DatabaseService {
    
    private static DatabaseService instance;
    private static final String DB_URL = "jdbc:sqlite:app.db";
    
    private Connection connection;
    
    private DatabaseService() {
        initializeDatabase();
    }
    
    public static DatabaseService getInstance() {
        if (instance == null) {
            instance = new DatabaseService();
        }
        return instance;
    }
    
    private void initializeDatabase() {
        try {
            connection = DriverManager.getConnection(DB_URL);
            connection.setAutoCommit(false);
            
            // Tabloları oluştur
            createTables();
            connection.commit();
            
            System.out.println("SQLite veritabanı bağlantısı başarılı!");
        } catch (SQLException e) {
            System.err.println("Veritabanı bağlantı hatası: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public void createTables() throws SQLException {
        // Önce mevcut appointments tablosunu kontrol et ve gerekirse yeniden oluştur
        try (Statement stmt = connection.createStatement()) {
            // Eski tabloyu kontrol et
            ResultSet rs = stmt.executeQuery("PRAGMA table_info(appointments)");
            boolean hasCancelledAt = false;
            while (rs.next()) {
                String columnName = rs.getString("name");
                if ("cancelled_at".equals(columnName)) {
                    hasCancelledAt = true;
                    break;
                }
            }
            
            // Eğer eski şema varsa, tabloyu yeniden oluştur
            if (hasCancelledAt) {
                System.out.println("Eski appointments tablosu bulundu, yeniden oluşturuluyor...");
                stmt.execute("DROP TABLE IF EXISTS appointments");
            }
        } catch (SQLException e) {
            // Tablo yoksa devam et
            System.out.println("Appointments tablosu kontrol edilemedi, yeni oluşturulacak: " + e.getMessage());
        }
        
        String createUsersTable = """
            PRAGMA foreign_keys = ON;
            CREATE TABLE IF NOT EXISTS users (
              id TEXT PRIMARY KEY,
              username TEXT UNIQUE,
              password TEXT,
              name TEXT,
              surname TEXT,
              tcNo TEXT,
              role TEXT,
              phone TEXT,
              email TEXT
            );
            """;
        
        String createDoctorsTable = """
            CREATE TABLE IF NOT EXISTS doctors (
              user_id TEXT PRIMARY KEY,
              branch TEXT,
              startHour INTEGER,
              endHour INTEGER,
              FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE
            );
            """;
        
        String createPatientsTable = """
            CREATE TABLE IF NOT EXISTS patients (
              user_id TEXT PRIMARY KEY,
              FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE
            );
            """;
        
        String createAppointmentsTable = """
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
            """;
        
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createUsersTable);
            stmt.execute(createDoctorsTable);
            stmt.execute(createPatientsTable);
            stmt.execute(createAppointmentsTable);
            
            // Index'leri oluştur
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_appointments_date ON appointments(date)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_appointments_state ON appointments(state)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_appointments_patient_id ON appointments(patient_id)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_appointments_doctor_id ON appointments(doctor_id)");
            
            System.out.println("Tüm tablolar başarıyla oluşturuldu!");
        }
    }
    
    // RANDEVU İŞLEMLERİ
    
    public void saveAppointment(Appointment appointment) {
        if (appointment == null) {
            System.out.println("HATA: Kaydedilecek randevu null!");
            return;
        }
        
        String sql = """
            INSERT OR REPLACE INTO appointments (id, patient_id, doctor_id, date, time, state, note)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, appointment.getId());
            pstmt.setString(2, appointment.getPatient() != null ? appointment.getPatient().getId() : null);
            pstmt.setString(3, appointment.getDoctor() != null ? appointment.getDoctor().getId() : null);
            
            // Tarih ve saat bilgisini ayır
            String dateTime = appointment.getDate();
            String date = dateTime;
            String time = "";
            if (dateTime != null && dateTime.contains(" - ")) {
                String[] parts = dateTime.split(" - ");
                date = parts[0];
                if (parts.length > 1) {
                    time = parts[1];
                }
            }
            
            pstmt.setString(4, date);
            pstmt.setString(5, time);
            pstmt.setString(6, appointment.getState() != null ? appointment.getState().getStatus() : "SCHEDULED");
            pstmt.setString(7, null); // note
            
            pstmt.executeUpdate();
            connection.commit();
            
            System.out.println("RANDEVU SQLite VERİTABANINA KAYDEDİLDİ: " + appointment.getId());
        } catch (SQLException e) {
            System.err.println("✗ HATA: Randevu kaydetme hatası: " + e.getMessage());
            System.err.println("  Randevu ID: " + appointment.getId());
            System.err.println("  Patient ID: " + (appointment.getPatient() != null ? appointment.getPatient().getId() : "NULL"));
            System.err.println("  Doctor ID: " + (appointment.getDoctor() != null ? appointment.getDoctor().getId() : "NULL"));
            System.err.println("  Tarih: " + appointment.getDate());
            
            // Eğer tablo şeması hatası varsa, tabloyu yeniden oluştur
            if (e.getMessage() != null && e.getMessage().contains("no column named")) {
                System.err.println("  UYARI: Tablo şeması hatası tespit edildi! Tablo yeniden oluşturulacak...");
                try {
                    connection.rollback();
                    // Tabloyu yeniden oluştur
                    createTables();
                    connection.commit();
                    System.out.println("  ✓ Tablo başarıyla yeniden oluşturuldu!");
                    // Tekrar dene
                    System.out.println("  Randevu tekrar kaydediliyor...");
                    saveAppointment(appointment);
                    return;
                } catch (SQLException ex) {
                    System.err.println("  ✗ Tablo yeniden oluşturma hatası: " + ex.getMessage());
                    ex.printStackTrace();
                }
            }
            
            e.printStackTrace();
            try {
                connection.rollback();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
    }
    
    public List<Appointment> getAppointmentsByPatientId(String patientId) {
        List<Appointment> appointments = new ArrayList<>();
        // Hem ID hem username ile arama yap
        String sql = """
            SELECT a.* FROM appointments a
            INNER JOIN users u ON a.patient_id = u.id
            WHERE a.patient_id = ? OR u.username = ?
            """;
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, patientId);
            pstmt.setString(2, patientId); // ID olarak verilen değer username de olabilir
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                Appointment appointment = mapResultSetToAppointment(rs);
                if (appointment != null) {
                    appointments.add(appointment);
                }
            }
        } catch (SQLException e) {
            System.err.println("Randevu sorgulama hatası: " + e.getMessage());
            e.printStackTrace();
        }
        
        return appointments;
    }
    
    public List<Appointment> getAppointmentsByDoctorId(String doctorId) {
        List<Appointment> appointments = new ArrayList<>();
        // Hem ID hem username ile arama yap
        String sql = """
            SELECT a.* FROM appointments a
            INNER JOIN users u ON a.doctor_id = u.id
            WHERE a.doctor_id = ? OR u.username = ?
            """;
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, doctorId);
            pstmt.setString(2, doctorId); // ID olarak verilen değer username de olabilir
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                Appointment appointment = mapResultSetToAppointment(rs);
                if (appointment != null) {
                    appointments.add(appointment);
                }
            }
        } catch (SQLException e) {
            System.err.println("Randevu sorgulama hatası: " + e.getMessage());
            e.printStackTrace();
        }
        
        return appointments;
    }
    
    // Username'e göre randevu arama (alternatif metod)
    public List<Appointment> getAppointmentsByPatientUsername(String username) {
        List<Appointment> appointments = new ArrayList<>();
        String sql = """
            SELECT a.* FROM appointments a
            INNER JOIN users u ON a.patient_id = u.id
            WHERE LOWER(u.username) = LOWER(?)
            """;
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                Appointment appointment = mapResultSetToAppointment(rs);
                if (appointment != null) {
                    appointments.add(appointment);
                }
            }
        } catch (SQLException e) {
            System.err.println("Randevu sorgulama hatası: " + e.getMessage());
            e.printStackTrace();
        }
        
        return appointments;
    }
    
    public List<Appointment> getAppointmentsByDoctorUsername(String username) {
        List<Appointment> appointments = new ArrayList<>();
        String sql = """
            SELECT a.* FROM appointments a
            INNER JOIN users u ON a.doctor_id = u.id
            WHERE LOWER(u.username) = LOWER(?)
            """;
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                Appointment appointment = mapResultSetToAppointment(rs);
                if (appointment != null) {
                    appointments.add(appointment);
                }
            }
        } catch (SQLException e) {
            System.err.println("Randevu sorgulama hatası: " + e.getMessage());
            e.printStackTrace();
        }
        
        return appointments;
    }
    
    public List<Appointment> getAllAppointments() {
        List<Appointment> appointments = new ArrayList<>();
        String sql = "SELECT * FROM appointments";
        
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                Appointment appointment = mapResultSetToAppointment(rs);
                if (appointment != null) {
                    appointments.add(appointment);
                }
            }
        } catch (SQLException e) {
            System.err.println("Randevu sorgulama hatası: " + e.getMessage());
            e.printStackTrace();
        }
        
        return appointments;
    }
    
    private Appointment mapResultSetToAppointment(ResultSet rs) throws SQLException {
        String id = rs.getString("id");
        String patientId = rs.getString("patient_id");
        String doctorId = rs.getString("doctor_id");
        String date = rs.getString("date");
        String time = rs.getString("time");
        String state = rs.getString("state");
        
        // Tarih ve saati birleştir
        String fullDate = date;
        if (time != null && !time.isEmpty()) {
            fullDate = date + " - " + time;
        }
        
        // Patient ve Doctor nesnelerini veritabanından al
        Patient patient = getPatientById(patientId);
        Doctor doctor = getDoctorById(doctorId);
        
        if (patient == null || doctor == null) {
            System.out.println("UYARI: Randevu için Patient veya Doctor bulunamadı!");
            System.out.println("  Patient ID: " + patientId + ", Doctor ID: " + doctorId);
            return null;
        }
        
        Appointment appointment = new Appointment(id, patient, doctor, fullDate);
        
        // State'i ayarla
        if (state != null) {
            switch (state) {
                case "CANCELLED":
                    appointment.setState(new CancelledState());
                    break;
                case "COMPLETED":
                    appointment.setState(new CompletedState());
                    break;
                case "NO_SHOW":
                    appointment.setState(new NoShowState());
                    break;
                case "SCHEDULED":
                default:
                    appointment.setState(new ScheduledState());
                    break;
            }
        }
        
        return appointment;
    }
    
    private Patient getPatientById(String id) {
        if (id == null) return null;
        
        // Önce ID ile ara
        String sql = """
            SELECT u.*, p.user_id 
            FROM users u 
            INNER JOIN patients p ON u.id = p.user_id 
            WHERE u.id = ? OR LOWER(u.username) = LOWER(?)
            """;
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, id);
            pstmt.setString(2, id); // ID olarak verilen değer username de olabilir
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return new Patient(
                    rs.getString("id"),
                    rs.getString("username"),
                    rs.getString("password"),
                    rs.getString("name"),
                    rs.getString("surname"),
                    rs.getString("tcNo")
                );
            }
        } catch (SQLException e) {
            System.err.println("Patient sorgulama hatası: " + e.getMessage());
            e.printStackTrace();
        }
        
        return null;
    }
    
    private Doctor getDoctorById(String id) {
        if (id == null) return null;
        
        // Önce ID ile ara, bulamazsa username ile ara
        String sql = """
            SELECT u.*, d.branch 
            FROM users u 
            INNER JOIN doctors d ON u.id = d.user_id 
            WHERE u.id = ? OR LOWER(u.username) = LOWER(?)
            """;
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, id);
            pstmt.setString(2, id); // ID olarak verilen değer username de olabilir
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return new Doctor(
                    rs.getString("id"),
                    rs.getString("username"),
                    rs.getString("password"),
                    rs.getString("name"),
                    rs.getString("surname"),
                    rs.getString("tcNo"),
                    rs.getString("branch")
                );
            }
        } catch (SQLException e) {
            System.err.println("Doctor sorgulama hatası: " + e.getMessage());
            e.printStackTrace();
        }
        
        return null;
    }
    
    public void updateAppointmentState(String appointmentId, String state) {
        String sql = "UPDATE appointments SET state = ? WHERE id = ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, state);
            pstmt.setString(2, appointmentId);
            pstmt.executeUpdate();
            connection.commit();
            
            System.out.println("Randevu durumu güncellendi: " + appointmentId + " -> " + state);
        } catch (SQLException e) {
            System.err.println("Randevu güncelleme hatası: " + e.getMessage());
            e.printStackTrace();
            try {
                connection.rollback();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
    }
    
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}

