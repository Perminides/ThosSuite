package app.misc.messages;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.PrintStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Dry-Run-Version des WhatsApp-InitialImports zur Diagnose des Attachment-Duplikat-Problems.
 *
 * Änderungen gegenüber WhatsAppInitialImport:
 * - Kein Schreiben in die Suite-DB
 * - Kein Dateisystem-Zugriff (Attachment-Existenz wird nicht geprüft)
 * - Kein Check ob Nachrichten bereits importiert waren
 * - insertAttachment: sofortige Ausgabe wenn dieselbe source_id mehr als einmal auftaucht,
 *   inklusive ob gerade ein Album offen ist und welcher Opener aktiv ist
 * - insertMessage: nur Logging, kein DB-Schreiben
 * - Alle Album-Warnings werden mit vollständigem Kontext ausgegeben
 */
public class WhatsAppInitialImportDryRun {

    private static final String WHATSAPP_DB  = "C:\\Users\\permi\\Desktop\\WhatsApp\\msgstore.db.unencrypted.java";
    private static final String SUITE_DB     = "C:\\Users\\permi\\Documents\\Gedächtnis Lernen und so\\ThosSuite\\data\\thossuite.db";
    private static final String MEDIA_SOURCE = "C:\\Users\\permi\\Desktop\\WhatsApp\\Pixel Media";
    private static final String SOURCE       = "whatsapp";
    private static final String PUA_MAPPING  = "C:\\Users\\permi\\Desktop\\WhatsApp\\old_smileys_mapper.txt";

    /** Nachrichten vor diesem Timestamp werden nicht verarbeitet (Unix ms). */
    private static final long START_TIMESTAMP = 1711214117000L; // anpassen!

    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ------------------------------------------------------------------------------------
    // Laufzeit-State
    // ------------------------------------------------------------------------------------

    private final Map<String, Integer> chatIdByRawIdentifier  = new HashMap<>();
    private final Map<Integer, Boolean> blacklistedByChat      = new HashMap<>();
    private final Map<String, Integer> contactIdByRawIdentifier = new HashMap<>();
    private final Map<Integer, Set<Integer>> chatMembersByChatId = new HashMap<>();
    private final Map<Integer, Boolean> knownTypes             = new HashMap<>();
    private final Map<Integer, String> puaMapping              = new HashMap<>();

    /**
     * Offene Album-Opener pro "chatRowId:senderRaw".
     * Wert: [source_id, timestamp] des Openers.
     */
    private final Map<String, long[]> openAlbumByKey = new HashMap<>();

    /**
     * Tracking aller insertAttachment-Aufrufe pro source_id.
     * source_id → Liste der Pfade (für Duplikat-Erkennung).
     */
    private final Map<Long, List<String>> attachmentCallsById = new HashMap<>();

    /** source_id der Nachricht die gerade verarbeitet wird (für Kontext-Logging). */
    private long currentSourceId;
    private long currentTimestamp;
    private String currentAlbumKey;

    private Connection wa;
    private Connection suite;

    // ------------------------------------------------------------------------------------
    // Main
    // ------------------------------------------------------------------------------------

    public static void main(String[] args) throws Exception {
        new WhatsAppInitialImportDryRun().run();
    }

    private void run() throws Exception {
    	
    	System.setOut(new PrintStream(new FileOutputStream("C:\\Users\\permi\\Desktop\\dryrun.txt")));
    	System.setErr(new PrintStream(new FileOutputStream("C:\\Users\\permi\\Desktop\\dryrun.txt", true)));
    	
        System.out.println("=== WhatsApp Initial Import DRY RUN ===");
        System.out.println();

        wa    = DriverManager.getConnection("jdbc:sqlite:" + WHATSAPP_DB);
        suite = DriverManager.getConnection("jdbc:sqlite:" + SUITE_DB);

        loadPuaMapping();
        loadKnownTypes();
        loadExistingChatsAndContacts();

        System.out.println("Starte DryRun ab Timestamp: " + START_TIMESTAMP + " (" + formatTs(START_TIMESTAMP) + ")");
        System.out.println();

        processMessages();

        wa.close();
        suite.close();
        System.out.println();
        System.out.println("=== DryRun abgeschlossen ===");
    }

