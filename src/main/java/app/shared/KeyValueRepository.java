package app.shared;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/**
 * Zugriff auf die key_values-Tabelle: persistente Laufzeitwerte, die sich ueber
 * Neustarts hinweg halten (Import-Zeitpunkte, Datei-Hashes, UI-Praeferenzen wie
 * Sortierreihenfolge oder zuletzt gewaehltes Skin).
 * <p>
 * Speichert und liefert rohe Strings; jede Typumwandlung macht die Config-Fassade.
 * Package-private. Nach aussen spricht nur die Fassade.
 * <p>
 * Kontrakt: throw on miss. Ein unbekannter Key ist ein Bug. Jeder Key wird vor
 * seinem ersten Lauf mit Startwert angelegt (gleiche Disziplin wie bei Config-Keys),
 * darum gibt es kein Optional und keine legitime Abwesenheit.
 */
class KeyValueRepository {

    String get(String key) {
        String sql = "SELECT value FROM key_values WHERE key = ?";
        Connection con = DB.getConnection();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, key);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("value");
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Fehler beim Lesen von key_values fuer key='" + key + "'", e);
        }
        throw new RuntimeException("key_values-Key fehlt: " + key);
    }

    void set(String key, String value) {
        String sql = "INSERT INTO key_values (key, value) VALUES (?, ?) "
                + "ON CONFLICT(key) DO UPDATE SET value = excluded.value";
        Connection con = DB.getConnection();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, key);
            ps.setString(2, value);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("Fehler beim Schreiben von key_values fuer key='" + key + "'", e);
        }
    }

    void delete(String key) {
        String sql = "DELETE FROM key_values WHERE key = ?";
        Connection con = DB.getConnection();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, key);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("Fehler beim Loeschen von key_values fuer key='" + key + "'", e);
        }
    }

    /** Alle Keys der Tabelle. Nur fuer den Startup-Kollisions-Check der Fassade. */
    List<String> allKeys() {
        String sql = "SELECT key FROM key_values";
        Connection con = DB.getConnection();
        List<String> keys = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                keys.add(rs.getString("key"));
            }
        } catch (Exception e) {
            throw new RuntimeException("Fehler beim Lesen der Keys aus key_values", e);
        }
        return keys;
    }
}