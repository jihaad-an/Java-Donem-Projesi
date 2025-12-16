package ui;

import models.Doctor;
import models.AbstractUser;
import factory.UserFactory;
import services.Database;

import javax.swing.*;
import java.awt.*;

public class DoctorRegistrationDialog extends JDialog {
    private models.Doctor created;

    public DoctorRegistrationDialog(JFrame parent) {
        super(parent, "Doktor Kayıt", true);
        setLayout(new BorderLayout(10, 10));
        
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Header
        JLabel headerLabel = new JLabel("Doktor Kaydı");
        headerLabel.setFont(new Font("Arial", Font.BOLD, 18));
        mainPanel.add(headerLabel, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridLayout(0, 2, 12, 12));
        form.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JTextField username = new JTextField();
        username.setFont(new Font("Arial", Font.PLAIN, 11));
        username.setPreferredSize(new Dimension(200, 30));
        JPasswordField password = new JPasswordField();
        password.setFont(new Font("Arial", Font.PLAIN, 11));
        password.setPreferredSize(new Dimension(200, 30));
        JTextField name = new JTextField();
        name.setFont(new Font("Arial", Font.PLAIN, 11));
        JTextField surname = new JTextField();
        surname.setFont(new Font("Arial", Font.PLAIN, 11));
        JTextField tc = new JTextField();
        tc.setFont(new Font("Arial", Font.PLAIN, 11));
        JTextField phone = new JTextField();
        phone.setFont(new Font("Arial", Font.PLAIN, 11));
        JTextField email = new JTextField();
        email.setFont(new Font("Arial", Font.PLAIN, 11));
        String[] departments = new String[]{
                "Kardiyoloji",
                "Dahiliye",
                "Genel Cerrahi",
                "Göz Hastalıkları",
                "Kulak Burun Boğaz",
                "Ortopedi",
                "Nöroloji",
                "Dermatoloji"
        };
        JComboBox<String> branch = new JComboBox<>(departments);
        branch.setFont(new Font("Arial", Font.PLAIN, 11));
        branch.setPreferredSize(new Dimension(200, 30));
        JTextField startH = new JTextField("9");
        startH.setFont(new Font("Arial", Font.PLAIN, 11));
        JTextField endH = new JTextField("17");
        endH.setFont(new Font("Arial", Font.PLAIN, 11));

        // Basitleştirilmiş: kullanıcı adı, şifre ve branş zorunlu
        form.add(createLabel("Kullanıcı Adı *")); form.add(username);
        form.add(createLabel("Şifre *")); form.add(password);
        form.add(createLabel("Ad")); form.add(name);
        form.add(createLabel("Soyad")); form.add(surname);
        form.add(createLabel("TC No")); form.add(tc);
        form.add(createLabel("Telefon")); form.add(phone);
        form.add(createLabel("Email")); form.add(email);
        form.add(createLabel("Branş *")); form.add(branch);
        form.add(createLabel("Başlangıç Saati")); form.add(startH);
        form.add(createLabel("Bitiş Saati")); form.add(endH);

        mainPanel.add(form, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        JButton ok = new JButton("Kayıt Ol");
        ok.setFont(new Font("Arial", Font.BOLD, 12));
        ok.setPreferredSize(new Dimension(120, 35));
        JButton cancel = new JButton("İptal");
        cancel.setFont(new Font("Arial", Font.BOLD, 12));
        cancel.setPreferredSize(new Dimension(120, 35));
        buttons.add(cancel); buttons.add(ok);
        mainPanel.add(buttons, BorderLayout.SOUTH);
        
        add(mainPanel);

        ok.addActionListener(e -> {
            String u = username.getText().trim();
            String p = new String(password.getPassword());
            String n = name.getText().trim();
            String s = surname.getText().trim();
            String tcNo = tc.getText().trim();
            String ph = phone.getText().trim();
            String em = email.getText().trim();
            String br = (String) branch.getSelectedItem();
            String sh = startH.getText().trim();
            String eh = endH.getText().trim();

            if (u.isEmpty() || p.isEmpty() || br.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Kullanıcı adı, şifre ve branş gerekli.");
                return;
            }
            if (!em.isEmpty() && !em.contains("@")) {
                JOptionPane.showMessageDialog(this, "Geçersiz email adresi.");
                return;
            }
            // benzersiz kontrol
            String uLower = u.toLowerCase();
            String emLower = em.trim().toLowerCase();
            for (AbstractUser ex : Database.getInstance().getUsers().values()){
                String exUser = ex.getUsername() == null ? "" : ex.getUsername().trim().toLowerCase();
                String exEmail = ex.getEmail() == null ? "" : ex.getEmail().trim().toLowerCase();
                if (!exUser.isEmpty() && exUser.equals(uLower)) {
                    JOptionPane.showMessageDialog(this, "Bu kullanıcı adı zaten kullanılıyor.");
                    return;
                }
                if (!emLower.isEmpty() && !exEmail.isEmpty() && exEmail.equals(emLower)) {
                    JOptionPane.showMessageDialog(this, "Bu email zaten kullanılıyor.");
                    return;
                }
            }

            int sHour = 9, eHour = 17;
            try {
                sHour = Integer.parseInt(sh);
                eHour = Integer.parseInt(eh);
                if (sHour < 0 || sHour > 23 || eHour <= 0 || eHour > 24 || sHour >= eHour) {
                    JOptionPane.showMessageDialog(this, "Geçersiz saat aralığı.");
                    return;
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Saatler sayısal olmalı.");
                return;
            }

            String id = String.valueOf(System.currentTimeMillis() % 100000);
            if (n.isEmpty()) n = u;
            if (s.isEmpty()) s = "";
            Doctor dObj = (Doctor) UserFactory.createUser("DOCTOR", id, u.trim(), p, n, s, tcNo, br);
            dObj.setStartHour(sHour);
            dObj.setEndHour(eHour);
            dObj.setPhone(ph);
            dObj.setEmail(em.trim());
            Database.getInstance().saveDoctor(dObj);
            this.created = dObj;
            JOptionPane.showMessageDialog(this, "Doktor kaydı başarılı. Sisteme otomatik giriş yapılıyor.");
            dispose();
        });

        cancel.addActionListener(e -> dispose());

        pack();
        setLocationRelativeTo(parent);
    }

    private JLabel createLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Arial", Font.PLAIN, 11));
        return label;
    }

    public Doctor getCreatedUser(){
        return created;
    }
}
