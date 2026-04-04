package app.misc;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
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
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Scanner;
import java.util.Set;

/**
 *
 * Wenn Kontakt schon bei Signal bekannt:
 * UPDATE msg_messages SET from_contact = 2 WHERE from_contact = 33;
 * UPDATE msg_chat_members SET contact_id = 2 WHERE contact_id = 33;
 * UPDATE msg_contact_mapping SET contact_id = 2 WHERE contact_id = 33;
 * DELETE FROM msg_contacts WHERE contact_id = 33;
 *
 *	Todos:
 * - Im Daily Import muss abgefragt werden, ob der Kontakt bereits existiert.
 * - Attachments dürfen im Filename niemals nicht ein "," enthalten, das bringt unser SQL durcheinander. Die müssen konsequent durch "_" ersetzt werden
 * - Am besten dann auch keine Kommas in den DB-Tabelllen erlauben für attachments, geht das einfach?
 * - Attachment-Pfade müssen dann natürlich relativ gespeichert werden, die aktuellen bereinigt (falls da was zu tun)
 * - Wir kopieren die Anhänge aktuell nciht in den Target-Ordner. Mach das später einfach manuell und schau, ob da viel unreferenziertes rumliegt. Ich denkle nicht.
 *
 * Es kommt manchmal vor, dass der path des attachments null ist. Das in Kombi mit einer Nachricht mit leerem Content → Blockieren einfach...
 *
 * TODO: System.exit(-1) bei erstem Quote und erstem Type-99 entfernen sobald debuggt.
 */

/**
 * Initialer WhatsApp-Import in die ThosSuite-Datenbank.
 *
 * Ablauf:
 *   1. Alle bekannten Message-Types aus msg_message_types laden
 *   2. Nachrichten ab START_TIMESTAMP sequenziell verarbeiten (nach timestamp)
 *   3. Unbekannte Chats interaktiv abfragen (importieren ja/nein)
 *   4. Unbekannte Kontakte interaktiv benennen
 *   5. Album-Gruppen (Type 99 + Kinder) zusammenführen
 *   6. Anhänge aus Quellordner in Zielordner kopieren
 *   7. Quotes auflösen via message_quoted.key_id → message._id
 *
 * Restartfähig: START_TIMESTAMP auf den letzten importierten Timestamp setzen.
 */
public class WhatsAppInitialImport {

    private static final String WHATSAPP_DB  = "C:\\Users\\Markgraf\\Desktop\\WhatsApp\\msgstore.db.unencrypted.java";
    private static final String SUITE_DB     = "C:\\Users\\Markgraf\\OneDrive\\ThosSuite\\data\\thossuite.db";
    private static final String MEDIA_SOURCE = "C:\\Users\\Markgraf\\Desktop\\WhatsApp\\Pixel Media";
    private static final String MEDIA_TARGET = "C:\\Users\\Markgraf\\Desktop\\WhatsApp\\Target Media";
    private static final String SOURCE       = "whatsapp";
    private static final String PUA_MAPPING  = "C:\\Users\\Markgraf\\Desktop\\WhatsApp\\old_smileys_mapper.txt";

    /** Nachrichten vor diesem Timestamp werden nicht importiert (Unix ms). */
    private static final long START_TIMESTAMP = 1638351549000L; // anpassen!

    /**
     * Anhänge die nach diesem Timestamp gesendet wurden aber nicht gefunden werden
     * lösen eine WARNING aus. Vorher: still ignorieren.
     * Format: Unix-Timestamp in Millisekunden.
     */
    private static final long ATTACHMENT_WARNING_SINCE = 1575586801L; 

    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ------------------------------------------------------------------------------------
    // Laufzeit-State
    // ------------------------------------------------------------------------------------

    /** raw_identifier -> chat_id (aus msg_chats, inkl. neu angelegter). */
    private final Map<String, Integer> chatIdByRawIdentifier = new HashMap<>();

    /** chat_id -> blacklisted */
    private final Map<Integer, Boolean> blacklistedByChat = new HashMap<>();

    /** raw_identifier -> contact_id (aus msg_contacts, inkl. neu angelegter). */
    private final Map<String, Integer> contactIdByRawIdentifier = new HashMap<>();

