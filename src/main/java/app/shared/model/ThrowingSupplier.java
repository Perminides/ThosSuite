package app.shared.model;

/** Ein Lieferant, der werfen darf — Gegenstück zu {@link ThrowingConsumer}. */
public interface ThrowingSupplier<T> {
    T get() throws Exception;
}
