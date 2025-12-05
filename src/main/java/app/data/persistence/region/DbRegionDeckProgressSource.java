package app.data.persistence.region;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import app.data.AppClock;
import app.data.LearnStat;
import app.data.RegionSessionSpec;
import app.data.persistence.DB;

public class DbRegionDeckProgressSource {

	public DbRegionDeckProgressSource() {
	}

	LearnStat load(RegionSessionSpec sessionSpec) {
		Connection conn = DB.getConnection();
		String sql = "SELECT * FROM region_learn_stat where deck = '" + sessionSpec.getDeckType().getId() + "' and mode = '" + sessionSpec.getMode().name() + "'";
		try {
			Statement statement = conn.createStatement();
			ResultSet rs = statement.executeQuery(sql);
			if (!rs.next()) { // Vielleicht ein neues Set und noch nicht alle Modes begonnen...
				statement.close();
				return null;
			}
			LocalDate firstPlayed = LocalDate.parse(rs.getString("first_played"));
			LocalDate lastPlayed = LocalDate.parse(rs.getString("last_played"));
			int level = rs.getInt("level");
			int wrongCount = rs.getInt("wrong_count");
			LearnStat result = new LearnStat(firstPlayed, lastPlayed, level, wrongCount);
			statement.close();
			return result;
		} catch (Exception e) {
			throw new RuntimeException(
					"Ui, ich bekomme die Stats für die RegionsSession nicht: "
			+ sessionSpec.getDeckType().getDisplayName() + " - " + sessionSpec.getMode());
		}
	}
	
	void save (RegionSessionSpec spec, LearnStat stats, boolean correct, String wrongId) {
		Connection conn = DB.getConnection();
        String logSQL = "INSERT INTO region_log (played_timestamp, deck, mode, correct_flag, wrong_region_id) VALUES (?, ?, ?, ?, ?)";
        String learnStatSQL = "INSERT INTO region_learn_stat (deck, mode, first_played, last_played, level, wrong_count) "
        		+ "VALUES (?, ?, ?, ?, ?, ?) ON CONFLICT (deck, mode) DO UPDATE SET "
        		+ "last_played = excluded.last_played, "
        		+ "level = excluded.level, "
        		+ "wrong_count = excluded.wrong_count";
		try {
			conn.setAutoCommit(false); // Unbedingt! 40 Karten mit Autocommit: Schleife 783 ms. Ohne: 11 ms
			PreparedStatement psLog = conn.prepareStatement(logSQL);
			PreparedStatement psLearn = conn.prepareStatement(learnStatSQL);
			psLearn.setString(1, spec.getDeckType().getId());
			psLearn.setString(2, spec.getMode().name());
			psLearn.setString(3, AppClock.TODAY.toString()); // Wird nur im Insert-Fall genutzt
			psLearn.setString(4, AppClock.TODAY.toString());
			psLearn.setInt(5, stats.getCurrentLevel());
			psLearn.setInt(6, stats.getWrongCount());
			psLearn.execute();

			psLog.setString(1, LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS).toString());
			psLog.setString(2, spec.getDeckType().getId());
			psLog.setString(3, spec.getMode().name());
			psLog.setBoolean(4, correct);
			psLog.setString(5, wrongId);
			psLog.execute();

			conn.commit();
			conn.close();
		} catch (Exception e) {
			throw new RuntimeException("Probleme beim Speichern des Fortschritts", e);
		}
	}
}
