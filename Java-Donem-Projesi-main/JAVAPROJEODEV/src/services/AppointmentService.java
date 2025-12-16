package services;

import models.*;
import models.states.*;
import java.util.*;
import java.time.LocalDateTime;

public class AppointmentService {

    private NotificationService notificationService = new NotificationService();

    // 1) RANDEVU OLUŞTURMA (Çalışma saati kontrolü eklendi)
    public Appointment createAppointment(String id, Patient p, Doctor d, String date, String time){

        // Önce doktorun çalışma saatlerine uygun mu?
        if (!isWithinWorkingHours(d, time)) {
            System.out.println("Hata: Doktor bu saatlerde çalışmıyor (" + d.getStartHour() + ":00 - " + d.getEndHour() + ":00).");
            return null;
        }

        // Sonra o saatte başka randevu var mı?
        if (!isTimeAvailable(d, date, time)) {
            System.out.println("Hata: Bu saat dolu! Randevu oluşturulamadı.");
            return null;
        }

        Appointment a = new Appointment(id, p, d, date);
        a.setTime(time);

        Database.getInstance().saveAppointment(a);

        // Bildirimler (hem hasta hem doktoru bildir)
        notificationService.notifyObservers(p, "Randevunuz oluşturuldu: " + date + " - " + time);
        notificationService.notifyObservers(d, "Yeni randevu: " + date + " - " + time);

        return a;
    }

    // YENİ: DOKTOR ÇALIŞMA SAATİ KONTROLÜ
    private boolean isWithinWorkingHours(Doctor d, String time) {
        // time formatı "14:30" gibi geliyorsa, saati alıp int'e çeviriyoruz
        try {
            int hour = Integer.parseInt(time.split(":")[0]);
            return hour >= d.getStartHour() && hour < d.getEndHour();
        } catch (Exception e) {
            System.out.println("Saat formatı hatası!");
            return false;
        }
    }

    // 2) SAAT DOLULUK KONTROLÜ (İptal edilenleri dolu saymaz)
    public boolean isTimeAvailable(Doctor doctor, String date, String time){
        for (Appointment a : Database.getInstance().getAppointments().values()) {
            // İptal edilen randevular saati doldurmaz, onları pas geçiyoruz
            if (a.getState() instanceof CancelledState) continue;

            if (a.getDoctor().equals(doctor) &&
                    a.getDate().equals(date) &&
                    a.getTime().equals(time)) {
                return false;
            }
        }
        return true;
    }

    // 3) RANDEVUYA NOT EKLEME
    public void addNote(String appointmentId, String note){
        Appointment a = Database.getInstance().getAppointments().get(appointmentId);
        if (a != null) {
            a.setNote(note);
            System.out.println("Muayene notu kaydedildi: " + note);
        }
    }

    // Secure addNote: only the doctor of the appointment can add a note
    public boolean addNoteByDoctor(String appointmentId, String note, Doctor doctor) {
        Appointment a = Database.getInstance().getAppointments().get(appointmentId);
        if (a == null) {
            System.out.println("Randevu bulunamadı.");
            return false;
        }
        if (!a.getDoctor().getId().equals(doctor.getId())) {
            System.out.println("Yetki hatası: Bu randevuya not ekleyemezsiniz.");
            return false;
        }
        a.setNote(note);
        Database.getInstance().saveAppointment(a);
        System.out.println("Muayene notu kaydedildi: " + note);
        // Notify patient that a note was added
        notificationService.notifyObservers(a.getPatient(), "Doktorunuz bir not ekledi: " + note);
        return true;
    }

    // 4) RANDEVU DURUMU GÜNCELLEME
    public void markCompleted(String id){
        Appointment a = Database.getInstance().getAppointments().get(id);
        if (a != null) {
            a.setState(new CompletedState());
            Database.getInstance().saveAppointment(a);
            System.out.println("Randevu tamamlandı olarak işaretlendi.");
            // Bildirimler
            notificationService.notifyObservers(a.getPatient(), "Randevunuz tamamlandı: " + a.getDate() + " " + a.getTime());
            notificationService.notifyObservers(a.getDoctor(), "Randevu tamamlandı: ID " + a.getId());
        }
    }

