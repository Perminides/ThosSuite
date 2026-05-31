package app.misc.weekday;

import app.config.Config;
import app.data.persistence.DB;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class WeekdayImporter {

    public static void main(String[] args) throws IOException, SQLException {
    	Config.init("C:/Users/Markgraf/OneDrive/ThosSuite/");
        Path file = Path.of("C:/Users/Markgraf/OneDrive/Geographie Suite/Spielstand/WeekdayStats.csv");
        String sql = "INSERT OR IGNORE INTO weekday (day, date, seconds_needed) VALUES (?, ?, ?)";

        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            for (String line : Files.readAllLines(file)) {
                if (line.isBlank()) continue;
                String[] parts = line.split(";");
                ps.setString(1, parts[0]);
                ps.setString(2, parts[1]);
                ps.setInt(3, Integer.parseInt(parts[2]));
                ps.executeUpdate();
            }
        }
        System.out.println("Import abgeschlossen.");
    }
}