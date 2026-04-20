package app.messaging;

import app.config.Config;
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
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;



/**
 * Importiert neue Signal-Nachrichten seit dem letzten erfolgreichen Import-Run in die ThosSuite-DB.
 *
 * <h2>Ablauf</h2>
 * <ol>
 *   <li>Caches aus ThosSuite-DB laden (bekannte Chats, Kontakte, Blacklist).</li>
 *   <li>Untere Importgrenze ermitteln: MAX(sent_at) der bereits importierten Signal-Nachrichten
 *       als Sekunden, minus 5 Minuten Überlappungspuffer. Sind noch keine vorhanden,
 *       wird Epoch (0) verwendet.</li>
 *   <li>Obere Importgrenze: jetzt − 5 Minuten (Puffer gegen noch nicht vollständig
 *       geschriebene Nachrichten in der Signal-DB).</li>
 *   <li>Ist das Fenster leer (obere ≤ untere Grenze) → Abbruch, nichts zu tun.</li>
 *   <li>Neue Nachrichten importieren, Attachments entschlüsseln. Bereits importierte
 *       Nachrichten im Überlappungsbereich werden per DB-Abfrage erkannt und übersprungen.</li>
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
 * isErased, bereits importiert, Blacklist, kein Body und keine Attachments). Änderungen
 * an der Filterlogik müssen nur hier vorgenommen werden.
 * 
 * <h2>ToDos</h2>
 * <ol>
 * 	<li>Bei neuem Kontakt muss ich bestehenden auswählen können, also einen von Whatsapp z. B. Momentan legt der immer stillschweigend einen neuen an...</li>
 *  <li>Für die Integration ins Tagebuch, wird das Generieren von Thumbnails benötigt. Und damit muss das wohl vor dem Schließen des Splahscreens passieren. Also es wird zu einem Pre-Task</li>
 * </ol>
 * 
 * @TODO: Diverses Signalimport
 */
public class SignalIncrementalImport {

    private static final String MY_SERVICE_ID = "439b7480-bdcd-409f-a397-0fb85faa3a83";
    private static final DateTimeFormatter DB_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    /** Puffer am oberen Ende des Importfensters: Nachrichten jünger als dieser Wert werden ignoriert. */
    private static final long CUTOFF_BUFFER_MS = 5 * 60 * 1000L;

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

        // Schritt 2: Letzten Import-Timestamp lesen.
        // null = noch keine Signal-Nachrichten importiert → untere Grenze ist Epoch (alles importieren).
        // Puffer von 5 Minuten nach unten: sent_at ist nur sekundengenau, damit Nachrichten
        // der letzten importierten Sekunde nicht übersprungen werden. Bereits importierte
        // Nachrichten in diesem Überlappungsbereich werden in der Filterkette per DB-Abfrage erkannt
        // und übersprungen.
        long lastRunMs = Optional.ofNullable(repo.getLastImportRunTimestamp()).orElse(0L) - CUTOFF_BUFFER_MS;
        long cutoffMs  = System.currentTimeMillis() - CUTOFF_BUFFER_MS;

        if (cutoffMs <= lastRunMs) {
            Log.info(this, "[signal] Importfenster leer (lastRun=" + lastRunMs + ", cutoff=" + cutoffMs + "), nichts zu tun.");
            return;
        }

        String signalUrl = "jdbc:sqlite:" + Config.getString("signal.path") + "/sql/db.sqlite"
            + "?cipher=sqlcipher&key=x'" + Config.getString("signal.key") + "'&legacy=4";

        try (Connection signal = DriverManager.getConnection(signalUrl);
             Connection thos   = DB.getNewConnection()) {

            thos.setAutoCommit(false);

            try {
                importMessages(signal, thos, lastRunMs, cutoffMs);
                thos.commit();
            } catch (Exception e) {
                thos.rollback();
                throw new RuntimeException("Signal-Import fehlgeschlagen, Rollback durchgeführt", e);
            }

        } catch (SQLException e) {
            throw new RuntimeException("Signal-DB-Verbindung fehlgeschlagen", e);
        }

