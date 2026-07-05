package app.activity.model;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

/**
 * Unveränderliches Abbild einer aufgezeichneten Aktivität aus der Google-Health-API
 * (Datentyp {@code exercise}, Methode {@code list}) — exakt in Googles Einheiten und Form.
 *
 * <p>Trägt nur, was die Punkteberechnung und der Vergleich brauchen. Kein displayName
 * (lokalisiert, mehrdeutig), keine Splits, keine Herzfrequenz.</p>
 *
 * @param exerciseType          stabiler Enum-Wert (z.B. WALKING, BIKING, SPINNING) — das
 *                              Schaltkriterium; NICHT der lokalisierte displayName
 * @param distanceMillimeters   Distanz in Millimetern (Googles Einheit); {@code null}, wenn
 *                              die Aktivität keine Distanz führt (z.B. Spinning)
 * @param steps                 Schritte innerhalb der Aktivität; {@code null}, wenn nicht
 *                              vorhanden (Google lässt das Feld dann weg, statt 0 zu senden)
 * @param startTimeUtc          Startzeitpunkt in UTC
 * @param startUtcOffsetSeconds Offset zur lokalen Zeit in Sekunden (zum Aufzeichnungszeitpunkt)
 */
public record Exercise(
        String exerciseType,
        Long distanceMillimeters,
        Integer steps,
        Instant startTimeUtc,
        int startUtcOffsetSeconds) {

    /**
     * Lokaler Kalendertag der Aktivität. Muss selbst berechnet werden, weil die
     * {@code exercise}-Antwort — anders als Schritte — keine civilStartTime mitliefert.
     * Der Offset wird auf die UTC-Zeit angewendet.
     */
    public LocalDate localDate() {
        return startTimeUtc.atOffset(ZoneOffset.ofTotalSeconds(startUtcOffsetSeconds)).toLocalDate();
    }

    /**
     * Distanz in Kilometern. DIE gekapselte mm→km-Umrechnung — Googles Millimeter naiv
     * weiterzurechnen ergäbe Faktor-Million-Fehler. {@code null}, wenn keine Distanz da ist.
     */
    public Double distanceKm() {
        return distanceMillimeters == null ? null : distanceMillimeters / 1_000_000.0;
    }
}