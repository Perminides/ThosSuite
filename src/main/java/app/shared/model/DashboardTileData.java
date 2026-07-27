package app.shared.model;

/**
 * Eine Dashboard-Kachel als reine Daten — Zahl oben, Beschriftung unten.
 *
 * <p>Das Grenzobjekt zwischen `controller` und der Oberfläche: der Controller sammelt die Werte aus
 * den Features, die {@code DashboardScreenView} baut daraus die Kacheln. Der Controller fasst dabei
 * keinen javafx-Typ mehr an.</p>
 */
public record DashboardTileData(String value, String label) {}