    // Secure markCompleted: only doctor of the appointment can mark completed
    public boolean markCompletedByDoctor(String id, Doctor doctor) {
        Appointment a = Database.getInstance().getAppointments().get(id);
        if (a == null) {
            System.out.println("Randevu bulunamadı.");
            return false;
        }
        if (!a.getDoctor().getId().equals(doctor.getId())) {
            System.out.println("Yetki hatası: Bu randevu üzerinde işlem yapamazsınız.");
            return false;
        }
        markCompleted(id);
        return true;
    }

    public void markNoShow(String id){
        Appointment a = Database.getInstance().getAppointments().get(id);
        if (a != null) {
            a.setState(new NoShowState());
            Database.getInstance().saveAppointment(a);
            System.out.println("Hasta gelmedi olarak işaretlendi.");
            // Bildirimler
            notificationService.notifyObservers(a.getPatient(), "Randevunuz için gelinmedi olarak işaretlendi: " + a.getDate() + " " + a.getTime());
            notificationService.notifyObservers(a.getDoctor(), "Hasta gelmedi: ID " + a.getId());
        }
    }

    // Secure markNoShow: only doctor of the appointment can mark no-show
    public boolean markNoShowByDoctor(String id, Doctor doctor) {
        Appointment a = Database.getInstance().getAppointments().get(id);
        if (a == null) {
            System.out.println("Randevu bulunamadı.");
            return false;
        }
        if (!a.getDoctor().getId().equals(doctor.getId())) {
            System.out.println("Yetki hatası: Bu randevu üzerinde işlem yapamazsınız.");
            return false;
        }
        markNoShow(id);
        return true;
    }

    // 5) TARİH ARALIĞI FİLTRELEME
    public List<Appointment> getAppointmentsBetween(Doctor doctor, String start, String end){
        List<Appointment> list = new ArrayList<>();
        // String formatının "YYYY-MM-DD" olduğunu varsayıyoruz (compareTo için)
        for (Appointment a : Database.getInstance().getAppointments().values()) {
            if (a.getDoctor().equals(doctor) &&
                    a.getDate().compareTo(start) >= 0 &&
                    a.getDate().compareTo(end) <= 0) {
                list.add(a);
            }
        }
        return list;
    }

    // 6) DOKTOR ARAMA (İsim veya Branşa göre)
    public List<Doctor> searchDoctors(String query) {
        List<Doctor> results = new ArrayList<>();
        query = query.toLowerCase();

        // Database.getUsers() üzerinden arama yapıyoruz
        for (AbstractUser user : Database.getInstance().getUsers().values()) {
            if (user instanceof Doctor) {
                Doctor d = (Doctor) user;
                if (d.getName().toLowerCase().contains(query) ||
                        d.getSurname().toLowerCase().contains(query) ||
                        d.getBranch().toLowerCase().contains(query)) {
                    results.add(d);
                }
            }
        }
        return results;
    }

    // 7) HASTA ARAMA (İsim veya TC'ye göre)
    public List<Patient> searchPatients(String query) {
        List<Patient> results = new ArrayList<>();
        query = query.toLowerCase();

        for (AbstractUser user : Database.getInstance().getUsers().values()) {
            if (user instanceof Patient) {
                Patient p = (Patient) user;
                if (p.getName().toLowerCase().contains(query) ||
                        p.getSurname().toLowerCase().contains(query) ||
                        p.getTcNo().contains(query)) {
                    results.add(p);
                }
            }
        }
        return results;
    }

