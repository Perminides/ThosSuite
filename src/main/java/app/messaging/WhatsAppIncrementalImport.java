package app.messaging;

import app.data.persistence.DB;
import app.data.persistence.KeyValueRepository;
import app.data.persistence.MessageRepository;
import app.data.persistence.WhatsAppSourceRepository;
import app.ui.components.WhatsAppChatDialog;
import app.ui.components.WhatsAppContactDialog;
import app.util.Log;
import app.config.Config;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 
 * TODOS
 * <ul>
 * <li>Unbekannter Chat Fenster lässt sich nicht verschieben, warum nicht?</li>
 * <li>Unbekannter Contact Fenster auch nicht??? Leider nicht probiert.</li>
 * <li>Abschlussnachricht nicht in unserem Skin!</li>
 * </ul>
 * 
 * Inkrementeller WhatsApp-Import in die ThosSuite-Datenbank.
 *
 * <p>Wird einmal täglich nach der konfigurierten Uhrzeit ({@code whatsapp.daystartHour})
 * ausgeführt, sofern sich die Vollbackup-Datei ({@code msgstore.db.crypt15}) seit dem
 * letzten Import verändert hat (SHA-256-Hash-Vergleich).</p>
 *
 * <p>Ablauf:
 * <ol>
 *   <li>Prüfen ob heute bereits ein Check stattgefunden hat → ggf. abbrechen</li>
 *   <li>Hash der crypt15-Datei vergleichen → keine Änderung → fertig</li>
 *   <li>Zielverzeichnis für Attachments prüfen</li>
 *   <li>crypt15 entschlüsseln in Temp-Verzeichnis</li>
 *   <li>Transaktion öffnen, Nachrichten sequenziell importieren</li>
 *   <li>Commit, Hash und letzten Check-Zeitpunkt speichern</li>
 *   <li>Attachments verschieben (PostTask)</li>
 *   <li>Temp-Datei löschen</li>
 * </ol>
 * </p>
 *
 * <p>Konfigurationsschlüssel:
 * <ul>
 *   <li>{@code whatsapp.path}            – Verzeichnis der crypt15-Datei (Filen-Sync)</li>
 *   <li>{@code whatsapp.attachmentDir}   – Zielverzeichnis für verschobene Attachments</li>
 *   <li>{@code whatsapp.key}             – 64-stelliger Hex-E2E-Backup-Schlüssel</li>
 *   <li>{@code whatsapp.daystartHour}    – Stunde ab der ein neuer "Tag" beginnt (0–23)</li>
 *   <li>{@code whatsapp.warningAfterDays}– Warnung wenn seit so vielen Tagen kein Import</li>
 * </ul>
 * </p>
 *
 */
public class WhatsAppIncrementalImport {

    private static final String CRYPT15_FILENAME  = "msgstore.db.crypt15";
    private static final String KV_LAST_CHECK     = "whatsapp.lastCheck";
    private static final String KV_LAST_HASH      = "whatsapp.lastHash";
    private static final String KV_LAST_IMPORT    = "whatsapp.lastImport";
    private static final String SOURCE            = "whatsapp";
    private static final String MEDIA             = "Media/";
    private static final DateTimeFormatter DT     = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final KeyValueRepository       kvRepo    = new KeyValueRepository();
    private final MessageRepository        msgRepo   = new MessageRepository();
    private final WhatsAppCrypt15Decryptor decryptor = new WhatsAppCrypt15Decryptor();
    private final WhatsAppSourceRepository waRepo    = new WhatsAppSourceRepository();

    // Config
    private final String whatsAppDir; 
    private final Path   crypt15Path;
    private final Path   attachmentDir;
    private final String hexKey;
    private final int    dayStartHour;
    private final int    warningAfterDays;

    // Laufzeit-State
    private final Map<String, Integer>       knownChats    = new LinkedHashMap<>();
    private final Map<String, Boolean>       blacklisted   = new LinkedHashMap<>();
    private final Map<String, Integer>       knownContacts = new LinkedHashMap<>();
    private final Map<Integer, Set<Integer>> chatMembers   = new LinkedHashMap<>();

    /** Album-State: albumKey → [sourceId, timestamp] des offenen Openers. */
    private final Map<String, long[]> openAlbumByKey = new LinkedHashMap<>();

