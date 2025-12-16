package ui;

import models.AbstractUser;
import models.Doctor;
import models.Patient;
import models.Appointment;
import models.states.*;
import java.time.LocalDate;
import services.AppointmentService;
import services.AuthService;
import services.Database;
// UserFactory importu (kullanılıyorsa kalsın)
import factory.UserFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

public class MainWindow {

    private JFrame frame;
    private AuthService authService = new AuthService();
    private AppointmentService appointmentService = new AppointmentService();

    public MainWindow() {
        frame = new JFrame("Hastane Randevu Sistemi - GUI");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // 1. GÖRSEL DÜZENLEME: Pencere boyutu küçültüldü (Daha kompakt)
        frame.setSize(500, 400);
        frame.setLocationRelativeTo(null);
        showLogin();
        frame.setVisible(true);
    }

    private void showLogin() {
        JPanel mainPanel = new JPanel(new BorderLayout(5, 5));
        // Kenar boşlukları azaltıldı
        mainPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        mainPanel.setBackground(new Color(230, 245, 250));

        // --- Header (Başlık) ---
        JPanel headerPanel = new JPanel();
        headerPanel.setBackground(new Color(41, 128, 185));
        JLabel titleLabel = new JLabel("Hastane Randevu Sistemi");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 16)); // Font biraz küçültüldü
        titleLabel.setForeground(Color.WHITE);
        headerPanel.add(titleLabel);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // --- Login Form ---
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(new Color(230, 245, 250));
        GridBagConstraints c = new GridBagConstraints();
        // Elemanlar arası boşluklar sıkılaştırıldı
        c.insets = new Insets(2, 5, 2, 5);
        c.fill = GridBagConstraints.HORIZONTAL;

        JLabel userLabel = new JLabel("Kullanıcı Adı/Email:");
        userLabel.setFont(new Font("Arial", Font.PLAIN, 11));
        c.gridx = 0; c.gridy = 0; p.add(userLabel, c);

        JTextField userField = new JTextField(15);
        userField.setFont(new Font("Arial", Font.PLAIN, 11));
        userField.setPreferredSize(new Dimension(150, 25)); // Yükseklik inceltildi
        c.gridx = 1; p.add(userField, c);

        JLabel passLabel = new JLabel("Şifre:");
        passLabel.setFont(new Font("Arial", Font.PLAIN, 11));
        c.gridx = 0; c.gridy = 1; p.add(passLabel, c);

        JPasswordField passField = new JPasswordField(15);
        passField.setFont(new Font("Arial", Font.PLAIN, 11));
        passField.setPreferredSize(new Dimension(150, 25)); // Yükseklik inceltildi
        c.gridx = 1; p.add(passField, c);

        // Giriş Butonu
        c.gridx = 0; c.gridy = 2; c.gridwidth = 2;
        c.insets = new Insets(8, 5, 2, 5); // Buton üstüne biraz boşluk

        JButton loginBtn = new JButton("Giriş");
        loginBtn.setFont(new Font("Arial", Font.BOLD, 12));
        loginBtn.setPreferredSize(new Dimension(100, 30)); // Buton boyutu ayarlandı
        loginBtn.setBackground(new Color(46, 204, 113));
        loginBtn.setForeground(Color.WHITE);
        loginBtn.setFocusPainted(false);
        p.add(loginBtn, c);

        // Kayıt Butonları
        c.gridy = 3;
        c.insets = new Insets(5, 5, 5, 5);
        JPanel regPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        regPanel.setBackground(new Color(230, 245, 250));

        JButton regPatient = new JButton("Hasta Kayıt");
        regPatient.setFont(new Font("Arial", Font.BOLD, 10));
        regPatient.setPreferredSize(new Dimension(100, 25));
        regPatient.setBackground(new Color(52, 152, 219));
        regPatient.setForeground(Color.WHITE);
        regPatient.setFocusPainted(false);

        JButton regDoctor = new JButton("Doktor Kayıt");
        regDoctor.setFont(new Font("Arial", Font.BOLD, 10));
        regDoctor.setPreferredSize(new Dimension(100, 25));
        regDoctor.setBackground(new Color(155, 89, 182));
        regDoctor.setForeground(Color.WHITE);
        regDoctor.setFocusPainted(false);

        regPanel.add(regPatient);
        regPanel.add(regDoctor);
        p.add(regPanel, c);

        mainPanel.add(p, BorderLayout.CENTER);

        frame.getContentPane().removeAll();
        frame.getContentPane().add(mainPanel);
        frame.revalidate();
        frame.repaint();

        // --- LOGIN AKSİYONLARI ---
        loginBtn.addActionListener(e -> {
            String user = userField.getText().trim();
            String pass = new String(passField.getPassword());
            AbstractUser u = authService.login(user, pass);
            if (u == null) {
                JOptionPane.showMessageDialog(frame, "Giriş başarısız. Bilgileri kontrol ediniz.");
                return;
            }
            if (u instanceof Doctor) showDoctorPanel((Doctor)u);
            else showPatientPanel((Patient)u);
        });

        regPatient.addActionListener(ev -> {
            PatientRegistrationDialog dlg = new PatientRegistrationDialog(frame);
            dlg.setVisible(true);
            models.Patient created = dlg.getCreatedUser();
            if (created != null) {
                JOptionPane.showMessageDialog(frame, "Hoşgeldiniz, " + created.getName());
                showPatientPanel(created);
            }
        });

        regDoctor.addActionListener(ev -> {
            DoctorRegistrationDialog dlg = new DoctorRegistrationDialog(frame);
            dlg.setVisible(true);
            models.Doctor created = dlg.getCreatedUser();
            if (created != null) {
                JOptionPane.showMessageDialog(frame, "Hoşgeldiniz Dr. " + created.getName());
                showDoctorPanel(created);
            }
        });
    }

    private void showPatientPanel(Patient pat) {
        // Hasta paneli içerik çok olduğu için biraz büyütüyoruz
        frame.setSize(500, 450);
        frame.setLocationRelativeTo(null);

        JPanel p = new JPanel(new BorderLayout());
        JPanel top = new JPanel(new FlowLayout());
        top.add(new JLabel("Hoşgeldiniz, " + pat.getName()));
        JButton logout = new JButton("Çıkış"); top.add(logout);
        p.add(top, BorderLayout.NORTH);

        JPanel center = new JPanel(new GridLayout(0,1, 5, 5));
        center.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JButton newApp = new JButton("Yeni Randevu Al");
        JButton listApps = new JButton("Randevularımı Göster");
        JButton cancelApp = new JButton("Randevu İptal Et");
        JButton updateContact = new JButton("İletişim Bilgilerini Güncelle");
        JButton changePass = new JButton("Şifre Değiştir");
        JButton rescheduleApp = new JButton("Randevu Güncelle (Reschedule)");
        JButton historyBtn = new JButton("Randevu Geçmişi / İptaller");

        center.add(newApp); center.add(listApps); center.add(cancelApp); center.add(updateContact); center.add(changePass); center.add(rescheduleApp); center.add(historyBtn);
        p.add(center, BorderLayout.CENTER);

        frame.getContentPane().removeAll();
        frame.getContentPane().add(p);
        frame.revalidate(); frame.repaint();

        logout.addActionListener(ev -> {
            // Çıkışta tekrar küçük boyuta dön
            frame.setSize(360, 250);
            frame.setLocationRelativeTo(null);
            showLogin();
        });

        newApp.addActionListener(ev -> {
            String[] departments = {"Kardiyoloji", "Dahiliye", "Genel Cerrahi", "Göz Hastalıkları", "Kulak Burun Boğaz", "Ortopedi", "Nöroloji", "Dermatoloji"};
            String dept = (String) JOptionPane.showInputDialog(frame, "Bölüm seçin:", "Bölümler", JOptionPane.PLAIN_MESSAGE, null, departments, departments[0]);
            if (dept == null) return;

            List<Doctor> results = appointmentService.searchDoctors(dept);
            if (results.isEmpty()) { JOptionPane.showMessageDialog(frame, "Bu bölümde doktor bulunamadı."); return; }

            String[] names = new String[results.size()];
            for (int i=0;i<results.size();i++) names[i]=results.get(i).getName()+" "+results.get(i).getSurname()+" ("+results.get(i).getBranch()+")";
            String sel = (String) JOptionPane.showInputDialog(frame, "Doktor seçin:", "Seçim", JOptionPane.PLAIN_MESSAGE, null, names, names[0]);
            if (sel == null) return;

            int idx = java.util.Arrays.asList(names).indexOf(sel);
            Doctor chosen = results.get(idx >= 0 ? idx : 0);

            String date = JOptionPane.showInputDialog(frame, "Randevu Tarihi (YYYY-MM-DD):");
            if (date==null || date.trim().isEmpty()) return;
            try { java.time.LocalDate.parse(date.trim()); }
            catch (Exception ex) { JOptionPane.showMessageDialog(frame, "Tarih formatı hatalı. Lütfen YYYY-MM-DD şeklinde girin."); return; }

            java.util.List<String> slots = appointmentService.getAvailableTimes(chosen, date, 30);
            if (slots.isEmpty()) { JOptionPane.showMessageDialog(frame, "Bu tarihte müsait saat yok."); return; }
            String chosenTime = (String) JOptionPane.showInputDialog(frame, "Saat seçin:", "Saat", JOptionPane.PLAIN_MESSAGE, null, slots.toArray(new String[0]), slots.get(0));
            if (chosenTime==null) return;

            String newId = String.valueOf(System.currentTimeMillis()%10000);
            appointmentService.createAppointment(newId, pat, chosen, date, chosenTime);
            services.Database.getInstance().refreshCache();
            JOptionPane.showMessageDialog(frame, "Randevu oluşturuldu: " + date + " " + chosenTime);
        });

        // "MAMI" HATASI İÇİN GÜVENLİ LİSTELEME
        listApps.addActionListener(ev -> {
            Database.getInstance().refreshCache();
            String today = LocalDate.now().toString();
            StringBuilder sb = new StringBuilder();

            for (Appointment a : Database.getInstance().getAppointments().values()){
                // ID Kontrolü (Güvenli)
                String appPatId = a.getPatient() != null ? a.getPatient().getId() : "";
                if (!appPatId.trim().equals(pat.getId().trim())) continue;

                if (a.getState() instanceof CancelledState || a.getState() instanceof CompletedState || a.getState() instanceof NoShowState) continue;
                if (a.getDate().compareTo(today) < 0) continue;

                sb.append(a.getId()).append(" - ").append(a.getDate()).append(" ").append(a.getTime()).append(" - Dr: ").append(a.getDoctor().getName());
                if (a.getNote() != null && !a.getNote().isEmpty()) sb.append(" [Not: ").append(a.getNote()).append("]");
                sb.append("\n");
            }
            JOptionPane.showMessageDialog(frame, sb.length()==0?"Gelecek randevunuz yok":sb.toString());
        });

        cancelApp.addActionListener(ev -> {
            Database.getInstance().refreshCache();
            java.util.List<Appointment> apps = new java.util.ArrayList<>();
            for (Appointment a : Database.getInstance().getAppointments().values()) {
                String appPatId = a.getPatient() != null ? a.getPatient().getId() : "";
                if (appPatId.trim().equals(pat.getId().trim())) {
                    if (!(a.getState() instanceof CancelledState)) {
                        apps.add(a);
                    }
                }
            }
            if (apps.isEmpty()) { JOptionPane.showMessageDialog(frame, "İptal edilecek aktif randevunuz yok."); return; }
            String[] choices = new String[apps.size()];
            for (int i=0;i<apps.size();i++) {
                Appointment a = apps.get(i);
                choices[i] = a.getId() + " - " + a.getDate() + " " + a.getTime() + " - Dr. " + a.getDoctor().getName();
            }
            String sel = (String) JOptionPane.showInputDialog(frame, "İptal etmek istediğiniz randevuyu seçin:", "Randevu Seç", JOptionPane.PLAIN_MESSAGE, null, choices, choices[0]);
            if (sel == null) return;
            int idx = java.util.Arrays.asList(choices).indexOf(sel);
            Appointment chosen = apps.get(idx);
            boolean ok = appointmentService.cancelAppointmentByUser(chosen.getId(), pat);
            if (!ok) JOptionPane.showMessageDialog(frame, "İptal başarısız veya yetkiniz yok.");
            else JOptionPane.showMessageDialog(frame, "Randevu iptal edildi. Bildirim gönderildi.");
        });

        updateContact.addActionListener(ev -> {
            JPanel container = new JPanel(new BorderLayout(6,6));
            JPanel form = new JPanel(new GridLayout(0,2,6,6));
            form.add(new JLabel("Telefon:"));
            JTextField phoneField = new JTextField(pat.getPhone() == null ? "" : pat.getPhone());
            form.add(phoneField);
            JCheckBox editEmail = new JCheckBox("Email'i Değiştir");
            form.add(editEmail);
            JTextField emailField = new JTextField(pat.getEmail() == null ? "" : pat.getEmail());
            emailField.setEnabled(false);
            form.add(emailField);
            container.add(form, BorderLayout.CENTER);
            editEmail.addItemListener(il -> emailField.setEnabled(editEmail.isSelected()));

            int result = JOptionPane.showConfirmDialog(frame, container, "İletişim Bilgilerini Güncelle", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (result != JOptionPane.OK_OPTION) return;

            String newPhone = phoneField.getText().trim();
            String newEmail = emailField.getText().trim();
            String phoneArg = newPhone.equals(pat.getPhone() == null ? "" : pat.getPhone()) ? null : newPhone;
            String emailArg = null;
            if (editEmail.isSelected()) emailArg = newEmail.equals(pat.getEmail() == null ? "" : pat.getEmail()) ? null : newEmail;

            appointmentService.updateContactInfo(pat, phoneArg, emailArg);
            JOptionPane.showMessageDialog(frame, "Güncellendi.");
        });

        changePass.addActionListener(ev -> {
            String oldp = JOptionPane.showInputDialog(frame, "Eski şifre:");
            String newp = JOptionPane.showInputDialog(frame, "Yeni şifre:");
            appointmentService.changePassword(pat, oldp, newp);
            JOptionPane.showMessageDialog(frame, "Tamam.");
        });

        rescheduleApp.addActionListener(ev -> {
            Database.getInstance().refreshCache();
            java.util.List<Appointment> apps = new java.util.ArrayList<>();
            for (Appointment a : Database.getInstance().getAppointments().values()) {
                String appPatId = a.getPatient() != null ? a.getPatient().getId() : "";
                if (appPatId.trim().equals(pat.getId().trim())) {
                    if (!(a.getState() instanceof CancelledState)) apps.add(a);
                }
            }
            if (apps.isEmpty()) { JOptionPane.showMessageDialog(frame, "Güncellenecek randevunuz yok."); return; }
            String[] choices = new String[apps.size()];
            for (int i=0;i<apps.size();i++) {
                Appointment a = apps.get(i);
                choices[i] = a.getId() + " - " + a.getDate() + " " + a.getTime() + " - Dr. " + a.getDoctor().getName();
            }
            String sel = (String) JOptionPane.showInputDialog(frame, "Güncellenecek randevuyu seçin:", "Randevu Seç", JOptionPane.PLAIN_MESSAGE, null, choices, choices[0]);
            if (sel == null) return;
            int idx = java.util.Arrays.asList(choices).indexOf(sel);
            Appointment a = apps.get(idx);
            if (!a.getPatient().getId().trim().equals(pat.getId().trim())) { JOptionPane.showMessageDialog(frame, "Bu randevuyu değiştirmeye yetkiniz yok."); return; }

            String date = JOptionPane.showInputDialog(frame, "Yeni Tarih (YYYY-MM-DD):", a.getDate());
            if (date==null || date.trim().isEmpty()) return;
            java.util.List<String> slots = appointmentService.getAvailableTimes(a.getDoctor(), date, 30);
            if (slots.isEmpty()) { JOptionPane.showMessageDialog(frame, "Bu tarihte müsait saat yok."); return; }
            String chosenTime = (String) JOptionPane.showInputDialog(frame, "Yeni saat seçin:", "Saat", JOptionPane.PLAIN_MESSAGE, null, slots.toArray(new String[0]), slots.get(0));
            if (chosenTime==null) return;
            boolean ok = appointmentService.rescheduleAppointmentByUser(a.getId(), date, chosenTime, pat);
            if (ok) JOptionPane.showMessageDialog(frame, "Randevu başarıyla güncellendi. Bildirim gönderildi.");
            else JOptionPane.showMessageDialog(frame, "Randevu güncellenemedi.");
        });

        historyBtn.addActionListener(ev -> {
            Database.getInstance().refreshCache();
            StringBuilder sb = new StringBuilder();
            String today = LocalDate.now().toString();
            java.util.List<Appointment> historyApps = new java.util.ArrayList<>();
            for (Appointment a : Database.getInstance().getAppointments().values()){
                String appPatId = a.getPatient() != null ? a.getPatient().getId() : "";
                if (!appPatId.trim().equals(pat.getId().trim())) continue;
                boolean isPast = a.getDate().compareTo(today) < 0;
                if (a.getState() instanceof CancelledState || a.getState() instanceof CompletedState || a.getState() instanceof NoShowState || isPast) {
                    historyApps.add(a);
                }
            }
            historyApps.sort((a1, a2) -> {
                String t1 = (a1.getState() instanceof CancelledState && a1.getCancelledAt() != null) ? a1.getCancelledAt() : a1.getDate() + "T" + a1.getTime();
                String t2 = (a2.getState() instanceof CancelledState && a2.getCancelledAt() != null) ? a2.getCancelledAt() : a2.getDate() + "T" + a2.getTime();
                return t2.compareTo(t1);
            });
            for (Appointment a : historyApps) {
                sb.append(a.getId()).append(" - ").append(a.getDate()).append(" ").append(a.getTime()).append(" - Dr: ").append(a.getDoctor().getName()).append(" - Durum: ").append(a.getState().getClass().getSimpleName());
                if (a.getNote() != null && !a.getNote().isEmpty()) sb.append(" [Not: ").append(a.getNote()).append("]");
                sb.append("\n");
            }
            JOptionPane.showMessageDialog(frame, sb.length()==0?"Geçmiş veya iptal bulunamadı":sb.toString());
        });
    }

    private void showDoctorPanel(Doctor doc) {
        frame.setSize(500, 400);
        frame.setLocationRelativeTo(null);

        JPanel p = new JPanel(new BorderLayout());
        JPanel top = new JPanel(new FlowLayout());
        top.add(new JLabel("Hoşgeldiniz Dr. " + doc.getName()));
        JButton logout = new JButton("Çıkış"); top.add(logout);
        p.add(top, BorderLayout.NORTH);

        JPanel center = new JPanel(new GridLayout(0,1, 5, 5));
        center.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JButton listApps = new JButton("Randevularımı Listele");
        JButton cancelled = new JButton("İptal Edilenleri Gör");
        JButton markDone = new JButton("Randevu Durumunu Güncelle");
        JButton addNote = new JButton("Muayene Notu Ekle");
        JButton setHours = new JButton("Çalışma Saatlerini Güncelle");
        center.add(listApps); center.add(cancelled); center.add(markDone); center.add(addNote); center.add(setHours);
        p.add(center, BorderLayout.CENTER);

        frame.getContentPane().removeAll();
        frame.getContentPane().add(p);
        frame.revalidate(); frame.repaint();

        logout.addActionListener(ev -> {
            frame.setSize(360, 250);
            frame.setLocationRelativeTo(null);
            showLogin();
        });

        listApps.addActionListener(ev -> {
            StringBuilder sb = new StringBuilder();
            for (Appointment a : appointmentService.getAppointmentsBetween(doc, "2020-01-01", "2030-12-31")){
                sb.append(a.getId()).append(" - ").append(a.getDate()).append(" ").append(a.getTime()).append(" - Hasta: ").append(a.getPatient().getName()).append("\n");
            }
            JOptionPane.showMessageDialog(frame, sb.length()==0?"Randevu yok":sb.toString());
        });

        cancelled.addActionListener(ev -> {
            StringBuilder sb = new StringBuilder();
            for (Appointment a : appointmentService.getCancelledAppointmentsForDoctor(doc)){
                sb.append(a.getId()).append(" - ").append(a.getDate()).append(" ").append(a.getTime()).append(" - Hasta: ").append(a.getPatient().getName()).append("\n");
            }
            JOptionPane.showMessageDialog(frame, sb.length()==0?"İptal yok":sb.toString());
        });

        markDone.addActionListener(ev -> {
            java.util.List<Appointment> apps = appointmentService.getAppointmentsBetween(doc, "2020-01-01", "2030-12-31");
            if (apps.isEmpty()) { JOptionPane.showMessageDialog(frame, "Güncellenecek randevunuz yok."); return; }
            String[] choices = new String[apps.size()];
            for (int i=0;i<apps.size();i++) {
                Appointment a = apps.get(i);
                choices[i] = a.getId() + " - " + a.getDate() + " " + a.getTime() + " - Hasta: " + a.getPatient().getName();
            }
            String selApp = (String) JOptionPane.showInputDialog(frame, "Durumunu değiştireceğiniz randevuyu seçin:", "Randevu Seç", JOptionPane.PLAIN_MESSAGE, null, choices, choices[0]);
            if (selApp == null) return;
            int idx = java.util.Arrays.asList(choices).indexOf(selApp);
            Appointment chosen = apps.get(idx);
            String[] opts = {"Tamamlandı","Gelmedi"};
            int sel = JOptionPane.showOptionDialog(frame, "Seçin:", "Durum", JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, opts, opts[0]);
            if (sel==0) appointmentService.markCompletedByDoctor(chosen.getId(), doc);
            else if (sel==1) appointmentService.markNoShowByDoctor(chosen.getId(), doc);
            JOptionPane.showMessageDialog(frame, "Güncellendi. Bildirim gönderildi.");
        });

        addNote.addActionListener(ev -> {
            java.util.List<Appointment> apps = appointmentService.getAppointmentsBetween(doc, "2020-01-01", "2030-12-31");
            if (apps.isEmpty()) { JOptionPane.showMessageDialog(frame, "Not eklenecek randevunuz yok."); return; }
            String[] choices = new String[apps.size()];
            for (int i=0;i<apps.size();i++) {
                Appointment a = apps.get(i);
                choices[i] = a.getId() + " - " + a.getDate() + " " + a.getTime() + " - Hasta: " + a.getPatient().getName();
            }
            String selApp = (String) JOptionPane.showInputDialog(frame, "Not eklenecek randevuyu seçin:", "Randevu Seç", JOptionPane.PLAIN_MESSAGE, null, choices, choices[0]);
            if (selApp == null) return;
            int idx = java.util.Arrays.asList(choices).indexOf(selApp);
            Appointment chosen = apps.get(idx);
            String note = JOptionPane.showInputDialog(frame, "Not:");
            if (note == null) return;
            appointmentService.addNoteByDoctor(chosen.getId(), note, doc);
            JOptionPane.showMessageDialog(frame, "Not eklendi. Hasta bilgilendirildi.");
        });

        setHours.addActionListener(ev -> {
            String sh = JOptionPane.showInputDialog(frame, "Başlangıç saati (saat, 0-23):", doc.getStartHour());
            String eh = JOptionPane.showInputDialog(frame, "Bitiş saati (saat, 1-24):", doc.getEndHour());
            try {
                int s = Integer.parseInt(sh);
                int e = Integer.parseInt(eh);
                if (s < 0 || s > 23 || e <= 0 || e > 24 || s >= e) {
                    JOptionPane.showMessageDialog(frame, "Geçersiz saat aralığı.");
                    return;
                }
                doc.setStartHour(s);
                doc.setEndHour(e);
                services.Database.getInstance().saveDoctor(doc);
                JOptionPane.showMessageDialog(frame, "Çalışma saatleri güncellendi: " + s + ":00 - " + e + ":00");
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(frame, "Geçersiz giriş.");
            }
        });
    }

    public static void main(String[] args){
        Database.getInstance();
        SwingUtilities.invokeLater(() -> new MainWindow());
    }
}