    // 8) DOKTORUN İPTAL EDİLEN RANDEVULARINI GÖRMESİ
    public List<Appointment> getCancelledAppointmentsForDoctor(Doctor doctor) {
        List<Appointment> cancelledList = new ArrayList<>();

        for (Appointment a : Database.getInstance().getAppointments().values()) {
            if (a.getDoctor().equals(doctor) && (a.getState() instanceof CancelledState)) {
                cancelledList.add(a);
            }
        }
        return cancelledList;
    }

    // 9) İPTAL ETME
    public void cancelAppointment(String appointmentId) {
        Appointment appointment = Database.getInstance().getAppointments().get(appointmentId);

        if (appointment != null) {
            appointment.setState(new CancelledState());
            appointment.setCancelledAt(LocalDateTime.now().toString());
            Database.getInstance().saveAppointment(appointment);
            System.out.println("Randevu İPTAL edildi.");

            notificationService.notifyObservers(
                appointment.getPatient(),
                "Randevunuz iptal edildi! ID: " + appointmentId
            );
            notificationService.notifyObservers(
                appointment.getDoctor(),
                "Bir randevunuz iptal edildi! ID: " + appointmentId
            );
        }
    }

    // Secure cancel: only the patient who owns the appointment (or the doctor of the appointment) can cancel
    public boolean cancelAppointmentByUser(String appointmentId, AbstractUser actor) {
        Appointment appointment = Database.getInstance().getAppointments().get(appointmentId);
        if (appointment == null) {
            System.out.println("Randevu bulunamadı.");
            return false;
        }
        // Patient can cancel their own appointment
        if (actor instanceof Patient) {
            if (!appointment.getPatient().getId().equals(actor.getId())) {
                System.out.println("Yetki hatası: Bu randevuyu iptal etmeye yetkiniz yok.");
                return false;
            }
            cancelAppointment(appointmentId);
            return true;
        }
        // Doctor may cancel their own appointment
        if (actor instanceof Doctor) {
            if (!appointment.getDoctor().getId().equals(actor.getId())) {
                System.out.println("Yetki hatası: Bu randevuyu iptal etmeye yetkiniz yok.");
                return false;
            }
            cancelAppointment(appointmentId);
            return true;
        }
        System.out.println("Yetki hatası: Kullanıcı tipi tanınmadı.");
        return false;
    }

    // --- SON EKLENENLER: GÜNCELLEME İŞLEMLERİ ---

    // 10) RANDEVU SAATİ GÜNCELLEME (RESCHEDULE)
    public void rescheduleAppointment(String appointmentId, String newDate, String newTime) {
        Appointment a = Database.getInstance().getAppointments().get(appointmentId);
        if (a == null) {
            System.out.println("Randevu bulunamadı.");
            return;
        }

        // Doktorun yeni saatteki uygunluğunu kontrol et
        if (!isWithinWorkingHours(a.getDoctor(), newTime)) {
            System.out.println("Hata: Doktor bu saatte çalışmıyor.");
            return;
        }
        if (!isTimeAvailable(a.getDoctor(), newDate, newTime)) {
            System.out.println("Hata: Yeni seçilen saat dolu.");
            return;
        }

        // Güncelle
        // Appointment sınıfında setDate metodu zaten mevcut, buradan hem tarihi hem saati güncelliyoruz
        a.setDate(newDate);
        a.setTime(newTime);
        System.out.println("Randevu güncellendi: " + newDate + " " + newTime);
        Database.getInstance().saveAppointment(a);
        // Bildirimler: hastaya ve doktora haber ver
        notificationService.notifyObservers(a.getPatient(), "Randevu saatiniz değişti: " + newDate + " " + newTime);
        notificationService.notifyObservers(a.getDoctor(), "Randevu zamanlandı/taşındı: ID " + a.getId() + " -> " + newDate + " " + newTime);
    }