    // Für den PostTask: Attachments die nach erfolgreichem Commit verschoben werden
    private final List<AttachmentMove> pendingMoves = new ArrayList<>();

    // Zähler für das Abschluss-Alert
    private int importedMessages    = 0;
    private int importedAttachments = 0;

    public WhatsAppIncrementalImport() {
    	this.whatsAppDir      = Config.get("whatsapp.folder.path");
        this.crypt15Path      = Path.of(Config.get("whatsapp.folder.path"), "Databases", CRYPT15_FILENAME);
        this.attachmentDir    = Path.of(Config.get("whatsapp.attachmentDir"));
        this.hexKey           = Config.get("whatsapp.key");
        this.dayStartHour     = Config.getInt("whatsapp.daystartHour");
        this.warningAfterDays = Config.getInt("whatsapp.warningAfterDays");
    }

    // -------------------------------------------------------------------------
    // Einstiegspunkt
    // -------------------------------------------------------------------------

    /**
     * Prüft ob ein Import fällig ist und führt ihn ggf. durch.
     * Wird vom Controller beim Start der Suite aufgerufen.
     */
    public void run() throws Exception {
        if (!isCheckDue()) {
        	Log.info(this.getClass(), "Kein WhatsApp-Import fällig.");
        	return;
        }

        String currentHash = computeHash(crypt15Path);
        String storedHash  = kvRepo.get(KV_LAST_HASH).orElse(null);

        if (Objects.equals(currentHash, storedHash)) {
            checkWarning();
            Log.info(this.getClass(), "WhatsApp: Hash der Datei hat sich nicht geändert.");
            updateKeyValue(currentHash);
            return;
        }

        validateAttachmentDir();
        
        Path tempDb = decryptToTemp();
        runImport(tempDb);
    }

    // -------------------------------------------------------------------------
    // Check-Zeitpunkt
    // -------------------------------------------------------------------------

    /**
     * Prüft ob ein Check heute (im Sinne des konfigurierten Tagesstarts) bereits
     * stattgefunden hat. Ein "Tag" beginnt um {@code whatsapp.daystartHour} Uhr
     * und endet 24 Stunden später.
     */
    private boolean isCheckDue() {
        LocalDateTime now      = LocalDateTime.now();
        LocalDateTime dayStart = LocalDate.now().atTime(LocalTime.of(dayStartHour, 0));

        if (now.isBefore(dayStart)) {
            dayStart = dayStart.minusDays(1);
        }

        Optional<String> lastCheck = kvRepo.get(KV_LAST_CHECK);
        if (lastCheck.isEmpty()) return true;

        LocalDateTime lastCheckTime = LocalDateTime.parse(lastCheck.get(), DT);
        return lastCheckTime.isBefore(dayStart);
    }

    /**
     * Gibt eine Warnung aus wenn seit {@code whatsapp.warningAfterDays} Tagen
     * kein erfolgreicher Import stattgefunden hat.
     */
    private void checkWarning() {
        Optional<String> lastImport = kvRepo.get(KV_LAST_IMPORT);
        if (lastImport.isEmpty()) return;

        LocalDateTime lastImportTime = LocalDateTime.parse(lastImport.get(), DT);
        if (lastImportTime.isBefore(LocalDateTime.now().minusDays(warningAfterDays))) {
            Alert alert = new Alert(AlertType.WARNING);
            alert.setTitle("WhatsApp-Import");
            alert.setHeaderText("Kein Import seit " + warningAfterDays + " Tagen");
            alert.setContentText("Letzter erfolgreicher Import: " + lastImport.get());
            alert.showAndWait();
        }
        Log.warn(this.getClass(), "WhatsApp-import: Uff. Schon lange kein Import mehr gelaufen. Warnung ausgegeben.");
    }

    // -------------------------------------------------------------------------
    // Vorbedingungen
    // -------------------------------------------------------------------------

    private void validateAttachmentDir() {
        if (!Files.exists(attachmentDir))
            throw new IllegalStateException("[FAILFAST] Attachment-Zielverzeichnis existiert nicht: " + attachmentDir);
        if (!Files.isWritable(attachmentDir))
            throw new IllegalStateException("[FAILFAST] Attachment-Zielverzeichnis nicht beschreibbar: " + attachmentDir);
    }

