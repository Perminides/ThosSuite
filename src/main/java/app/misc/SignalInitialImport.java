package app.misc;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 *  * - Attachments dürfen im Filename niemals nicht ein "," enthalten, das bringt unser SQL durcheinander. Die müssen konsequent durch "_" ersetzt werden
 * - Am besten dann auch keine Kommas in den DB-Tabelllen erlauben für attachments, geht das einfach?
 * - 3 Attachments in der Suite mit KOmmas und die sind ncoh von Signal, da kümmern wir uns später darum
 */

/**
 * Einmaliger Initialimport aller Signal-Nachrichten in die ThosSuite-Datenbank.
 *
 * <h2>Voraussetzungen</h2>
 * <ul>
 *   <li>Die Signal-Datenbank muss vorab entschlüsselt als plaintext SQLite vorliegen
 *       (Signal verschlüsselt mit SQLCipher; Entschlüsselung erfolgt extern, z. B. mit
 *       dem Signal-Key aus {@code config.json} und sqlcipher).</li>
 *   <li>Die ThosSuite-Datenbank muss das Messaging-Schema enthalten.</li>
 *   <li>{@code DRY_RUN = true} für einen Probedurchlauf ohne Datenbankänderungen,
 *       {@code DRY_RUN = false} für den echten Import.</li>
 * </ul>
 *
 * <h2>ThosSuite-Schema (Messaging)</h2>
 * <pre>
 * msg_contacts        — Personen (contact_id, display_name)
 * msg_contact_mapping — serviceId → contact_id (source, raw_identifier, contact_id)
 * msg_chats           — Chats, 1:1 und Gruppen (chat_id, source, raw_identifier, is_group, display_name, blacklisted)
 * msg_chat_members    — Wer hat in welchem Chat geschrieben, lazy befüllt (chat_id, contact_id)
 * msg_messages        — Nachrichten (source, source_id, sent_at, from_contact, chat_id, content, quote_message_id)
 * msg_message_attachment — Anhänge (source, source_id, path, thumb_path, available)
 * </pre>
 *
 * <h2>Identifikatoren in Signal</h2>
 * Signal Desktop unterscheidet zwei UUID-Typen pro Kontakt:
 * <ul>
 *   <li><b>conversationId</b> ({@code conversations.id}): interne ID der Conversation.
 *       Wird als {@code raw_identifier} in {@code msg_chats} gespeichert.</li>
 *   <li><b>serviceId</b> ({@code conversations.serviceId}, ACI): stabiler Account-Identifier
 *       des Kontakts, überlebt Nummernwechsel. Wird als {@code raw_identifier} in
 *       {@code msg_contact_mapping} gespeichert. Bei eingehenden Nachrichten steht der
 *       tatsächliche Absender in {@code messages.sourceServiceId}.</li>
 * </ul>
 * Die eigene serviceId ist als Konstante {@code MY_SERVICE_ID} hinterlegt.
 *
 * <h2>Gefilterte Messages</h2>
 * <ul>
 *   <li>Nur {@code type = 'incoming'} und {@code type = 'outgoing'} (Whitelist).</li>
 *   <li>{@code isErased = 1} → übersprungen.</li>
 *   <li>Kein Body und keine Attachments → übersprungen mit WARNING.</li>
 * </ul>
 *
 * <h2>Chats</h2>
 * Lazy angelegt beim ersten Auftreten einer unbekannten {@code conversationId}.
 * Der Nutzer wird interaktiv gefragt ob importiert werden soll. Bei Ablehnung wird
 * {@code blacklisted = 1} in {@code msg_chats} gesetzt und alle weiteren Nachrichten
 * dieses Chats übersprungen.
 *
 * <h2>Kontakte</h2>
 * Lazy über {@code serviceId} angelegt. Auch der eigene Kontakt ("Ich") kommt beim
 * ersten Auftreten einer ausgehenden Nachricht rein. Bei outgoing ohne {@code sourceServiceId}
 * wird {@code MY_SERVICE_ID} verwendet.
 *
 * <h2>Quote-Auflösung (dreistufig)</h2>
 * <ol>
 *   <li>Direkt über {@code quote.messageId} (UUID) — JOIN auf {@code messages.id}.</li>
 *   <li>Fallback: {@code quote.id} (Timestamp) — JOIN auf {@code messages.sent_at}.</li>
 *   <li>Fallback: {@code quote.id} gegen Timestamps in {@code $.editHistory} — liefert
 *       die UUID der zugehörigen Hauptzeile.</li>
 * </ol>
 * Ist die Quote nicht auflösbar → FailFast. Ein leerer Leerstring wird nie in die Suite
 * geschrieben.
 *
 * <h2>Attachments</h2>
 * Pro Message alle Einträge aus {@code message_attachments} mit
 * {@code path IS NOT NULL AND localKey IS NOT NULL AND version = 2}.
 * AES/CBC-Entschlüsselung, Dateiname {@code <name>_<signalMsgId>_<seq>.<ext>}.
 * Existiert die Ausgabedatei bereits → übersprungen (Idempotenz).
 * Fehlt die Quelldatei → WARNING, Attachment übersprungen, Message trotzdem importiert.
 * {@code available}-Flag: 1 wenn Datei nach Import vorhanden, 0 wenn Quelldatei fehlte.
 *
 * <h2>Transaktionsverhalten</h2>
 * Alle Schreiboperationen laufen in einer einzigen Transaktion.
 * Bei {@code DRY_RUN = true} → rollback. Bei {@code DRY_RUN = false} → commit.
 * IDs per {@code getGeneratedKeys()}, nie selbst gezählt außer im DRY_RUN.
 */
