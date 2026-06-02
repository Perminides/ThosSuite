package app.learn.anki.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import app.learn.anki.model.Card;
import app.learn.model.Deck;
import app.learn.model.LearnStat;
import app.shared.AppClock;
import app.shared.DB;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;

class DbDeckProgressSource {
		
		DbDeckProgressSource() {
		}

		Map<String, LearnStat> loadAll(Deck type) {
			Connection conn = DB.getConnection();
	        String sql = "SELECT * FROM card_learn_stat where deck = '" + type.getDisplayName() + "'";
	        Map<String, LearnStat> result = new HashMap<>();
	        try (PreparedStatement ps = conn.prepareStatement(sql);
	             ResultSet rs = ps.executeQuery()) {
	            while (rs.next()) {
	                String id = rs.getString("card_id");
	                LocalDate firstPlayed = LocalDate.parse(rs.getString("first_played"));
	                LocalDate lastPlayed = LocalDate.parse(rs.getString("last_played"));
	                int level = rs.getInt("level");
	                int wrongCount = rs.getInt("wrong_count");
	                result.put(id, new LearnStat(firstPlayed, lastPlayed, level, wrongCount));
	            }
	        } catch (SQLException e) {
	            throw new RuntimeException("Fehler beim Lesen der Hint-Progress-Daten", e);
	        }
	        return result;
	    }
		
		void saveLearned(Deck type, List<Card> cardsFromSession) {
		    String logSQL = "INSERT INTO card_log (deck, card_id, played_timestamp, correct_flag) VALUES (?, ?, ?, ?)";
		    String learnStatSQL = "INSERT INTO card_learn_stat (deck, card_id, first_played, last_played, level, wrong_count) "
		            + "VALUES (?, ?, ?, ?, ?, ?) ON CONFLICT (deck, card_id) DO UPDATE SET "
		            + "last_played = excluded.last_played, "
		            + "level = excluded.level, "
		            + "wrong_count = excluded.wrong_count";
		    try (Connection conn = DB.getNewConnection(); // Unbedingt! 40 Karten mit Autocommit: Schleife 783 ms. Ohne: 11 ms
		         PreparedStatement psLog = conn.prepareStatement(logSQL);
		         PreparedStatement psLearn = conn.prepareStatement(learnStatSQL)) {
		        for (Card card : cardsFromSession) {
		            if (card.getProgress().isCorrectlyAnswered() == null)
		                continue;

		            psLearn.setString(1, type.getDisplayName());
		            psLearn.setInt(2, card.getId());
		            psLearn.setString(3, AppClock.TODAY.toString()); // Wird nur im Insert-Fall genutzt
		            psLearn.setString(4, AppClock.TODAY.toString());
		            psLearn.setInt(5, card.getLearnStat().getCurrentLevel());
		            psLearn.setInt(6, card.getLearnStat().getWrongCount());
		            psLearn.execute();

		            psLog.setString(1, type.getDisplayName());
		            psLog.setInt(2, card.getId());
		            psLog.setBoolean(4, card.getProgress().isCorrectlyAnswered());
		            //!Später: Remove this dirty hack and go with millis if this happens more often...
		            boolean saved = false;
		            int attempts = 0;
		            while (!saved && attempts < 5) {
		                try {
		                    psLog.setString(3, card.getProgress().getPlayedTimestamp().truncatedTo(ChronoUnit.SECONDS).minus(attempts, ChronoUnit.SECONDS).toString());
		                    psLog.execute();
		                    saved = true;
		                } catch (SQLException e) {
		                    if (e.getMessage().contains("UNIQUE constraint")) {
		                        attempts++;
		                    } else {
		                        throw e;
		                    }
		                    new Alert(AlertType.WARNING, psLog.getParameterMetaData() + " - " + card.getId() + " Ich versuche es evtl. nochmal...", ButtonType.OK).showAndWait();
		                }
		            }
		            if (!saved)
		                throw new RuntimeException("Alter, nach 5 Versuchen konnte ich immer noch nicht speichern...");
		        }
		        conn.commit();
		    } catch (Exception e) {
		        throw new RuntimeException("Probleme beim Speichern des Fortschritts", e);
		    }
		}
		
		int getInitialDue(Deck type) {
		    Connection conn = DB.getConnection();
		    String sql = "select count(*) "
		            + "from card_learn_stat "
		            + "where deck = '" + type.getDisplayName() + "'"
		            + "and (date(last_played, '+' || level || ' days') <= '" + AppClock.TODAY + "' "
		            + "or date(last_played) = '" + AppClock.TODAY + "')";
		    try (Statement statement = conn.createStatement();
		         ResultSet rs = statement.executeQuery(sql)) {
		        rs.next();
		        return rs.getInt(1);
		    } catch (Exception e) {
		        throw new RuntimeException("Problem beim Berechnen des initial due counts...");
		    }
		}
}