    // -------------------------------------------------------------------------
    // Entschlüsselung
    // -------------------------------------------------------------------------

    private Path decryptToTemp() {
        try {
            Path tempFile = Path.of(System.getProperty("java.io.tmpdir"), "thossuite-whatsapp.db");
            decryptor.decrypt(hexKey, crypt15Path, tempFile);
            return tempFile;
        } catch (Exception e) {
            throw new RuntimeException("[FAILFAST] Entschlüsselung der crypt15-Datei fehlgeschlagen", e);
        }
    }

    // -------------------------------------------------------------------------
    // Import
    // -------------------------------------------------------------------------

    private void runImport(Path tempDb) throws Exception {
    	Log.info(this.getClass(), "WhatsApp-Import startet.");
        String waUrl = "jdbc:sqlite:" + tempDb.toAbsolutePath();

        try (Connection wa   = DriverManager.getConnection(waUrl);
             Connection thos = DB.getNewConnection()) {

            thos.setAutoCommit(false);
            loadState(thos);
            processMessages(wa, thos);
            thos.commit();
        }
        // Compiler-Pflicht: try-with-resources ruft close() implizit auf, dessen checked Exception
        // muss explizit behandelt werden — wir werfen sie unverändert weiter.
        catch (Exception e) {
            throw e;
        }

        copyAttachments();
        String currentHash = computeHash(crypt15Path);
        updateKeyValue(currentHash);

        showSummaryAlert();
    }

    // -------------------------------------------------------------------------
    // State laden
    // -------------------------------------------------------------------------

    private void loadState(Connection thos) throws SQLException {
        for (Map.Entry<String, Integer> e : msgRepo.loadKnownChats(SOURCE).entrySet()) {
            knownChats.put(e.getKey(), e.getValue());
            blacklisted.put(e.getKey(), false);
        }
        for (String id : msgRepo.loadBlacklistedChatIds(SOURCE)) {
            knownChats.put(id, -1);
            blacklisted.put(id, true);
        }
        knownContacts.putAll(msgRepo.loadKnownContacts(SOURCE));

        try (var ps = thos.prepareStatement("SELECT chat_id, contact_id FROM msg_chat_members");
             var rs = ps.executeQuery()) {
            while (rs.next()) {
                chatMembers
                    .computeIfAbsent(rs.getInt("chat_id"), _ -> new HashSet<>())
                    .add(rs.getInt("contact_id"));
            }
        }
    }

    // -------------------------------------------------------------------------
    // Nachrichten-Loop
    // -------------------------------------------------------------------------

    private void processMessages(Connection wa, Connection thos) throws Exception {
        String lastSourceId = msgRepo.getLastImportedSourceId(SOURCE);
        long   lastId       = lastSourceId != null ? Long.parseLong(lastSourceId) : 0L;
        long   lastTs       = lastSourceId != null
            ? parseTs(msgRepo.getSentAtForSourceId(SOURCE, lastSourceId))
            : 0L;

        Map<Integer, Boolean> messageTypeToIgnore = loadMessageTypes(thos);

        waRepo.forEachMessage(wa, lastId, lastTs,
            rs -> processRow(rs, wa, thos, messageTypeToIgnore, lastId, lastTs));
    }

