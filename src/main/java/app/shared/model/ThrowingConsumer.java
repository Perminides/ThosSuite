package app.shared.model;
public interface ThrowingConsumer<T> {
    void accept(T t) throws Exception;
}