public class SignalInitialImport {

    // --- Konfiguration ---
    private static final boolean DRY_RUN = true;

    private static final String SIGNAL_DB_PATH    = "C:\\Users\\Markgraf\\Desktop\\signal_plain.sqlite";
    private static final String THOSSUITE_DB_PATH = "C:\\Users\\Markgraf\\OneDrive\\ThosSuite\\data\\thossuite.db";
    private static final String SIGNAL_ATTACH_IN  = "C:\\Users\\Markgraf\\AppData\\Roaming\\Signal\\attachments.noindex\\";
    private static final String SIGNAL_ATTACH_OUT = "C:\\Users\\Markgraf\\Desktop\\signal_attachments_import\\";

    private static final String MY_SERVICE_ID = "439b7480-bdcd-409f-a397-0fb85faa3a83";

    private static final DateTimeFormatter DB_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private static final Map<String, String> EXTENSION_MAP = Map.ofEntries(
        Map.entry("image/jpeg",           "jpeg"),
        Map.entry("image/png",            "png"),
        Map.entry("image/gif",            "gif"),
        Map.entry("image/webp",           "webp"),
        Map.entry("image/avif",           "avif"),
        Map.entry("video/mp4",            "mp4"),
        Map.entry("audio/aac",            "aac"),
        Map.entry("audio/mpeg",           "mp3"),
        Map.entry("audio/mp4",            "m4a"),
        Map.entry("audio/ogg",            "ogg"),
        Map.entry("application/pdf",      "pdf"),
        Map.entry("text/x-signal-plain",  "txt"),
        Map.entry("text/comma-separated-values",  "csv"),
        Map.entry("application/x-zip-compressed", "zip"),
        Map.entry("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",       "xlsx"),
        Map.entry("application/vnd.openxmlformats-officedocument.wordprocessingml.document", "docx")
        // application/octet-stream: Extension wird aus fileName abgeleitet
    );

    // conversationId (Signal) -> chat_id (ThosSuite)
    private static final Map<String, Integer> chatByConversationId = new LinkedHashMap<>();

    // serviceId (Signal) -> contact_id (ThosSuite)
    private static final Map<String, Integer> contactByServiceId = new LinkedHashMap<>();

