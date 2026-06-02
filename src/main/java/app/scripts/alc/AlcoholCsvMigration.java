package app.scripts.alc;

import java.io.BufferedReader;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.LocalDate;

import app.shared.Config;
import app.shared.DB;
import app.shared.Log;

/**
 * Einmalige Migration: CSV → alcohol_days Tabelle
 * 
 * Format: 2026-01-17;green
 */
public class AlcoholCsvMigration {
    
    public static void main(String[] args) {
        String csvPath = "C:/Users/Markgraf/OneDrive/Geographie Suite/Spielstand/AlcStats - Migration.csv"; // ANPASSEN!
        Config.init("C:/Users/Markgraf/OneDrive/ThosSuite/");
        migrateFromCsv(csvPath);
    }
    
    public static void migrateFromCsv(String csvPath) {
        String sql = "INSERT INTO alcohol_days (date, status) VALUES (?, ?) " +
                     "ON CONFLICT(date) DO UPDATE SET status = ?";
        
        int imported = 0;
        int errors = 0;
        
        try (Connection conn = DB.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             BufferedReader reader = new BufferedReader(new FileReader(csvPath))) {
            
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                
                try {
                    String[] parts = line.split(";");
                    if (parts.length != 2) {
                        Log.warn("AlcoholCsvMigration", "Ungültige Zeile: " + line);
                        errors++;
                        continue;
                    }
                    
                    LocalDate date = LocalDate.parse(parts[0].trim());
                    String status = parts[1].trim().toUpperCase();
                    
                    // Validierung
                    if (!status.equals("GREEN") && !status.equals("YELLOW") && !status.equals("RED")) {
                        Log.warn("AlcoholCsvMigration", "Ungültiger Status: " + status + " in Zeile: " + line);
                        errors++;
                        continue;
                    }
                    
                    stmt.setString(1, date.toString());
                    stmt.setString(2, status);
                    stmt.setString(3, status);
                    stmt.executeUpdate();
                    
                    imported++;
                    
                } catch (Exception e) {
                    Log.error("AlcoholCsvMigration", "Fehler bei Zeile: " + line, e);
                    errors++;
                }
            }
            
            System.out.println("=== MIGRATION ABGESCHLOSSEN ===");
            System.out.println("Importiert: " + imported + " Einträge");
            System.out.println("Fehler: " + errors);
            
        } catch (Exception e) {
            throw new RuntimeException("Migration fehlgeschlagen", e);
        }
    }
}