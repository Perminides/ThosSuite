package app.weekday.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

import app.shared.DB;
import app.weekday.model.WeekdayStats;

public class WeekdayRepository {
	
    public boolean playedToday() {
        String sql = "SELECT 1 FROM weekday WHERE day = ? LIMIT 1";
        Connection con = DB.getConnection();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, LocalDate.now().toString());
            ResultSet rs = ps.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            throw new RuntimeException("Fehler beim Laden der Weekday-Stats", e);
        }
    }

    public void save(LocalDate puzzleDate, int secondsNeeded) {
        String sql = "INSERT INTO weekday (day, date, seconds_needed) VALUES (?, ?, ?)";
        Connection con = DB.getConnection();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, LocalDate.now().toString());
            ps.setString(2, puzzleDate.toString());
            ps.setInt(3, secondsNeeded);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Fehler beim Speichern der Weekday-Stats", e);
        }
    }
    
    public WeekdayStats getWeekdayStats(){
        String sql = "SELECT seconds_needed FROM weekday ORDER BY day DESC";
        int currentStreak = 0;
        int maxStreak = 0;
        int runningStreak = 0;
        boolean currentStreakDone = false;

        Connection con = DB.getConnection();
        try (PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int seconds = rs.getInt("seconds_needed");
                if (seconds == -1) {
                    currentStreakDone = true;
                    maxStreak = Math.max(maxStreak, runningStreak);
                    runningStreak = 0;
                } else {
                    runningStreak++;
                    if (!currentStreakDone)
                        currentStreak++;
                }
            }
        } catch (Exception e) {
			throw new RuntimeException("Ui, Probleme beim Holen der WochentagsStats...", e);
		}
        maxStreak = Math.max(maxStreak, runningStreak);

        return new WeekdayStats(currentStreak, maxStreak);
    }
}