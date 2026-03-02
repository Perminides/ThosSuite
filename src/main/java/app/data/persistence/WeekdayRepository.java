package app.data.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

public class WeekdayRepository {

    public boolean playedToday() {
        String sql = "SELECT 1 FROM weekday WHERE day = ? LIMIT 1";
        try (Connection con = DB.getConnection();
        	PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, LocalDate.now().toString());
            ResultSet rs = ps.executeQuery();
            return rs.next();
        } catch (SQLException e) {
	        throw new RuntimeException("Fehler beim Laden der Weekday-Stats", e);
	    }
    }

    public void save(LocalDate puzzleDate, int secondsNeeded) {
        String sql = "INSERT INTO weekday (day, date, seconds_needed) VALUES (?, ?, ?)";
        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, LocalDate.now().toString());
            ps.setString(2, puzzleDate.toString());
            ps.setInt(3, secondsNeeded);
            ps.executeUpdate();
        } catch (SQLException e) {
	        throw new RuntimeException("Fehler beim Speichern der Weekday-Stats", e);
	    }
    }
}