    /** chat_id -> Set von contact_ids die bereits als Member eingetragen sind. */
    private final Map<Integer, Set<Integer>> chatMembersByChatId = new HashMap<>();

    /** Bekannte Message-Types: type -> ignore? */
    private final Map<Integer, Boolean> knownTypes = new HashMap<>();

    /** PUA-Codepoint -> Standard-Unicode-String (kann mehrere Codepoints enthalten). */
    private final Map<Integer, String> puaMapping = new HashMap<>();

    /** Offene Album-Gruppen pro "chat_row_id:senderRaw". */
    private final Map<String, AlbumAccumulator> openAlbums = new HashMap<>();

    private Connection wa;
    private Connection suite;
    private final Scanner scanner = new Scanner(System.in);

    // ------------------------------------------------------------------------------------
    // Main
    // ------------------------------------------------------------------------------------

    public static void main(String[] args) throws Exception {
        new WhatsAppInitialImport().run();
    }

    private void run() throws Exception {
        System.out.println("=== WhatsApp Initial Import ===");
        System.out.println();

        wa    = DriverManager.getConnection("jdbc:sqlite:" + WHATSAPP_DB);
        suite = DriverManager.getConnection("jdbc:sqlite:" + SUITE_DB);
        suite.setAutoCommit(true);

        loadPuaMapping();
        loadKnownTypes();
        loadExistingChatsAndContacts();

        System.out.println("Starte Import ab Timestamp: " + START_TIMESTAMP + " (" + formatTs(START_TIMESTAMP) + ")");
        System.out.println();

        processMessages();

        // Noch offene Alben abschließen
        for (AlbumAccumulator acc : openAlbums.values()) {
            System.out.println("WARNING: Offenes Album beim Ende des Imports – " + acc.describe());
            flushAlbum(acc);
        }

        wa.close();
        suite.close();
        System.out.println();
        System.out.println("=== Import abgeschlossen ===");
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
                // Ziel kann mehrere Codepoints sein (z.B. "0023-20E3")
                StringBuilder target = new StringBuilder();
                for (String hex : parts[1].split("-")) {
                    target.appendCodePoint(Integer.parseInt(hex, 16));
                }
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
            if (mapped != null) {
                sb.append(mapped);
            } else {
                sb.appendCodePoint(cp);
            }
            i += Character.charCount(cp);
        }
        return sb.toString();
    }

    private void loadKnownTypes() throws Exception {
        System.out.print("Lade msg_message_types... ");
        try (Statement st = suite.createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT type_id, ignore FROM msg_message_types WHERE source = '" + SOURCE + "'")) {
            while (rs.next()) {
                knownTypes.put(Integer.parseInt(rs.getString("type_id")), "1".equals(rs.getString("ignore")));
            }
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
                while (rs.next()) {
                    contactIdByRawIdentifier.put(rs.getString("raw_identifier"), rs.getInt("contact_id"));
                }
            }

            try (ResultSet rs = st.executeQuery("SELECT chat_id, contact_id FROM msg_chat_members")) {
                while (rs.next()) {
                    chatMembersByChatId
                        .computeIfAbsent(rs.getInt("chat_id"), k -> new HashSet<>())
                        .add(rs.getInt("contact_id"));
                }
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

                // 1. Bekannter Type?
                if (!knownTypes.containsKey(type)) {
                    throw new RuntimeException(
                        "UNBEKANNTER MESSAGE TYPE " + type + " bei Nachricht _id=" + sourceId
                        + " – Import abgebrochen. Bitte msg_message_types aktualisieren.");
                }

                // 2. Ignorierter Type?
                if (knownTypes.get(type)) {
                    skipped++;
                    continue;
                }

                // 3. Chat auflösen
                int chatId = resolveChat(rawIdentifier, subject, isGroup, timestamp);
                if (chatId == -1) {
                    skipped++;
                    continue; // blacklisted
                }

                // 4. Album-Logik
                String albumKey = chatRowId + ":" + fromContactRaw;

                if (type == 99) {
                    // TODO: System.exit entfernen sobald Type-99-Logik debuggt ist
                    System.out.println("[DEBUG] Erster Type-99 gefunden: _id=" + sourceId
                        + " timestamp=" + formatTs(timestamp));
                    System.exit(-1);

                    if (openAlbums.containsKey(albumKey)) {
                        AlbumAccumulator old = openAlbums.get(albumKey);
                        System.out.println("WARNING: Neues Album öffnet altes ohne Abschluss – " + old.describe());
                        flushAlbum(old);
                    }
                    openAlbums.put(albumKey, new AlbumAccumulator(sourceId, timestamp, fromContactRaw, chatId));
                    skipped++;
                    continue;
                }

                if (openAlbums.containsKey(albumKey)) {
                    AlbumAccumulator acc = openAlbums.get(albumKey);
                    boolean withinWindow = (timestamp - acc.timestamp) <= 2 * 60 * 1000L;
                    boolean isChild      = origFlags != 0 && content == null;

                    if (withinWindow && isChild) {
                        acc.childFilePaths.add(filePath);
                        acc.childSourceIds.add(sourceId);
                        skipped++;
                        continue;
                    } else {
                        if (!withinWindow) {
                            System.out.println("WARNING: 2-Minuten-Grenze überschritten, schließe Album – " + acc.describe());
                        }
                        flushAlbum(acc);
                        openAlbums.remove(albumKey);
                        // aktuelle Nachricht normal weiterverarbeiten
                    }
                }

                // 5. Kontakt auflösen + Chat-Member eintragen
                int fromContactId = resolveContact(fromContactRaw);
                insertChatMemberIfNew(chatId, fromContactId);

                // 6. Quote auflösen
                String resolvedQuoteSourceId = null;
                if (quoteKeyId != null) {
                    resolvedQuoteSourceId = resolveQuote(quoteKeyId, sourceId);
                }

                // 7. Anhang verarbeiten
                String  attachmentPath      = null;
                boolean attachmentAvailable = false;

                if (filePath != null) {
                    String cleanPath = filePath.replace(",", "_");
                    File src = new File(MEDIA_SOURCE, filePath);
                    if (src.exists()) {
                        File tgt = new File(MEDIA_TARGET, cleanPath);
                        //Files.copy(src.toPath(), tgt.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        attachmentAvailable = true;
                    } else if (timestamp >= ATTACHMENT_WARNING_SINCE) {
                        System.out.println("WARNING: Anhang nicht gefunden: " + filePath
                            + " (_id=" + sourceId + ", " + formatTs(timestamp) + ")");
                    }
                    attachmentPath = cleanPath;
                } else if (messageUrl != null) {
                    attachmentPath      = messageUrl;
                    attachmentAvailable = false;
                } else if (content == null) {
                    throw new IllegalStateException(
                        "[FAILFAST] Message ohne content, file_path und message_url: _id=" + sourceId
                        + " type=" + type + " timestamp=" + formatTs(timestamp));
                }

                // 8. INSERT
                insertMessage(sourceId, timestamp, fromContactId, chatId, content, resolvedQuoteSourceId);

                if (attachmentPath != null) {
                    insertAttachment(sourceId, attachmentPath, attachmentAvailable);
                }

                imported++;

                if (imported % 100 == 0) {
                    System.out.println("  " + imported + " Nachrichten importiert...");
                }
            }
        }

        System.out.println();
        System.out.println("Fertig. Verarbeitet: " + processed
                + ", importiert: " + imported
                + ", übersprungen: " + skipped);
    }

    // ------------------------------------------------------------------------------------
    // Quote-Auflösung
    // ------------------------------------------------------------------------------------

    /**
     * Löst einen Quote auf: JOIN message_quoted.key_id → message._id.
     * Gibt die source_id (WhatsApp _id als String) der gequoteten Message zurück,
     * oder NULL wenn nicht auflösbar (bekanntes WhatsApp-Phänomen bei bearbeiteten Nachrichten).
     */
    private String resolveQuote(String quoteKeyId, long quotingSourceId) throws Exception {
        try (PreparedStatement ps = wa.prepareStatement(
                "SELECT _id FROM message WHERE key_id = ?")) {
            ps.setString(1, quoteKeyId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return String.valueOf(rs.getLong("_id"));
                }
            }
        }
        System.out.println("WARNING: Quote nicht auflösbar: quoteKeyId=" + quoteKeyId
            + " (quotingSourceId=" + quotingSourceId + ")");
        return null;
    }

    // ------------------------------------------------------------------------------------
    // Album-Flush
    // ------------------------------------------------------------------------------------

    private void flushAlbum(AlbumAccumulator acc) throws Exception {
        System.out.println("Album zusammengeführt: source_id=" + acc.sourceId
                + ", Sender=" + acc.senderRaw
                + ", Zeitpunkt=" + formatTs(acc.timestamp)
                + ", Kinder=" + acc.childSourceIds.size());

        if (acc.childSourceIds.isEmpty()) {
            System.out.println("WARNING: Album ohne Kinder – source_id=" + acc.sourceId);
        }

        int fromContactId = resolveContact(acc.senderRaw);
        insertChatMemberIfNew(acc.chatId, fromContactId);

        insertMessage(acc.sourceId, acc.timestamp, fromContactId, acc.chatId, null, null);

        for (String filePath : acc.childFilePaths) {
            if (filePath == null) continue;
            String cleanPath = filePath.replace(",", "_");
            File src = new File(MEDIA_SOURCE, filePath);
            if (src.exists()) {
                File tgt = new File(MEDIA_TARGET, cleanPath);
                Files.copy(src.toPath(), tgt.toPath(), StandardCopyOption.REPLACE_EXISTING);
                insertAttachment(acc.sourceId, cleanPath, true);
            } else {
                if (acc.timestamp >= ATTACHMENT_WARNING_SINCE) {
                    System.out.println("WARNING: Album-Anhang nicht gefunden: " + filePath);
                }
                insertAttachment(acc.sourceId, cleanPath, false);
            }
        }
    }

    // ------------------------------------------------------------------------------------
    // Chat-Auflösung
    // ------------------------------------------------------------------------------------

    private int resolveChat(String rawIdentifier, String subject, boolean isGroup, long timestamp) throws Exception {
        if (chatIdByRawIdentifier.containsKey(rawIdentifier)) {
            int chatId = chatIdByRawIdentifier.get(rawIdentifier);
            return blacklistedByChat.get(chatId) ? -1 : chatId;
        }

        System.out.println();
        System.out.println(formatTs(timestamp) + " - " + timestamp);
        System.out.println("Unbekannter Chat:");
        System.out.println("  raw_identifier : " + rawIdentifier);
        System.out.println("  subject        : " + (subject != null ? subject : "(kein Name)"));
        System.out.println("  Gruppe         : " + (isGroup ? "ja" : "nein"));
        System.out.print("Importieren? [j/n]: ");
        String answer = scanner.nextLine().trim().toLowerCase();
        Boolean doImport = null;
        if (answer.equals("j"))
            doImport = true;
        else if (answer.equals("n"))
            doImport = false;
        else
            System.exit(-1);

        System.out.println(doImport ? "Ok, wir importieren." : "Ok, dann lassen wir das besser :-)");

        String displayName = (subject != null && !subject.isBlank()) ? subject : rawIdentifier;
        if (doImport && (subject == null || subject.isBlank())) {
            System.out.print("Anzeigename: ");
            String typed = scanner.nextLine().trim();
            if (!typed.isBlank()) displayName = typed;
        }

        int chatId;
        try (PreparedStatement ps = suite.prepareStatement(
                "INSERT INTO msg_chats (source, raw_identifier, is_group, display_name, blacklisted) " +
                "VALUES (?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, SOURCE);
            ps.setString(2, rawIdentifier);
            ps.setInt(3, isGroup ? 1 : 0);
            ps.setString(4, displayName);
            ps.setInt(5, doImport ? 0 : 1);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                chatId = keys.getInt(1);
            }
        }

        chatIdByRawIdentifier.put(rawIdentifier, chatId);
        blacklistedByChat.put(chatId, !doImport);
        return doImport ? chatId : -1;
    }

    // ------------------------------------------------------------------------------------
    // Kontakt-Auflösung
    // ------------------------------------------------------------------------------------

    private int resolveContact(String rawIdentifier) throws Exception {
        if (contactIdByRawIdentifier.containsKey(rawIdentifier)) {
            return contactIdByRawIdentifier.get(rawIdentifier);
        }

        System.out.println();
        System.out.println("Unbekannter Kontakt: " + rawIdentifier);
        System.out.print("Anzeigename: ");
        String displayName = scanner.nextLine().trim();
        if (displayName.isBlank()) displayName = rawIdentifier;

        int contactId;
        try (PreparedStatement ps = suite.prepareStatement(
                "INSERT INTO msg_contacts (display_name) VALUES (?)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, displayName);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                contactId = keys.getInt(1);
            }
        }

        try (PreparedStatement ps = suite.prepareStatement(
                "INSERT INTO msg_contact_mapping (source, raw_identifier, contact_id) VALUES (?, ?, ?)")) {
            ps.setString(1, SOURCE);
            ps.setString(2, rawIdentifier);
            ps.setInt(3, contactId);
            ps.executeUpdate();
        }

        contactIdByRawIdentifier.put(rawIdentifier, contactId);
        return contactId;
    }

    // ------------------------------------------------------------------------------------
    // Chat-Member
    // ------------------------------------------------------------------------------------

    private void insertChatMemberIfNew(int chatId, int contactId) throws Exception {
        Set<Integer> members = chatMembersByChatId.computeIfAbsent(chatId, k -> new HashSet<>());
        if (members.contains(contactId)) return;

        try (PreparedStatement ps = suite.prepareStatement(
                "INSERT OR IGNORE INTO msg_chat_members (chat_id, contact_id) VALUES (?, ?)")) {
            ps.setInt(1, chatId);
            ps.setInt(2, contactId);
            ps.executeUpdate();
        }

        members.add(contactId);
    }

    // ------------------------------------------------------------------------------------
    // INSERT-Helpers
    // ------------------------------------------------------------------------------------

    private void insertMessage(long sourceId, long timestamp, int fromContactId,
                               int chatId, String content, String quoteMessageId) throws Exception {
        try (PreparedStatement ps = suite.prepareStatement(
                "INSERT OR IGNORE INTO msg_messages " +
                "(source, source_id, sent_at, from_contact, chat_id, content, quote_message_id) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)")) {
            ps.setString(1, SOURCE);
            ps.setLong(2, sourceId);
            ps.setString(3, formatTs(timestamp));
            ps.setInt(4, fromContactId);
            ps.setInt(5, chatId);
            ps.setString(6, content);
            ps.setString(7, quoteMessageId);  // NULL wenn kein Quote oder nicht auflösbar
            ps.executeUpdate();
        }
    }

    private void insertAttachment(long sourceId, String filename, boolean available) throws Exception {
        try (PreparedStatement ps = suite.prepareStatement(
                "INSERT OR IGNORE INTO msg_message_attachment " +
                "(source, source_id, path, available) VALUES (?, ?, ?, ?)")) {
            ps.setString(1, SOURCE);
            ps.setLong(2, sourceId);
            ps.setString(3, filename);
            ps.setInt(4, available ? 1 : 0);
            ps.executeUpdate();
        }
    }

    // ------------------------------------------------------------------------------------
    // Hilfsmethoden
    // ------------------------------------------------------------------------------------

    private String formatTs(long unixMs) {
        return LocalDateTime
                .ofInstant(Instant.ofEpochMilli(unixMs), ZoneId.systemDefault())
                .format(DT);
    }

    // ------------------------------------------------------------------------------------
    // Album-Accumulator
    // ------------------------------------------------------------------------------------

    private static class AlbumAccumulator {
        final long   sourceId;
        final long   timestamp;
        final String senderRaw;
        final int    chatId;
        final List<String> childFilePaths = new ArrayList<>();
        final List<Long>   childSourceIds = new ArrayList<>();

        AlbumAccumulator(long sourceId, long timestamp, String senderRaw, int chatId) {
            this.sourceId  = sourceId;
            this.timestamp = timestamp;
            this.senderRaw = senderRaw;
            this.chatId    = chatId;
        }

        String describe() {
            return "source_id=" + sourceId + ", Sender=" + senderRaw
                + ", Kinder=" + childSourceIds.size();
        }
    }
}