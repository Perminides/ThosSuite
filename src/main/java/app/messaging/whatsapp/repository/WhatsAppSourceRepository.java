package app.messaging.whatsapp.repository;

import java.sql.*;

import app.shared.ThrowingConsumer;

/**
 * Kapselt alle Lesezugriffe auf die entschlüsselte WhatsApp-DB (read-only).
 * Die Verbindung wird extern verwaltet und hereingereicht.
 */
public class WhatsAppSourceRepository {

    // -------------------------------------------------------------------------
    // Nachrichten
    // -------------------------------------------------------------------------

    /**
     * Iteriert über alle Nachrichten mit _id > lastId OR timestamp > lastTs
     * und ruft den Consumer für jede Row auf.
     * Sortierung: timestamp ASC, _id ASC.
     *
     * <p>Gefiltert wird status@broadcast. Alle weiteren Filter (Type, blacklisted,
     * bereits importiert) erfolgen im Caller.</p>
     *
     * @param lastId  höchste bereits importierte source_id, oder 0 beim ersten Import
     * @param lastTs  timestamp der letzten importierten Nachricht in Unix-ms, oder 0
     */
    public void forEachMessage(Connection wa, long lastId, long lastTs,
                               ThrowingConsumer<ResultSet> consumer) throws Exception {
        String sql =
            "SELECT m._id, m.timestamp, m.from_me, m.message_type, " +
            "       m.origination_flags, m.chat_row_id, m.sender_jid_row_id, " +
            "       CASE " +
            "         WHEN m.message_type = 5  THEN '[Standort wurde geteilt]' " +
            "         WHEN m.message_type = 16 THEN '[Live-Standort wurde geteilt]' " +
            "         ELSE m.text_data " +
            "       END AS content, " +
            "       CASE " +
            "         WHEN m.from_me = 1 THEN 'me' " +
            "         WHEN m.sender_jid_row_id = 0 THEN COALESCE(jmapc.user, j.user) " +
            "         ELSE COALESCE(jmap.user, js.user) " +
            "       END AS from_contact_raw, " +
            "       COALESCE(jmapc.user, j.user) || '@' || COALESCE(jmapc.server, j.server) AS raw_identifier, " +
            "       c.subject, " +
            "       CASE WHEN COALESCE(jmapc.server, j.server) = 'g.us' THEN 1 ELSE 0 END AS is_group, " +
            "       mm.file_path, " +
            "       mm.message_url, " +
            "       mq.key_id AS quote_key_id " +
            "FROM message m " +
            "JOIN chat c ON m.chat_row_id = c._id " +
            "JOIN jid j ON c.jid_row_id = j._id " +
            "LEFT JOIN jid_map jmc ON jmc.lid_row_id = j._id " +
            "LEFT JOIN jid jmapc ON jmc.jid_row_id = jmapc._id " +
            "LEFT JOIN jid js ON m.sender_jid_row_id = js._id " +
            "LEFT JOIN jid_map jm ON jm.lid_row_id = m.sender_jid_row_id " +
            "LEFT JOIN jid jmap ON jm.jid_row_id = jmap._id " +
            "LEFT JOIN message_media mm ON mm.message_row_id = m._id " +
            "LEFT JOIN message_quoted mq ON mq.message_row_id = m._id " +
            "WHERE (m._id > ? OR m.timestamp > ?) " +
            "AND COALESCE(jmapc.user, j.user) || '@' || COALESCE(jmapc.server, j.server) != 'status@broadcast' " +
            "ORDER BY m.timestamp ASC, m._id ASC";

        try (var ps = wa.prepareStatement(sql)) {
            ps.setLong(1, lastId);
            ps.setLong(2, lastTs);
            try (var rs = ps.executeQuery()) {
                while (rs.next())
                    consumer.accept(rs);
            }
        }
    }

    // -------------------------------------------------------------------------
    // Quote-Auflösung
    // -------------------------------------------------------------------------

    /**
     * Löst einen Quote zweistufig auf:
     * <ol>
     *   <li>Direkt via key_id → message._id</li>
     *   <li>Fallback via chat_row_id + timestamp (bekanntes WhatsApp-Phänomen
     *       bei bearbeiteten Nachrichten. Bei denen ist die key_id nicht mehr auffindbar)</li>
     * </ol>
     *
     * @return source_id der gequoteten Nachricht als String
     * @throws IllegalStateException [FAILFAST] wenn beide Stufen fehlschlagen,
     *                               oder wenn der Fallback mehrere Treffer liefert
     */
    public String resolveQuote(Connection wa, String quoteKeyId, long quotingSourceId,
                               int chatRowId, long timestamp) throws SQLException {
        // Stufe 1: direkt via key_id
        try (var ps = wa.prepareStatement("SELECT _id FROM message WHERE key_id = ?")) {
            ps.setString(1, quoteKeyId);
            try (var rs = ps.executeQuery()) {
                if (rs.next()) return String.valueOf(rs.getLong("_id"));
            }
        }

        // Stufe 2: Fallback via chat_row_id + timestamp
        try (var ps = wa.prepareStatement(
                "SELECT _id FROM message WHERE chat_row_id = ? AND timestamp = ?")) {
            ps.setInt(1, chatRowId);
            ps.setLong(2, timestamp);
            try (var rs = ps.executeQuery()) {
                if (rs.next()) {
                    String found = String.valueOf(rs.getLong("_id"));
                    if (rs.next())
                        throw new IllegalStateException(
                            "[FAILFAST] Quote-Fallback via chat_row_id+timestamp liefert mehrere Treffer: " +
                            "chatRowId=" + chatRowId + " timestamp=" + timestamp);
                    return found;
                }
            }
        }

        throw new IllegalStateException(
            "[FAILFAST] Quote nicht auflösbar via key_id und chat_row_id+timestamp: " +
            "quoteKeyId=" + quoteKeyId + " (quotingSourceId=" + quotingSourceId + ")");
    }
}