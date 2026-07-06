package app.shared;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Zentrale Fassade fuer alle Suite-Werte. Eine einzige oeffentliche Tür ueber zwei
 * package-private Stores:
 * <ul>
 *   <li>{@link ConfigFileSource} — unveränderliche Werte aus der config-Datei plus
 *       die daraus abgeleiteten (computed) Pfade.</li>
 *   <li>{@link KeyValueRepository} — veränderliche Laufzeitwerte in der key_values-Tabelle.</li>
 * </ul>
 * Aufrufer wissen nicht, woher ein Wert kommt. Sie lesen und schreiben nur hier.
 * Die Fassade kennt als Einzige die Partition und routet danach:
 * Key in der unveränderlichen Menge (Datei oder computed) -&gt; FileSource, sonst -&gt; key_values.
 * <p>
 * Kontrakt: throw on miss. Schreiben auf einen unveränderlichen Key wirft (FailFast).
 * Jede Typumwandlung passiert hier, einmal und zentral.
 */
public class Config {

    private static ConfigFileSource fileSource;
    private static KeyValueRepository keyValues;

    private Config() {}

    public static void init(String folderPath) {
        fileSource = new ConfigFileSource(folderPath);

        Path dbFolder = Path.of(fileSource.get("dbFolder"));
        DB.init(dbFolder.resolve("thossuite.db"), dbFolder.resolve("movies.db"));

        keyValues = new KeyValueRepository();

        checkCollisions();
    }

    /**
     * Startup-Invariante: kein Tabellen-Key traegt den Namen eines Keys aus der
     * unveraenderlichen Menge. Diese Kollision entsteht erst durch den gemeinsamen
     * Namensraum der Fassade — hier wird sie einmalig bewacht.
     */
    private static void checkCollisions() {
        for (String key : keyValues.allKeys()) {
            if (fileSource.contains(key)) {
                throw new RuntimeException(
                        "Kollision: Key liegt in config-Datei/computed UND in key_values: " + key);
            }
        }
    }

    // -------------------------------------------------------------------------
    // Lesen
    // -------------------------------------------------------------------------

    public static String get(String key) {
        if (fileSource.contains(key)) {
            return fileSource.get(key);
        }
        return keyValues.get(key);
    }

    public static String getString(String key) {
        return get(key);
    }

    /**
     * Optionaler Config-Wert mit Default. Bewusst nur auf der unveraenderlichen Menge:
     * Tabellenwerte sind vorab angelegt und kennen keinen Default.
     */
    public static String get(String key, String defaultValue) {
        if (fileSource.contains(key)) {
            return fileSource.get(key);
        }
        return defaultValue;
    }

    public static int getInt(String key) {
        String value = get(key);
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new RuntimeException("Int angefragt, aber der Wert ist kein Int: " + key + " = " + value, e);
        }
    }

    public static int getInt(String key, int defaultValue) {
        if (fileSource.contains(key)) {
            return getInt(key);
        }
        return defaultValue;
    }

    public static Path getPath(String key) {
        return Path.of(get(key));
    }

    public static LocalDateTime getTime(String key) {
        return LocalDateTime.parse(get(key));
    }

    public static int getDaysSince(String key) {
        String value = get(key);
        LocalDate then = LocalDate.parse(value.substring(0, 10));
        return (int) ChronoUnit.DAYS.between(then, LocalDate.now());
    }

    // -------------------------------------------------------------------------
    // Schreiben (nur veraenderliche Keys; Datei/computed wirft)
    // -------------------------------------------------------------------------

    public static void set(String key, String value) {
        if (fileSource.contains(key)) {
            throw new RuntimeException("Unveraenderlicher Key kann nicht geschrieben werden: " + key);
        }
        keyValues.set(key, value);
    }

    public static void setInt(String key, int value) {
        set(key, String.valueOf(value));
    }

    public static void setTime(String key, LocalDateTime time) {
        set(key, time.truncatedTo(ChronoUnit.SECONDS).toString());
    }

    public static void delete(String key) {
        if (fileSource.contains(key)) {
            throw new RuntimeException("Unveraenderlicher Key kann nicht geloescht werden: " + key);
        }
        keyValues.delete(key);
    }
}