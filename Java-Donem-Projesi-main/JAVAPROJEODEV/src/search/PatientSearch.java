package search;

import models.*;
import services.Database;
import java.util.ArrayList;
import java.util.List;

public class PatientSearch implements SearchStrategy {

    @Override
    public List<AbstractUser> search(String keyword) {
        List<AbstractUser> result = new ArrayList<>();

        for(Patient p : Database.getInstance().getPatients().values()){
            if(p.getName().contains(keyword) || p.getSurname().contains(keyword))
                result.add(p);
        }
        return result;
    }
}
