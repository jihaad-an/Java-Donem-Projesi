import ui.MainWindow;
import services.Database;

public class RunApp {
    public static void main(String[] args) {
        // Ensure database is initialized
        Database.getInstance();
        
        // Launch GUI
        javax.swing.SwingUtilities.invokeLater(() -> new MainWindow());
    }
}
