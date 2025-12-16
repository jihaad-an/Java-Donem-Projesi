package factory;

public abstract class AbstractFactory<T> {
    public abstract T create(String... args);
}
