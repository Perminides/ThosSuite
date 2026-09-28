package app.shared;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DB {
	
	private static Path dbPath = null;
	private static Path tmdbDbPath = null;
	private static Connection connection = null;
	private static Connection tmdbConnection = null;
	
	public static void init (Path dbPath, Path tmdbDbPath) {
		DB.dbPath = dbPath;
		DB.tmdbDbPath = tmdbDbPath;
	}
	
	/**
	 * Gibt die gemeinsame Singleton-Connection zur ThosSuite-Datenbank zurück.
	 *
	 * Diese Connection wird lazy initialisiert und bleibt danach die gesamte Laufzeit offen.
	 * Sie ist für den normalen Datenbankbetrieb in der gesamten Suite
	 * gedacht — sowohl für Lesezugriffe als auch für nicht-transaktionale Schreiboperationen.
	 *
	 * Achtung: Da immer dieselbe Connection-Instanz zurückgegeben wird, darf diese
	 * Methode nicht während einer laufenden Transaktion genutzt werden. Für
	 * transaktionale Operationen stattdessen {@link #getNewConnection()} verwenden.
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
	 *
	 * <p>Das {@code Statement} für das PRAGMA bleibt bewusst ungeschlossen: {@code PRAGMA foreign_keys}
	 * ist ein Setter und liefert kein ResultSet, es gibt also keinen offenen Cursor und damit keinen
	 * {@code SQLITE_BUSY}-Fall. Es läuft einmal je Verbindungsaufbau und stirbt mit der Connection.</p>
	 */
	public static Connection getConnection() {
		try {
			if (connection != null && connection.isClosed())
				throw new IllegalStateException("[FAILFAST] Die Suite-Connection ist geschlossen. "
						+ "Sie bleibt die gesamte Laufzeit offen und wird nur beim Shutdown geschlossen — "
						+ "hier hat sie also jemand geschlossen, der es nicht durfte.");
			if (connection == null)
				connection = open(dbPath, true);
		} catch (SQLException e) {
			throw new RuntimeException("Suite-Connection: Zustand nicht prüfbar", e);
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
		return open(dbPath, false);
	}
	
	/**
	 * Gibt die gemeinsame Singleton-Connection zur Film-Datenbank zurück.
	 * 
	 **/
	public static Connection getTmdbConnection() {
		try {
			if (tmdbConnection != null && tmdbConnection.isClosed())
				throw new IllegalStateException("[FAILFAST] Die Film-Connection ist geschlossen. "
						+ "Sie bleibt die gesamte Laufzeit offen und wird nur beim Shutdown geschlossen — "
						+ "hier hat sie also jemand geschlossen, der es nicht durfte.");
			if (tmdbConnection == null)
				tmdbConnection = open(tmdbDbPath, true);
		} catch (SQLException e) {
			throw new RuntimeException("Film-Connection: Zustand nicht prüfbar", e);
		}
		return tmdbConnection;
	}
	
	/**
	 * Öffnet eine neue, dedizierte Verbindung zur Film-Datenbank mit AutoCommit=false.
	 */
	public static Connection getNewTmdbConnection() {
		return open(tmdbDbPath, false);
	}

	/**
	 * Die einzige Stelle, an der eine Verbindung entsteht — damit {@code PRAGMA foreign_keys = ON}
	 * nicht viermal dasteht und eine fünfte Verbindungsart sie nicht vergessen kann. Ein fehlendes
	 * PRAGMA meldet sich nämlich nicht, es schaltet still die Fremdschlüsselprüfung ab.
	 *
	 * <p>Das {@code Statement} für das PRAGMA bleibt bewusst ungeschlossen — die Begründung steht
	 * bei {@link #getConnection()}.</p>
	 */
	private static Connection open(Path path, boolean autoCommit) {
		try {
			Connection c = DriverManager.getConnection("jdbc:sqlite:" + path);
			c.setAutoCommit(autoCommit);
			c.createStatement().execute("PRAGMA foreign_keys = ON");
			return c;
		} catch (SQLException e) {
			throw new RuntimeException("Verbindung zu " + path + " fehlgeschlagen", e);
		}
	}
	
	public static void closeConnection() {
		try {
		if (connection != null && !connection.isClosed())
			connection.close();
		if (tmdbConnection != null && !tmdbConnection.isClosed())
			tmdbConnection.close();
		} catch (SQLException e) {
			throw new RuntimeException("SQL error while closing connection ", e);
		}
	}
}