    // "chatId:contactId" -> bereits eingetragen
    private static final Set<String> chatMemberCache = new HashSet<>();

    // conversationId -> blacklisted (abgelehnte Chats)
    private static final Set<String> blacklistedChats = new HashSet<>();

    // signalMsgId -> laufende Attachment-Nummer für eindeutige Dateinamen
    private static final Map<String, Integer> attachSeqPerMsg = new HashMap<>();

    // DRY_RUN-Zähler (bei echtem Run werden IDs per getGeneratedKeys() geholt)
    private static int nextContactId = 1;
    private static int nextChatId    = 1;

    private static final Scanner scanner = new Scanner(System.in);

    // -------------------------------------------------------------------------

    public static void main(String[] args) throws Exception {
        System.out.println("=== SignalInitialImport — DRY_RUN=" + DRY_RUN + " ===\n");
        Files.createDirectories(Path.of(SIGNAL_ATTACH_OUT));

        try (Connection signalConn = DriverManager.getConnection("jdbc:sqlite:" + SIGNAL_DB_PATH);
             Connection thosConn   = DriverManager.getConnection("jdbc:sqlite:" + THOSSUITE_DB_PATH)) {

            thosConn.setAutoCommit(false);

            importMessages(signalConn, thosConn);

            System.out.println("\n=== Fertig. ===");
            if (DRY_RUN) {
                System.out.println("DRY_RUN aktiv — kein Commit, keine Datenbankänderung, keine Dateien geschrieben.");
                thosConn.rollback();
            } else {
                thosConn.commit();
                System.out.println("Commit erfolgreich.");
            }
        }
    }

    // -------------------------------------------------------------------------
    // Hauptschleife
    // -------------------------------------------------------------------------

