package search;

import models.*;
import services.Database;
import java.util.ArrayList;
import java.util.List;

public class DoctorSearch implements SearchStrategy {

    @Override
    public List<AbstractUser> search(String keyword) {
        List<AbstractUser> result = new ArrayList<>();

        for(Doctor d : Database.getInstance().getDoctors().values()){
            if(d.getName().contains(keyword) || d.getBranch().contains(keyword))
                result.add(d);
        }
        return result;
    }
}