    private void processRow(ResultSet rs, Connection wa, Connection thos,
                            Map<Integer, Boolean> messageTypeToIgnore,
                            long lastId, long lastTs) throws Exception {
        long    sourceId       = rs.getLong("_id");
        long    timestamp      = rs.getLong("timestamp");
        int     type           = rs.getInt("message_type");
        int     chatRowId      = rs.getInt("chat_row_id");
        String  rawIdentifier  = rs.getString("raw_identifier");
        String  fromContactRaw = rs.getString("from_contact_raw");
        String  content        = rs.getString("content");
        String  filePath       = rs.getString("file_path");
        String  messageUrl     = rs.getString("message_url");
        long    origFlags      = rs.getLong("origination_flags");
        String  subject        = rs.getString("subject");
        boolean isGroup        = rs.getInt("is_group") == 1;
        String  quoteKeyId     = rs.getString("quote_key_id");
        String  sourceIdStr    = String.valueOf(sourceId);

        // Konsistenzcheck
        if (sourceId < lastId || timestamp < lastTs)
            throw new IllegalStateException(
                "[FAILFAST] Konsistenzfehler: Nachricht _id=" + sourceId +
                " timestamp=" + timestamp + " liegt vor dem letzten importierten Wert " +
                "(lastId=" + lastId + ", lastTs=" + lastTs + ")");

        // Bereits importiert? (Überlappungsbereich, kein Fehler)
        if (msgRepo.isAlreadyImported(thos, SOURCE, sourceIdStr)) return;
        
        // Zweistufig mit unbekannten Messagetypes, weil im Hintergrund gibt es ständig
        // ignorierte Types in Chats, die ich sicher nie anfassen werde wollen. Ignore-Check also hier schon.
        boolean unknownMessageTypeFound = false;
        // Bekannter Type? null = unbekannt → FailFast, true = ignorieren, false = importieren
        Boolean ignore = messageTypeToIgnore.get(type);
        if (ignore == null)
        	unknownMessageTypeFound = true;
        else if (ignore)
        	return;
        
        // Chat auflösen — gibt -1 zurück wenn blacklisted, siehe resolveChat()
        int chatId = resolveChat(thos, rawIdentifier, subject, isGroup, timestamp);
        if (chatId == -1) return;
        
        // Und jetzt nachgelagert der Unknown-Check, aber erst wenn wir wissen, dass es einen interessanten Chat betrifft.
        if (unknownMessageTypeFound)
            throw new IllegalStateException(
                    "[FAILFAST] Unbekannter message_type=" + type +
                    " bei _id=" + sourceId + " — bitte msg_message_types aktualisieren");

        // Album-Kind-Erkennung
        String albumKey  = chatRowId + ":" + fromContactRaw;
        long[] openAlbum = openAlbumByKey.get(albumKey);

        if (type != 99 && origFlags != 0 && content == null && filePath != null && openAlbum != null) {
            long openerSourceId  = openAlbum[0];
            long openerTimestamp = openAlbum[1];
            long diffMs          = timestamp - openerTimestamp;

            if (diffMs > 60_000L) {
                // Album-Kind liegt mehr als 1 Minute nach dem Opener — ungewöhnlich, aber
                // kann bei großen Videos durch lange Upload-Zeiten vorkommen.
                // Alert zur manuellen Prüfung, Import läuft trotzdem weiter.
                Alert alert = new Alert(AlertType.WARNING);
                alert.setTitle("WhatsApp-Import — Album-Warnung");
                alert.setHeaderText("Album-Kind liegt " + (diffMs / 1000) + "s nach Opener");
                alert.setContentText(
                    "Kind:     _id=" + sourceId + "  (" + formatTs(timestamp) + ")\n" +
                    "Opener:   _id=" + openerSourceId + "  (" + formatTs(openerTimestamp) + ")\n" +
                    "albumKey: " + albumKey + "\n" +
                    "filePath: " + filePath + "\n\n" +
                    "Das Attachment wird trotzdem an den Opener gehängt.");
                alert.showAndWait();
            }

            AttachmentResolution res = resolveAttachment(filePath, sourceId);
            msgRepo.insertAttachment(thos, SOURCE, String.valueOf(openerSourceId), res.relativePath(), res.available());
            importedAttachments++;
            return;
        }

        // Keine Kind-Erkennung → Album-Marker schließen (außer beim 99er, der überschreibt ihn)
        if (type != 99) openAlbumByKey.remove(albumKey);
        if (type == 99) openAlbumByKey.put(albumKey, new long[]{sourceId, timestamp});

        // Kontakt auflösen
        int fromContactId = resolveContact(thos, fromContactRaw);
        insertChatMember(thos, chatId, fromContactId);

        // Quote auflösen
        // Bekanntes WhatsApp-Phänomen: Quotes auf bearbeitete Nachrichten können nicht
        // aufgelöst werden (key_id nicht mehr vorhanden) → FailFast, bei Bedarf abschwächbar.
        String resolvedQuote = null;
        if (quoteKeyId != null)
            resolvedQuote = waRepo.resolveQuote(wa, quoteKeyId, sourceId, chatRowId, timestamp);

        // Attachment auflösen
        String  attachmentPath      = null;
        boolean attachmentAvailable = false;

        if (filePath != null) {
            AttachmentResolution res = resolveAttachment(filePath, sourceId);
            attachmentPath      = res.relativePath();
            attachmentAvailable = res.available();

        } else if (messageUrl != null) {
            // messageUrl verweist auf ein Medium das nicht lokal gespeichert ist
            // (z.B. abgelaufene WhatsApp-CDN-Links). Wird als nicht-verfügbares
            // Attachment gespeichert damit der Verweis erhalten bleibt.
            attachmentPath      = messageUrl.replace(",", "_");
            attachmentAvailable = false;

        } else if ((content == null || content.isBlank()) && type != 99) {
            // Nachricht ohne Content, Datei und URL → still ignorieren
            return;
        }

        // Einfügen — Message zuerst, dann Attachment (FK-Constraint)
        String sentAt = formatTs(timestamp);
        msgRepo.insertMessage(thos, SOURCE, sourceIdStr, sentAt, fromContactId, chatId, content, resolvedQuote);

        if (attachmentPath != null) {
            msgRepo.insertAttachment(thos, SOURCE, sourceIdStr, attachmentPath, attachmentAvailable);
            importedAttachments++;
        }

        importedMessages++;
    }

