package app.alc.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import app.alc.model.DayEntry;
import app.alc.model.RatioEntry;
import app.alc.model.Status;
import app.shared.AppClock;
import app.shared.DB;
import app.shared.Log;

public class Repository {

    /**
     * Lädt alle Tage im angegebenen Zeitraum mit berechneter Balance.
     * Balance wird on-the-fly berechnet basierend auf historischen Ratios.
     */
	public List<DayEntry> getDaysInRange(LocalDate from, LocalDate to) {
	    List<DayEntry> result = new ArrayList<>();
	    
	    List<RatioEntry> ratios = getAllRatios();
	    
	    // NEU: Balance VOR dem Zeitraum berechnen
	    int balanceBeforeRange = calculateBalanceUntil(from.minusDays(1), ratios);
	    
	    String sql = "SELECT date, status FROM alcohol_days WHERE date >= ? AND date <= ? ORDER BY date ASC";
	    
	    Connection conn = DB.getConnection();
	    try (PreparedStatement stmt = conn.prepareStatement(sql)) {
	        
	        stmt.setString(1, from.toString());
	        stmt.setString(2, to.toString());
	        
	        try (ResultSet rs = stmt.executeQuery()) {
	            int balance = balanceBeforeRange; // Statt 0!
	            
	            while (rs.next()) {
	                LocalDate date = LocalDate.parse(rs.getString("date"));
	                Status status = Status.valueOf(rs.getString("status"));
	                
	                RatioEntry ratio = getRatioForDate(date, ratios);
	                balance += getPointsForStatus(status, ratio);
	                
	                result.add(new DayEntry(date, status, balance));
	            }
	        }
	        
	        Log.debug(this, "Geladen: " + result.size() + " Tage von " + from + " bis " + to + " (Start-Balance: " + balanceBeforeRange + ")");
	        
	    } catch (SQLException e) {
	        throw new RuntimeException("Fehler beim Laden der Alkohol-Tage", e);
	    }
	    
	    return result;
	}
    
    /**
     * Speichert oder aktualisiert den Status für einen Tag.
     */
    public void saveDayStatus(LocalDate date, Status status) {
        String sql = "INSERT INTO alcohol_days (date, status) VALUES (?, ?) " +
                     "ON CONFLICT(date) DO UPDATE SET status = ?";
        
        Connection conn = DB.getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, date.toString());
            stmt.setString(2, status.name());
            stmt.setString(3, status.name());
            
            stmt.executeUpdate();
            Log.debug(this, "Gespeichert: " + date + " -> " + status);
            
        } catch (SQLException e) {
            throw new RuntimeException("Fehler beim Speichern von " + date, e);
        }
    }
    
    /**
     * Holt die aktuell gültige Ratio (für heute).
     */
    public RatioEntry getCurrentRatio() {
        return getRatioForDate(LocalDate.now(), getAllRatios());
    }
    
    /**
     * Lädt alle Ratios aus der DB (sortiert nach valid_from).
     */
    public List<RatioEntry> getAllRatios() {
        List<RatioEntry> result = new ArrayList<>();
        String sql = "SELECT valid_from, green_points, yellow_points, red_points " +
                     "FROM alcohol_ratios ORDER BY valid_from ASC";
        
        Connection conn = DB.getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                LocalDate validFrom = LocalDate.parse(rs.getString("valid_from"));
                int greenPoints = rs.getInt("green_points");
                int yellowPoints = rs.getInt("yellow_points");
                int redPoints = rs.getInt("red_points");
                
                result.add(new RatioEntry(validFrom, greenPoints, yellowPoints, redPoints));
            }
            
            Log.debug(this, "Geladen: " + result.size() + " Ratios");
            
        } catch (SQLException e) {
            throw new RuntimeException("Fehler beim Laden der Ratios", e);
        }
        
        return result;
    }
    
    public LocalDate getLastEntryDate() {
        String sql = "SELECT MAX(date) as last_date FROM alcohol_days";
        
        Connection conn = DB.getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            if (rs.next()) {
                String dateStr = rs.getString("last_date");
                if (dateStr != null) {
                    return LocalDate.parse(dateStr);
                }
            }
            return null;

        } catch (SQLException e) {
            throw new RuntimeException("Fehler beim Ermitteln des letzten Eintrags", e);
        }
    }
    
    /**
     * Gibt die aktuelle Balance zurück (Stand heute).
     */
    public int getCurrentBalance() {
        LocalDate today = AppClock.TODAY;
        return calculateBalanceUntil(today, getAllRatios());
    }
    
    // === HELPER METHODS ===
    
    private int calculateBalanceUntil(LocalDate until, List<RatioEntry> ratios) {
        String sql = "SELECT date, status FROM alcohol_days WHERE date <= ? ORDER BY date ASC";
        Connection conn = DB.getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, until.toString());
            
            try (ResultSet rs = stmt.executeQuery()) {
                int balance = 0;
                
                while (rs.next()) {
                    LocalDate date = LocalDate.parse(rs.getString("date"));
                    Status status = Status.valueOf(rs.getString("status"));
                    
                    RatioEntry ratio = getRatioForDate(date, ratios);
                    balance += getPointsForStatus(status, ratio);
                }
                
                return balance;
            }
            
        } catch (SQLException e) {
            throw new RuntimeException("Fehler beim Berechnen der Balance bis " + until, e);
        }
    }
    
    /**
     * Findet die gültige Ratio für ein bestimmtes Datum.
     * Nimmt die neueste Ratio mit validFrom <= date.
     */
    private RatioEntry getRatioForDate(LocalDate date, List<RatioEntry> ratios) {
        RatioEntry current = null;
        
        for (RatioEntry ratio : ratios) {
            if (!ratio.validFrom().isAfter(date)) {
                current = ratio;
            } else {
                break; // Liste ist sortiert, weiter brauchen wir nicht
            }
        }
        
        if (current == null) {
            throw new RuntimeException("Keine Ratio gefunden für " + date);
        }
        
        return current;
    }
    
    /**
     * Berechnet Punkte für einen Status basierend auf Ratio.
     */
    private int getPointsForStatus(Status status, RatioEntry ratio) {
        return switch (status) {
            case GREEN -> ratio.greenPoints();
            case YELLOW -> ratio.yellowPoints();
            case RED -> -ratio.redPoints(); // Negativ!
        };
    }
}