    // Secure reschedule: only the patient who owns the appointment (or the doctor) can reschedule
    public boolean rescheduleAppointmentByUser(String appointmentId, String newDate, String newTime, AbstractUser actor) {
        Appointment a = Database.getInstance().getAppointments().get(appointmentId);
        if (a == null) {
            System.out.println("Randevu bulunamadı.");
            return false;
        }
        // Patient can reschedule own appointment
        if (actor instanceof Patient) {
            if (!a.getPatient().getId().equals(actor.getId())) {
                System.out.println("Yetki hatası: Bu randevuyu değiştirmeye yetkiniz yok.");
                return false;
            }
            // Delegate to existing reschedule logic (which checks availability)
            rescheduleAppointment(appointmentId, newDate, newTime);
            return true;
        }
        // Doctor can reschedule their own appointment
        if (actor instanceof Doctor) {
            if (!a.getDoctor().getId().equals(actor.getId())) {
                System.out.println("Yetki hatası: Bu randevuyu değiştirmeye yetkiniz yok.");
                return false;
            }
            rescheduleAppointment(appointmentId, newDate, newTime);
            return true;
        }
        System.out.println("Yetki hatası: Kullanıcı tipi tanınmadı.");
        return false;
    }

    // 11) PROFİL GÜNCELLEME (İletişim Bilgileri)
    // AbstractUser sınıfında setPhone/setEmail yoksa bu kısmı kendine göre uyarla.
    // Şimdilik sadece ekrana basıyoruz.
    public void updateContactInfo(AbstractUser user, String newPhone, String newEmail) {
        if (user != null) {
            // Only update fields that are provided (non-null). This allows callers
            // to pass null for fields they don't want to change.
            if (newPhone != null) user.setPhone(newPhone);
            if (newEmail != null) user.setEmail(newEmail);
            System.out.println("Kullanıcı (" + user.getName() + ") iletişim bilgileri güncellendi: Tel=" + user.getPhone() + ", Email=" + user.getEmail());
            Database.getInstance().saveUser(user);
        }
    }

    // 13) Bir doktora ait seçilen tarihteki müsait saatleri döndürür (saat başı slotlar)
    public java.util.List<String> getAvailableTimes(Doctor doctor, String date) {
        return getAvailableTimes(doctor, date, 60);
    }

    /**
     * Return available times for a doctor on a date with customizable slot length in minutes (e.g., 30 or 60).
     */
    public java.util.List<String> getAvailableTimes(Doctor doctor, String date, int slotMinutes) {
        java.util.List<String> slots = new java.util.ArrayList<>();
        int start = doctor.getStartHour();
        int end = doctor.getEndHour();

        int totalMinutes = (end - start) * 60;
        int slotsCount = totalMinutes / slotMinutes;

        for (int i = 0; i < slotsCount; i++) {
            int minutesFromStart = i * slotMinutes;
            int hour = start + (minutesFromStart / 60);
            int minute = minutesFromStart % 60;
            String time = String.format("%02d:%02d", hour, minute);
            if (isTimeAvailable(doctor, date, time)) {
                slots.add(time);
            }
        }
        return slots;
    }

    // 14) Hasta için tarih aralığına göre randevu listeleme
    public java.util.List<Appointment> getAppointmentsForPatientBetween(Patient patient, String start, String end) {
        java.util.List<Appointment> list = new java.util.ArrayList<>();
        for (Appointment a : Database.getInstance().getAppointments().values()) {
            if (a.getPatient().equals(patient) &&
                    a.getDate().compareTo(start) >= 0 &&
                    a.getDate().compareTo(end) <= 0) {
                list.add(a);
            }
        }
        return list;
    }

    // 12) ŞİFRE DEĞİŞTİRME
    public void changePassword(AbstractUser user, String oldPass, String newPass) {
        if (user.getPassword().equals(oldPass)) {
            user.setPassword(newPass);
            System.out.println("Şifre başarıyla değiştirildi.");
            Database.getInstance().saveUser(user);
        } else {
            System.out.println("Hata: Eski şifre hatalı!");
        }
    }
}