    // -------------------------------------------------------------------------
    // Chat-Auflösung
    // -------------------------------------------------------------------------

    /**
     * Löst einen rawIdentifier zu einer chat_id auf.
     * Gibt -1 zurück wenn der Chat blacklisted ist — der Aufrufer überspringt die Nachricht dann.
     */
    private int resolveChat(Connection thos, String rawIdentifier, String subject,
                            boolean isGroup, long timestamp) throws Exception {
        if (knownChats.containsKey(rawIdentifier))
            return blacklisted.getOrDefault(rawIdentifier, false) ? -1 : knownChats.get(rawIdentifier);

        WhatsAppChatDialog.Result result = WhatsAppChatDialog.show(rawIdentifier, subject, isGroup, formatTs(timestamp));

        String  displayName = result.displayName();
        boolean doImport    = result.doImport();

        int chatId = msgRepo.insertChat(thos, SOURCE, rawIdentifier, isGroup, displayName, !doImport);
        knownChats.put(rawIdentifier, chatId);
        blacklisted.put(rawIdentifier, !doImport);
        return doImport ? chatId : -1;
    }

    // -------------------------------------------------------------------------
    // Kontakt-Auflösung
    // -------------------------------------------------------------------------

    private int resolveContact(Connection thos, String rawIdentifier) throws Exception {
        if (knownContacts.containsKey(rawIdentifier))
            return knownContacts.get(rawIdentifier);

        WhatsAppContactDialog.Result result = WhatsAppContactDialog.show(
            rawIdentifier,
            msgRepo.loadKnownContactsByDisplayName(SOURCE)
        );

        int contactId;
        if (result.existingContactId() != null) {
            contactId = result.existingContactId();
        } else {
            contactId = msgRepo.insertContact(thos, result.newDisplayName());
        }

        msgRepo.insertContactMapping(thos, SOURCE, rawIdentifier, contactId);
        knownContacts.put(rawIdentifier, contactId);
        return contactId;
    }

    // -------------------------------------------------------------------------
    // Chat-Member
    // -------------------------------------------------------------------------

    private void insertChatMember(Connection thos, int chatId, int contactId) throws SQLException {
        Set<Integer> members = chatMembers.computeIfAbsent(chatId, _ -> new HashSet<>());
        if (members.contains(contactId)) return;
        msgRepo.insertChatMemberIfAbsent(thos, chatId, contactId);
        members.add(contactId);
    }

    // -------------------------------------------------------------------------
    // Attachment-Auflösung
    // -------------------------------------------------------------------------

