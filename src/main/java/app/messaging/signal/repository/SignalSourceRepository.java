package app.messaging.signal.repository;


import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import app.messaging.signal.model.AttachmentInfo;
import app.messaging.signal.model.ContactInfo;
import app.messaging.signal.model.ConversationInfo;
import app.shared.ThrowingConsumer;

/**
 * Kapselt alle Lesezugriffe auf die externe Signal-DB (read-only).
 * Die Verbindung wird extern verwaltet und hereingereicht.
 */
public class SignalSourceRepository {

    // -------------------------------------------------------------------------
    // Importfenster
    // -------------------------------------------------------------------------

    /**
     * Gibt den sent_at-Wert (Millisekunden seit Epoch) der angegebenen source_id zurück.
     * Ist die source_id null (noch kein Import) → wird 0 zurückgegeben.
     * Ist die source_id nicht in der Signal-DB → FailFast.
     */
    private long getSentAtForSourceId(Connection signal, String sourceId) throws SQLException {
        if (sourceId == null) return 0L;
        try (var ps = signal.prepareStatement("SELECT sent_at FROM messages WHERE id = ?")) {
            ps.setString(1, sourceId);
            try (var rs = ps.executeQuery()) {
                if (!rs.next())
                    throw new IllegalStateException("[FAILFAST] Letzte importierte source_id='" + sourceId
                        + "' nicht in Signal-DB — Datenkonsistenzproblem");
                return rs.getLong("sent_at");
            }
        }
    }

    // -------------------------------------------------------------------------
    // Nachrichten
    // -------------------------------------------------------------------------

    /**
     * Iteriert über alle Nachrichten nach der letzten bekannten Nachricht bis cutoffMs
     * und ruft den Consumer auf. Der 5-Minuten-Puffer nach unten stellt sicher dass
     * Nachrichten mit demselben sent_at wie die letzte importierte Nachricht nicht
     * übersehen werden. Filterung nach Typ, isErased und Inhalt erfolgt im Caller.
     *
     * @param lastSourceId  source_id der letzten importierten Nachricht, oder null beim ersten Import
     * @param cutoffMs      obere Grenze (exklusiv), Millisekunden seit Epoch
     */
    public void forEachMessageInWindow(Connection signal, String lastSourceId, long cutoffMs,
                                ThrowingConsumer<ResultSet> consumer) throws Exception {
        long fromSentAt = getSentAtForSourceId(signal, lastSourceId);
        String sql = """
                SELECT id, conversationId, type, body, sent_at, sourceServiceId, isErased,
                       json_extract(json, '$.quote')           AS quote_json,
                       json_extract(json, '$.quote.messageId') AS quote_message_id,
                       json_extract(json, '$.quote.id')        AS quote_id
                FROM messages
                WHERE sent_at > ? - 300000 AND sent_at < ?
                ORDER BY sent_at ASC
                """;
        try (var ps = signal.prepareStatement(sql)) {
            ps.setLong(1, fromSentAt);
            ps.setLong(2, cutoffMs);
            try (var rs = ps.executeQuery()) {
                while (rs.next())
                    consumer.accept(rs);
            }
        }
    }

    // -------------------------------------------------------------------------
    // Chats
    // -------------------------------------------------------------------------

    /**
     * Liest Metadaten einer Conversation aus der Signal-DB.
     * Existiert sie nicht → FailFast.
     */
    public ConversationInfo loadConversation(Connection signal, String conversationId) throws SQLException {
        try (var ps = signal.prepareStatement("""
                SELECT type, name, profileName, profileFamilyName, profileFullName,
                       json_extract(json, '$.messageCount')     AS messageCount,
                       json_extract(json, '$.sharedGroupNames') AS sharedGroupNames
                FROM conversations WHERE id = ?
                """)) {
            ps.setString(1, conversationId);
            try (var rs = ps.executeQuery()) {
                if (!rs.next())
                    throw new IllegalStateException("[FAILFAST] conversationId='" + conversationId + "' nicht in Signal-conversations");
                return new ConversationInfo(
                    "group".equals(rs.getString("type")),
                    rs.getString("name"),
                    rs.getString("profileName"),
                    rs.getString("profileFamilyName"),
                    rs.getString("profileFullName"),
                    rs.getInt("messageCount"),
                    rs.getString("sharedGroupNames")
                );
            }
        }
    }

