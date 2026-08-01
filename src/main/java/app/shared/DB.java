package app.shared;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;

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
	 *
	 * <p>Das {@code Statement} für das PRAGMA bleibt bewusst ungeschlossen: {@code PRAGMA foreign_keys}
	 * ist ein Setter und liefert kein ResultSet, es gibt also keinen offenen Cursor und damit keinen
	 * {@code SQLITE_BUSY}-Fall. Es läuft einmal je Verbindungsaufbau und stirbt mit der Connection.</p>
	 */
	public static Connection getConnection() {
		try {
			if (connection != null && connection.isClosed()) {
				// Aktuell darf nicht von ganz oben aus shared in den Skin gegriffen werden. Das ist
				// ja aber für Alerts ein bisschen unglücklich, oder? Wobei es hier gerade das einzige
				// Mal wirklich stört. Ich habe aber auch keine bessere Idee.
				// TODO: Dieser Alert bleibt ungestylt, weil er keinen Owner setzt und damit die
				// Hauptscene (und deren Stylesheet) nicht erbt. Gilt genauso für die vier rohen
				// Alerts in ThosSuiteApp. Beim Start ist das teils unvermeidbar — da existiert die
				// Hauptscene noch nicht. Hier läuft die Suite aber schon; ob man die späteren Fälle
				// auf Alerts.show(…) umstellt, ist offen.
				Alert alert = new Alert(AlertType.WARNING);
				alert.setContentText("Shit. Die connection ist closed? Wer ist der Übeltäter?");
				alert.showAndWait();
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
				// Aktuell darf nicht von ganz oben in den Skin gegriffen werden
				Alert alert = new Alert(AlertType.WARNING);
				alert.setContentText("Shit. Die connection ist closed? Wer ist der Übeltäter?");
				alert.showAndWait();
			}
			if (tmdbConnection == null || tmdbConnection.isClosed()) {
				tmdbConnection = DriverManager.getConnection("jdbc:sqlite:" + tmdbDbPath.toString());
				tmdbConnection.createStatement().execute("PRAGMA foreign_keys = ON");
			}
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
		if (tmdbConnection != null && !tmdbConnection.isClosed())
			tmdbConnection.close();
		} catch (SQLException e) {
			throw new RuntimeException("SQL error while closing connection ", e);
		}
	}
}
