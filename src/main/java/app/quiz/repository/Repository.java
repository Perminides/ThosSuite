package app.quiz.repository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;

import app.quiz.model.Prompt;
import app.quiz.model.QuizAction;
import app.quiz.model.Question;
import app.quiz.model.Suggestion;
import app.shared.DB;

/**
 * Zugriff auf die Quiz-DB. Befüllt wird sie von einem Import außerhalb der Suite; die Suite liest
 * die Fragen und schreibt nur die Antwort (Status und Zeitpunkt) und Kartenvorschläge.
 */
public class Repository {

	// Ohne festes Muster ließe LocalDateTime.toString() die Sekunden weg, wenn sie null sind
	private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

	private static final String QUESTION_COLUMNS = "q.id, q.category, q.topic, q.question, q.answer";

	/**
	 * Die nächste offene Frage, oder {@code null}, wenn keine mehr offen ist. Fragen mit Vorschlag von
	 * Claude kommen zuerst — über die wurde schon nachgedacht, sie sollen nicht wochenlang liegen —,
	 * innerhalb beider Gruppen zufällig.
	 *
	 * @param excluded Fragen, die in dieser Runde übersprungen wurden
	 */
	public Question nextOpen(Set<Integer> excluded) {
		StringBuilder sql = new StringBuilder("SELECT " + QUESTION_COLUMNS + " FROM question q WHERE q.status = 'open'");
		if (!excluded.isEmpty()) {
			sql.append(" AND q.id NOT IN (?");
			sql.append(", ?".repeat(excluded.size() - 1));
			sql.append(")");
		}
		sql.append(" ORDER BY EXISTS (SELECT 1 FROM card_suggestion s WHERE s.question_id = q.id) DESC, RANDOM() LIMIT 1");
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

	/** Der neueste Vorschlag von Claude zu einer Frage, oder {@code null}, wenn es keinen gibt. */
	public String latestSuggestion(int questionId) {
		String sql = "SELECT text FROM card_suggestion WHERE question_id = ? ORDER BY id DESC LIMIT 1";
		try (PreparedStatement ps = DB.getQuizConnection().prepareStatement(sql)) {
			ps.setInt(1, questionId);
			try (ResultSet rs = ps.executeQuery()) {
				return rs.next() ? rs.getString("text") : null;
			}
		} catch (SQLException e) {
			throw new RuntimeException("Fehler beim Holen des Kartenvorschlags", e);
		}
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

	/** Speichert die Antwort auf eine Frage: neuer Status und der Zeitpunkt der Antwort. */
	public void record(int questionId, QuizAction action) {
		String sql = "UPDATE question SET status = ?, answered_at = ? WHERE id = ?";
		try (PreparedStatement ps = DB.getQuizConnection().prepareStatement(sql)) {
			ps.setString(1, action.dbValue());
			ps.setString(2, now());
			ps.setInt(3, questionId);
			if (ps.executeUpdate() != 1)
				throw new IllegalStateException("[FAILFAST] Quizfrage " + questionId + " nicht gefunden.");
		} catch (SQLException e) {
			throw new RuntimeException("Fehler beim Speichern der Quizantwort", e);
		}
	}

	/**
	 * Speichert einen Kartenvorschlag samt Hinweis. Jeder Vorschlag bekommt eine eigene Zeile; der
	 * neueste gilt. Die Frage bleibt unberührt — ein Vorschlag ist keine Antwort.
	 *
	 * @param instruction der Hinweis an Claude, oder {@code null}
	 */
	public void saveSuggestion(int questionId, Suggestion suggestion, int promptVersion, String instruction) {
		String sql = "INSERT INTO card_suggestion (question_id, created_at, model, prompt_version, text, instruction) VALUES (?, ?, ?, ?, ?, ?)";
		try (PreparedStatement ps = DB.getQuizConnection().prepareStatement(sql)) {
			ps.setInt(1, questionId);
			ps.setString(2, now());
			ps.setString(3, suggestion.model());
			ps.setInt(4, promptVersion);
			ps.setString(5, suggestion.text());
			ps.setString(6, instruction);
			ps.executeUpdate();
		} catch (SQLException e) {
			throw new RuntimeException("Fehler beim Speichern des Kartenvorschlags", e);
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
