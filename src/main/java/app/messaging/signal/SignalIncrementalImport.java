package app.messaging.signal;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import app.messaging.repository.MessageRepository;
import app.messaging.signal.model.AttachmentInfo;
import app.messaging.signal.model.ContactInfo;
import app.messaging.signal.model.ConversationInfo;
import app.messaging.signal.repository.SignalSourceRepository;
import app.shared.Config;
import app.shared.DB;
import app.shared.Log;
import app.shared.ThrowingConsumer;
import app.shared.model.DialogButton;
import app.shared.skin.SkinService;

/**
 * Importiert neue Signal-Nachrichten seit dem letzten erfolgreichen Import-Run in die ThosSuite-DB.
 *
 * <h2>Ablauf</h2>
 * <ol>
 *   <li>Caches aus ThosSuite-DB laden (bekannte Chats, Kontakte, Blacklist).</li>
 *   <li>Letzte importierte source_id aus der Suite-DB lesen. Darüber wird der zugehörige
 *       sent_at direkt aus der Signal-DB gelesen — keine Zeitzonenkonvertierung nötig.
 *       Sind noch keine Nachrichten importiert, wird Epoch (0) als untere Grenze verwendet.</li>
 *   <li>Obere Importgrenze: jetzt − 5 Minuten (Puffer gegen noch nicht vollständig
 *       geschriebene Nachrichten in der Signal-DB).</li>
 *   <li>Neue Nachrichten (sent_at >= untere Grenze AND sent_at < obere Grenze) importieren,
 *       Attachments entschlüsseln. Bereits importierte Nachrichten im Überlappungsbereich
 *       (sent_at == untere Grenze) werden per DB-Abfrage erkannt und übersprungen.</li>
 *   <li>Commit. Bei Exception → Rollback.</li>
 * </ol>
 *
 * <h2>Neuer Chat</h2>
 * Blockierender Alert im FX-Thread. Import pausiert bis zur Nutzerantwort.
 * Bei Ablehnung wird blacklisted=1 in msg_chats gesetzt.
 *
 * <h2>Attachments</h2>
 * AES/CBC-Entschlüsselung. Ausgabepfad: [config:signalAttachmentsFolder]\dateiname.
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
 * <ul>
 * 	<li>@TODO: Bei neuem Kontakt muss ich bestehenden auswählen können, also einen von Whatsapp z. B. Momentan legt der immer stillschweigend einen neuen an...</li>
 *  <li>Wir brauchen noch einen monatlichen Importcheck, der den letzten Monat überprüft, ob alle Nachrichten drin sind.</li>
 *  <li>Sollten Chats nicht auch nur zeitweise stummgeschaltet werden dürfen? Also z. B. der Betriebssport-Chat in den Zeiten, wo ich nicht angemeldet bin?</li>
 * </ul>
 * 
 * @TODO: Diverses Signalimport
 */
public class SignalIncrementalImport {

