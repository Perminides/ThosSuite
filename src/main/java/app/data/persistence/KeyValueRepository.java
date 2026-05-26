package app.data.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

/**
 * Generisches Repository für persistente Laufzeitwerte in der key_values-Tabelle.
 * <p>
 * Gedacht für Werte die sich zur Laufzeit ändern und über Neustarts hinweg erhalten
 * bleiben sollen, aber nicht in die .properties-Datei gehören — z.B. Zeitpunkte
 * des letzten Imports, Hash-Werte zuletzt verarbeiteter Dateien, oder UI-Präferenzen
 * wie zuletzt gewähltes Skin oder Sortierreihenfolge.
 * </p>
 * <p>
 * Schlüssel-Konvention: Präfix mit Punkt als Trenner, z.B.
 * {@code whatsapp.lastImportHash}, {@code ui.skinClass}, {@code ui.sortOrder}.
 * </p>
 */
public class KeyValueRepository {

	// TODO: Sortierreihenfolge und letztes Skin noch implementieren.
	
    // -------------------------------------------------------------------------
    // Lesen
    // -------------------------------------------------------------------------

    /**
     * Gibt den gespeicherten Wert für den angegebenen Schlüssel zurück,
     * oder {@link Optional#empty()} wenn der Schlüssel nicht existiert.
     */
    public String get(String key) {
        String sql = "SELECT value FROM key_values WHERE key = ?";
        Connection con = DB.getConnection();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, key);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString("value") : null;
            }
        } catch (Exception e) {
            throw new RuntimeException("Fehler beim Lesen von key_values für key='" + key + "'", e);
        }
    }
    
    public Integer getInteger(String key) {
        String value = get(key);
        if (value == null)
            return null;
        return Integer.parseInt(value);
    }

    public LocalDateTime getTime(String key) {
        String value = get(key);
        if (value == null)
            return null;
        return LocalDateTime.parse(value);
    }

    // -------------------------------------------------------------------------
    // Schreiben
    // -------------------------------------------------------------------------

    /**
     * Speichert einen Wert unter dem angegebenen Schlüssel.
     * Existiert der Schlüssel bereits, wird der Wert überschrieben (UPSERT).
     */
    public void set(String key, String value) {
        String sql = "INSERT INTO key_values (key, value) VALUES (?, ?) ON CONFLICT(key) DO UPDATE SET value = excluded.value";
        Connection con = DB.getConnection();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, key);
            ps.setString(2, value);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("Fehler beim Schreiben von key_values für key='" + key + "'", e);
        }
    }
    
    public void setInteger(String key, Integer value) {
    	set (key, String.valueOf(value));
    }
    
    public void setTime(String key, LocalDateTime time) {
    	set (key, time.truncatedTo(ChronoUnit.SECONDS).toString());
    }
    
    public Integer getDaysSince(String key) {
    	return (int) ChronoUnit.DAYS.between(LocalDate.parse(get(key).substring(0, 10)), LocalDate.now());
    }

    /**
     * Löscht den Eintrag für den angegebenen Schlüssel, falls vorhanden.
     */
    public void delete(String key) {
        String sql = "DELETE FROM key_values WHERE key = ?";
        Connection con = DB.getConnection();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, key);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("Fehler beim Löschen von key_values für key='" + key + "'", e);
        }
    }
}