package app.data.persistence;

import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import app.config.Config;
import app.ui.skin.SkinService;
import app.util.Log;

public class DB {
	
	private static Path origDbPath = null;
	private static Path tmpDbPath = null;
	private static Connection connection = null;
	private static Connection nonAutoCommitConnection = null;

	/**
	 * Kopiert die DB ins tmp-Verzeichnis, welches in der config konfiguriert sein muss.
	 * FailFast: Wenn dort bereits eine DB liegt oder das Verzeichnis nicht existiert,
	 * wird abgebrochen.
	 * 
	 * @throws Exception
	 */
	public static void init() throws Exception {
		Path tmpDbFolder = Path.of(Config.get("tmpDB.folder"));
		if (!Files.exists(tmpDbFolder))
			throw new RuntimeException("Der Temp-Ordner für die DB exitiert nicht");
		tmpDbPath = Path.of(tmpDbFolder.toString(), "thossuite.db");
		if (Files.exists(tmpDbPath))
			throw new RuntimeException("Es existiert eine DB im Temp-Ordner. Bitte verschieben oder löschen");
		origDbPath = Path.of(Config.get("dbFolder") + "thossuite.db");
		Files.copy(origDbPath, tmpDbPath, StandardCopyOption.COPY_ATTRIBUTES);
	}
	
	/**
	 * Kopiert die temporäre Datei wieder in den originalen Ordner.
	 * Wenn sich etwas geändert hat zumindest.
	 * 
	 * @throws Exception
	 */
	public static void shutdown() throws Exception {
	    DB.closeConnection();
	    
	    while (true) {
	        try {
	            if (Files.mismatch(tmpDbPath, origDbPath) != -1L)
	                Files.move(tmpDbPath, origDbPath, StandardCopyOption.REPLACE_EXISTING);
	            else
	                Files.delete(tmpDbPath);
	            return;
	        } catch (FileSystemException  e) {
	        	Log.info(DB.class, "Konnte nicht kopieren. SQLiteStudio noch offen?");
	            SkinService.get().createAlert(null, "Datenbank gesperrt",
	                "Bitte SQLiteStudio schließen und dann OK klicken.", false, false).showAndWait();
	        }
	    }
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
				SkinService.get().createAlert(null, "Warnung", "Shit. Die connection ist closed? Wer ist der Übeltäter?", false, false).showAndWait();
			}
			if (connection == null || connection.isClosed())
				connection = DriverManager.getConnection("jdbc:sqlite:" + tmpDbPath.toString());
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
				connection = DriverManager.getConnection("jdbc:sqlite:" + tmpDbPath.toString());
				connection.setAutoCommit(false);
		} catch (Exception e) {
			throw new RuntimeException("SQL error while getting connection", e);
		}
		return connection;
	}
	
	public static void closeConnection() {
		try {
		if (connection != null && !connection.isClosed())
			connection.close();
		if (nonAutoCommitConnection != null && !nonAutoCommitConnection.isClosed())
			nonAutoCommitConnection.close();
		} catch (SQLException e) {
			throw new RuntimeException("SQL error while closing connection ", e);
		}
	}
}