    private static final String MY_SERVICE_ID = "439b7480-bdcd-409f-a397-0fb85faa3a83";
    private static final String signalId = "signal";

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
        Map.entry("text/plain",  "txt"),
        Map.entry("text/comma-separated-values",          "csv"),
        Map.entry("application/x-zip-compressed",         "zip"),
        Map.entry("application/zip",                      "zip"),
        Map.entry("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",       "xlsx"),
        Map.entry("application/vnd.openxmlformats-officedocument.wordprocessingml.document", "docx")
    );

    private final MessageRepository       repo   = new MessageRepository();
    private final SignalSourceRepository source = new SignalSourceRepository();

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
        blacklistedChats.addAll(repo.loadBlacklistedChatIds(signalId));
        chatByConversationId.putAll(repo.loadKnownChats(signalId));
        contactByServiceId.putAll(repo.loadKnownContacts(signalId));

        long cutoffMs = System.currentTimeMillis() - CUTOFF_BUFFER_MS;

        String signalUrl = "jdbc:sqlite:" + Config.getString("signal.externalPath") + "/sql/db.sqlite"
            + "?cipher=sqlcipher&key=x'" + Config.getString("signal.key") + "'&legacy=4";

        try (Connection signalConnection = DriverManager.getConnection(signalUrl);
             Connection suiteConnection   = DB.getNewConnection()) {

            // Schritt 2: Letzte importierte source_id aus Suite-DB — Signal-DB macht den Rest
            String lastSourceId = repo.getLastImportedSourceId(signalId);

            Log.info(this, "[signal] Importiere Nachrichten ab source_id=" + lastSourceId
                + " bis " + LocalDateTime.ofInstant(Instant.ofEpochMilli(cutoffMs), ZoneId.systemDefault()));

            suiteConnection.setAutoCommit(false);

            try {
                importMessages(signalConnection, suiteConnection, lastSourceId, cutoffMs);
                suiteConnection.commit();
            } catch (Exception e) {
                suiteConnection.rollback();
                throw new RuntimeException("Signal-Import fehlgeschlagen, Rollback durchgeführt", e);
            }

        } catch (SQLException e) {
            throw new RuntimeException("Signal-DB-Verbindung fehlgeschlagen", e);
        }

        Log.info(this, "[signal] Import abgeschlossen.");
    }

    // -------------------------------------------------------------------------
    // Filterkette
    // -------------------------------------------------------------------------

    /**
     * Iteriert über alle importierbaren Signal-Nachrichten nach der letzten bekannten
     * Nachricht bis cutoffMs und ruft den Consumer für jede auf.
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
     * @param signalConnection        Verbindung zur Signal-DB
     * @param lastSourceId  source_id der letzten importierten Nachricht, oder null beim ersten Import
     * @param cutoffMs      obere Grenze (exklusiv), Millisekunden seit Epoch
     * @param suiteConnection          ThosSuite-Verbindung (für Already-imported-Check und lazy Chat-Anlage)
     * @param consumer      Wird für jede importierbare Nachricht aufgerufen
     */
    private void forEachImportableMessage(Connection signalConnection, String lastSourceId, long cutoffMs,
                                          Connection suiteConnection,
                                          ThrowingConsumer<SignalMessage> consumer) throws Exception {
        source.forEachMessageInWindow(signalConnection, lastSourceId, cutoffMs, rs -> {
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

            if (!"incoming".equals(msgType) && !"outgoing".equals(msgType)) return;
            if (isErased == 1) return;
            if (repo.isAlreadyImported(suiteConnection, signalId, signalMsgId)) return;

            if (!chatByConversationId.containsKey(conversationId)
                    && !blacklistedChats.contains(conversationId)) {
                ensureChat(signalConnection, suiteConnection, conversationId);
            }
            if (blacklistedChats.contains(conversationId)) return;

            List<AttachmentResult> attachments = resolveAttachments(signalConnection, signalMsgId);

            if (isBlank(body) && attachments.isEmpty()) {
                Log.warn(this, "[signal] Keine Inhalte, übersprungen: signalMsgId=" + signalMsgId
                    + " type=" + msgType + " conversationId=" + conversationId);
                return;
            }

            String effectiveServiceId = (outgoing && isBlank(sourceServiceId))
                ? MY_SERVICE_ID : sourceServiceId;
            if (isBlank(effectiveServiceId))
                throw new IllegalStateException("[FAILFAST] effectiveServiceId blank für signalMsgId=" + signalMsgId);

            consumer.accept(new SignalMessage(
                signalMsgId, conversationId, body, sentAtMs,
                effectiveServiceId, outgoing, quoteJson, quoteMsgId, quoteId, attachments
            ));
        });
    }

    // -------------------------------------------------------------------------
    // Hauptschleife
    // -------------------------------------------------------------------------

    private void importMessages(Connection signalConnection, Connection suiteConnection,
                                String lastSourceId, long cutoffMs) throws Exception {
        int[] msgCount    = {0};
        int[] attachCount = {0};

        forEachImportableMessage(signalConnection, lastSourceId, cutoffMs, suiteConnection, msg -> {

            if (!contactByServiceId.containsKey(msg.effectiveServiceId()))
                ensureContact(signalConnection, suiteConnection, msg.effectiveServiceId());

            int fromContact = contactByServiceId.get(msg.effectiveServiceId());
            int chatId      = chatByConversationId.get(msg.conversationId());
            ensureChatMember(suiteConnection, chatId, fromContact);

            String resolvedQuoteMsgId = resolveQuote(signalConnection, msg.signalMsgId(),
                msg.quoteJson(), msg.quoteMsgId(), msg.quoteId());

            repo.insertMessage(suiteConnection, signalId, msg.signalMsgId(),
            	    LocalDateTime.ofInstant(Instant.ofEpochMilli(msg.sentAtMs()), ZoneId.systemDefault()),
            	    fromContact, chatId, msg.body(), resolvedQuoteMsgId);
            msgCount[0]++;

            for (AttachmentResult att : msg.attachments()) {
                repo.insertAttachment(suiteConnection, signalId, msg.signalMsgId(), att.relativePath(), att.available());
                attachCount[0]++;
            }
        });

        Log.info(this, "[signal] Importiert: " + msgCount[0] + " Nachrichten, " + attachCount[0] + " Attachments.");
    }

    // -------------------------------------------------------------------------
    // Chat lazy anlegen
    // -------------------------------------------------------------------------

    private void ensureChat(Connection signalConnection, Connection suiteConnection, String conversationId) throws SQLException {
        ConversationInfo conv = source.loadConversation(signalConnection, conversationId);

        String displayName = conv.isGroup()
            ? (isBlank(conv.name()) ? "Gruppe_" + conversationId : conv.name())
            : resolveDisplayName(conv.name(), conv.profileName(), conv.profileFamilyName(), conv.profileFullName(), conversationId);

        String info = "Name: " + displayName
            + "\nGruppe: " + conv.isGroup()
            + "\nNachrichten: " + conv.messageCount()
            + (isBlank(conv.sharedGroupNames()) || "[]".equals(conv.sharedGroupNames()) ? "" : "\nGemeinsame Gruppen: " + conv.sharedGroupNames());

        DialogButton result = SkinService.get()
            .showAlert("Neuer Signal-Chat", info, DialogButton.IMPORT, DialogButton.BLACKLIST);

        boolean doImport = result == DialogButton.IMPORT;
        int chatId = repo.insertChat(suiteConnection, signalId, conversationId, conv.isGroup(), displayName, !doImport);

        if (doImport)
            chatByConversationId.put(conversationId, chatId);
        else
            blacklistedChats.add(conversationId);
    }

    // -------------------------------------------------------------------------
    // Kontakt lazy anlegen
    // -------------------------------------------------------------------------

    private void ensureContact(Connection signalConnection, Connection suiteConnection, String serviceId) throws SQLException {
        ContactInfo contact = source.loadContact(signalConnection, serviceId);
        String displayName = resolveDisplayName(contact.name(), contact.profileName(),
            contact.profileFamilyName(), contact.profileFullName(), serviceId);
        int cid = repo.insertContact(suiteConnection, displayName);
        repo.insertContactMapping(suiteConnection, signalId, serviceId, cid);
        contactByServiceId.put(serviceId, cid);
        Log.info(this, "[signal] Kontakt angelegt: '" + displayName + "'");
    }

    // -------------------------------------------------------------------------
    // Chat-Member
    // -------------------------------------------------------------------------

    private void ensureChatMember(Connection suiteConnection, int chatId, int contactId) throws SQLException {
        String key = chatId + ":" + contactId;
        if (chatMemberCache.contains(key)) return;
        chatMemberCache.add(key);
        repo.insertChatMemberIfAbsent(suiteConnection, chatId, contactId);
    }

    // -------------------------------------------------------------------------
    // Attachments
    // -------------------------------------------------------------------------

    /**
     * Einige Attachments haben keinen vorgegebenen Filename in der Signal-DB (z. B. Vorschauen oder lange Nachrichten, die als txt-Datei gespeichert wurden). Der Name wird wie folgt aufgelöst:<br>
     * <ul><li>Filename wenn vorhanden ohne Endung, sonst "attachment"</li>
     * <li>+ "_"</li>
     * <li>+ originale Message-Id aus der SignalDB. Message-Id nicht Attachment-Id!</li>
     * <li>+ "_"</li>
     * <li>+ Hochzählendes int bei mehreren Attachments pro Nachricht. Dass wir hier nicht einfach das Feld orderInMessage aus der Signal-DB genommen haben, war ein fehler, aber es funktioniert anscheinend.</li>
     * <li>+ "."</li>
     * <li>+ Dateiendung aus dem Mapping. Da es ja manchmal keins gab und wir eh nur bekannte importieren wollen, ignorieren wir hier die Endung aus dem filename, falls der bekannt sein sollte. Wir nehmen stattdessen den aus unserer internen Mapping-Tabelle. Ja, die liegt nur im Code vor. Ob das clever ist, da sind sich Claude ("super") und Thorsten ("Mist") uneins.</li></ul> 
     * 
     * @param signalConnection
     * @param signalMsgId
     * @return
     * @throws Exception
     */
    private List<AttachmentResult> resolveAttachments(Connection signalConnection,
                                                       String signalMsgId) throws Exception {
        List<AttachmentResult> results = new ArrayList<>();

        for (AttachmentInfo att : source.loadAttachments(signalConnection, signalMsgId)) {
            if (att.size() <= 0)
                throw new IllegalStateException("[FAILFAST] size=" + att.size() + " für Attachment in messageId=" + signalMsgId);

            String ext;
            if (isBlank(att.contentType()) || "application/octet-stream".equals(att.contentType())) {
                if (isBlank(att.fileName()))
                    throw new IllegalStateException("[FAILFAST] Kein contentType und kein fileName für messageId=" + signalMsgId);
                int dot = att.fileName().lastIndexOf('.');
                if (dot < 0 || dot == att.fileName().length() - 1)
                    throw new IllegalStateException("[FAILFAST] Keine Extension in fileName='" + att.fileName() + "' für messageId=" + signalMsgId);
                ext = att.fileName().substring(dot + 1);
            } else {
                ext = EXTENSION_MAP.get(att.contentType());
                if (ext == null)
                    throw new IllegalStateException("[FAILFAST] Unbekannter contentType='" + att.contentType()
                        + "' attachmentType='" + att.attachmentType() + "' für messageId=" + signalMsgId);
            }

            String baseName = sanitizeFileName(!isBlank(att.fileName()) ? stripExtension(att.fileName()) : "attachment");
            int    seq      = attachSeqPerMsg.merge(signalMsgId, 1, Integer::sum);
            String outName  = baseName + "_" + signalMsgId + "_" + seq + "." + ext;
         // !Sofort: Wir haben ein attachments.folder genau hierfür, welches dann von signal, whatsapp und diary genutzt werden kann.
            Path   attachDir = Config.getPath("signalAttachmentsFolder");
            Path   outPath   = attachDir.resolve(outName);

            if (!Files.exists(outPath)) {
                Path encryptedFile = Config.getPath("signal.externalPath").resolve("attachments.noindex").resolve(att.path());
                if (!Files.exists(encryptedFile))
                    throw new IllegalStateException("[FAILFAST] Quelldatei nicht gefunden: " + encryptedFile + " (messageId=" + signalMsgId + ")");
                decryptAttachment(encryptedFile, att.localKey(), att.size(), outPath);
            }

            results.add(new AttachmentResult(outName, true));
        }
        return results;
    }

    // -------------------------------------------------------------------------
    // Quote-Auflösung (dreistufig)
    // -------------------------------------------------------------------------

    private String resolveQuote(Connection signalConnection, String signalMsgId,
                                String quoteJson, String quoteMsgId, long quoteId) throws SQLException {
        if (quoteJson == null) return null;

        if (!isBlank(quoteMsgId)) {
            String resolved = source.resolveQuoteByMessageId(signalConnection, quoteMsgId);
            if (resolved != null) return resolved;
            throw new IllegalStateException("[FAILFAST] quote.messageId='" + quoteMsgId
                + "' nicht in Signal-DB (signalMsgId=" + signalMsgId + ")");
        }

        if (quoteId > 0) {
            String resolved = source.resolveQuoteByTimestamp(signalConnection, quoteId);
            if (resolved != null) return resolved;
            resolved = source.resolveQuoteByEditHistory(signalConnection, quoteId);
            if (resolved != null) return resolved;
        }

        throw new IllegalStateException("[FAILFAST] Quote nicht auflösbar für signalMsgId=" + signalMsgId
            + " quote.id=" + quoteId + " quote.messageId=" + quoteMsgId);
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

        if (!Files.exists(outPath.getParent()))
        	throw new IllegalStateException("[FAILFAST] Attachment-Verzeichnis existiert nicht: " + outPath.getParent());
        Files.write(outPath, Arrays.copyOf(plaintext, size));
        if (Files.notExists(outPath))
            throw new RuntimeException("[FAILFAST] Attachment wurde nicht geschrieben: " + outPath);
    }

    // -------------------------------------------------------------------------
    // Hilfsmethoden
    // -------------------------------------------------------------------------

    private String resolveDisplayName(String name, String profileName,
                                      String profileFamilyName, String profileFullName,
                                      String fallback) {
        List<String> candidates = new ArrayList<>();
        addIfNotBlank(candidates, stripBidiControls(name));
        addIfNotBlank(candidates, buildFullName(profileName, profileFamilyName));
        addIfNotBlank(candidates, stripBidiControls(profileFullName));
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