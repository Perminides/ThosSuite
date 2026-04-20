package app.data.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;

/**
 * Datenbankzugriffe für den Signal-Incrementalimport.
 * Liest aus der ThosSuite-DB und schreibt in sie zurück.
 * Die Signal-DB-Verbindung wird in SignalIncrementalImport verwaltet.
 */
public class SignalRepository {

    // -------------------------------------------------------------------------
    // Lesen
    // -------------------------------------------------------------------------

    /**
     * Gibt den sent_at-Timestamp der zuletzt importierten Signal-Nachricht zurück
     * (Millisekunden seit Epoch), oder null wenn noch keine Signal-Nachrichten importiert wurden.
     *
     * Wird als untere Grenze des Importfensters verwendet.
     */
    public Long getLastImportRunTimestamp() {
        String sql = """
                SELECT CAST((strftime('%s', MAX(sent_at)) * 1000) AS INTEGER)
                FROM msg_messages WHERE source = 'signal'
                """;
        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next() && rs.getString(1) != null)
                return rs.getLong(1);
            return null;
        } catch (Exception e) {
            throw new RuntimeException("Fehler beim Ermitteln des letzten Signal-Import-Timestamps", e);
        }
    }

    /**
     * Gibt alle conversationIds zurück, die in msg_chats als blacklisted markiert sind.
     */
    public Set<String> loadBlacklistedConversationIds() {
        String sql = "SELECT raw_identifier FROM msg_chats WHERE source = 'signal' AND blacklisted = 1";
        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            Set<String> result = new HashSet<>();
            while (rs.next())
                result.add(rs.getString("raw_identifier"));
            return result;
        } catch (Exception e) {
            throw new RuntimeException("Fehler beim Laden der Signal-Blacklist", e);
        }
    }

    /**
     * Gibt alle bekannten conversationIds (nicht blacklisted) mit ihrer chat_id zurück.
     */
    public java.util.Map<String, Integer> loadKnownChats() {
        String sql = "SELECT raw_identifier, chat_id FROM msg_chats WHERE source = 'signal' AND blacklisted = 0";
        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            java.util.Map<String, Integer> result = new java.util.LinkedHashMap<>();
            while (rs.next())
                result.put(rs.getString("raw_identifier"), rs.getInt("chat_id"));
            return result;
        } catch (Exception e) {
            throw new RuntimeException("Fehler beim Laden der bekannten Signal-Chats", e);
        }
    }

    /**
     * Gibt alle bekannten serviceIds mit ihrer contact_id zurück.
     */
    public java.util.Map<String, Integer> loadKnownContacts() {
        String sql = "SELECT raw_identifier, contact_id FROM msg_contact_mapping WHERE source = 'signal'";
        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            java.util.Map<String, Integer> result = new java.util.LinkedHashMap<>();
            while (rs.next())
                result.put(rs.getString("raw_identifier"), rs.getInt("contact_id"));
            return result;
        } catch (Exception e) {
            throw new RuntimeException("Fehler beim Laden der bekannten Signal-Kontakte", e);
        }
    }

    // -------------------------------------------------------------------------
    // Schreiben (alle innerhalb einer extern verwalteten Transaktion,
    // außer saveLastImportRunTimestamp — der wird nach dem Commit aufgerufen)
    // -------------------------------------------------------------------------

    /**
     * Prüft ob eine Signal-Nachricht bereits in der Suite-DB vorhanden ist.
     * Wird in der Filterkette genutzt um Duplikate im Überlappungspuffer zu überspringen.
     */
    public boolean isAlreadyImported(Connection thos, String signalMsgId) throws SQLException {
        try (var ps = thos.prepareStatement(
                "SELECT 1 FROM msg_messages WHERE source = 'signal' AND source_id = ?")) {
            ps.setString(1, signalMsgId);
            try (var rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public int insertContact(Connection thos, String displayName) throws SQLException {
        try (var ps = thos.prepareStatement(
                "INSERT INTO msg_contacts (display_name) VALUES (?)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, displayName);
            ps.executeUpdate();
            try (var rs = ps.getGeneratedKeys()) {
                if (!rs.next()) throw new IllegalStateException("[FAILFAST] Kein generated key nach INSERT contact '" + displayName + "'");
                return rs.getInt(1);
            }
        }
    }

    public void insertContactMapping(Connection thos, String serviceId, int contactId) throws SQLException {
        try (var ps = thos.prepareStatement(
                "INSERT INTO msg_contact_mapping (source, raw_identifier, contact_id) VALUES (?, ?, ?)")) {
            ps.setString(1, "signal");
            ps.setString(2, serviceId);
            ps.setInt(3, contactId);
            ps.executeUpdate();
        }
    }

    public int insertChat(Connection thos, String conversationId, boolean isGroup,
                          String displayName, boolean blacklisted) throws SQLException {
        try (var ps = thos.prepareStatement(
                "INSERT INTO msg_chats (source, raw_identifier, is_group, display_name, blacklisted) VALUES (?, ?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, "signal");
            ps.setString(2, conversationId);
            ps.setInt(3, isGroup ? 1 : 0);
            ps.setString(4, displayName);
            ps.setInt(5, blacklisted ? 1 : 0);
            ps.executeUpdate();
            try (var rs = ps.getGeneratedKeys()) {
                if (!rs.next()) throw new IllegalStateException("[FAILFAST] Kein generated key nach INSERT chat '" + displayName + "'");
                return rs.getInt(1);
            }
        }
    }

    public void insertChatMemberIfAbsent(Connection thos, int chatId, int contactId) throws SQLException {
        try (var ps = thos.prepareStatement(
                "INSERT OR IGNORE INTO msg_chat_members (chat_id, contact_id) VALUES (?, ?)")) {
            ps.setInt(1, chatId);
            ps.setInt(2, contactId);
            ps.executeUpdate();
        }
    }

    public void insertMessage(Connection thos, String signalMsgId, String sentAt,
                              int fromContact, int chatId, String body,
                              String resolvedQuoteMsgId) throws SQLException {
        try (var ps = thos.prepareStatement(
                "INSERT INTO msg_messages (source, source_id, sent_at, from_contact, chat_id, content, quote_message_id) VALUES (?, ?, ?, ?, ?, ?, ?)")) {
            ps.setString(1, "signal");
            ps.setString(2, signalMsgId);
            ps.setString(3, sentAt);
            ps.setInt(4, fromContact);
            ps.setInt(5, chatId);
            ps.setString(6, body);
            ps.setString(7, resolvedQuoteMsgId);
            ps.executeUpdate();
        }
    }

    public void insertAttachment(Connection thos, String signalMsgId, String relativePath,
                                 boolean available) throws SQLException {
        try (var ps = thos.prepareStatement(
                "INSERT INTO msg_message_attachment (source, source_id, path, thumb_path, available) VALUES (?, ?, ?, NULL, ?)")) {
            ps.setString(1, "signal");
            ps.setString(2, signalMsgId);
            ps.setString(3, relativePath);
            ps.setInt(4, available ? 1 : 0);
            ps.executeUpdate();
        }
    }
}