    /**
     * Löst einen Attachment-Pfad auf: prüft Existenz im Quell- und Zielverzeichnis,
     * trägt ggf. einen PendingMove ein und gibt den relativen Pfad und Verfügbarkeit zurück.
     * Schreibt nichts in die DB — Insert liegt beim Aufrufer (FK-Constraint: Message zuerst).
     *
     * @param filePath  Pfad wie er in der WhatsApp-DB steht (beginnt mit "Media/")
     * @param sourceId  Nachrichten-ID für FailFast-Meldungen
     */
    private AttachmentResolution resolveAttachment(String filePath, long sourceId) {
        if (!filePath.startsWith(MEDIA))
            throw new IllegalStateException(
                "[FAILFAST] Unerwarteter Attachment-Pfad ohne '" + MEDIA +
                "'-Präfix: " + filePath + " (_id=" + sourceId + ")");

        String relativePath = filePath.replace(",", "_");
        Path   source       = Path.of(whatsAppDir, filePath);
        Path   target       = attachmentDir.resolve(relativePath);

        if (Files.exists(source)) {
            pendingMoves.add(new AttachmentMove(source, target));
            return new AttachmentResolution(relativePath, true);
        } else if (Files.exists(target)) {
            return new AttachmentResolution(relativePath, true);
        } else {
            throw new IllegalStateException(
                "[FAILFAST] Attachment nicht gefunden in Quelle '" + source +
                "' und nicht im Zielverzeichnis '" + target +
                "' (_id=" + sourceId + ")");
        }
    }

    private record AttachmentResolution(String relativePath, boolean available) {}

    // -------------------------------------------------------------------------
    // PostTask: Attachments verschieben
    // -------------------------------------------------------------------------

    /**
     * Ja. Mir wäre ein Verschieben auch lieber. Aber einen Sync-Modus, der Dateien nur genau 1x auf den Computer spielt
     * und dann nicht darauf reagiert, wenn ich die dort lösche, den gibt es leider nicht. Man kann Löschungen derart
     * ignorieren, dass die nicht in die Cloud zurückgespiegelt werden. Aber nicht sicherstellen, dass die dann gelöscht
     * bleiben lokal. Was ja iwie auch verständlich ist für ne Backup / Sicherungslösung. Aber für mich gerade etwas doof.
     */
    private void copyAttachments() {
        for (AttachmentMove move : pendingMoves) {
            try {
                Files.createDirectories(move.target().getParent());
                Files.copy(move.source(), move.target(), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                throw new IllegalStateException(
                    "[FAILFAST] Attachment-Verschiebung fehlgeschlagen — das hätte nie passieren dürfen. " +
                    "Bitte Konsistenz-Check durchführen. Quelle: " + move.source() +
                    " Ziel: " + move.target(), e);
            }
        }
    }

    private record AttachmentMove(Path source, Path target) {}

    // -------------------------------------------------------------------------
    // Abschluss-Alert
    // -------------------------------------------------------------------------

    private void showSummaryAlert() {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle("WhatsApp-Import");
        alert.setHeaderText("Import abgeschlossen");
        alert.setContentText(importedMessages + " Nachrichten und " +
                             importedAttachments + " Anhänge importiert.");
        alert.showAndWait();
    }

    // -------------------------------------------------------------------------
    // Hilfsmethoden
    // -------------------------------------------------------------------------

    private Map<Integer, Boolean> loadMessageTypes(Connection thos) throws SQLException {
        Map<Integer, Boolean> result = new LinkedHashMap<>();
        try (var ps = thos.prepareStatement(
                "SELECT type_id, ignore FROM msg_message_types WHERE source = ?")) {
            ps.setString(1, SOURCE);
            try (var rs = ps.executeQuery()) {
                while (rs.next())
                    result.put(rs.getInt("type_id"), rs.getInt("ignore") == 1);
            }
        }
        return result;
    }

    private String computeHash(Path file) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = Files.readAllBytes(file);
            byte[] hash  = digest.digest(bytes);
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("[FAILFAST] SHA-256-Hash der crypt15-Datei konnte nicht berechnet werden", e);
        }
    }

    private String formatTs(long unixMs) {
        return java.time.LocalDateTime
            .ofInstant(java.time.Instant.ofEpochMilli(unixMs), java.time.ZoneId.systemDefault())
            .format(DT);
    }

    private long parseTs(String sentAt) {
        return java.time.LocalDateTime.parse(sentAt, DT)
            .atZone(java.time.ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli();
    }
    
    private void updateKeyValue(String currentHash) {
        kvRepo.set(KV_LAST_HASH, currentHash);
        kvRepo.set(KV_LAST_IMPORT, LocalDateTime.now().format(DT));
        kvRepo.set(KV_LAST_CHECK, LocalDateTime.now().format(DT));
    }
}