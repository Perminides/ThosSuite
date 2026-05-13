package app.data.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import app.config.Config;

public class MattressRepository {

    public record MattressTurn(LocalDateTime turnedAt, String direction) {}

    public MattressTurn getLastTurn() {
        String sql = "SELECT turned_at, direction FROM mattress_turns ORDER BY turned_at DESC LIMIT 1";
        Connection con = DB.getConnection();
        try (PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next())
                return new MattressTurn(
                    LocalDateTime.parse(rs.getString("turned_at")),
                    rs.getString("direction"));
            return null;
        } catch (Exception e) {
            throw new RuntimeException("Ui, Probleme beim Laden des letzten Matratzen-Turns", e);
        }
    }

    public void save(LocalDateTime turnedAt, String direction) {
        String sql = "INSERT INTO mattress_turns (turned_at, direction) VALUES (?, ?)";
        Connection con = DB.getConnection();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, turnedAt.withNano(0).toString());
            ps.setString(2, direction);
            ps.executeUpdate();
        }  catch (Exception e) {
			throw new RuntimeException("Ui, Probleme beim Speichern des Matratzen-Turns", e);
		}
    }
    
    public long getDaysUntilNextTurn() {
        MattressTurn last = getLastTurn();
        if (last == null)
            throw new RuntimeException("Keine Matratzen-Einträge vorhanden.");
        return Config.getInt("mattress.dueAfterWeeks", 4) * 7 - ChronoUnit.DAYS.between(last.turnedAt(), LocalDateTime.now());
    }
}