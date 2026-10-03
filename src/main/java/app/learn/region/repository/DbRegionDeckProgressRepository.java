package app.learn.region.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

import app.learn.model.Deck;
import app.learn.model.LearnStat;
import app.learn.region.model.Mode;
import app.learn.region.model.SessionSpec;
import app.shared.AppClock;
import app.shared.DB;

public class DbRegionDeckProgressRepository {

	/**
	 * Die ganze Tabelle, nach Deck und Modus aufgeteilt.
	 *
	 * <p>Wer hier fehlt, wurde noch nie gespielt — und nimmt damit am Lernen nicht teil. Das ist
	 * gewollt: die erste Zeile in {@code region_learn_stat} ist der Schalter, mit dem ein Deck
	 * dazukommt.</p>
	 */
	public Map<Deck, Map<Mode, LearnStat>> loadAll() {
	    Connection conn = DB.getConnection();
	    String sql = "SELECT * FROM region_learn_stat";
	    Map<Deck, Map<Mode, LearnStat>> result = new HashMap<>();
	    try (Statement statement = conn.createStatement();
	         ResultSet rs = statement.executeQuery(sql)) {
	        while (rs.next()) {
	            Deck deck = Deck.fromId(rs.getString("deck"));
	            Mode mode = Mode.valueOf(rs.getString("mode"));
	            LocalDate firstPlayed = LocalDate.parse(rs.getString("first_played"));
	            LocalDate lastPlayed  = LocalDate.parse(rs.getString("last_played"));
	            LearnStat stat = new LearnStat(firstPlayed, lastPlayed, rs.getInt("level"), rs.getInt("wrong_count"));
	            result.computeIfAbsent(deck, _ -> new HashMap<>()).put(mode, stat);
	        }
	    } catch (Exception e) {
	        throw new RuntimeException("Ui, ich bekomme die Stats für die RegionsSessions nicht", e);
	    }
	    return result;
	}
	
	public void save(SessionSpec spec, LearnStat stats, boolean correct, String wrongId) {
	    String logSQL = "INSERT INTO region_log (played_timestamp, deck, mode, correct_flag, wrong_region_id) VALUES (?, ?, ?, ?, ?)";
	    String learnStatSQL = "INSERT INTO region_learn_stat (deck, mode, first_played, last_played, level, wrong_count) "
	            + "VALUES (?, ?, ?, ?, ?, ?) ON CONFLICT (deck, mode) DO UPDATE SET "
	            + "last_played = excluded.last_played, "
	            + "level = excluded.level, "
	            + "wrong_count = excluded.wrong_count";
	    try (Connection conn = DB.getNewConnection(); // Unbedingt! 40 Karten mit Autocommit: Schleife 783 ms. Ohne: 11 ms
	         PreparedStatement psLearn = conn.prepareStatement(learnStatSQL);
	         PreparedStatement psLog = conn.prepareStatement(logSQL)) {
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
	    } catch (Exception e) {
	        throw new RuntimeException("Probleme beim Speichern des Fortschritts", e);
	    }
	}
}
