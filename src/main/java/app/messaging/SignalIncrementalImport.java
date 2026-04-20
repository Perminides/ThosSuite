package app.messaging;

import app.config.Config;
import app.data.AppClock;
import app.data.persistence.DB;
import app.data.persistence.SignalRepository;
import app.ui.skin.SkinService;
import app.util.Log;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;



/**
 * Täglicher Incrementalimport neuer Signal-Nachrichten in die ThosSuite-DB.
 *
 * <h2>Ablauf</h2>
 * <ol>
 *   <li>Caches aus ThosSuite-DB laden (bekannte Chats, Kontakte, Blacklist).</li>
 *   <li>Letzten importierten Tag ermitteln. Ist er gestern → Abbruch, nichts zu tun.</li>
 *   <li>Integritätscheck: Alle importierbaren Signal-Nachrichten des letzten Tages
 *       müssen in der Suite-DB vorhanden sein. Fehlt eine → FailFast.</li>
 *   <li>Neue Nachrichten (DATE(sent_at) > letzter importierter Tag, &lt; heute) importieren.</li>
 *   <li>Attachments entschlüsseln und ins konfigurierte Verzeichnis schreiben.</li>
 *   <li>Commit. Bei Exception → Rollback.</li>
 * </ol>
 *
 * <h2>Neuer Chat</h2>
 * Blockierender Alert im FX-Thread. Import pausiert bis zur Nutzerantwort.
 * Bei Ablehnung wird blacklisted=1 in msg_chats gesetzt.
 *
 * <h2>Attachments</h2>
 * AES/CBC-Entschlüsselung. Ausgabepfad: [config:signal.attachmentDir]\dateiname.
 * In der DB wird nur der Dateiname (relativ zum signal-Verzeichnis) gespeichert.
 * Quelldatei fehlt → FailFast.
 *
 * <h2>Transaktionsverhalten</h2>
 * Eine einzige Transaktion über den kompletten Import. Entweder alles oder nichts.
 *
 * <h2>Filterkette</h2>
 * {@link #forEachImportableMessage} kapselt die vollständige Filterkette (Typ-Whitelist,
 * isErased, Blacklist, kein Body und keine Attachments) und wird sowohl vom
 * Integritätscheck als auch vom Import genutzt. Änderungen an der Filterlogik
 * müssen nur hier vorgenommen werden.
 * @TODO: Diverses Signalimport
 * <h2>ToDos</h2>
 * <ol>
 * 	<li>@TODO: Bei neuem Kontakt muss ich bestehenden auswählen können, also einen von Whatsapp z. B. Momentan legt der immer stillschweigend einen neuen an...</li>
 * </ol>
 */
