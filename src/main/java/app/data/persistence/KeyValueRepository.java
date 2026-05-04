package app.data.persistence;

import java.sql.*;
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

    // -------------------------------------------------------------------------
    // Lesen
    // -------------------------------------------------------------------------

    /**
     * Gibt den gespeicherten Wert für den angegebenen Schlüssel zurück,
     * oder {@link Optional#empty()} wenn der Schlüssel nicht existiert.
     */
    public Optional<String> get(String key) {
        String sql = "SELECT value FROM key_values WHERE key = ?";
        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, key);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(rs.getString("value")) : Optional.empty();
            }
        } catch (Exception e) {
            throw new RuntimeException("Fehler beim Lesen von key_values für key='" + key + "'", e);
        }
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
        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, key);
            ps.setString(2, value);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("Fehler beim Schreiben von key_values für key='" + key + "'", e);
        }
    }

    /**
     * Löscht den Eintrag für den angegebenen Schlüssel, falls vorhanden.
     */
    public void delete(String key) {
        String sql = "DELETE FROM key_values WHERE key = ?";
        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, key);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("Fehler beim Löschen von key_values für key='" + key + "'", e);
        }
    }
}