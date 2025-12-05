package app.data;

import java.time.LocalDate;

public final class AppClock {
    public static final LocalDate TODAY = LocalDate.now();
    private AppClock() {}
    public static void init() {
        // Lädt die Klasse, macht nichts weiter
    }
}