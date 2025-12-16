package models;

public class Doctor extends AbstractUser {

    private String branch;

    // YENİ: Çalışma Saatleri (Örn: 9 ve 17)
    private int startHour = 9;
    private int endHour = 17;

    public Doctor(String id, String username, String password, String name, String surname, String tcNo, String branch) {
        super(id, username, password, name, surname, tcNo);
        this.branch = branch;
    }

    public String getBranch(){ return branch; }

    // Çalışma saatlerini yönetmek için Getter/Setter
    public int getStartHour() { return startHour; }
    public void setStartHour(int startHour) { this.startHour = startHour; }

    public int getEndHour() { return endHour; }
    public void setEndHour(int endHour) { this.endHour = endHour; }

    @Override
    public String getRole() {
        return "DOCTOR";
    }
}