package search;

import models.AbstractUser;

import java.util.List;

public interface SearchStrategy {
    List<AbstractUser> search(String keyword);
}
