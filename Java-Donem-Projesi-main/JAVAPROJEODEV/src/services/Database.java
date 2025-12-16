package services;

import models.AbstractUser;
import models.Appointment;
import models.Doctor;
import models.Patient;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class Database {

    private static Database instance;

    // In-memory caches for compatibility with existing code
    private Map<String, Patient> patients = new HashMap<>();
    private Map<String, Doctor> doctors = new HashMap<>();
    private Map<String, Appointment> appointments = new HashMap<>();
    private Map<String, AbstractUser> users = new HashMap<>();

    // JDBC
    private Connection conn;
    private final String DB_URL = "jdbc:sqlite:app.db";

    private Database(){
        try {
            connect();
            initSchema();
            loadAll();
        } catch (Exception e) {
            System.err.println("Database init error: " + e.getMessage());
        }
    }

    public static Database getInstance(){
        if(instance == null) instance = new Database();
        return instance;
    }

    private void connect() throws SQLException, ClassNotFoundException {
        // Load SQLite driver
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            // If driver not found, propagate with message
            System.err.println("SQLite JDBC driver not found. Please add sqlite-jdbc jar to classpath.");
            throw e;
        }
        conn = DriverManager.getConnection(DB_URL);
    }

    private void initSchema() throws SQLException {
        Statement st = conn.createStatement();
        // users table
        st.executeUpdate("CREATE TABLE IF NOT EXISTS users (id TEXT PRIMARY KEY, username TEXT, password TEXT, name TEXT, surname TEXT, tcNo TEXT, role TEXT, phone TEXT, email TEXT)");
        // unique indexes to enforce username/email uniqueness
        st.executeUpdate("CREATE UNIQUE INDEX IF NOT EXISTS idx_users_username ON users(username)");
        st.executeUpdate("CREATE UNIQUE INDEX IF NOT EXISTS idx_users_email ON users(email)");
        // doctors table
        st.executeUpdate("CREATE TABLE IF NOT EXISTS doctors (user_id TEXT PRIMARY KEY, branch TEXT, startHour INTEGER, endHour INTEGER, FOREIGN KEY(user_id) REFERENCES users(id))");
        // patients table
        st.executeUpdate("CREATE TABLE IF NOT EXISTS patients (user_id TEXT PRIMARY KEY, FOREIGN KEY(user_id) REFERENCES users(id))");
        // appointments (added cancelled_at to track cancellation timestamp)
        st.executeUpdate("CREATE TABLE IF NOT EXISTS appointments (id TEXT PRIMARY KEY, patient_id TEXT, doctor_id TEXT, date TEXT, time TEXT, state TEXT, note TEXT, cancelled_at TEXT, FOREIGN KEY(patient_id) REFERENCES users(id), FOREIGN KEY(doctor_id) REFERENCES users(id))");
        st.close();
    }

    private void loadAll() throws SQLException {
        // Load users
        Statement st = conn.createStatement();
        ResultSet rs = st.executeQuery("SELECT * FROM users");
        while(rs.next()){
            String id = rs.getString("id");
            String username = rs.getString("username");
            String password = rs.getString("password");
            String name = rs.getString("name");
            String surname = rs.getString("surname");
            String tcNo = rs.getString("tcNo");
            String role = rs.getString("role");
            String phone = rs.getString("phone");
            String email = rs.getString("email");

            if ("DOCTOR".equals(role)){
                // doctor details will be filled after doctors query
                Doctor d = new Doctor(id, username, password, name, surname, tcNo, "");
                d.setPhone(phone);
                d.setEmail(email);
                users.put(id, d);
                doctors.put(id, d);
            } else {
                Patient p = new Patient(id, username, password, name, surname, tcNo);
                p.setPhone(phone);
                p.setEmail(email);
                users.put(id, p);
                patients.put(id, p);
            }
        }
        rs.close();

        // Load doctor extra fields
        rs = st.executeQuery("SELECT * FROM doctors");
        while(rs.next()){
            String uid = rs.getString("user_id");
            String branch = rs.getString("branch");
            int start = rs.getInt("startHour");
            int end = rs.getInt("endHour");
            Doctor d = doctors.get(uid);
            if (d != null) {
                d.setStartHour(start);
                d.setEndHour(end);
                // can't set branch via setter, but constructor took branch; set via reflection simple approach:
                try {
                    java.lang.reflect.Field f = Doctor.class.getDeclaredField("branch");
                    f.setAccessible(true);
                    f.set(d, branch);
                } catch (Exception e) {
                    // ignore
                }
            }
        }
        rs.close();

        // Load appointments
        rs = st.executeQuery("SELECT * FROM appointments");
        while(rs.next()){
            String id = rs.getString("id");
            String pid = rs.getString("patient_id");
            String did = rs.getString("doctor_id");
            String date = rs.getString("date");
            String time = rs.getString("time");
            String state = rs.getString("state");
            String note = rs.getString("note");
            String cancelledAt = null;
            try { cancelledAt = rs.getString("cancelled_at"); } catch (Exception e) { /* older DB may not have column */ }

            Patient p = patients.get(pid);
            Doctor d = doctors.get(did);
            if (p != null && d != null) {
                Appointment a = new Appointment(id, p, d, date);
                a.setTime(time);
                a.setNote(note == null ? "" : note);
                if (cancelledAt != null) a.setCancelledAt(cancelledAt);
                // set state using simple mapping
                try {
                    models.states.AppointmentState stt = new models.states.ScheduledState();
                    if ("CANCELLED".equals(state)) stt = new models.states.CancelledState();
                    else if ("COMPLETED".equals(state)) stt = new models.states.CompletedState();
                    else if ("NO_SHOW".equals(state)) stt = new models.states.NoShowState();
                    a.setState(stt);
                } catch (Exception e) { }
                appointments.put(id, a);
            }
        }
        rs.close();
        st.close();
    }

    // Basic getters for existing code
    public Map<String, Patient> getPatients(){ return patients; }
    public Map<String, Doctor> getDoctors(){ return doctors; }
    public Map<String, Appointment> getAppointments(){ return appointments; }
    public Map<String, AbstractUser> getUsers(){ return users; }

    // Refresh cache from database (reload all data)
    public void refreshCache() {
        try {
            patients.clear();
            doctors.clear();
            appointments.clear();
            users.clear();
            loadAll();
        } catch (SQLException e) {
            System.err.println("Error refreshing cache: " + e.getMessage());
        }
    }

    // Save or update user
    public void saveUser(AbstractUser user){
        users.put(user.getId(), user);
        if (user instanceof Doctor) doctors.put(user.getId(), (Doctor) user);
        if (user instanceof Patient) patients.put(user.getId(), (Patient) user);

        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT OR REPLACE INTO users(id, username, password, name, surname, tcNo, role, phone, email) VALUES(?,?,?,?,?,?,?,?,?)")){
            ps.setString(1, user.getId());
            ps.setString(2, user.getUsername() == null ? null : user.getUsername().trim());
            ps.setString(3, user.getPassword());
            ps.setString(4, user.getName() == null ? null : user.getName().trim());
            ps.setString(5, user.getSurname() == null ? null : user.getSurname().trim());
            ps.setString(6, user.getTcNo() == null ? null : user.getTcNo().trim());
            ps.setString(7, user.getRole());
            ps.setString(8, user.getPhone() == null ? null : user.getPhone().trim());
            ps.setString(9, user.getEmail() == null ? null : user.getEmail().trim());
            ps.executeUpdate();
        } catch (SQLException e){ System.err.println("saveUser error: " + e.getMessage()); }
    }

    public void saveDoctor(Doctor d){
        saveUser(d);
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT OR REPLACE INTO doctors(user_id, branch, startHour, endHour) VALUES(?,?,?,?)")){
            ps.setString(1, d.getId());
            ps.setString(2, d.getBranch());
            ps.setInt(3, d.getStartHour());
            ps.setInt(4, d.getEndHour());
            ps.executeUpdate();
        } catch (SQLException e){ System.err.println("saveDoctor error: " + e.getMessage()); }
    }

    public void savePatient(Patient p){
        saveUser(p);
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT OR REPLACE INTO patients(user_id) VALUES(?)")){
            ps.setString(1, p.getId());
            ps.executeUpdate();
        } catch (SQLException e){ System.err.println("savePatient error: " + e.getMessage()); }
    }

    public void saveAppointment(Appointment a){
        appointments.put(a.getId(), a);
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT OR REPLACE INTO appointments(id, patient_id, doctor_id, date, time, state, note, cancelled_at) VALUES(?,?,?,?,?,?,?,?)")){
            ps.setString(1, a.getId());
            ps.setString(2, a.getPatient().getId());
            ps.setString(3, a.getDoctor().getId());
            ps.setString(4, a.getDate());
            ps.setString(5, a.getTime());
            ps.setString(6, a.getState().getStatus());
            ps.setString(7, a.getNote());
            ps.setString(8, a.getCancelledAt());
            ps.executeUpdate();
        } catch (SQLException e){ System.err.println("saveAppointment error: " + e.getMessage()); }
    }

    public void updateAppointmentState(String appointmentId, String newState){
        Appointment a = appointments.get(appointmentId);
        if (a == null) return;
        try (PreparedStatement ps = conn.prepareStatement("UPDATE appointments SET state=? WHERE id=?")){
            ps.setString(1, newState);
            ps.setString(2, appointmentId);
            ps.executeUpdate();
        } catch (SQLException e){ System.err.println("updateAppointmentState error: " + e.getMessage()); }
    }

}