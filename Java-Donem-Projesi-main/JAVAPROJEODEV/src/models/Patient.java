package models;

public class Patient extends AbstractUser {
    public Patient(String id, String username, String password, String name, String surname, String tcNo) {
        super(id, username, password, name, surname, tcNo);
    }

    @Override
    public String getRole() {
        return "PATIENT";
    }
}
