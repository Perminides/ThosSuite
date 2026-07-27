package app.shared.model;

/**
 * Die Maße einer Dashboard-Kachel.
 *
 * <p>Die Höhe ist bereits aufsummiert (oberer plus unterer Teil) — der Skin teilt sie intern für
 * das CSS, nach außen interessiert nur die Gesamthöhe.</p>
 */
public record DashboardTileStyle(double width, double height) {}
