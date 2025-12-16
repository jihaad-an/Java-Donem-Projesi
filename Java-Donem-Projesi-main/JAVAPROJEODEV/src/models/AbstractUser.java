package models;

import observer.Observer;

public abstract class AbstractUser implements Observer {
    private String id;
    private String username;
    private String password;
    private String name;
    private String surname;
    private String tcNo;

    // İletişim Bilgileri
    private String phone;
    private String email;

    public AbstractUser(String id, String username, String password, String name, String surname, String tcNo) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.name = name;
        this.surname = surname;
        this.tcNo = tcNo;
        // Null pointer hatası almamak için boş başlatıyoruz
        this.phone = "";
        this.email = "";
    }

    // --- GETTER & SETTER ---
    public String getId(){ return id; }
    public String getUsername(){ return username; }

    public String getPassword(){ return password; }
    public void setPassword(String password){ this.password = password; } // Şifre değiştirme için

    public String getName(){ return name; }
    public String getSurname(){ return surname; }
    public String getTcNo(){ return tcNo; }

    public String getPhone(){ return phone; }
    public void setPhone(String phone){ this.phone = phone; }

    public String getEmail(){ return email; }
    public void setEmail(String email){ this.email = email; }

    @Override
    public void update(String message){
        System.out.println("BİLDİRİM (" + username + "): " + message);
    }

    public abstract String getRole();
}