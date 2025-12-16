package services;

import models.AbstractUser;
import models.Doctor;
import models.Patient;

public class AuthService {

    public AbstractUser login(String usernameOrEmail, String password) {

        if (usernameOrEmail == null || password == null) {
            return null;
        }

        Database db = Database.getInstance();

        String input = usernameOrEmail.trim();
        String pwd = password.trim();

        // =======================
        // CHECK PATIENTS
        // =======================
        for (Patient p : db.getPatients().values()) {

            String username = p.getUsername() != null ? p.getUsername().trim() : "";
            String email    = p.getEmail()    != null ? p.getEmail().trim()    : "";
            String pass     = p.getPassword() != null ? p.getPassword().trim() : "";

            boolean usernameMatch = !username.isEmpty() && username.equalsIgnoreCase(input);
            boolean emailMatch    = !email.isEmpty()    && email.equalsIgnoreCase(input);
            boolean passwordMatch = pass.equals(pwd);

            if ((usernameMatch || emailMatch) && passwordMatch) {
                return p;
            }
        }

        // =======================
        // CHECK DOCTORS
        // =======================
        for (Doctor d : db.getDoctors().values()) {

            String username = d.getUsername() != null ? d.getUsername().trim() : "";
            String email    = d.getEmail()    != null ? d.getEmail().trim()    : "";
            String pass     = d.getPassword() != null ? d.getPassword().trim() : "";

            boolean usernameMatch = !username.isEmpty() && username.equalsIgnoreCase(input);
            boolean emailMatch    = !email.isEmpty()    && email.equalsIgnoreCase(input);
            boolean passwordMatch = pass.equals(pwd);

            if ((usernameMatch || emailMatch) && passwordMatch) {
                return d;
            }
        }

        return null;
    }
}