    // ------------------------------------------------------------------------------------
    // Initialisierung
    // ------------------------------------------------------------------------------------

    private void loadPuaMapping() throws Exception {
        System.out.print("Lade PUA-Mapping... ");
        try (BufferedReader br = new BufferedReader(new FileReader(PUA_MAPPING))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                String[] parts = line.split("\\s+");
                if (parts.length < 2) continue;
                int puaCp = Integer.parseInt(parts[0], 16);
                StringBuilder target = new StringBuilder();
                for (String hex : parts[1].split("-"))
                    target.appendCodePoint(Integer.parseInt(hex, 16));
                puaMapping.put(puaCp, target.toString());
            }
        }
        System.out.println(puaMapping.size() + " Mappings geladen.");
    }

    private String convertPua(String text) {
        if (text == null) return null;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); ) {
            int cp = text.codePointAt(i);
            String mapped = puaMapping.get(cp);
            if (mapped != null) sb.append(mapped);
            else sb.appendCodePoint(cp);
            i += Character.charCount(cp);
        }
        return sb.toString();
    }

    private void loadKnownTypes() throws Exception {
        System.out.print("Lade msg_message_types... ");
        try (Statement st = suite.createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT type_id, ignore FROM msg_message_types WHERE source = '" + SOURCE + "'")) {
            while (rs.next())
                knownTypes.put(Integer.parseInt(rs.getString("type_id")), "1".equals(rs.getString("ignore")));
        }
        System.out.println(knownTypes.size() + " Types geladen.");
    }

    private void loadExistingChatsAndContacts() throws Exception {
        System.out.print("Lade bestehende Chats, Kontakte und Chat-Members... ");
        try (Statement st = suite.createStatement()) {
            try (ResultSet rs = st.executeQuery(
                    "SELECT chat_id, raw_identifier, blacklisted FROM msg_chats WHERE source = '" + SOURCE + "'")) {
                while (rs.next()) {
                    int chatId = rs.getInt("chat_id");
                    chatIdByRawIdentifier.put(rs.getString("raw_identifier"), chatId);
                    blacklistedByChat.put(chatId, rs.getInt("blacklisted") == 1);
                }
            }
            try (ResultSet rs = st.executeQuery(
                    "SELECT c.contact_id, m.raw_identifier " +
                    "FROM msg_contacts c JOIN msg_contact_mapping m ON c.contact_id = m.contact_id " +
                    "WHERE m.source = '" + SOURCE + "'")) {
                while (rs.next())
                    contactIdByRawIdentifier.put(rs.getString("raw_identifier"), rs.getInt("contact_id"));
            }
            try (ResultSet rs = st.executeQuery("SELECT chat_id, contact_id FROM msg_chat_members")) {
                while (rs.next())
                    chatMembersByChatId
                        .computeIfAbsent(rs.getInt("chat_id"), k -> new HashSet<>())
                        .add(rs.getInt("contact_id"));
            }
        }
        System.out.println("OK (" + chatIdByRawIdentifier.size() + " Chats, "
                + contactIdByRawIdentifier.size() + " Kontakte).");
        System.out.println();
    }

    // ------------------------------------------------------------------------------------
    // Haupt-Loop
    // ------------------------------------------------------------------------------------

    private void processMessages() throws Exception {
        String sql =
            "SELECT m._id, m.timestamp, m.from_me, m.message_type, m.text_data, " +
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
            "WHERE m.timestamp >= " + START_TIMESTAMP + " " +
            "AND COALESCE(jmapc.user, j.user) || '@' || COALESCE(jmapc.server, j.server) != 'status@broadcast' " +
            "ORDER BY m.timestamp ASC";

        int processed = 0;
        int skipped   = 0;
        int imported  = 0;

        try (Statement st = wa.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                processed++;

                int     type           = rs.getInt("message_type");
                long    sourceId       = rs.getLong("_id");
                long    timestamp      = rs.getLong("timestamp");
                int     chatRowId      = rs.getInt("chat_row_id");
                String  rawIdentifier  = rs.getString("raw_identifier");
                String  fromContactRaw = rs.getString("from_contact_raw");
                String  content        = convertPua(rs.getString("content"));
                String  filePath       = rs.getString("file_path");
                String  messageUrl     = rs.getString("message_url");
                long    origFlags      = rs.getLong("origination_flags");
                String  subject        = rs.getString("subject");
                boolean isGroup        = rs.getInt("is_group") == 1;
                String  quoteKeyId     = rs.getString("quote_key_id");

                currentSourceId  = sourceId;
                currentTimestamp = timestamp;
                currentAlbumKey  = chatRowId + ":" + fromContactRaw;

                // 1. Bekannter Type?
                if (!knownTypes.containsKey(type)) {
                    System.out.println("[DRYRUN-FAILFAST] Unbekannter Type=" + type + " _id=" + sourceId);
                    return;
                }

                // 2. Ignorierter Type?
                if (knownTypes.get(type)) {
                    skipped++;
                    continue;
                }

                // 3. Chat auflösen
                int chatId = resolveChat(rawIdentifier, subject, isGroup);
                if (chatId == -1) {
                    skipped++;
                    continue;
                }

                // 4. Album-Kind-Erkennung
                String albumKey  = currentAlbumKey;
                long[] openAlbum = openAlbumByKey.get(albumKey);

                if (type != 99 && origFlags != 0 && content == null && filePath != null && openAlbum != null) {
                    long openerSourceId  = openAlbum[0];
                    long openerTimestamp = openAlbum[1];
                    long diffMs          = timestamp - openerTimestamp;

                    if (diffMs > 2 * 60 * 1000L) {
                        System.out.println("[DRYRUN-WARNING] Album-Kind liegt " + (diffMs / 1000) + "s nach Opener"
                            + " — _id=" + sourceId + " (" + formatTs(timestamp) + ")"
                            + " opener_source_id=" + openerSourceId + " (" + formatTs(openerTimestamp) + ")"
                            + " albumKey=" + albumKey
                            + " → Attachment wird TROTZDEM an Opener gehängt!");
                    }

                    String cleanPath = filePath.replace(",", "_");
                    System.out.println("[DRYRUN] Album-Kind erkannt: _id=" + sourceId
                        + " → Attachment an Opener source_id=" + openerSourceId
                        + " path=" + cleanPath);
                    dryRunInsertAttachment(openerSourceId, cleanPath);
                    skipped++;
                    continue;
                }

                // Keine Kind-Erkennung → Marker schließen
                if (type != 99) {
                    if (openAlbumByKey.containsKey(albumKey)) {
                        long[] closed = openAlbumByKey.remove(albumKey);
                        System.out.println("[DRYRUN] Album-Marker geschlossen für albumKey=" + albumKey
                            + " (Opener war source_id=" + closed[0] + ")");
                    }
                }

                // 5. Type-99: Opener setzen
                if (type == 99) {
                    openAlbumByKey.put(albumKey, new long[]{sourceId, timestamp});
                    System.out.println("[DRYRUN] Album-Opener gesetzt: source_id=" + sourceId
                        + " albumKey=" + albumKey + " (" + formatTs(timestamp) + ")");
                }

                // 6. Kontakt auflösen (DryRun: nur in-memory)
                resolveContact(fromContactRaw);

                // 7. Attachment
                String attachmentPath = null;
                if (filePath != null) {
                    attachmentPath = filePath.replace(",", "_");
                } else if (messageUrl != null) {
                    attachmentPath = messageUrl;
                } else if (content == null && type != 99) {
                    System.out.println("[DRYRUN-FAILFAST] Message ohne content, file_path und message_url:"
                        + " _id=" + sourceId + " type=" + type);
                    return;
                }

                // 8. INSERT (DryRun: nur loggen)
                System.out.println("[DRYRUN] insertMessage: source_id=" + sourceId
                    + " (" + formatTs(timestamp) + ") content=" + (content != null ? "'" + content + "'" : "null"));

                if (attachmentPath != null) {
                    dryRunInsertAttachment(sourceId, attachmentPath);
                }

                imported++;
            }
        }

        System.out.println();
        System.out.println("DryRun fertig. Verarbeitet: " + processed
                + ", würde importieren: " + imported
                + ", übersprungen: " + skipped);
    }

    // ------------------------------------------------------------------------------------
    // DryRun-Attachment: sofortige Ausgabe bei Duplikaten
    // ------------------------------------------------------------------------------------

    private void dryRunInsertAttachment(long sourceId, String path) {
        List<String> calls = attachmentCallsById.computeIfAbsent(sourceId, k -> new ArrayList<>());

        System.out.println("[DRYRUN] insertAttachment: source_id=" + sourceId + " path=" + path);

        if (calls.contains(path)) {
            System.out.println("[DRYRUN-DUPLIKAT] Gleiches Attachment nochmals für source_id=" + sourceId
                + " path=" + path
                + " | aktuell verarbeitete Nachricht: _id=" + currentSourceId
                + " (" + formatTs(currentTimestamp) + ")"
                + " albumKey=" + currentAlbumKey
                + " | offene Alben: " + formatOpenAlbums());
        }

        calls.add(path);
    }

    private String formatOpenAlbums() {
        if (openAlbumByKey.isEmpty()) return "(keine)";
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, long[]> e : openAlbumByKey.entrySet())
            sb.append(e.getKey()).append("→source_id=").append(e.getValue()[0])
              .append("(").append(formatTs(e.getValue()[1])).append(") ");
        return sb.toString().trim();
    }

    // ------------------------------------------------------------------------------------
    // Chat-Auflösung (DryRun: nur in-memory, keine DB-Schreibzugriffe)
    // ------------------------------------------------------------------------------------

    private int resolveChat(String rawIdentifier, String subject, boolean isGroup) {
        if (chatIdByRawIdentifier.containsKey(rawIdentifier)) {
            int chatId = chatIdByRawIdentifier.get(rawIdentifier);
            return blacklistedByChat.get(chatId) ? -1 : chatId;
        }
        // DryRun: unbekannte Chats werden mit einer Fake-ID eingetragen und importiert
        int fakeId = -(chatIdByRawIdentifier.size() + 1);
        String displayName = (subject != null && !subject.isBlank()) ? subject : rawIdentifier;
        System.out.println("[DRYRUN] Neuer Chat: rawIdentifier=" + rawIdentifier
            + " subject=" + displayName + " isGroup=" + isGroup + " → fakeId=" + fakeId);
        chatIdByRawIdentifier.put(rawIdentifier, fakeId);
        blacklistedByChat.put(fakeId, false);
        return fakeId;
    }

    // ------------------------------------------------------------------------------------
    // Kontakt-Auflösung (DryRun: nur in-memory)
    // ------------------------------------------------------------------------------------

    private int resolveContact(String rawIdentifier) {
        if (contactIdByRawIdentifier.containsKey(rawIdentifier))
            return contactIdByRawIdentifier.get(rawIdentifier);
        int fakeId = -(contactIdByRawIdentifier.size() + 1);
        System.out.println("[DRYRUN] Neuer Kontakt: rawIdentifier=" + rawIdentifier + " → fakeId=" + fakeId);
        contactIdByRawIdentifier.put(rawIdentifier, fakeId);
        return fakeId;
    }

    // ------------------------------------------------------------------------------------
    // Hilfsmethoden
    // ------------------------------------------------------------------------------------

    private String formatTs(long unixMs) {
        return LocalDateTime
                .ofInstant(Instant.ofEpochMilli(unixMs), ZoneId.systemDefault())
                .format(DT);
    }
}