package utils;

import services.AuthService;
import models.AbstractUser;

public class TestLogin {
    public static void main(String[] args) {
        AuthService auth = new AuthService();
        String user = args.length>0?args[0]:"yusufi";
        String pass = args.length>1?args[1]:"123";
        AbstractUser u = auth.login(user, pass);
        if (u == null) {
            System.out.println("LOGIN_FAILED for input: '"+user+"' / '"+pass+"'");
        } else {
            System.out.println("LOGIN_OK: " + u.getId() + " (" + u.getUsername() + ")");
        }
    }
}
