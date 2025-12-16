package command;

import models.Doctor;
import models.Patient;
import services.AppointmentService;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

public class CreateAppointmentCommand implements Command {

    private AppointmentService service;
    private String id;
    private Patient patient;
    private Doctor doctor;
    private String date;  // "YYYY-MM-DD HH:mm" formatında birleşik gelebilir

    // Constructor: Tek tarih parametresi alır (İçeride parçalayacak)
    public CreateAppointmentCommand(AppointmentService service,
                                    String id,
                                    Patient patient,
                                    Doctor doctor,
                                    String date) {
        this.service = service;
        this.id = id;
        this.patient = patient;
        this.doctor = doctor;
        this.date = date;
    }

    @Override
    public void execute() {
        // 1. ADIM: Önce 4 parametreli (Saatsiz) metodu dene
        // createAppointment(String, Patient, Doctor, String)
        try {
            Method m4 = service.getClass().getMethod(
                    "createAppointment",
                    String.class, Patient.class, Doctor.class, String.class
            );
            m4.invoke(service, id, patient, doctor, date);
            return; // Başarılıysa çık
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            // 4 parametreli yoksa veya hata verdiyse devam et...
        }

        // 2. ADIM: Tarih ve Saati ayır
        // "2025-01-10 15:00" -> part1: "2025-01-10", part2: "15:00"
        String datePart = date;
        String timePart = ""; // Varsayılan boş

        String[] parts = date.trim().split("\\s+"); // Boşluğa göre böl
        if (parts.length >= 2) {
            datePart = parts[0];
            timePart = parts[1];
        }

        // 3. ADIM: 5 parametreli (Saatli) metodu dene
        // createAppointment(String, Patient, Doctor, String, String)
        try {
            Method m5 = service.getClass().getMethod(
                    "createAppointment",
                    String.class, Patient.class, Doctor.class, String.class, String.class
            );

            // Eğer saat yoksa "00:00" veya boş gönderilebilir, servisteki kontrole bağlı
            if(timePart.isEmpty()) timePart = "09:00";

            m5.invoke(service, id, patient, doctor, datePart, timePart);
            return; // Başarılıysa çık
        } catch (NoSuchMethodException e) {
            System.err.println("Hata: AppointmentService içinde uygun metod bulunamadı!");
        } catch (IllegalAccessException | InvocationTargetException e) {
            System.err.println("Hata: createAppointment çalıştırılırken sorun oluştu: " + e.getMessage());
        }
    }
}