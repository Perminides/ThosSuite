package app.activity.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

import app.activity.model.GoalHistoryEntry;
import app.activity.model.WeekData;
import app.shared.DB;
import app.shared.Log;

/**
 * Persistiert die Tagespunkte und liest Wochen und Wochenziele zurück.
 *
 * <h3>Schema</h3>
 * <pre>
 * activity              (date TEXT PRIMARY KEY, points INTEGER, remark TEXT,
 *                        raw_data TEXT, adjusted_data TEXT)
 * activity_goal_history (valid_from DATE PRIMARY KEY, weekly_goal INTEGER NOT NULL)
 * activity_weekly_points — View, aggregiert activity ab Montag zu Wochen
 * </pre>
 *
 * <p>Wochenpunkte werden nicht gespeichert, sondern im View aggregiert.</p>
 *
 * <p>{@code raw_data} und {@code adjusted_data} hängen direkt am Punkte-Datensatz des Tages —
 * keine eigene Audit-Tabelle, weil es pro Tag genau einen Stand gibt. {@code adjusted_data}
 * bleibt leer, wenn der Review-Dialog nichts verändert hat; leer heißt also "keine Korrektur"
 * und nie "noch nicht reviewt", weil erst nach dem Review geschrieben wird.</p>
 */
public class Repository {

    /**
     * Das Datum des zuletzt importierten Tages.
     *
     * @return das letzte importierte Datum, oder {@code null}, wenn noch nichts importiert wurde
     */
    public LocalDate getLastImportedDate() {
        String sql = "SELECT MAX(date) as last_date FROM activity";
        Connection conn = DB.getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            if (rs.next()) {
                String dateString = rs.getString("last_date");
                if (dateString != null) {
                    LocalDate lastDate = LocalDate.parse(dateString);
                    Log.debug(this, "Letztes importiertes Datum: " + lastDate);
                    return lastDate;
                }
            }

            Log.debug(this, "Keine Aktivitätsdaten in der DB gefunden");
            return null;

        } catch (SQLException e) {
            throw new RuntimeException("Fehler beim Laden des letzten Import-Datums", e);
        }
    }

    /**
     * Schreibt den fertig reviewten Tag. Ein bestehender Eintrag wird ersetzt.
     *
     * @param date         der Tag
     * @param points       die berechneten Punkte
     * @param rawData      die Rohwerte der API als JSON
     * @param adjustedData die im Dialog korrigierten Werte als JSON, oder {@code null},
     *                     wenn nichts korrigiert wurde
     */
    public void saveDay(LocalDate date, int points, String rawData, String adjustedData) {
        String sql = "REPLACE INTO activity (date, points, raw_data, adjusted_data) VALUES (?, ?, ?, ?)";

        Connection conn = DB.getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, date.toString());
            stmt.setInt(2, points);
            stmt.setString(3, rawData);
            stmt.setString(4, adjustedData);

            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected > 0) {
                Log.info(this, "Tag gespeichert: " + date + " → " + points + " Punkte");
            } else {
                throw new RuntimeException("Fehler beim Speichern des Tages " + date);
            }

        } catch (SQLException e) {
            throw new RuntimeException("Fehler beim Speichern des Tages " + date, e);
        }
    }

    /**
     * Das Wochenziel, das am gegebenen Datum galt — der neueste Eintrag, dessen
     * {@code valid_from} nicht hinter dem Datum liegt.
     */
    public int getWeeklyGoalForDate(LocalDate date) {
        String sql = """
            SELECT weekly_goal
            FROM activity_goal_history
            WHERE valid_from <= ?
            ORDER BY valid_from DESC
            LIMIT 1
            """;

        Connection conn = DB.getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, date.toString());
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getInt("weekly_goal");
            } else {
                throw new RuntimeException("Kein Wochenziel gefunden für " + date);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Fehler beim Laden des Wochenziels", e);
        }
    }

    /**
     * Die Punkte der Woche, die das gegebene Datum enthält.
     *
     * @return die Gesamtpunkte dieser Woche, oder 0, wenn für die Woche nichts vorliegt
     */
    public int getPointsForWeek(LocalDate date) {
        LocalDate monday = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        String sql = """
            SELECT points
            FROM activity_weekly_points
            WHERE week_start = ?
            """;

        Connection conn = DB.getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, monday.toString());

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("points");
            } else {
                return 0;
            }

        } catch (SQLException e) {
            throw new RuntimeException("Fehler beim Laden der Wochenpunkte", e);
        }
    }

    /**
     * Alle Wochen im angegebenen Zeitraum, aufsteigend sortiert.
     *
     * @param from Start-Datum (sollte ein Montag sein, wird aber nicht geprüft)
     * @param to   End-Datum (sollte ein Sonntag sein, wird aber nicht geprüft)
     */
    public List<WeekData> getWeeksInRange(LocalDate from, LocalDate to) {
        String sql = """
            SELECT week_start, points, remark
            FROM activity_weekly_points
            WHERE week_start >= ? AND week_start <= ?
            ORDER BY week_start ASC
            """;

        List<WeekData> result = new ArrayList<>();

        Connection conn = DB.getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, from.toString());
            stmt.setString(2, to.toString());

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                LocalDate weekStart = LocalDate.parse(rs.getString("week_start"));
                int points = rs.getInt("points");
                String remark = rs.getString("remark");
                result.add(new WeekData(weekStart, points, remark));
            }

            Log.debug(this, "Geladene Wochen im Zeitraum " + from + " bis " + to + ": " + result.size());
            return result;

        } catch (SQLException e) {
            throw new RuntimeException("Fehler beim Laden der Wochendaten", e);
        }
    }

    /**
     * Die komplette Ziel-Historie, aufsteigend nach {@code validFrom}.
     */
    public List<GoalHistoryEntry> getAllGoalHistory() {
        String sql = """
            SELECT valid_from, weekly_goal
            FROM activity_goal_history
            ORDER BY valid_from ASC
            """;

        List<GoalHistoryEntry> result = new ArrayList<>();

        Connection conn = DB.getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                LocalDate validFrom = LocalDate.parse(rs.getString("valid_from"));
                int weeklyGoal = rs.getInt("weekly_goal");
                result.add(new GoalHistoryEntry(validFrom, weeklyGoal));
            }

            Log.debug(this, "Geladene Ziel-Einträge: " + result.size());
            return result;

        } catch (SQLException e) {
            throw new RuntimeException("Fehler beim Laden der Ziel-Historie", e);
        }
    }
}