    private static void importMessages(Connection signal, Connection thos) throws Exception {
        System.out.println("--- Import ---");

        int msgCount      = 0;
        int attachCount   = 0;
        int ignoredCount  = 0;  // falscher type
        int erasedCount   = 0;  // isErased = 1
        int emptyCount    = 0;  // kein body, keine attachments
        int skippedCount  = 0;  // blacklisted chat

        try (var msgStmt     = signal.createStatement();
             var msgRs       = msgStmt.executeQuery("""
                SELECT id, conversationId, type, body, sent_at, sourceServiceId, isErased,
                       json_extract(json, '$.quote')           AS quote_json,
                       json_extract(json, '$.quote.messageId') AS quote_message_id,
                       json_extract(json, '$.quote.id')        AS quote_id
                FROM messages
                ORDER BY sent_at ASC
                """);
             var insertMsg    = thos.prepareStatement("""
                INSERT INTO msg_messages (source, source_id, sent_at, from_contact, chat_id, content, quote_message_id)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """);
             var insertAttach = thos.prepareStatement("""
                INSERT INTO msg_message_attachment (source, source_id, path, thumb_path, available)
                VALUES (?, ?, ?, ?, ?)
                """)) {

            while (msgRs.next()) {
                String  signalMsgId     = msgRs.getString("id");
                String  conversationId  = msgRs.getString("conversationId");
                String  msgType         = msgRs.getString("type");
                String  body            = msgRs.getString("body");
                long    sentAtMs        = msgRs.getLong("sent_at");
                String  sourceServiceId = msgRs.getString("sourceServiceId");
                int     isErased        = msgRs.getInt("isErased");
                String  quoteJson       = msgRs.getString("quote_json");
                String  quoteMsgId      = msgRs.getString("quote_message_id");
                long    quoteId         = msgRs.getLong("quote_id");   // 0 wenn NULL
                boolean outgoing        = "outgoing".equals(msgType);

                // Whitelist: nur incoming und outgoing
                if (!"incoming".equals(msgType) && !"outgoing".equals(msgType)) {
                    ignoredCount++;
                    continue;
                }

                // Gelöschte Nachrichten überspringen
                if (isErased == 1) {
                    erasedCount++;
                    continue;
                }

                // Chat lazy anlegen (inkl. Blacklist-Rückfrage)
                if (!chatByConversationId.containsKey(conversationId)
                        && !blacklistedChats.contains(conversationId)) {
                    ensureChat(signal, thos, conversationId);
                }

                // Blacklisted Chats skippen
                if (blacklistedChats.contains(conversationId)) {
                    skippedCount++;
                    continue;
                }

                int chatId = chatByConversationId.get(conversationId);

                // from_contact bestimmen
                // outgoing: sourceServiceId kann null sein → MY_SERVICE_ID nehmen
                // incoming: sourceServiceId ist immer gefüllt
                String effectiveServiceId = (outgoing && isBlank(sourceServiceId))
                    ? MY_SERVICE_ID
                    : sourceServiceId;

                if (isBlank(effectiveServiceId)) {
                    fail("[FAILFAST] effectiveServiceId ist blank für signalMsgId=" + signalMsgId
                        + " type=" + msgType);
                }

                if (!contactByServiceId.containsKey(effectiveServiceId)) {
                    ensureContact(signal, thos, effectiveServiceId);
                }

                int fromContact = contactByServiceId.get(effectiveServiceId);

                // Chat-Member lazy eintragen
                ensureChatMember(thos, chatId, fromContact);

                // Attachments dieser Message verarbeiten
                List<AttachmentResult> attachments = resolveAttachmentsForMessage(signal, signalMsgId);

                // Nachrichten ohne Body und ohne Attachments überspringen
                if (isBlank(body) && attachments.isEmpty()) {
                    System.out.println("[WARNING] Keine Inhalte, übersprungen: signalMsgId=" + signalMsgId
                        + " sent_at=" + millisToDbString(sentAtMs));
                    emptyCount++;
                    continue;
                }

                // Quote auflösen
                String resolvedQuoteMsgId = resolveQuote(signal, signalMsgId, quoteJson, quoteMsgId, quoteId);

                // Message inserieren
                insertMsg.setString(1, "signal");
                insertMsg.setString(2, signalMsgId);
                insertMsg.setString(3, millisToDbString(sentAtMs));
                insertMsg.setInt   (4, fromContact);
                insertMsg.setInt   (5, chatId);
                insertMsg.setString(6, body);
                insertMsg.setString(7, resolvedQuoteMsgId);  // NULL wenn kein Quote
                insertMsg.executeUpdate();
                msgCount++;

                for (AttachmentResult att : attachments) {
                    insertAttach.setString(1, "signal");
                    insertAttach.setString(2, signalMsgId);
                    insertAttach.setString(3, att.path());
                    insertAttach.setNull  (4, Types.VARCHAR);   // thumb_path: nicht importiert
                    insertAttach.setInt   (5, att.available() ? 1 : 0);
                    insertAttach.executeUpdate();
                    attachCount++;
                }
            }
        }

        System.out.println("[import] Messages: " + msgCount
            + ", Attachments: " + attachCount
            + ", Ignoriert (type): " + ignoredCount
            + ", Ignoriert (erased): " + erasedCount
            + ", Ignoriert (leer): " + emptyCount
            + ", Übersprungen (blacklist): " + skippedCount);
    }

    // -------------------------------------------------------------------------
    // Quote-Auflösung (dreistufig)
    // -------------------------------------------------------------------------

