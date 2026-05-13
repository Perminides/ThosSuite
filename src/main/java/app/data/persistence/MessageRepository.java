package app.data.persistence;

import java.sql.*;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Datenbankzugriffe auf die ThosSuite-DB für alle Messaging-Quellen (Signal, WhatsApp, …).
 * Jede Methode erwartet die Quellkennung ("signal", "whatsapp") als Parameter.
 * Die Verbindung zur jeweiligen Quell-DB wird in den quellspezifischen SourceRepositories gekapselt.
 */
public class MessageRepository {

    // -------------------------------------------------------------------------
    // Lesen
    // -------------------------------------------------------------------------

    /**
     * Gibt die source_id der zuletzt importierten Nachricht der angegebenen Quelle zurück,
     * oder null wenn noch keine Nachrichten dieser Quelle importiert wurden.
     * <p>
     * Bei Signal wird diese ID genutzt, um in der Signal-DB den zugehörigen sent_at-Timestamp
     * abzuleiten und das Importfenster zu bestimmen.
     * </p>
     */
    public String getLastImportedSourceId(String source) {
        String sql = "SELECT source_id FROM msg_messages WHERE source = ? ORDER BY sent_at DESC LIMIT 1";
        Connection con = DB.getConnection();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, source);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString("source_id") : null;
            }
        } catch (Exception e) {
            throw new RuntimeException("Fehler beim Ermitteln der letzten importierten Nachricht für source='" + source + "'", e);
        }
    }

    /**
     * Gibt den gespeicherten sent_at-Wert (yyyy-MM-dd HH:mm:ss) einer Nachricht
     * anhand ihrer source_id zurück.
     * <p>
     * Wird beim WhatsApp-Import für den Konsistenzcheck genutzt: die höchste bekannte
     * source_id wird mit dem zugehörigen Timestamp verglichen, um sicherzustellen dass
     * ID-Reihenfolge und Timestamp-Reihenfolge übereinstimmen.
     * </p>
     *
     * @throws IllegalStateException [FAILFAST] wenn die sourceId nicht in der Suite-DB gefunden wird
     */
    public String getSentAtForSourceId(String source, String sourceId) {
        String sql = "SELECT sent_at FROM msg_messages WHERE source = ? AND source_id = ?";
        Connection con = DB.getConnection();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, source);
            ps.setString(2, sourceId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next())
                    throw new IllegalStateException("[FAILFAST] source_id='" + sourceId
                        + "' für source='" + source + "' nicht in Suite-DB — Datenkonsistenzproblem");
                return rs.getString("sent_at");
            }
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Fehler beim Laden des Timestamps für source_id='" + sourceId + "'", e);
        }
    }

    /**
     * Gibt alle raw_identifiers zurück, die in msg_chats für die angegebene Quelle
     * als blacklisted markiert sind.
     */
    public Set<String> loadBlacklistedChatIds(String source) {
        String sql = "SELECT raw_identifier FROM msg_chats WHERE source = ? AND blacklisted = 1";
        Connection con = DB.getConnection();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, source);
            try (ResultSet rs = ps.executeQuery()) {
                Set<String> result = new HashSet<>();
                while (rs.next())
                    result.add(rs.getString("raw_identifier"));
                return result;
            }
        } catch (Exception e) {
            throw new RuntimeException("Fehler beim Laden der Blacklist für source='" + source + "'", e);
        }
    }

    /**
     * Gibt alle bekannten, nicht-blacklisted Chats der angegebenen Quelle zurück
     * als Map von raw_identifier → chat_id.
     */
    public Map<String, Integer> loadKnownChats(String source) {
        String sql = "SELECT raw_identifier, chat_id FROM msg_chats WHERE source = ? AND blacklisted = 0";
        Connection con = DB.getConnection();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, source);
            try (ResultSet rs = ps.executeQuery()) {
                Map<String, Integer> result = new LinkedHashMap<>();
                while (rs.next())
                    result.put(rs.getString("raw_identifier"), rs.getInt("chat_id"));
                return result;
            }
        } catch (Exception e) {
            throw new RuntimeException("Fehler beim Laden der bekannten Chats für source='" + source + "'", e);
        }
    }

    /**
     * Gibt alle bekannten Kontakt-Mappings der angegebenen Quelle zurück
     * als Map von raw_identifier → contact_id.
     */
    public Map<String, Integer> loadKnownContacts(String source) {
        String sql = "SELECT raw_identifier, contact_id FROM msg_contact_mapping WHERE source = ?";
        Connection con = DB.getConnection();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, source);
            try (ResultSet rs = ps.executeQuery()) {
                Map<String, Integer> result = new LinkedHashMap<>();
                while (rs.next())
                    result.put(rs.getString("raw_identifier"), rs.getInt("contact_id"));
                return result;
            }
        } catch (Exception e) {
            throw new RuntimeException("Fehler beim Laden der bekannten Kontakte für source='" + source + "'", e);
        }
    }

    /**
     * Prüft ob eine Nachricht der angegebenen Quelle bereits in der Suite-DB vorhanden ist.
     * Wird in der Filterkette genutzt, um Duplikate im Überlappungsbereich zu überspringen.
     */
    public boolean isAlreadyImported(Connection thos, String source, String sourceId) throws SQLException {
        try (var ps = thos.prepareStatement(
                "SELECT 1 FROM msg_messages WHERE source = ? AND source_id = ?")) {
            ps.setString(1, source);
            ps.setString(2, sourceId);
            try (var rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * Gibt die Anzahl der heute importierten Nachrichten aller Quellen zurück.
     */
    public int getMessageCountToday() {
        String sql = "SELECT count(*) FROM msg_messages WHERE date(sent_at) = date('now')";
        Connection con = DB.getConnection();
        try (PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (Exception e) {
            throw new RuntimeException("Fehler beim Laden der Nachrichten von heute.", e);
        }
    }

    // -------------------------------------------------------------------------
    // Schreiben (alle innerhalb einer extern verwalteten Transaktion)
    // -------------------------------------------------------------------------

    /**
     * Legt einen neuen Kontakt an und gibt die generierte contact_id zurück.
     */
    public int insertContact(Connection thos, String displayName) throws SQLException {
        try (var ps = thos.prepareStatement(
                "INSERT INTO msg_contacts (display_name) VALUES (?)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, displayName);
            ps.executeUpdate();
            try (var rs = ps.getGeneratedKeys()) {
                if (!rs.next())
                    throw new IllegalStateException("[FAILFAST] Kein generated key nach INSERT contact '" + displayName + "'");
                return rs.getInt(1);
            }
        }
    }

    /**
     * Verknüpft einen raw_identifier (z.B. serviceId bei Signal, JID bei WhatsApp)
     * mit einem bestehenden Kontakt in msg_contact_mapping.
     */
    public void insertContactMapping(Connection thos, String source, String rawIdentifier,
                                     int contactId) throws SQLException {
        try (var ps = thos.prepareStatement(
                "INSERT INTO msg_contact_mapping (source, raw_identifier, contact_id) VALUES (?, ?, ?)")) {
            ps.setString(1, source);
            ps.setString(2, rawIdentifier);
            ps.setInt(3, contactId);
            ps.executeUpdate();
        }
    }

    /**
     * Legt einen neuen Chat an und gibt die generierte chat_id zurück.
     */
    public int insertChat(Connection thos, String source, String rawIdentifier,
                          boolean isGroup, String displayName, boolean blacklisted) throws SQLException {
        try (var ps = thos.prepareStatement(
                "INSERT INTO msg_chats (source, raw_identifier, is_group, display_name, blacklisted) VALUES (?, ?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, source);
            ps.setString(2, rawIdentifier);
            ps.setInt(3, isGroup ? 1 : 0);
            ps.setString(4, displayName);
            ps.setInt(5, blacklisted ? 1 : 0);
            ps.executeUpdate();
            try (var rs = ps.getGeneratedKeys()) {
                if (!rs.next())
                    throw new IllegalStateException("[FAILFAST] Kein generated key nach INSERT chat '" + displayName + "'");
                return rs.getInt(1);
            }
        }
    }

    /**
     * Trägt einen Kontakt als Chat-Mitglied ein, falls noch nicht vorhanden.
     * Verwendet INSERT OR IGNORE, da Duplikate erwartet werden können.
     */
    public void insertChatMemberIfAbsent(Connection thos, int chatId, int contactId) throws SQLException {
        try (var ps = thos.prepareStatement(
                "INSERT OR IGNORE INTO msg_chat_members (chat_id, contact_id) VALUES (?, ?)")) {
            ps.setInt(1, chatId);
            ps.setInt(2, contactId);
            ps.executeUpdate();
        }
    }

    /**
     * Fügt eine Nachricht in msg_messages ein.
     *
     * @param sourceId           Quellspezifische Nachrichten-ID (Signal: UUID, WhatsApp: _id als String)
     * @param sentAt             Formatierter Zeitstempel (yyyy-MM-dd HH:mm:ss)
     * @param fromContact        contact_id des Absenders
     * @param chatId             chat_id des zugehörigen Chats
     * @param content            Nachrichtentext, darf null sein
     * @param resolvedQuoteMsgId source_id der gequoteten Nachricht, oder null
     */
    public void insertMessage(Connection thos, String source, String sourceId, String sentAt,
                              int fromContact, int chatId, String content,
                              String resolvedQuoteMsgId) throws SQLException {
        try (var ps = thos.prepareStatement(
                "INSERT INTO msg_messages (source, source_id, sent_at, from_contact, chat_id, content, quote_message_id) VALUES (?, ?, ?, ?, ?, ?, ?)")) {
            ps.setString(1, source);
            ps.setString(2, sourceId);
            ps.setString(3, sentAt);
            ps.setInt(4, fromContact);
            ps.setInt(5, chatId);
            ps.setString(6, content);
            ps.setString(7, resolvedQuoteMsgId);
            ps.executeUpdate();
        }
    }

    /**
     * Fügt einen Anhang in msg_message_attachment ein.
     *
     * @param sourceId      Quellspezifische Nachrichten-ID, auf die sich der Anhang bezieht
     * @param relativePath  Relativer Pfad zur Anhangsdatei im konfigurierten Attachment-Verzeichnis
     * @param available     true wenn die Datei physisch vorhanden ist
     */
    public void insertAttachment(Connection thos, String source, String sourceId,
                                 String relativePath, boolean available) throws SQLException {
        try (var ps = thos.prepareStatement(
                "INSERT INTO msg_message_attachment (source, source_id, path, thumb_path, available) VALUES (?, ?, ?, NULL, ?)")) {
            ps.setString(1, source);
            ps.setString(2, sourceId);
            ps.setString(3, relativePath);
            ps.setInt(4, available ? 1 : 0);
            ps.executeUpdate();
        }
    }
    
    /**
     * Gibt alle bekannten Kontakte der angegebenen Quelle zurück
     * als Map von display_name → contact_id.
     * Wird für die Autocomplete-Vorschlagsliste im Kontakt-Dialog genutzt.
     */
    public Map<String, Integer> loadKnownContactsByDisplayName(String source) {
        String sql = """
                SELECT c.display_name, cm.contact_id
                FROM msg_contacts c
                JOIN msg_contact_mapping cm ON cm.contact_id = c.contact_id
                WHERE cm.source = ?
                """;
        Connection con = DB.getConnection();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, source);
            try (ResultSet rs = ps.executeQuery()) {
                Map<String, Integer> result = new LinkedHashMap<>();
                while (rs.next())
                    result.put(rs.getString("display_name"), rs.getInt("contact_id"));
                return result;
            }
        } catch (Exception e) {
            throw new RuntimeException("Fehler beim Laden der Kontakte nach Anzeigename für source='" + source + "'", e);
        }
    }
}