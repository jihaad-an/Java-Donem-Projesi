package models;

import models.states.AppointmentState;
import models.states.ScheduledState;

public class Appointment {

    private String id;
    private Patient patient;
    private Doctor doctor;
    private String date;
    private String time;
    private AppointmentState state;
    private String note = "";
    private String cancelledAt = null; // ISO datetime when appointment was cancelled

    public Appointment(String id, Patient patient, Doctor doctor, String date) {
        this.id = id;
        this.patient = patient;
        this.doctor = doctor;
        this.date = date;
        this.state = new ScheduledState();
    }

    // --- GETTER - SETTER ---

    public String getId() { return id; }
    public Patient getPatient() { return patient; }
    public Doctor getDoctor() { return doctor; }

    public String getDate() { return date; }
    // YENİ: Randevu erteleme için gerekli olan metod
    public void setDate(String date) { this.date = date; }

    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public AppointmentState getState() {
        return state;
    }

    public void setState(AppointmentState state) {
        this.state = state;
    }

    public String getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(String cancelledAt) { this.cancelledAt = cancelledAt; }
}