    /**
     * Löst die quote_message_id auf. Gibt NULL zurück wenn kein Quote vorhanden.
     * Gibt die UUID der gequoteten Message zurück wenn ein Quote vorhanden und auflösbar.
     * FailFast wenn ein Quote vorhanden aber nicht auflösbar ist.
     */
    private static String resolveQuote(Connection signal, String signalMsgId,
                                        String quoteJson, String quoteMsgId, long quoteId) throws SQLException {
        // Kein Quote vorhanden
        if (quoteJson == null) return null;

        // Stufe 1: messageId direkt als UUID vorhanden und nicht leer
        if (!isBlank(quoteMsgId)) {
            String resolved = resolveQuoteByMessageId(signal, quoteMsgId);
            if (resolved != null) return resolved;
            // messageId war gefüllt aber nicht in der DB — das ist suspicious
            fail("[FAILFAST] quote.messageId='" + quoteMsgId + "' nicht in Signal-DB gefunden"
                + " (signalMsgId=" + signalMsgId + ")");
        }

        // Stufe 2: Timestamp-Fallback über quote.id = sent_at
        if (quoteId > 0) {
            String resolved = resolveQuoteByTimestamp(signal, quoteId);
            if (resolved != null) return resolved;

            // Stufe 3: quote.id gegen editHistory-Timestamps
            resolved = resolveQuoteByEditHistory(signal, quoteId);
            if (resolved != null) return resolved;
        }

        fail("[FAILFAST] Quote nicht auflösbar für signalMsgId=" + signalMsgId
            + " quote.id=" + quoteId + " quote.messageId=" + quoteMsgId);
        return null; // nie erreicht
    }

    /** Stufe 1: JOIN auf messages.id */
    private static String resolveQuoteByMessageId(Connection signal, String messageId) throws SQLException {
        try (var ps = signal.prepareStatement("SELECT id FROM messages WHERE id = ?")) {
            ps.setString(1, messageId);
            try (var rs = ps.executeQuery()) {
                return rs.next() ? rs.getString("id") : null;
            }
        }
    }

    /** Stufe 2: JOIN auf messages.sent_at */
    private static String resolveQuoteByTimestamp(Connection signal, long quoteId) throws SQLException {
        try (var ps = signal.prepareStatement(
                "SELECT id FROM messages WHERE sent_at = ?")) {
            ps.setLong(1, quoteId);
            try (var rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                String found = rs.getString("id");
                // Sicherheitscheck: Timestamp darf nicht mehrdeutig sein
                if (rs.next()) fail("[FAILFAST] quote.id=" + quoteId
                    + " matcht mehrere Rows in messages.sent_at");
                return found;
            }
        }
    }