public class SignalIncrementalImport {

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
        Map.entry("text/comma-separated-values",          "csv"),
        Map.entry("application/x-zip-compressed",         "zip"),
        Map.entry("application/zip",                      "zip"),
        Map.entry("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",       "xlsx"),
        Map.entry("application/vnd.openxmlformats-officedocument.wordprocessingml.document", "docx")
    );

    private final SignalRepository repo = new SignalRepository();

    // Caches — zu Beginn aus der Suite-DB geladen
    private final Map<String, Integer> chatByConversationId = new LinkedHashMap<>();
    private final Map<String, Integer> contactByServiceId   = new LinkedHashMap<>();
    private final Set<String>          blacklistedChats     = new HashSet<>();
    private final Set<String>          chatMemberCache      = new HashSet<>();

    // signalMsgId -> laufende Attachment-Nummer für eindeutige Dateinamen
    private final Map<String, Integer> attachSeqPerMsg = new HashMap<>();

    // -------------------------------------------------------------------------

    public void run() {
        // Schritt 1: Caches laden
        blacklistedChats.addAll(repo.loadBlacklistedConversationIds());
        chatByConversationId.putAll(repo.loadKnownChats());
        contactByServiceId.putAll(repo.loadKnownContacts());

        // Schritt 2: Letzten importierten Tag ermitteln
        LocalDate lastDay = repo.getLastImportedDay();
        if (lastDay != null && lastDay.equals(AppClock.TODAY.minusDays(1))) {
            Log.info(this, "[signal] Bereits gestern importiert, nichts zu tun.");
            return;
        }

        String signalUrl = "jdbc:sqlite:" + Config.getString("signal.path") + "/sql/db.sqlite"
            + "?cipher=sqlcipher&key=x'" + Config.getString("signal.key") + "'&legacy=4";

        try (Connection signal = DriverManager.getConnection(signalUrl);
             Connection thos   = DB.getNewConnection()) {

            thos.setAutoCommit(false);

            try {
                // Schritt 3: Integritätscheck letzter importierter Tag
                if (lastDay != null)
                    checkIntegrity(signal, lastDay);

                // Schritt 4+5: Import
                importMessages(signal, thos, lastDay);

                thos.commit();
                Log.info(this, "[signal] Import abgeschlossen.");

            } catch (Exception e) {
                thos.rollback();
                Log.error(SignalIncrementalImport.class, "Signal-Import fehlgeschlagen, Rollback durchgeführt", e);
                throw new RuntimeException("Signal-Import fehlgeschlagen, Rollback durchgeführt", e);
            }

        } catch (SQLException e) {
        	Log.error(SignalIncrementalImport.class, "Signal-DB-Verbindung fehlgeschlagen", e);
            throw new RuntimeException("Signal-DB-Verbindung fehlgeschlagen", e);
        }
    }

    // -------------------------------------------------------------------------
    // Gemeinsame Filterkette
    // -------------------------------------------------------------------------

    /**
     * Iteriert über alle importierbaren Signal-Nachrichten und ruft den Consumer für jede auf.
     *
     * <p>Kapselt die vollständige Filterkette:
     * <ul>
     *   <li>Typ-Whitelist: nur incoming und outgoing</li>
     *   <li>isErased = 1 → übersprungen</li>
     *   <li>Blacklisted Chat → übersprungen</li>
     *   <li>Kein Body und keine Attachments → übersprungen</li>
     * </ul>
     *
     * <p>Wird sowohl vom Integritätscheck ({@link #checkIntegrity}) als auch vom
     * Import ({@link #importMessages}) genutzt. Änderungen an der Filterlogik
     * müssen nur hier vorgenommen werden.
     *
     * @param signal      Verbindung zur Signal-DB
     * @param dateClause  SQL-DATE-Bedingung als String, wird direkt nach WHERE DATE(...) eingefügt
     * @param params      Parameter für die dateClause in Reihenfolge
     * @param allowWrite  true beim Import (legt Chats an, entschlüsselt Attachments),
     *                    false beim Integritätscheck (unbekannter Chat → FailFast)
     * @param thos        ThosSuite-Verbindung (nur benötigt wenn allowWrite=true)
     * @param consumer    Wird für jede importierbare Nachricht aufgerufen
     */
    private void forEachImportableMessage(Connection signal, String dateClause, List<String> params,
                                          boolean allowWrite, Connection thos,
                                          ThrowingConsumer consumer) throws Exception {
        String sql = """
                SELECT id, conversationId, type, body, sent_at, sourceServiceId, isErased,
                       json_extract(json, '$.quote')           AS quote_json,
                       json_extract(json, '$.quote.messageId') AS quote_message_id,
                       json_extract(json, '$.quote.id')        AS quote_id
                FROM messages
                WHERE DATE(sent_at / 1000, 'unixepoch', 'localtime') \
                """ + dateClause + "\nORDER BY sent_at ASC";

        try (var ps = signal.prepareStatement(sql)) {
            for (int i = 0; i < params.size(); i++)
                ps.setString(i + 1, params.get(i));

            try (var rs = ps.executeQuery()) {
                while (rs.next()) {
                    String  signalMsgId     = rs.getString("id");
                    String  conversationId  = rs.getString("conversationId");
                    String  msgType         = rs.getString("type");
                    String  body            = rs.getString("body");
                    long    sentAtMs        = rs.getLong("sent_at");
                    String  sourceServiceId = rs.getString("sourceServiceId");
                    int     isErased        = rs.getInt("isErased");
                    String  quoteJson       = rs.getString("quote_json");
                    String  quoteMsgId      = rs.getString("quote_message_id");
                    long    quoteId         = rs.getLong("quote_id");
                    boolean outgoing        = "outgoing".equals(msgType);

                    if (!"incoming".equals(msgType) && !"outgoing".equals(msgType)) continue;
                    if (isErased == 1) continue;

                    if (!chatByConversationId.containsKey(conversationId)
                            && !blacklistedChats.contains(conversationId)) {
                        if (!allowWrite)
                            throw new IllegalStateException("[FAILFAST] Integritätscheck: unbekannter Chat conversationId='" + conversationId + "'");
                        ensureChat(signal, thos, conversationId);
                    }
                    if (blacklistedChats.contains(conversationId)) continue;

                    List<AttachmentResult> attachments = resolveAttachments(signal, signalMsgId, allowWrite);

                    if (isBlank(body) && attachments.isEmpty()) {
                        Log.warn(this, "[signal] Keine Inhalte, übersprungen: signalMsgId=" + signalMsgId);
                        continue;
                    }

                    String effectiveServiceId = (outgoing && isBlank(sourceServiceId))
                        ? MY_SERVICE_ID : sourceServiceId;
                    if (isBlank(effectiveServiceId))
                        throw new IllegalStateException("[FAILFAST] effectiveServiceId blank für signalMsgId=" + signalMsgId);

                    consumer.accept(new SignalMessage(
                        signalMsgId, conversationId, body, sentAtMs,
                        effectiveServiceId, outgoing, quoteJson, quoteMsgId, quoteId, attachments
                    ));
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // Integritätscheck
    // -------------------------------------------------------------------------

    /**
     * Prüft ob alle importierbaren Signal-Nachrichten des letzten Tages in der Suite-DB vorhanden sind.
     *
     * Beim Incrementalimport sind alle Chats bereits aus der Suite-DB in die Caches geladen.
     * Ein unbekannter Chat am letzten importierten Tag wäre ein Widerspruch — er hätte beim
     * damaligen Import angelegt werden müssen. Tritt er auf, deutet das auf ein
     * Datenkonsistenzproblem hin und wird als FailFast behandelt.
     *
     * Fehlt eine importierbare Nachricht in der Suite-DB → FailFast.
     */
    private void checkIntegrity(Connection signal, LocalDate day) throws Exception {
        Set<String> importedIds = repo.loadImportedSourceIdsForDay(day);
        forEachImportableMessage(signal, "= ?", List.of(day.toString()), false, null, msg -> {
            if (!importedIds.contains(msg.signalMsgId()))
                throw new IllegalStateException("[FAILFAST] Integritätscheck: Signal-Nachricht nicht in Suite-DB: source_id="
                    + msg.signalMsgId() + " day=" + day);
        });
    }

    // -------------------------------------------------------------------------
    // Hauptschleife
    // -------------------------------------------------------------------------

    private void importMessages(Connection signal, Connection thos, LocalDate lastDay) throws Exception {
        String cutoff = lastDay != null ? lastDay.toString() : "1970-01-01";
        int[] msgCount    = {0};
        int[] attachCount = {0};

        forEachImportableMessage(signal,
            "> ? AND DATE(sent_at / 1000, 'unixepoch', 'localtime') < ?",
            List.of(cutoff, AppClock.TODAY.toString()),
            true, thos, msg -> {

                if (!contactByServiceId.containsKey(msg.effectiveServiceId()))
                    ensureContact(signal, thos, msg.effectiveServiceId());

                int fromContact = contactByServiceId.get(msg.effectiveServiceId());
                int chatId      = chatByConversationId.get(msg.conversationId());
                ensureChatMember(thos, chatId, fromContact);

                String resolvedQuoteMsgId = resolveQuote(signal, msg.signalMsgId(),
                    msg.quoteJson(), msg.quoteMsgId(), msg.quoteId());

                repo.insertMessage(thos, msg.signalMsgId(), millisToDbString(msg.sentAtMs()),
                    fromContact, chatId, msg.body(), resolvedQuoteMsgId);
                msgCount[0]++;

                for (AttachmentResult att : msg.attachments()) {
                    repo.insertAttachment(thos, msg.signalMsgId(), att.relativePath(), att.available());
                    attachCount[0]++;
                }
            });

        Log.info(this, "[signal] Importiert: " + msgCount[0] + " Nachrichten, " + attachCount[0] + " Attachments.");
    }

    // -------------------------------------------------------------------------
    // Chat lazy anlegen
    // -------------------------------------------------------------------------

    private void ensureChat(Connection signal, Connection thos, String conversationId) throws SQLException {
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

                boolean isGroup     = "group".equals(rs.getString("type"));
                String  displayName = isGroup
                    ? (isBlank(rs.getString("name")) ? "Gruppe_" + conversationId : rs.getString("name"))
                    : resolveDisplayName(rs, conversationId);
                int     messageCount = rs.getInt("messageCount");
                String  sharedGroups = rs.getString("sharedGroupNames");

                String info = "Name: " + displayName
                    + "\nGruppe: " + isGroup
                    + "\nNachrichten: " + messageCount
                    + (isBlank(sharedGroups) || "[]".equals(sharedGroups) ? "" : "\nGemeinsame Gruppen: " + sharedGroups);

                ButtonType importBtn    = new ButtonType("Importieren", ButtonBar.ButtonData.YES);
                ButtonType blacklistBtn = new ButtonType("Blacklist",   ButtonBar.ButtonData.NO);

                Optional<ButtonType> result = SkinService.get()
                    .createAlert(SkinService.getOwnerWindow(), "Neuer Signal-Chat", info, importBtn, blacklistBtn)
                    .showAndWait();

                boolean doImport = result.isPresent() && result.get() == importBtn;
                int chatId = repo.insertChat(thos, conversationId, isGroup, displayName, !doImport);

                if (doImport)
                    chatByConversationId.put(conversationId, chatId);
                else
                    blacklistedChats.add(conversationId);
            }
        }
    }

    // -------------------------------------------------------------------------
    // Kontakt lazy anlegen
    // -------------------------------------------------------------------------

    private void ensureContact(Connection signal, Connection thos, String serviceId) throws SQLException {
        try (var ps = signal.prepareStatement("""
                SELECT name, profileName, profileFamilyName, profileFullName,
                       json_extract(json, '$.sharedGroupNames') AS sharedGroupNames
                FROM conversations WHERE serviceId = ?
                """)) {
            ps.setString(1, serviceId);
            try (var rs = ps.executeQuery()) {
                if (!rs.next())
                    throw new IllegalStateException("[FAILFAST] serviceId='" + serviceId + "' nicht in Signal-conversations");

                String displayName = resolveDisplayName(rs, serviceId);
                int cid = repo.insertContact(thos, displayName);
                repo.insertContactMapping(thos, serviceId, cid);
                contactByServiceId.put(serviceId, cid);
                Log.info(this, "[signal] Kontakt angelegt: '" + displayName + "'");
            }
        }
    }

    // -------------------------------------------------------------------------
    // Chat-Member
    // -------------------------------------------------------------------------

    private void ensureChatMember(Connection thos, int chatId, int contactId) throws SQLException {
        String key = chatId + ":" + contactId;
        if (chatMemberCache.contains(key)) return;
        chatMemberCache.add(key);
        repo.insertChatMemberIfAbsent(thos, chatId, contactId);
    }

    // -------------------------------------------------------------------------
    // Attachments
    // -------------------------------------------------------------------------

    /**
     * Gibt alle importierbaren Attachments einer Message zurück.
     *
     * @param allowWrite true beim Import: Quelldatei wird entschlüsselt und ins Zielverzeichnis geschrieben.
     *                   false beim Integritätscheck: Keine Dateioperationen — Attachments des letzten
     *                   Tages existieren bereits auf der Platte.
     */
    private List<AttachmentResult> resolveAttachments(Connection signal, String signalMsgId,
                                                       boolean allowWrite) throws Exception {
        List<AttachmentResult> results = new ArrayList<>();

        try (var ps = signal.prepareStatement("""
                SELECT path, localKey, fileName, size, contentType, attachmentType
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

                    if (size <= 0)
                        throw new IllegalStateException("[FAILFAST] size=" + size + " für Attachment in messageId=" + signalMsgId);

                    String ext;
                    if (isBlank(contentType) || "application/octet-stream".equals(contentType)) {
                        if (isBlank(fileName))
                            throw new IllegalStateException("[FAILFAST] Kein contentType und kein fileName für messageId=" + signalMsgId);
                        int dot = fileName.lastIndexOf('.');
                        if (dot < 0 || dot == fileName.length() - 1)
                            throw new IllegalStateException("[FAILFAST] Keine Extension in fileName='" + fileName + "' für messageId=" + signalMsgId);
                        ext = fileName.substring(dot + 1);
                    } else {
                        ext = EXTENSION_MAP.get(contentType);
                        if (ext == null)
                            throw new IllegalStateException("[FAILFAST] Unbekannter contentType='" + contentType
                                + "' attachmentType='" + attachType + "' für messageId=" + signalMsgId);
                    }

                    String baseName  = sanitizeFileName(!isBlank(fileName) ? stripExtension(fileName) : "attachment");
                    int    seq       = attachSeqPerMsg.merge(signalMsgId, 1, Integer::sum);
                    String outName   = baseName + "_" + signalMsgId + "_" + seq + "." + ext;
                    Path   attachDir = Path.of(Config.getString("signal.attachmentDir"));
                    Path   outPath   = attachDir.resolve(outName);

                    boolean available;
                    if (Files.exists(outPath)) {
                        available = true;
                    } else if (allowWrite) {
                        Path encryptedFile = Path.of(Config.getString("signal.path") + "/attachments.noindex/").resolve(signalPath);
                        if (!Files.exists(encryptedFile))
                            throw new IllegalStateException("[FAILFAST] Quelldatei nicht gefunden: " + encryptedFile + " (messageId=" + signalMsgId + ")");
                        decryptAttachment(encryptedFile, localKey, size, outPath);
                        available = true;
                    } else {
                        // Integritätscheck: Datei wird erst beim Import erstellt, das ist kein Fehler
                        available = true;
                    }

                    results.add(new AttachmentResult(outName, available));
                }
            }
        }
        return results;
    }

    // -------------------------------------------------------------------------
    // Quote-Auflösung (dreistufig)
    // -------------------------------------------------------------------------

    private String resolveQuote(Connection signal, String signalMsgId,
                                String quoteJson, String quoteMsgId, long quoteId) throws SQLException {
        if (quoteJson == null) return null;

        if (!isBlank(quoteMsgId)) {
            String resolved = resolveQuoteByMessageId(signal, quoteMsgId);
            if (resolved != null) return resolved;
            throw new IllegalStateException("[FAILFAST] quote.messageId='" + quoteMsgId
                + "' nicht in Signal-DB (signalMsgId=" + signalMsgId + ")");
        }

        if (quoteId > 0) {
            String resolved = resolveQuoteByTimestamp(signal, quoteId);
            if (resolved != null) return resolved;
            resolved = resolveQuoteByEditHistory(signal, quoteId);
            if (resolved != null) return resolved;
        }

        throw new IllegalStateException("[FAILFAST] Quote nicht auflösbar für signalMsgId=" + signalMsgId
            + " quote.id=" + quoteId + " quote.messageId=" + quoteMsgId);
    }

    private String resolveQuoteByMessageId(Connection signal, String messageId) throws SQLException {
        try (var ps = signal.prepareStatement("SELECT id FROM messages WHERE id = ?")) {
            ps.setString(1, messageId);
            try (var rs = ps.executeQuery()) {
                return rs.next() ? rs.getString("id") : null;
            }
        }
    }

    private String resolveQuoteByTimestamp(Connection signal, long quoteId) throws SQLException {
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

    private String resolveQuoteByEditHistory(Connection signal, long quoteId) throws SQLException {
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
    // Entschlüsselung
    // -------------------------------------------------------------------------

    private void decryptAttachment(Path encryptedFile, String localKey, int size, Path outPath) throws Exception {
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
        if (Files.notExists(outPath))
            throw new RuntimeException("[FAILFAST] Attachment wurde nicht geschrieben: " + outPath);
    }

    // -------------------------------------------------------------------------
    // Hilfsmethoden
    // -------------------------------------------------------------------------

    private String resolveDisplayName(ResultSet rs, String fallback) throws SQLException {
        List<String> candidates = new ArrayList<>();
        addIfNotBlank(candidates, stripBidiControls(rs.getString("name")));
        addIfNotBlank(candidates, buildFullName(rs.getString("profileName"), rs.getString("profileFamilyName")));
        addIfNotBlank(candidates, stripBidiControls(rs.getString("profileFullName")));
        return candidates.stream().max(Comparator.comparingInt(String::length)).orElse(fallback);
    }

    private String buildFullName(String first, String last) {
        first = stripBidiControls(first);
        last  = stripBidiControls(last);
        if (isBlank(first) && isBlank(last)) return null;
        if (isBlank(first)) return last.trim();
        if (isBlank(last))  return first.trim();
        return first.trim() + " " + last.trim();
    }

    private String stripBidiControls(String s) {
        if (s == null) return null;
        return s.replaceAll("[\u2066-\u2069\u202A-\u202E\u200E\u200F]", "");
    }

    private boolean isBlank(String s)                      { return s == null || s.isBlank(); }
    private void addIfNotBlank(List<String> list, String s) { if (!isBlank(s)) list.add(s); }

    private String millisToDbString(long millis) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault()).format(DB_FORMAT);
    }

    private String sanitizeFileName(String name) {
        return name.replaceAll("[,:\\\\/*?\"<>|]", "_");
    }

    private String stripExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot > 0 ? fileName.substring(0, dot) : fileName;
    }

    // -------------------------------------------------------------------------
    // Records
    // -------------------------------------------------------------------------

    @FunctionalInterface
    private interface ThrowingConsumer {
        void accept(SignalMessage msg) throws Exception;
    }

    private record SignalMessage(
        String signalMsgId,
        String conversationId,
        String body,
        long sentAtMs,
        String effectiveServiceId,
        boolean outgoing,
        String quoteJson,
        String quoteMsgId,
        long quoteId,
        List<AttachmentResult> attachments
    ) {}

    private record AttachmentResult(String relativePath, boolean available) {}
}