        Log.info(this, "[signal] Import abgeschlossen.");
    }

    // -------------------------------------------------------------------------
    // Gemeinsame Filterkette
    // -------------------------------------------------------------------------

    /**
     * Iteriert über alle importierbaren Signal-Nachrichten im angegebenen Zeitfenster
     * und ruft den Consumer für jede auf.
     *
     * <p>Kapselt die vollständige Filterkette:
     * <ul>
     *   <li>Typ-Whitelist: nur incoming und outgoing</li>
     *   <li>isErased = 1 → übersprungen</li>
     *   <li>Bereits in Suite-DB vorhanden → übersprungen (Überlappungspuffer)</li>
     *   <li>Blacklisted Chat → übersprungen</li>
     *   <li>Kein Body und keine Attachments → übersprungen</li>
     * </ul>
     *
     * @param signal     Verbindung zur Signal-DB
     * @param lastRunMs  untere Grenze des Importfensters (exklusiv), Millisekunden seit Epoch
     * @param cutoffMs   obere Grenze des Importfensters (exklusiv), Millisekunden seit Epoch
     * @param thos       ThosSuite-Verbindung (für lazy Chat-Anlage und Already-imported-Check)
     * @param consumer   Wird für jede importierbare Nachricht aufgerufen
     */
    private void forEachImportableMessage(Connection signal, long lastRunMs, long cutoffMs,
                                          Connection thos,
                                          ThrowingConsumer consumer) throws Exception {
        String sql = """
                SELECT id, conversationId, type, body, sent_at, sourceServiceId, isErased,
                       json_extract(json, '$.quote')           AS quote_json,
                       json_extract(json, '$.quote.messageId') AS quote_message_id,
                       json_extract(json, '$.quote.id')        AS quote_id
                FROM messages
                WHERE sent_at > ? AND sent_at < ?
                ORDER BY sent_at ASC
                """;

        try (var ps = signal.prepareStatement(sql)) {
            ps.setLong(1, lastRunMs);
            ps.setLong(2, cutoffMs);

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
                    if (repo.isAlreadyImported(thos, signalMsgId)) continue;

                    if (!chatByConversationId.containsKey(conversationId)
                            && !blacklistedChats.contains(conversationId)) {
                        ensureChat(signal, thos, conversationId);
                    }
                    if (blacklistedChats.contains(conversationId)) continue;

                    List<AttachmentResult> attachments = resolveAttachments(signal, signalMsgId);

                    if (isBlank(body) && attachments.isEmpty()) {
                        Log.warn(this, "[signal] Keine Inhalte, übersprungen: signalMsgId=" + signalMsgId
                            + " type=" + msgType + " conversationId=" + conversationId);
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
    // Hauptschleife
    // -------------------------------------------------------------------------

    private void importMessages(Connection signal, Connection thos,
                                long lastRunMs, long cutoffMs) throws Exception {
        int[] msgCount    = {0};
        int[] attachCount = {0};

        Log.info(this, "[signal] Importiere Nachrichten: " + millisToDbString(lastRunMs)
            + " → " + millisToDbString(cutoffMs));

        forEachImportableMessage(signal, lastRunMs, cutoffMs, thos, msg -> {

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

    private List<AttachmentResult> resolveAttachments(Connection signal,
                                                       String signalMsgId) throws Exception {
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
                    } else {
                        Path encryptedFile = Path.of(Config.getString("signal.path") + "/attachments.noindex/").resolve(signalPath);
                        if (!Files.exists(encryptedFile))
                            throw new IllegalStateException("[FAILFAST] Quelldatei nicht gefunden: " + encryptedFile + " (messageId=" + signalMsgId + ")");
                        decryptAttachment(encryptedFile, localKey, size, outPath);
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