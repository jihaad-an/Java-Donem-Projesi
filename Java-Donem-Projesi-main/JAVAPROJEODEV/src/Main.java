import command.CreateAppointmentCommand;
import command.CancelAppointmentCommand;
import factory.UserFactory;
import models.*;
import services.*;

import java.util.Scanner;
import java.util.List;

public class Main {

    // Global değişkenler
    private static Scanner scanner = new Scanner(System.in);
    private static AppointmentService appointmentService = new AppointmentService();
    // Senin mevcut AuthService kodunu kullanıyoruz
    private static AuthService authService = new AuthService();
    private static AbstractUser currentUser = null;

    public static void main(String[] args) {

        System.out.println("=== HASTANE RANDEVU SİSTEMİNE HOŞGELDİNİZ ===");

        while (true) {
            if (currentUser == null) {
                // --- ANA EKRAN: Giriş veya Kayıt ---
                System.out.println("\n--- ANA PANEL ---");
                System.out.println("1. Giriş Yap");
                System.out.println("2. Kayıt Ol (Hasta)");
                System.out.println("3. Kayıt Ol (Doktor)");
                System.out.println("0. Çıkış");
                System.out.print("Seçiminiz: ");
                String mainChoice = scanner.nextLine().trim();

                if (mainChoice.equals("1")) {
                    // Login
                    System.out.println("\n--- GİRİŞ PANELİ ---");
                    System.out.print("Kullanıcı Adı veya Email: ");
                    String input = scanner.nextLine();
                    System.out.print("Şifre: ");
                    String pass = scanner.nextLine();
                    currentUser = authService.login(input, pass);
                    if (currentUser != null) System.out.println("Giriş Başarılı! Hoşgeldiniz: " + currentUser.getName());
                    else System.out.println("Hata: Bilgiler yanlış veya kullanıcı bulunamadı.");
                } else if (mainChoice.equals("2")) {
                    // Register patient
                    System.out.println("\n--- HASTA KAYIT ---");
                    System.out.print("Kullanıcı adı: "); String username = scanner.nextLine().trim();
                    System.out.print("Şifre: "); String password = scanner.nextLine();
                    System.out.print("İsim: "); String name = scanner.nextLine().trim();
                    System.out.print("Soyisim: "); String surname = scanner.nextLine().trim();
                    System.out.print("TC No (opsiyonel): "); String tc = scanner.nextLine().trim();
                    System.out.print("Email (opsiyonel): "); String email = scanner.nextLine().trim();

                    String id = String.valueOf(System.currentTimeMillis() % 100000);
                    Patient p = (Patient) UserFactory.createUser("PATIENT", id, username, password, name, surname, tc, "");
                    p.setEmail(email.isEmpty() ? null : email);
                    Database.getInstance().savePatient(p);
                    currentUser = p; // auto-login
                    System.out.println("Kayıt başarılı. Hoşgeldiniz, " + p.getName());

                } else if (mainChoice.equals("3")) {
                    // Register doctor
                    System.out.println("\n--- DOKTOR KAYIT ---");
                    System.out.print("Kullanıcı adı: "); String username = scanner.nextLine().trim();
                    System.out.print("Şifre: "); String password = scanner.nextLine();
                    System.out.print("İsim: "); String name = scanner.nextLine().trim();
                    System.out.print("Soyisim: "); String surname = scanner.nextLine().trim();
                    System.out.print("Branş: "); String branch = scanner.nextLine().trim();
                    System.out.print("Başlangıç saati (0-23): "); int sh = Integer.parseInt(scanner.nextLine().trim());
                    System.out.print("Bitiş saati (1-24): "); int eh = Integer.parseInt(scanner.nextLine().trim());
                    System.out.print("Email (opsiyonel): "); String email = scanner.nextLine().trim();

                    String id = String.valueOf(System.currentTimeMillis() % 100000);
                    Doctor d = (Doctor) UserFactory.createUser("DOCTOR", id, username, password, name, surname, "", branch);
                    d.setStartHour(sh); d.setEndHour(eh);
                    d.setEmail(email.isEmpty() ? null : email);
                    Database.getInstance().saveDoctor(d);
                    currentUser = d; // auto-login
                    System.out.println("Kayıt başarılı. Hoşgeldiniz Dr. " + d.getName());

                } else if (mainChoice.equals("0")) {
                    System.out.println("Program kapatılıyor.");
                    System.exit(0);
                } else {
                    System.out.println("Geçersiz seçim.");
                }

            } else {
                // --- ROL BAZLI MENÜ YÖNLENDİRMESİ ---
                if (currentUser.getRole().equals("DOCTOR")) {
                    showDoctorMenu((Doctor) currentUser);
                } else {
                    showPatientMenu((Patient) currentUser);
                }
            }
        }
    }

