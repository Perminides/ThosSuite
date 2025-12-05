package app.data.persistence;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import app.config.Config;

public class DB {
	
	private static Connection connection = null;

	
	public static Connection getConnection() {
		try {
			if (connection == null || connection.isClosed())
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
