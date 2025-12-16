package services; // Paket ismine dikkat et (services içinde olmalı)

import models.AbstractUser;
import observer.Observer; // Eğer Observer interface'ini implement ediyorsa

public class NotificationService {

    // Bu metodu çağırmaya çalışıyordun, o yüzden ekliyoruz
    public void notifyObservers(AbstractUser user, String message) {
        // Kullanıcının kendi update metodunu çağırır
        if (user != null) {
            user.update(message);
        }
    }
}