    // --- DOKTOR MENÜSÜ ---
    private static void showDoctorMenu(Doctor doc) {
        System.out.println("\n--- DOKTOR PANELİ (" + doc.getName() + ") ---");
        System.out.println("1. Randevularımı Listele");
        System.out.println("2. İptal Edilen Randevuları Gör");
        System.out.println("3. Randevu Durumu Güncelle (Gelmedi/Tamamlandı)");
        System.out.println("4. Muayene Notu Ekle");
        System.out.println("5. Profil/Çalışma Saati Görüntüle");
        System.out.println("0. Çıkış Yap");
        System.out.print("Seçiminiz: ");

        String choice = scanner.nextLine();
        switch (choice) {
            case "1":
                System.out.println("Randevularınız:");
                List<Appointment> apps = appointmentService.getAppointmentsBetween(doc, "2020-01-01", "2030-12-31");
                for(Appointment a : apps) {
                    System.out.println("ID: " + a.getId() + " | Tarih: " + a.getDate() + " " + a.getTime() + " | Hasta: " + a.getPatient().getName() + " | Durum: " + a.getState().getClass().getSimpleName());
                    if(!a.getNote().isEmpty()) System.out.println("   Not: " + a.getNote());
                }
                break;
            case "2":
                List<Appointment> cancelled = appointmentService.getCancelledAppointmentsForDoctor(doc);
                System.out.println("İptal Edilenler: " + cancelled.size() + " adet.");
                for(Appointment a : cancelled) System.out.println(" - " + a.getDate() + " " + a.getTime() + " (Hasta: " + a.getPatient().getName() + ")");
                break;
            case "3":
                System.out.print("Durumu değişecek Randevu ID: ");
                String rId = scanner.nextLine();
                System.out.println("1- Tamamlandı, 2- Gelmedi");
                String stateChoice = scanner.nextLine();
                if(stateChoice.equals("1")) appointmentService.markCompleted(rId);
                else if(stateChoice.equals("2")) appointmentService.markNoShow(rId);
                break;
            case "4":
                System.out.print("Not eklenecek Randevu ID: ");
                String noteId = scanner.nextLine();
                System.out.print("Notunuz: ");
                String note = scanner.nextLine();
                appointmentService.addNote(noteId, note);
                break;
            case "5":
                System.out.println("Branş: " + doc.getBranch());
                System.out.println("Çalışma Saatleri: " + doc.getStartHour() + ":00 - " + doc.getEndHour() + ":00");
                break;
            case "0":
                currentUser = null;
                System.out.println("Çıkış yapıldı.");
                break;
            default:
                System.out.println("Geçersiz işlem.");
        }
    }

