package app.tmp;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import app.shared.Config;

/**
 * Die Experiment-DB {@code data/wikidata.db}. Die Tabellen legt Perminides von Hand an.
 *
 * <p>Öffnet ihre Verbindung bewusst selbst statt über {@code DB}: Das Experiment wird wieder
 * ausgebaut und soll dabei nichts in {@code shared} hinterlassen.</p>
 */
class WikidataRepository implements AutoCloseable {

	private final Connection connection;

	WikidataRepository() {
		try {
			connection = DriverManager.getConnection("jdbc:sqlite:" + Config.getPath("dbFolder").resolve("wikidata.db"));
			connection.setAutoCommit(false);
		} catch (SQLException e) {
			throw new RuntimeException("Wikidata-DB nicht zu öffnen", e);
		}
	}

	boolean hasFetchOn(LocalDate day) {
		String sql = "SELECT 1 FROM wikidata_fetch WHERE substr(fetched_at, 1, 10) = ?";
		try (PreparedStatement ps = connection.prepareStatement(sql)) {
			ps.setString(1, day.toString());
			try (ResultSet rs = ps.executeQuery()) {
				return rs.next();
			}
		} catch (SQLException e) {
			throw new RuntimeException("Wikidata-DB: letzter Abruf nicht lesbar", e);
		}
	}

	/** Je Code der Inhalt seiner jüngsten Version; Codes ohne Version fehlen. */
	Map<String, String> loadLatestContents() {
		String sql = "SELECT code, content FROM wikidata_country c "
				+ "WHERE id = (SELECT MAX(id) FROM wikidata_country WHERE code = c.code)";
		Map<String, String> contents = new HashMap<>();
		try (PreparedStatement ps = connection.prepareStatement(sql);
			 ResultSet rs = ps.executeQuery()) {
			while (rs.next())
				contents.put(rs.getString("code"), rs.getString("content"));
		} catch (SQLException e) {
			throw new RuntimeException("Wikidata-DB: Länderversionen nicht lesbar", e);
		}
		return contents;
	}

	/** Der Abruf und seine geänderten Länder in einer Transaktion. */
	void saveSuccessfulFetch(String fetchedAt, long durationMs, Map<String, String> changed) {
		String fetchSql = "INSERT INTO wikidata_fetch (fetched_at, success, duration_ms, changed_countries, error) VALUES (?, 1, ?, ?, NULL)";
		String countrySql = "INSERT INTO wikidata_country (fetch_id, code, content) VALUES (?, ?, ?)";
		try {
			long fetchId;
			try (PreparedStatement ps = connection.prepareStatement(fetchSql, PreparedStatement.RETURN_GENERATED_KEYS)) {
				ps.setString(1, fetchedAt);
				ps.setLong(2, durationMs);
				ps.setInt(3, changed.size());
				ps.executeUpdate();
				try (ResultSet keys = ps.getGeneratedKeys()) {
					keys.next();
					fetchId = keys.getLong(1);
				}
			}
			try (PreparedStatement ps = connection.prepareStatement(countrySql)) {
				for (Map.Entry<String, String> entry : changed.entrySet()) {
					ps.setLong(1, fetchId);
					ps.setString(2, entry.getKey());
					ps.setString(3, entry.getValue());
					ps.addBatch();
				}
				ps.executeBatch();
			}
			connection.commit();
		} catch (SQLException e) {
			rollback();
			throw new RuntimeException("Wikidata-DB: Abruf nicht speicherbar", e);
		}
	}

	void saveFailedFetch(String fetchedAt, long durationMs, String error) {
		String sql = "INSERT INTO wikidata_fetch (fetched_at, success, duration_ms, changed_countries, error) VALUES (?, 0, ?, NULL, ?)";
		try (PreparedStatement ps = connection.prepareStatement(sql)) {
			ps.setString(1, fetchedAt);
			ps.setLong(2, durationMs);
			ps.setString(3, error);
			ps.executeUpdate();
			connection.commit();
		} catch (SQLException e) {
			rollback();
			throw new RuntimeException("Wikidata-DB: Fehlschlag nicht speicherbar", e);
		}
	}

	private void rollback() {
		try {
			connection.rollback();
		} catch (SQLException e) {
			throw new RuntimeException("Wikidata-DB: Rollback fehlgeschlagen", e);
		}
	}

	@Override
	public void close() {
		try {
			connection.close();
		} catch (SQLException e) {
			throw new RuntimeException("Wikidata-DB nicht zu schließen", e);
		}
	}
}
