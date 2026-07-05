package app.tmp;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import app.activity.model.Exercise;
import app.shared.Config;

/**
 * Übergangs-Logger: schreibt die geholten Health-Importdaten eingedampft in eine eigene
 * Datei {@code health_import.log} — eine JSON-Zeile pro Tag, symmetrisch zum Fitbit-Log,
 * damit sich beide nebeneinanderlegen lassen.
 *
 * <p>Pro Aktivität nur die Felder, die eine Rad-km-Diskrepanz erklären können:
 * {@code exerciseType}, {@code km} (bereits umgerechnet), {@code steps}, lokale Startzeit.
 * Ballast (Herzfrequenz, Splits, Herkunft) bleibt draußen.</p>
 *
 * <p><b>Wegwerf.</b> Im September ersetzt durch DB-Spalten am Punkte-Datensatz; diese
 * Datei-Variante fällt dann weg.</p>
 */
public class HealthImportLog {

    private static final ObjectMapper MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

    private final Path logFile = Config.getPath("logFolder").resolve("health_import.log");

    /**
     * Hängt eine Zeile für den Tag an. {@code steps} darf null sein (keine Health-Schritte
     * an dem Tag) — dann steht {@code null} im Log, nicht 0.
     */
    public void write(LocalDate date, Integer steps, List<Exercise> activities) {
        Map<String, Object> line = new LinkedHashMap<>();
        line.put("date", date);
        line.put("steps", steps);

        List<Map<String, Object>> activityList = new ArrayList<>();
        for (Exercise e : activities) {
            Map<String, Object> a = new LinkedHashMap<>();
            a.put("exerciseType", e.exerciseType());
            a.put("km", e.distanceKm());          // null, wenn keine Distanz
            a.put("steps", e.steps());            // null, wenn nicht vorhanden
            a.put("startTime", localStartTime(e));
            activityList.add(a);
        }
        line.put("activities", activityList);

        append(toJson(line));
    }

    /** Lokale Startzeit aus UTC-Zeit + Offset — für ein lesbares, mit dem Fitbit-Log vergleichbares Log. */
    private static OffsetDateTime localStartTime(Exercise e) {
        return e.startTimeUtc().atOffset(ZoneOffset.ofTotalSeconds(e.startUtcOffsetSeconds()));
    }

    private static String toJson(Map<String, Object> line) {
        try {
            return MAPPER.writeValueAsString(line);
        } catch (IOException e) {
            throw new RuntimeException("Fehler beim Serialisieren der Health-Logzeile", e);
        }
    }

    private void append(String jsonLine) {
        try {
            Files.writeString(logFile, jsonLine + System.lineSeparator(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new RuntimeException("Fehler beim Schreiben von " + logFile, e);
        }
    }
}