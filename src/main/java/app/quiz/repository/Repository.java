package app.quiz.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import app.quiz.model.PendingCard;
import app.quiz.model.Prompt;
import app.quiz.model.QuizAction;
import app.quiz.model.Question;
import app.quiz.model.Suggestion;
import app.shared.DB;

/**
 * Zugriff auf die Quiz-DB. Befüllt wird sie von einem Import außerhalb der Suite; die Suite liest
 * die Fragen und schreibt nur Status, Log und Kartenvorschläge.
 *
 * <p>Status und Log-Eintrag entstehen immer in derselben Transaktion — der Status ist nur ein
 * Zwischenspeicher, maßgeblich ist das Log.</p>
 */
public class Repository {

	// Ohne festes Muster ließe LocalDateTime.toString() die Sekunden weg, wenn sie null sind
	private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

	private static final String QUESTION_COLUMNS = "q.id, q.category, q.topic, q.question, q.answer";

	/**
	 * Eine zufällige offene Frage, oder {@code null}, wenn keine mehr offen ist.
	 *
	 * @param excluded Fragen, die in dieser Runde übersprungen wurden
	 */
	public Question randomOpen(Set<Integer> excluded) {
		StringBuilder sql = new StringBuilder("SELECT " + QUESTION_COLUMNS + " FROM question q WHERE q.status = 'open'");
		if (!excluded.isEmpty()) {
			sql.append(" AND q.id NOT IN (?");
			sql.append(", ?".repeat(excluded.size() - 1));
			sql.append(")");
		}
		sql.append(" ORDER BY RANDOM() LIMIT 1");
		try (PreparedStatement ps = DB.getQuizConnection().prepareStatement(sql.toString())) {
			int index = 1;
			for (int id : excluded)
				ps.setInt(index++, id);
			try (ResultSet rs = ps.executeQuery()) {
				return rs.next() ? readQuestion(rs) : null;
			}
		} catch (SQLException e) {
			throw new RuntimeException("Fehler beim Holen einer offenen Quizfrage", e);
		}
	}

	/** Fragen, aus denen eine Karte werden soll, mit dem neuesten Vorschlag; die älteste Anforderung zuerst. */
	public List<PendingCard> pendingCards() {
		String sql = "SELECT " + QUESTION_COLUMNS + ","
				+ " (SELECT s.text FROM card_suggestion s WHERE s.question_id = q.id ORDER BY s.id DESC LIMIT 1) AS suggestion"
				+ " FROM question q"
				+ " WHERE q.status IN ('" + QuizAction.CARD_REQUESTED.dbValue() + "', '" + QuizAction.CARD_SELF.dbValue() + "')"
				+ " ORDER BY (SELECT MAX(l.id) FROM question_log l WHERE l.question_id = q.id)";
		List<PendingCard> cards = new ArrayList<>();
		try (PreparedStatement ps = DB.getQuizConnection().prepareStatement(sql);
				ResultSet rs = ps.executeQuery()) {
			while (rs.next())
				cards.add(new PendingCard(readQuestion(rs), rs.getString("suggestion")));
		} catch (SQLException e) {
			throw new RuntimeException("Fehler beim Holen der offenen Karten", e);
		}
		return cards;
	}

	/** Der neueste Prompt. Ohne Prompt in der DB lässt sich kein Vorschlag holen — das ist ein Fehler. */
	public Prompt latestPrompt() {
		String sql = "SELECT version, text FROM prompt ORDER BY version DESC LIMIT 1";
		try (PreparedStatement ps = DB.getQuizConnection().prepareStatement(sql);
				ResultSet rs = ps.executeQuery()) {
			if (!rs.next())
				throw new IllegalStateException("[FAILFAST] Die Tabelle prompt in der Quiz-DB ist leer.");
			return new Prompt(rs.getInt("version"), rs.getString("text"));
		} catch (SQLException e) {
			throw new RuntimeException("Fehler beim Holen des Prompts", e);
		}
	}

	/** Setzt den Status einer Frage und schreibt den Log-Eintrag dazu. */
	public void record(int questionId, QuizAction action) {
		try (Connection con = DB.getNewQuizConnection()) {
			try {
				updateStatusAndLog(con, questionId, action);
				con.commit();
			} catch (SQLException | RuntimeException e) {
				con.rollback();
				throw e;
			}
		} catch (SQLException e) {
			throw new RuntimeException("Fehler beim Speichern der Quizantwort", e);
		}
	}

	/**
	 * Speichert einen Kartenvorschlag samt Hinweis und setzt die Frage auf „Karte angefordert“. Jeder
	 * Vorschlag bekommt eine eigene Zeile; der neueste gilt.
	 *
	 * @param instruction der Hinweis an Claude, oder {@code null}
	 */
	public void saveSuggestion(int questionId, Suggestion suggestion, int promptVersion, String instruction) {
		String sql = "INSERT INTO card_suggestion (question_id, created_at, model, prompt_version, text, instruction) VALUES (?, ?, ?, ?, ?, ?)";
		try (Connection con = DB.getNewQuizConnection()) {
			try {
				try (PreparedStatement ps = con.prepareStatement(sql)) {
					ps.setInt(1, questionId);
					ps.setString(2, now());
					ps.setString(3, suggestion.model());
					ps.setInt(4, promptVersion);
					ps.setString(5, suggestion.text());
					ps.setString(6, instruction);
					ps.executeUpdate();
				}
				updateStatusAndLog(con, questionId, QuizAction.CARD_REQUESTED);
				con.commit();
			} catch (SQLException | RuntimeException e) {
				con.rollback();
				throw e;
			}
		} catch (SQLException e) {
			throw new RuntimeException("Fehler beim Speichern des Kartenvorschlags", e);
		}
	}

	private void updateStatusAndLog(Connection con, int questionId, QuizAction action) throws SQLException {
		try (PreparedStatement ps = con.prepareStatement("UPDATE question SET status = ? WHERE id = ?")) {
			ps.setString(1, action.dbValue());
			ps.setInt(2, questionId);
			if (ps.executeUpdate() != 1)
				throw new IllegalStateException("[FAILFAST] Quizfrage " + questionId + " nicht gefunden.");
		}
		try (PreparedStatement ps = con.prepareStatement(
				"INSERT INTO question_log (question_id, created_at, action) VALUES (?, ?, ?)")) {
			ps.setInt(1, questionId);
			ps.setString(2, now());
			ps.setString(3, action.dbValue());
			ps.executeUpdate();
		}
	}

	private static Question readQuestion(ResultSet rs) throws SQLException {
		return new Question(rs.getInt("id"), rs.getString("category"), rs.getString("topic"),
				rs.getString("question"), rs.getString("answer"));
	}

	private static String now() {
		return LocalDateTime.now().format(TIMESTAMP_FORMAT);
	}
}