    // --- HASTA MENÜSÜ ---
    private static void showPatientMenu(Patient pat) {
        System.out.println("\n--- HASTA PANELİ (" + pat.getName() + ") ---");
        System.out.println("1. Yeni Randevu Al");
        System.out.println("2. Doktor Ara");
        System.out.println("3. Randevularımı Gör");
        System.out.println("4. Randevu İptal Et");
        System.out.println("5. Randevu Saati Değiştir (Reschedule)");
        System.out.println("6. Profil/Şifre İşlemleri");
        System.out.println("0. Çıkış Yap");
        System.out.print("Seçiminiz: ");

        String choice = scanner.nextLine();
        switch (choice) {
            case "1":
                System.out.print("Doktor Adı veya Branş (Örn: Mehmet veya Kardiyoloji): ");
                String docName = scanner.nextLine();
                List<Doctor> docs = appointmentService.searchDoctors(docName);
                if(docs.isEmpty()) { System.out.println("Doktor bulunamadı."); break; }

                // Eğer birden fazla doktor bulduysa listele ve seçim yaptır
                Doctor selectedDoc = null;
                if (docs.size() == 1) {
                    selectedDoc = docs.get(0);
                } else {
                    System.out.println("Eşleşen doktorlar:");
                    for (int i = 0; i < docs.size(); i++) {
                        Doctor d = docs.get(i);
                        System.out.println((i+1) + ". " + d.getName() + " " + d.getSurname() + " (" + d.getBranch() + ")");
                    }
                    System.out.print("Seçiminiz (numara): ");
                    try {
                        int sel = Integer.parseInt(scanner.nextLine());
                        if (sel < 1 || sel > docs.size()) { System.out.println("Geçersiz seçim."); break; }
                        selectedDoc = docs.get(sel-1);
                    } catch (NumberFormatException e) { System.out.println("Geçersiz giriş."); break; }
                }

                System.out.println("Doktor Seçildi: " + selectedDoc.getName() + " (" + selectedDoc.getBranch() + ")");

                // Tarih al
                System.out.print("Randevu Tarihi (YYYY-MM-DD): ");
                String dateInput = scanner.nextLine().trim();

                // Müsait saatleri getir ve göster
                List<String> available = appointmentService.getAvailableTimes(selectedDoc, dateInput);
                if (available.isEmpty()) {
                    System.out.println("Bu tarihte müsait saat yok.");
                    break;
                }
                System.out.println("Müsait Saatler:");
                for (int i = 0; i < available.size(); i++) {
                    System.out.println((i+1) + ". " + available.get(i));
                }
                System.out.print("Seçmek istediğiniz saatin numarasını girin: ");
                int timeSel = -1;
                try { timeSel = Integer.parseInt(scanner.nextLine()); } catch (NumberFormatException e) { System.out.println("Geçersiz giriş."); break; }
                if (timeSel < 1 || timeSel > available.size()) { System.out.println("Geçersiz seçim."); break; }

                String chosenTime = available.get(timeSel-1);
                String newId = String.valueOf(System.currentTimeMillis() % 10000);

                // Direkt servis çağrısı ile randevu oluştur
                appointmentService.createAppointment(newId, pat, selectedDoc, dateInput, chosenTime);
                break;

            case "2":
                System.out.print("Aranacak Doktor/Branş: ");
                String q = scanner.nextLine();
                List<Doctor> results = appointmentService.searchDoctors(q);
                for(Doctor d : results) System.out.println("- " + d.getName() + " " + d.getSurname() + " (" + d.getBranch() + ")");
                break;

            case "3":
                System.out.println("Randevularınız:");
                Database.getInstance().getAppointments().values().stream()
                        .filter(a -> a.getPatient().getId().equals(pat.getId()))
                        .forEach(a -> System.out.println("ID: " + a.getId() + " | " + a.getDate() + " " + a.getTime() + " | Dr. " + a.getDoctor().getName() + " | Durum: " + a.getState().getClass().getSimpleName()));
                break;

            case "4":
                System.out.print("İptal edilecek Randevu ID: ");
                String cancelId = scanner.nextLine();
                CancelAppointmentCommand cancelCmd = new CancelAppointmentCommand(appointmentService, cancelId);
                cancelCmd.execute();
                break;

            case "5":
                System.out.print("Güncellenecek Randevu ID: ");
                String upId = scanner.nextLine();
                System.out.print("Yeni Tarih (YYYY-MM-DD): ");
                String nDate = scanner.nextLine();
                System.out.print("Yeni Saat (HH:mm): ");
                String nTime = scanner.nextLine();
                appointmentService.rescheduleAppointment(upId, nDate, nTime);
                break;

            case "6":
                System.out.println("1- İletişim Bilgisi Güncelle");
                System.out.println("2- Şifre Değiştir");
                String pChoice = scanner.nextLine();
                if(pChoice.equals("1")) {
                    System.out.print("Yeni Telefon: "); String ph = scanner.nextLine();
                    System.out.print("Yeni Email: "); String em = scanner.nextLine();
                    appointmentService.updateContactInfo(pat, ph, em);
                } else if(pChoice.equals("2")) {
                    System.out.print("Eski Şifre: "); String oldP = scanner.nextLine();
                    System.out.print("Yeni Şifre: "); String newP = scanner.nextLine();
                    appointmentService.changePassword(pat, oldP, newP);
                }
                break;

            case "0":
                currentUser = null;
                System.out.println("Çıkış yapıldı.");
                break;
            default:
                System.out.println("Geçersiz seçim.");
        }
    }

    // (Önceden burada başlangıç verisi yaratılıyordu; artık kaldırıldı.)
}