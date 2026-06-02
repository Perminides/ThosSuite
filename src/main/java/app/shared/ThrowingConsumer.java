package app.shared;
public interface ThrowingConsumer<T> {
    void accept(T t) throws Exception;
}