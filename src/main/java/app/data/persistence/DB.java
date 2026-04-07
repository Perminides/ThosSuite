package app.data.persistence;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import app.config.Config;

public class DB {
	
	private static Connection connection = null;

	/**
	 * Gibt die gemeinsame Singleton-Connection zur ThosSuite-Datenbank zurück.
	 *
	 * Diese Connection wird lazy initialisiert und bei Bedarf neu geöffnet falls sie
	 * geschlossen wurde. Sie ist für den normalen Datenbankbetrieb in der gesamten Suite
	 * gedacht — sowohl für Lesezugriffe als auch für nicht-transaktionale Schreiboperationen.
	 *
	 * Achtung: Da immer dieselbe Connection-Instanz zurückgegeben wird, darf diese
	 * Methode nicht während einer laufenden Transaktion genutzt werden. Für
	 * transaktionale Operationen stattdessen {@link #getNewConnection()} verwenden.
	 */
	public static Connection getConnection() {
		try {
			if (connection == null || connection.isClosed())
				connection = DriverManager.getConnection("jdbc:sqlite:" + Config.get("dbFolder") + "thossuite.db");
		} catch (Exception e) {
			throw new RuntimeException("SQL error while getting connection", e);
		}
		return connection;
	}
	
	/**
	 * Öffnet eine neue, dedizierte Datenbankverbindung.
	 *
	 * Im Gegensatz zu {@link #getConnection()}, die eine geteilte Singleton-Connection
	 * zurückgibt, liefert diese Methode jedes Mal eine frische Connection.
	 *
	 * Anwendungsfall: Transaktionale Operationen, die eine exklusive Connection benötigen,
	 * die nicht mit anderen Datenbankzugriffen geteilt wird. Der Aufrufer ist vollständig
	 * verantwortlich für das Schließen der Connection, idealerweise per try-with-resources.
	 *
	 * Die Singleton-Connection aus {@link #getConnection()} darf während einer laufenden
	 * Transaktion nicht parallel genutzt werden, da Lesezugriffe aus dem Repository
	 * dieselbe Connection zurückbekommen und sie beim Schließen des Statements
	 * unbeabsichtigt beenden können.
	 */
	public static Connection getNewConnection() {
		Connection connection = null;
		try {
				connection = DriverManager.getConnection("jdbc:sqlite:" + Config.get("dbFolder") + "thossuite.db");
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
