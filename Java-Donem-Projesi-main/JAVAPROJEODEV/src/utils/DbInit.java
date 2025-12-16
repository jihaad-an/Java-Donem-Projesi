import services.Database;

public class DbInit {
    public static void main(String[] args) {
        Database.getInstance();
        System.out.println("DB initialized");
    }
}