    /**
     * Liest Kontaktdaten anhand der serviceId aus der Signal-DB.
     * Existiert sie nicht → FailFast.
     */
    public ContactInfo loadContact(Connection signal, String serviceId) throws SQLException {
        try (var ps = signal.prepareStatement("""
                SELECT name, profileName, profileFamilyName, profileFullName
                FROM conversations WHERE serviceId = ?
                """)) {
            ps.setString(1, serviceId);
            try (var rs = ps.executeQuery()) {
                if (!rs.next())
                    throw new IllegalStateException("[FAILFAST] serviceId='" + serviceId + "' nicht in Signal-conversations");
                return new ContactInfo(
                    rs.getString("name"),
                    rs.getString("profileName"),
                    rs.getString("profileFamilyName"),
                    rs.getString("profileFullName")
                );
            }
        }
    }

    // -------------------------------------------------------------------------
    // Attachments
    // -------------------------------------------------------------------------

    /**
     * Gibt alle Attachments einer Nachricht zurück (nur version=2, path und localKey vorhanden).
     */
    public List<AttachmentInfo> loadAttachments(Connection signal, String signalMsgId) throws SQLException {
        List<AttachmentInfo> results = new ArrayList<>();
        try (var ps = signal.prepareStatement("""
                SELECT path, localKey, fileName, size, contentType, attachmentType
                FROM message_attachments
                WHERE messageId = ? AND path IS NOT NULL AND localKey IS NOT NULL AND version = 2
                """)) {
            ps.setString(1, signalMsgId);
            try (var rs = ps.executeQuery()) {
                while (rs.next())
                    results.add(new AttachmentInfo(
                        rs.getString("path"),
                        rs.getString("localKey"),
                        rs.getString("fileName"),
                        rs.getInt("size"),
                        rs.getString("contentType"),
                        rs.getString("attachmentType")
                    ));
            }
        }
        return results;
    }

    // -------------------------------------------------------------------------
    // Quote-Auflösung
    // -------------------------------------------------------------------------

    /**
     * Sucht eine Nachricht anhand ihrer UUID. Gibt null zurück wenn nicht gefunden.
     */
    public String resolveQuoteByMessageId(Connection signal, String messageId) throws SQLException {
        try (var ps = signal.prepareStatement("SELECT id FROM messages WHERE id = ?")) {
            ps.setString(1, messageId);
            try (var rs = ps.executeQuery()) {
                return rs.next() ? rs.getString("id") : null;
            }
        }
    }

    /**
     * Sucht eine Nachricht anhand des sent_at-Timestamps.
     * Matcht mehr als eine → FailFast.
     */
    public String resolveQuoteByTimestamp(Connection signal, long quoteId) throws SQLException {
        try (var ps = signal.prepareStatement("SELECT id FROM messages WHERE sent_at = ?")) {
            ps.setLong(1, quoteId);
            try (var rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                String found = rs.getString("id");
                if (rs.next())
                    throw new IllegalStateException("[FAILFAST] quote.id=" + quoteId + " matcht mehrere Rows in messages.sent_at");
                return found;
            }
        }
    }

    /**
     * Sucht eine Nachricht anhand eines Timestamps in der EditHistory.
     */
    public String resolveQuoteByEditHistory(Connection signal, long quoteId) throws SQLException {
        try (var ps = signal.prepareStatement("""
                SELECT m.id
                FROM messages m, json_each(m.json, '$.editHistory') eh
                WHERE json_extract(eh.value, '$.timestamp') = ?
                """)) {
            ps.setLong(1, quoteId);
            try (var rs = ps.executeQuery()) {
                return rs.next() ? rs.getString("id") : null;
            }
        }
    }
}