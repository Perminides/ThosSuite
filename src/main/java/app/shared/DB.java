package app.shared;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;

public class DB {
	
	private static Path dbPath = null;
	private static Path tmdbDbPath = null;
	private static Connection connection = null;
	private static Connection tmdbConnection = null;
	
	static {
		dbPath = Path.of(Config.get("dbFolder") + "thossuite.db");
		tmdbDbPath = Path.of(Config.get("dbFolder") + "movies.db");
	}
	
	/**
	 * Gibt die gemeinsame Singleton-Connection zur ThosSuite-Datenbank zurück.
	 *
	 * Diese Connection wird lazy initialisiert und bei Bedarf neu geöffnet falls sie
	 * geschlossen wurde. Sie ist für den normalen Datenbankbetrieb in der gesamten Suite
	 * gedacht — sowohl für Lesezugriffe als auch für nicht-transaktionale Schreiboperationen.
	 *
	 * Achtung: Da immer dieselbe Connection-Instanz zurückgegeben wird, darf diese
	 * Methode nicht während einer laufenden Transaktion genutzt werden. Für
	 * transaktionale Operationen stattdessen {@link #getNonAutoCommitConnection()} verwenden.
	 *
	 * <p><strong>Wichtig: Diese Connection braucht niemals geschlossen werden.</strong>
	 * Sie ist für die gesamte Laufzeit der Suite offen und wird von allen Repositories
	 * gemeinsam genutzt.
	 *
	 * <p><strong>Wichtig: Alle Statements und ResultSets müssen zwingend per
	 * try-with-resources geschlossen werden.</strong> Ein offenes ResultSet auf dieser
	 * Connection verhindert den Commit auf {@link #getNewConnection()} und
	 * führt zu SQLITE_BUSY. Eclipse erkennt dies nicht automatisch, da die Ressourcen
	 * über Methodenaufrufe geholt werden — die Verantwortung liegt beim Aufrufer.
	 */
	public static Connection getConnection() {
		try {
			if (connection != null && connection.isClosed()) {
				new Alert(AlertType.WARNING, "Shit. Die connection ist closed? Wer ist der Übeltäter?", ButtonType.OK).showAndWait();
			}
			if (connection == null || connection.isClosed()) {
				connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath.toString());
				connection.createStatement().execute("PRAGMA foreign_keys = ON");
			}
		} catch (Exception e) {
			throw new RuntimeException("SQL error while getting connection", e);
		}
		return connection;
	}
	
	/**
	 * Öffnet eine neue, dedizierte Datenbankverbindung mit AutoCommit=false.
	 *
	 * Im Gegensatz zu {@link #getConnection()}, die eine geteilte Singleton-Connection
	 * zurückgibt, liefert diese Methode jedes Mal eine frische Connection.
	 *
	 * Anwendungsfall: Performancekritische Schreiboperationen, die viele Writes in einer
	 * einzigen Transaktion bündeln (z.B. Spielstand über viele Karten speichern).
	 * Der Aufrufer ist verantwortlich für explizites {@code commit()} am Ende sowie
	 * für das Schließen der Connection per try-with-resources.
	 *
	 * Achtung: Offene ResultSets auf {@link #getConnection()} blockieren den Commit.
	 * Alle Statements und ResultSets müssen daher vor dem Commit geschlossen sein.
	 */
	public static Connection getNewConnection() {
		Connection connection = null;
		try {
				connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath.toString());
				connection.setAutoCommit(false);
				connection.createStatement().execute("PRAGMA foreign_keys = ON");
		} catch (Exception e) {
			throw new RuntimeException("SQL error while getting connection", e);
		}
		return connection;
	}
	
	/**
	 * Gibt die gemeinsame Singleton-Connection zur Film-Datenbank zurück.
	 * 
	 **/
	public static Connection getTmdbConnection() {
		try {
			if (tmdbConnection != null && tmdbConnection.isClosed()) {
				new Alert(AlertType.WARNING, "Shit. Die connection ist closed? Wer ist der Übeltäter?", ButtonType.OK).showAndWait();
			}
			if (tmdbConnection == null || tmdbConnection.isClosed())
				tmdbConnection = DriverManager.getConnection("jdbc:sqlite:" + tmdbDbPath.toString());
			tmdbConnection.createStatement().execute("PRAGMA foreign_keys = ON");
		} catch (Exception e) {
			throw new RuntimeException("SQL error while getting connection", e);
		}
		return tmdbConnection;
	}
	
	/**
	 * Öffnet eine neue, dedizierte Verbindung zur Film-Datenbank mit AutoCommit=false.
	 */
	public static Connection getNewTmdbConnection() {
		Connection connection = null;
		try {
				connection = DriverManager.getConnection("jdbc:sqlite:" + tmdbDbPath.toString());
				connection.setAutoCommit(false);
				connection.createStatement().execute("PRAGMA foreign_keys = ON");
		} catch (Exception e) {
			throw new RuntimeException("SQL error while getting connection", e);
		}
		return connection;
	}
	
	public static void closeConnection() {
		try {
		if (connection != null && !connection.isClosed())
			connection.close();
		} catch (SQLException e) {
			throw new RuntimeException("SQL error while closing connection ", e);
		}
	}
}
