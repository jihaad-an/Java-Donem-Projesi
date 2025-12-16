package factory;

import models.AbstractUser;
import models.Doctor;
import models.Patient;

public class UserFactory {

    public static AbstractUser createUser(String type, String id, String username, String password,
                                          String name, String surname, String tcNo, String extra){
        if(type.equals("PATIENT")){
            return new Patient(id, username, password, name, surname, tcNo);
        } else if(type.equals("DOCTOR")){
            return new Doctor(id, username, password, name, surname, tcNo, extra);
        }
        return null;
    }
}
