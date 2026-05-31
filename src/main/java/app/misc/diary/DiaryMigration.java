package app.misc.diary;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

/**
 * Einmal-Migration: Liest Einträge aus der alten diary-Tabelle und schreibt sie
 * in die neuen Tabellen diary_entry, diary_tag, diary_entry_tag.
 * 
 * Sortiert nach logged aufsteigend, bricht bei unerwartetem Zustand ab.
 * Überspringt Einträge mit "Bauchumfang" oder "Gewicht" als einzigem Tag.
 */
public class DiaryMigration {

    // ---- HIER PFADE EINTRAGEN ----
    private static final String OLD_DB_PATH = "jdbc:sqlite:C:/Users/Markgraf/OneDrive/Geographie Suite/DB/all.db";
    private static final String NEW_DB_PATH = "jdbc:sqlite:C:/Users/Markgraf/OneDrive/ThosSuite/data/thossuite.db";

    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;

    public static void main(String[] args) throws Exception {
        try (Connection oldConn = DriverManager.getConnection(OLD_DB_PATH);
             Connection newConn = DriverManager.getConnection(NEW_DB_PATH)) {

            newConn.setAutoCommit(false);

            int migrated = 0;
            int skipped = 0;

            try (PreparedStatement readStmt = oldConn.prepareStatement(
                    "SELECT logged, dankbar, bemerkungen FROM diary ORDER BY logged ASC");
                 ResultSet rs = readStmt.executeQuery()) {

                while (rs.next()) {
                    String logged = rs.getString("logged");
                    String dankbar = rs.getString("dankbar");
                    String bemerkungen = rs.getString("bemerkungen");

                    // Validierung
                    if (logged == null || logged.isBlank()) {
                        throw new RuntimeException("Leerer Timestamp bei Eintrag #" + (migrated + skipped + 1));
                    }
                    if (dankbar == null || dankbar.isBlank()) {
                        throw new RuntimeException("Leeres dankbar-Feld bei Eintrag: " + logged);
                    }
                    if (bemerkungen == null || bemerkungen.isBlank()) {
                        throw new RuntimeException("Leeres bemerkungen-Feld bei Eintrag: " + logged);
                    }

                    // Tags parsen
                    List<String> tags = Arrays.stream(dankbar.split(","))
                            .map(String::trim)
                            .filter(t -> !t.isEmpty())
                            .toList();

                    // Bauchumfang/Gewicht überspringen
                    if (tags.size() == 1 && (tags.get(0).equalsIgnoreCase("Bauchumfang")
                            || tags.get(0).equalsIgnoreCase("Gewicht"))) {
                        skipped++;
                        System.out.println("SKIP: " + logged + " (" + dankbar + ")");
                        continue;
                    }

                    // Timestamp sekundengenau
                    LocalDateTime timestamp = LocalDateTime.parse(logged, TIMESTAMP_FORMAT)
                            .withNano(0);
                    LocalDate entryDate = timestamp.toLocalDate();

                    // Bei Duplikat: Sekunde draufzählen
                    String tsString = findFreeTimestamp(newConn, timestamp);

                    // Entry speichern
                    try (PreparedStatement ps = newConn.prepareStatement(
                            "INSERT INTO diary_entry (created_at, entry_date, text) VALUES (?, ?, ?)")) {
                        ps.setString(1, tsString);
                        ps.setString(2, entryDate.format(DATE_FORMAT));
                        ps.setString(3, bemerkungen.trim());
                        ps.executeUpdate();
                    }

                    // Tags speichern
                    for (String tag : tags) {
                        try (PreparedStatement ps = newConn.prepareStatement(
                                "INSERT OR IGNORE INTO diary_tag (name) VALUES (?)")) {
                            ps.setString(1, tag);
                            ps.executeUpdate();
                        }

                        try (PreparedStatement ps = newConn.prepareStatement(
                                "INSERT INTO diary_entry_tag (entry_created_at, tag_name) VALUES (?, ?)")) {
                            ps.setString(1, tsString);
                            ps.setString(2, tag);
                            ps.executeUpdate();
                        }
                    }

                    migrated++;
                    System.out.println("OK: " + tsString + " | Tags: " + tags + " | " 
                            + bemerkungen.substring(0, Math.min(50, bemerkungen.length())) + "...");
                }
            }

            newConn.commit();
            System.out.println("\n=== Migration abgeschlossen ===");
            System.out.println("Migriert: " + migrated);
            System.out.println("Übersprungen: " + skipped);
        }
    }

    private static String findFreeTimestamp(Connection conn, LocalDateTime base) throws SQLException {
        LocalDateTime candidate = base;
        while (true) {
            String tsString = candidate.format(TIMESTAMP_FORMAT);
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT 1 FROM diary_entry WHERE created_at = ?")) {
                ps.setString(1, tsString);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        return tsString;
                    }
                }
            }
            candidate = candidate.plusSeconds(1);
        }
    }
}