    /** Stufe 3: Timestamp gegen editHistory */
    private static String resolveQuoteByEditHistory(Connection signal, long quoteId) throws SQLException {
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

    // -------------------------------------------------------------------------
    // Chat lazy anlegen via conversationId — inkl. Blacklist-Rückfrage
    // -------------------------------------------------------------------------

    private static void ensureChat(Connection signal, Connection thos, String conversationId) throws SQLException {
        try (var ps = signal.prepareStatement("""
                SELECT type, name, profileName, profileFamilyName, profileFullName,
                       json_extract(json, '$.messageCount') AS messageCount,
                       json_extract(json, '$.sharedGroupNames') AS sharedGroupNames
                FROM conversations WHERE id = ?
                """)) {
            ps.setString(1, conversationId);
            try (var rs = ps.executeQuery()) {
                if (!rs.next()) fail("[FAILFAST] conversationId='" + conversationId + "' nicht in conversations gefunden");

                String  type    = rs.getString("type");
                boolean isGroup = "group".equals(type);
                String  displayName;

                if (isGroup) {
                    displayName = rs.getString("name");
                    if (isBlank(displayName)) displayName = "Gruppe_" + conversationId;
                } else {
                    displayName = resolveDisplayName(rs, conversationId);
                }

                int    messageCount     = rs.getInt("messageCount");
                String sharedGroupNames = rs.getString("sharedGroupNames");

                System.out.println("\n>>> Neuer Chat gefunden:");
                System.out.println("    Name:        " + displayName);
                System.out.println("    Gruppe:      " + isGroup);
                System.out.println("    Nachrichten: " + messageCount);
                if (!isBlank(sharedGroupNames) && !"[]".equals(sharedGroupNames)) {
                    System.out.println("    Gemeinsame Gruppen: " + sharedGroupNames);
                }
                System.out.print("    Importieren? (j/n): ");
                String answer = scanner.nextLine().trim().toLowerCase();

                if (!"j".equals(answer)) {
                    System.out.println("[blacklist] '" + displayName + "' wird übersprungen.");
                    blacklistedChats.add(conversationId);
                    insertChatBlacklisted(thos, conversationId, isGroup, displayName);
                    return;
                }

                int chatId = insertChat(thos, conversationId, isGroup, displayName, false);
                chatByConversationId.put(conversationId, chatId);
            }
        }
    }

    // -------------------------------------------------------------------------
    // Kontakt lazy anlegen via serviceId
    // -------------------------------------------------------------------------

    private static void ensureContact(Connection signal, Connection thos, String serviceId) throws SQLException {
        try (var ps = signal.prepareStatement("""
                SELECT name, profileName, profileFamilyName, profileFullName,
                       json_extract(json, '$.sharedGroupNames') AS sharedGroupNames
                FROM conversations WHERE serviceId = ?
                """)) {
            ps.setString(1, serviceId);
            try (var rs = ps.executeQuery()) {
                if (!rs.next()) fail("[FAILFAST] serviceId='" + serviceId + "' nicht in conversations gefunden");

                String displayName      = resolveDisplayName(rs, serviceId);
                String sharedGroupNames = rs.getString("sharedGroupNames");

                int cid = insertContact(thos, displayName);
                insertContactMapping(thos, serviceId, cid);
                contactByServiceId.put(serviceId, cid);

                System.out.println("[contact] Angelegt: '" + displayName + "'"
                    + (!isBlank(sharedGroupNames) && !"[]".equals(sharedGroupNames)
                       ? " Gruppen: " + sharedGroupNames : ""));
            }
        }
    }

    // -------------------------------------------------------------------------
    // Chat-Member lazy eintragen
    // -------------------------------------------------------------------------

    private static void ensureChatMember(Connection thos, int chatId, int contactId) throws SQLException {
        String key = chatId + ":" + contactId;
        if (chatMemberCache.contains(key)) return;
        chatMemberCache.add(key);
        if (!DRY_RUN) {
            try (var ps = thos.prepareStatement(
                    "INSERT OR IGNORE INTO msg_chat_members (chat_id, contact_id) VALUES (?, ?)")) {
                ps.setInt(1, chatId);
                ps.setInt(2, contactId);
                ps.executeUpdate();
            }
        }
    }

    // -------------------------------------------------------------------------
    // Attachments für eine Message auflösen
    // -------------------------------------------------------------------------

    /**
     * Gibt alle Attachments einer Message zurück.
     * Fehlende Quelldatei → WARNING + available=false, Message läuft trotzdem durch.
     * size <= 0 → FailFast.
     */
    private static List<AttachmentResult> resolveAttachmentsForMessage(Connection signal,
                                                                        String signalMsgId) throws Exception {
        List<AttachmentResult> results = new ArrayList<>();

        try (var ps = signal.prepareStatement("""
                SELECT path, localKey, fileName, size, contentType, attachmentType, sentAt
                FROM message_attachments
                WHERE messageId = ? AND path IS NOT NULL AND localKey IS NOT NULL AND version = 2
                """)) {
            ps.setString(1, signalMsgId);

            try (var rs = ps.executeQuery()) {
                while (rs.next()) {
                    String signalPath  = rs.getString("path");
                    String localKey    = rs.getString("localKey");
                    String fileName    = rs.getString("fileName");
                    int    size        = rs.getInt("size");
                    String contentType = rs.getString("contentType");
                    String attachType  = rs.getString("attachmentType");
                    long   sentAt      = rs.getLong("sentAt");

                    if (size <= 0) fail("[FAILFAST] size=" + size + " für Attachment in messageId=" + signalMsgId);

                    String ext;
                    if (isBlank(contentType) || "application/octet-stream".equals(contentType)) {
                        if (isBlank(fileName))
                            fail("[FAILFAST] Kein contentType und kein fileName für messageId=" + signalMsgId);
                        int dot = fileName.lastIndexOf('.');
                        if (dot < 0 || dot == fileName.length() - 1)
                            fail("[FAILFAST] Keine Extension in fileName='" + fileName + "' für messageId=" + signalMsgId);
                        ext = fileName.substring(dot + 1);
                    } else {
                        ext = EXTENSION_MAP.get(contentType);
                        if (ext == null)
                            fail("[FAILFAST] Unbekannter contentType='" + contentType
                                + "' attachmentType='" + attachType + "' für messageId=" + signalMsgId);
                    }

                    String baseName = sanitizeFileName(!isBlank(fileName) ? stripExtension(fileName) : "attachment");
                    int    seq      = attachSeqPerMsg.merge(signalMsgId, 1, Integer::sum);
                    String outName  = baseName + "_" + signalMsgId + "_" + seq + "." + ext;
                    Path   outPath  = Path.of(SIGNAL_ATTACH_OUT).resolve(outName);

                    boolean available;

                    if (!DRY_RUN) {
                        if (Files.exists(outPath)) {
                            System.out.println("[attachment] Bereits vorhanden: " + outName);
                            available = true;
                        } else {
                            Path encryptedFile = Path.of(SIGNAL_ATTACH_IN).resolve(signalPath);
                            if (!Files.exists(encryptedFile)) {
                                System.out.println("[WARNING] Quelldatei nicht gefunden, übersprungen: "
                                    + encryptedFile + " (messageId=" + signalMsgId + ")");
                                available = false;
                            } else {
                                decryptAttachment(signalPath, localKey, size, outPath);
                                available = true;
                            }
                        }
                    } else {
                        System.out.println("[attachment] "
                            + LocalDateTime.ofInstant(Instant.ofEpochMilli(sentAt), ZoneId.systemDefault())
                            + " DRY_RUN: würde entschlüsseln → " + outName);
                        available = true; // Im DRY_RUN optimistisch annehmen
                    }

                    results.add(new AttachmentResult(outPath.toString(), available));
                }
            }
        }
        return results;
    }

    // -------------------------------------------------------------------------
    // DB-Hilfsmethoden
    // -------------------------------------------------------------------------

    private static int insertContact(Connection thos, String displayName) throws SQLException {
        if (!DRY_RUN) {
            try (var ps = thos.prepareStatement(
                    "INSERT INTO msg_contacts (display_name) VALUES (?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, displayName);
                ps.executeUpdate();
                try (var rs = ps.getGeneratedKeys()) {
                    if (!rs.next()) fail("[FAILFAST] Kein generated key nach INSERT contact '" + displayName + "'");
                    return rs.getInt(1);
                }
            }
        }
        return nextContactId++;
    }

    private static void insertContactMapping(Connection thos, String rawIdentifier, int contactId) throws SQLException {
        if (!DRY_RUN) {
            try (var ps = thos.prepareStatement(
                    "INSERT OR IGNORE INTO msg_contact_mapping (source, raw_identifier, contact_id) VALUES (?, ?, ?)")) {
                ps.setString(1, "signal");
                ps.setString(2, rawIdentifier);
                ps.setInt   (3, contactId);
                ps.executeUpdate();
            }
        }
    }

    private static int insertChat(Connection thos, String rawIdentifier, boolean isGroup,
                                   String displayName, boolean blacklisted) throws SQLException {
        if (!DRY_RUN) {
            try (var ps = thos.prepareStatement(
                    "INSERT INTO msg_chats (source, raw_identifier, is_group, display_name, blacklisted) VALUES (?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, "signal");
                ps.setString(2, rawIdentifier);
                ps.setInt   (3, isGroup ? 1 : 0);
                ps.setString(4, displayName);
                ps.setInt   (5, blacklisted ? 1 : 0);
                ps.executeUpdate();
                try (var rs = ps.getGeneratedKeys()) {
                    if (!rs.next()) fail("[FAILFAST] Kein generated key nach INSERT chat '" + displayName + "'");
                    return rs.getInt(1);
                }
            }
        }
        return nextChatId++;
    }

    /** Blacklisted Chat: wird in msg_chats mit blacklisted=1 eingetragen, nicht in chatByConversationId. */
    private static void insertChatBlacklisted(Connection thos, String rawIdentifier,
                                               boolean isGroup, String displayName) throws SQLException {
        insertChat(thos, rawIdentifier, isGroup, displayName, true);
    }

    // -------------------------------------------------------------------------
    // Allgemeine Hilfsmethoden
    // -------------------------------------------------------------------------

    private static String resolveDisplayName(ResultSet rs, String fallback) throws SQLException {
        List<String> candidates = new ArrayList<>();
        addIfNotBlank(candidates, stripBidiControls(rs.getString("name")));
        addIfNotBlank(candidates, buildFullName(rs.getString("profileName"), rs.getString("profileFamilyName")));
        addIfNotBlank(candidates, stripBidiControls(rs.getString("profileFullName")));
        return candidates.stream()
            .max(Comparator.comparingInt(String::length))
            .orElse(fallback);
    }

    private static String buildFullName(String first, String last) {
        first = stripBidiControls(first);
        last  = stripBidiControls(last);
        if (isBlank(first) && isBlank(last)) return null;
        if (isBlank(first)) return last.trim();
        if (isBlank(last))  return first.trim();
        return first.trim() + " " + last.trim();
    }

    // Entfernt Unicode-Bidi-Steuerzeichen die Signal automatisch um Profilnamen setzt
    // U+2066-U+2069: LRI, RLI, FSI, PDI / U+202A-U+202E: LRE, RLE, PDF, LRO, RLO / U+200E, U+200F: LRM, RLM
    private static String stripBidiControls(String s) {
        if (s == null) return null;
        return s.replaceAll("[\u2066-\u2069\u202A-\u202E\u200E\u200F]", "");
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static void addIfNotBlank(List<String> list, String s) {
        if (s != null && !s.isBlank()) list.add(s);
    }

    private static String millisToDbString(long millis) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault())
            .format(DB_FORMAT);
    }

    private static String sanitizeFileName(String name) {
        return name.replaceAll("[:\\\\/*?\"<>|]", "_");
    }

    private static String stripExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot > 0 ? fileName.substring(0, dot) : fileName;
    }

    private static void decryptAttachment(String signalRelPath, String localKey, int size, Path outPath) throws Exception {
        Path encryptedFile = Path.of(SIGNAL_ATTACH_IN).resolve(signalRelPath);
        if (!Files.exists(encryptedFile)) throw new IOException("Datei nicht gefunden: " + encryptedFile);

        byte[] keyBytes   = Base64.getDecoder().decode(localKey);
        byte[] aesKey     = Arrays.copyOf(keyBytes, 32);
        byte[] fileBytes  = Files.readAllBytes(encryptedFile);
        byte[] iv         = Arrays.copyOf(fileBytes, 16);
        int    cipherLen  = fileBytes.length - 16 - 32;
        byte[] ciphertext = Arrays.copyOfRange(fileBytes, 16, 16 + cipherLen);

        Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(aesKey, "AES"), new IvParameterSpec(iv));
        byte[] plaintext = cipher.doFinal(ciphertext);

        Files.createDirectories(outPath.getParent());
        Files.write(outPath, Arrays.copyOf(plaintext, size));
    }

    private static void fail(String msg) {
        throw new IllegalStateException(msg);
    }

    // -------------------------------------------------------------------------

    private record AttachmentResult(String path, boolean available) {}
}