package app.messaging.whatsapp;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import app.messaging.ContactResolver;
import app.messaging.repository.MessageRepository;
import app.messaging.whatsapp.repository.WhatsAppSourceRepository;
import app.shared.Config;
import app.shared.DB;
import app.shared.Log;
import app.shared.model.ButtonEnum;
import app.shared.ui.Alerts;
import app.shared.ui.WhatsAppChatDialog;

/**
 * 
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
 *   <li>Transaktion öffnen, WhatsApp-DB gegen den Importstand prüfen, Nachrichten sequenziell importieren</li>
 *   <li>Commit, Hash und letzten Check-Zeitpunkt speichern</li>
 *   <li>Attachments kopieren (PostTask)</li>
 *   <li>Temp-Datei löschen</li>
 * </ol>
 * </p>
 *
 * <p>Konfigurationsschlüssel:
 * <ul>
 *   <li>{@code whatsapp.externalPath}    – Verzeichnis der crypt15-Datei (Filen-Sync)</li>
 *   <li>{@code whatsapp.AttachmentFolder}– Zielverzeichnis für verschobene Attachments</li>
 *   <li>{@code whatsapp.key}             – 64-stelliger Hex-E2E-Backup-Schlüssel</li>
 *   <li>{@code whatsapp.daystartHour}    – Stunde ab der ein neuer "Tag" beginnt (0–23)</li>
 *   <li>{@code whatsapp.warningAfterDays}– Warnung wenn seit so vielen Tagen kein Import</li>
 *   <li>{@code whatsapp.maxLowerIds}     – optional (Default 5): so viele gelesene Zeilen dürfen eine
 *       kleinere _id haben als die zuletzt importierte, bevor der Import abbricht</li>
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
    private static final String VIEW_ONCE         = "/data/user/0/com.whatsapp/files/ViewOnce/";
    private static final int    TYPE_VIEW_ONCE    = 42;
    private static final int    ANCHOR_COUNT      = 10;
    private static final String KEY_MAX_LOWER_IDS = "whatsapp.maxLowerIds";
    private static final int    DEFAULT_MAX_LOWER_IDS = 5;

    private final MessageRepository        msgRepo   = new MessageRepository();
    private final WhatsAppCrypt15Decryptor decryptor = new WhatsAppCrypt15Decryptor();
    private final WhatsAppSourceRepository waRepo    = new WhatsAppSourceRepository();

    // Config
    private Path   whatsAppExternalDir; 
    private Path   crypt15Path;
    private Path   attachmentDir;
    private String hexKey;
    private int    dayStartHour;
    private int    warningAfterDays;
    private int    maxLowerIds;

    // Laufzeit-State
    private final Map<String, Integer>       knownChats    = new LinkedHashMap<>();
    private final Map<String, Boolean>       blacklisted   = new LinkedHashMap<>();
    private ContactResolver contacts;

    /** Album-State: albumKey → [sourceId, timestamp] des offenen Openers. */
    private final Map<String, long[]> openAlbumByKey = new LinkedHashMap<>();

    // Für den PostTask: Attachments die nach erfolgreichem Commit verschoben werden
    private final List<AttachmentCopy> pendingCopies = new ArrayList<>();

    // Zähler für das Abschluss-Alert
    private int importedMessages    = 0;
    private int importedAttachments = 0;

    /**
     * Läuft erst hinter dem Wächter in {@link #run()}: Auf einem Rechner ohne WhatsApp bliebe das
     * Objekt sonst halb gebaut stehen, mit sieben leeren Feldern.
     */
    private void ladeKonfiguration() {
        this.whatsAppExternalDir = Config.getPath("whatsapp.externalPath");
        this.crypt15Path      = whatsAppExternalDir.resolve("Databases").resolve(CRYPT15_FILENAME);
        this.attachmentDir    = Config.getPath("attachments.folder").resolve("whatsapp");
        this.hexKey           = Config.get("whatsapp.key");
        this.dayStartHour     = Config.getInt("whatsapp.daystartHour");
        this.warningAfterDays = Config.getInt("whatsapp.warningAfterDays");
        this.maxLowerIds      = Config.getInt(KEY_MAX_LOWER_IDS, DEFAULT_MAX_LOWER_IDS);
    }

    // -------------------------------------------------------------------------
    // Einstiegspunkt
    // -------------------------------------------------------------------------

    /**
     * Prüft ob ein Import fällig ist und führt ihn ggf. durch.
     * Wird vom Controller beim Start der Suite aufgerufen.
     */
    public void run() throws Exception {
        // Schritt 0: Ist WhatsApp auf diesem Rechner überhaupt eingerichtet?
        if (Config.get("whatsapp.externalPath", null) == null) {
        	Log.info(this.getClass(), "WhatsApp nicht konfiguriert.");
        	return;
        }
        ladeKonfiguration();
        
        if (!isCheckDue()) {
        	Log.info(this.getClass(), "Kein WhatsApp-Import fällig.");
        	return;
        }
        
        String currentHash = computeHash(crypt15Path);
        String storedHash  = Config.get(KV_LAST_HASH);

        if (Objects.equals(currentHash, storedHash)) {
            checkWarning();
            Log.info(this.getClass(), "WhatsApp: Hash der Datei hat sich nicht geändert.");
        } else {
            validateAttachmentDir();
            runImport(decryptToTemp());
        }

        // Erst hier: wirft etwas davor, bleibt der Tag offen und der nächste Start versucht es erneut.
        Config.setTime(KV_LAST_CHECK, LocalDateTime.now());
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

        LocalDateTime lastCheck = Config.getTime(KV_LAST_CHECK);
        if (lastCheck == null) return true;
        return lastCheck.isBefore(dayStart);
    }

    /**
     * Gibt eine Warnung aus wenn seit {@code whatsapp.warningAfterDays} Tagen
     * kein erfolgreicher Import stattgefunden hat.
     */
    private void checkWarning() {
        LocalDateTime lastImportTime = Config.getTime(KV_LAST_IMPORT);
        if (lastImportTime == null) return;
        if (lastImportTime.isBefore(LocalDateTime.now().minusDays(warningAfterDays))) {
        	Log.warn(this.getClass(), "WhatsApp-import: Uff. Schon lange kein Import mehr gelaufen. Warnung ausgegeben.");
        	Alerts.show("Warnung WhatsApp-Import", "Kein Import seit " + warningAfterDays + " Tagen", ButtonEnum.OK);
        }
    }

    // -------------------------------------------------------------------------
    // Vorbedingungen
    // -------------------------------------------------------------------------

    private void validateAttachmentDir() {
        if (!Files.exists(attachmentDir))
            throw new IllegalStateException("Attachment-Zielverzeichnis existiert nicht: " + attachmentDir);
        if (!Files.isWritable(attachmentDir))
            throw new IllegalStateException("Attachment-Zielverzeichnis nicht beschreibbar: " + attachmentDir);
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
            throw new RuntimeException("Entschlüsselung der crypt15-Datei fehlgeschlagen", e);
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
            loadState();

            try {
                processMessages(wa, thos);
                thos.commit();
            } catch (Exception e) {
                thos.rollback();
                throw new RuntimeException("WhatsApp-Import fehlgeschlagen, Rollback durchgeführt", e);
            }
        }

        copyAttachments();
        String currentHash = computeHash(crypt15Path);
        Config.set(KV_LAST_HASH, currentHash);
        Config.setTime(KV_LAST_IMPORT, LocalDateTime.now());

        showSummaryAlert();
    }

    // -------------------------------------------------------------------------
    // State laden
    // -------------------------------------------------------------------------

    private void loadState() {
        for (Map.Entry<String, Integer> e : msgRepo.loadKnownChats(SOURCE).entrySet()) {
            knownChats.put(e.getKey(), e.getValue());
            blacklisted.put(e.getKey(), false);
        }
        for (String id : msgRepo.loadBlacklistedChatIds(SOURCE)) {
            knownChats.put(id, -1);
            blacklisted.put(id, true);
        }
        contacts = new ContactResolver(SOURCE, "WhatsApp", msgRepo);
    }

    // -------------------------------------------------------------------------
    // Nachrichten-Loop
    // -------------------------------------------------------------------------

    private void processMessages(Connection wa, Connection thos) throws Exception {
		String lastSourceId = msgRepo.getLastImportedSourceId(SOURCE);
		long lastId = lastSourceId != null ? Long.parseLong(lastSourceId) : 0L;
		LocalDateTime lastSentAt = lastSourceId != null ? msgRepo.getSentAtForSourceId(SOURCE, lastSourceId) : null;
		long lastTs = lastSentAt != null ? lastSentAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() : 0L;

        if (lastSourceId != null)
            checkSourceDb(wa, lastId, lastTs);

        Map<Integer, Boolean> messageTypeToIgnore = msgRepo.loadMessageTypes(SOURCE);

        waRepo.forEachMessage(wa, lastId, lastTs,
            rs -> processRow(rs, wa, thos, messageTypeToIgnore));
    }

    /**
     * Prüft vor dem ersten Lesen, ob die WhatsApp-DB noch zum Importstand passt. Hat WhatsApp die
     * _ids zurückgesetzt oder neu nummeriert, zeigt eine source_id auf eine fremde Nachricht: Neues
     * würde als „bereits importiert" übersprungen, Altes doppelt importiert.
     *
     * <p>Kein Vergleich pro Zeile: Nachzügler (neue _id, älterer Timestamp) sind bei WhatsApp normal,
     * etwa verspätet zugestellte Nachrichten oder „Gruppe erstellt" beim Beitritt zu einer alten Gruppe.</p>
     */
    private void checkSourceDb(Connection wa, long lastId, long lastTs) throws SQLException {
        Map<Long, LocalDateTime> anchors = msgRepo.loadHighestNumericSourceIds(SOURCE, ANCHOR_COUNT);
        long maxImportedId = anchors.keySet().iterator().next();
        long maxSourceId   = waRepo.getMaxMessageId(wa);
        if (maxSourceId < maxImportedId)
            throw new IllegalStateException(
                "Die WhatsApp-DB reicht nur bis _id=" + maxSourceId + ", importiert ist aber schon bis source_id=" +
                maxImportedId + ". WhatsApp hat die IDs offenbar zurückgesetzt oder neu nummeriert.");

        for (Map.Entry<Long, LocalDateTime> anchor : anchors.entrySet()) {
            Long ts = waRepo.getTimestamp(wa, anchor.getKey());
            if (ts == null)
                throw new IllegalStateException(
                    "Anker fehlt: Die importierte Nachricht source_id=" + anchor.getKey() + " (" + anchor.getValue() +
                    ") gibt es in der WhatsApp-DB nicht mehr. Entweder hat WhatsApp die IDs neu nummeriert, " +
                    "oder die Nachricht wurde auf dem Handy gelöscht.");
            LocalDateTime sourceSentAt = LocalDateTime.ofInstant(Instant.ofEpochMilli(ts), ZoneId.systemDefault())
                                                      .truncatedTo(ChronoUnit.SECONDS);
            if (!sourceSentAt.equals(anchor.getValue()))
                throw new IllegalStateException(
                    "Anker passt nicht: source_id=" + anchor.getKey() + " wurde mit " + anchor.getValue() +
                    " importiert, unter dieser _id steht in der WhatsApp-DB aber eine Nachricht von " + sourceSentAt +
                    ". WhatsApp hat die IDs offenbar neu nummeriert.");
        }

        int belowLastId = waRepo.countMessagesBelowId(wa, lastId, lastTs);
        if (belowLastId > maxLowerIds)
            throw new IllegalStateException(
                belowLastId + " Nachrichten, die der Import lesen würde, haben eine kleinere _id als die zuletzt " +
                "importierte (source_id=" + lastId + ", " + formatTs(lastTs) + "). Erlaubt sind " + maxLowerIds +
                ". Das spricht für neu nummerierte IDs. Ist trotzdem alles in Ordnung, läuft der Import mit '" +
                KEY_MAX_LOWER_IDS + "=" + belowLastId + "' in der config.txt durch.");
    }

    private void processRow(ResultSet rs, Connection wa, Connection thos,
                            Map<Integer, Boolean> messageTypeToIgnore) throws Exception {
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

        // Ungeöffnete Einmalansicht-Bilder zeigen in den privaten App-Speicher, der nie im Backup
        // landet. Nach dem Öffnen leert WhatsApp den Pfad ohnehin — also genauso behandeln wie
        // geöffnete: Der Verweis bleibt über message_url als nicht verfügbarer Anhang erhalten.
        if (type == TYPE_VIEW_ONCE && filePath != null && filePath.startsWith(VIEW_ONCE))
            filePath = null;

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
                    "Unbekannter message_type=" + type +
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
            	Alerts.show("WhatsApp-Import — Album-Warnung", "Album-Kind liegt " + (diffMs / 1000) + "s nach Opener\n\n" + "Kind:     _id=" + sourceId + "  (" + formatTs(timestamp) + ")\n" +
                    "Opener:   _id=" + openerSourceId + "  (" + formatTs(openerTimestamp) + ")\n" +
                    "albumKey: " + albumKey + "\n" +
                    "filePath: " + filePath + "\n\n" +
                    "Das Attachment wird trotzdem an den Opener gehängt.", ButtonEnum.OK);
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
        int fromContactId = contacts.resolve(thos, fromContactRaw, () -> null); // WhatsApp kennt keinen Namen zur JID
        contacts.ensureChatMember(thos, chatId, fromContactId);

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
        LocalDateTime sentAt = LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault());
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
    // Attachment-Auflösung
    // -------------------------------------------------------------------------

    /**
     * Löst einen Attachment-Pfad auf: prüft Existenz im Quell- und Zielverzeichnis,
     * trägt sie ggf. in `pendingCopies` ein und gibt den relativen Pfad und Verfügbarkeit zurück.
     * Schreibt nichts in die DB — Insert liegt beim Aufrufer (FK-Constraint: Message zuerst).
     *
     * @param filePath  Pfad wie er in der WhatsApp-DB steht (beginnt mit "Media/")
     * @param sourceId  Nachrichten-ID für FailFast-Meldungen
     */
    private AttachmentResolution resolveAttachment(String filePath, long sourceId) {
        if (!filePath.startsWith(MEDIA))
            throw new IllegalStateException(
                "Unerwarteter Attachment-Pfad ohne '" + MEDIA +
                "'-Präfix: " + filePath + " (_id=" + sourceId + ")");

        String relativePath = filePath.replace(",", "_");
        Path   source       = whatsAppExternalDir.resolve(filePath);
        Path   target       = attachmentDir.resolve(relativePath);

        if (Files.exists(source)) {
            pendingCopies.add(new AttachmentCopy(source, target));
            return new AttachmentResolution(relativePath, true);
        } else if (Files.exists(target)) {
            return new AttachmentResolution(relativePath, true);
        } else {
            throw new IllegalStateException(
                "Attachment nicht gefunden in Quelle '" + source +
                "' und nicht im Zielverzeichnis '" + target +
                "' (_id=" + sourceId + ")");
        }
    }

    private record AttachmentResolution(String relativePath, boolean available) {}

    // -------------------------------------------------------------------------
    // PostTask: Attachments kopieren
    // -------------------------------------------------------------------------

    /**
     * Ja. Mir wäre ein Verschieben auch lieber. Aber einen Sync-Modus, der Dateien nur genau 1x auf den Computer spielt
     * und dann nicht darauf reagiert, wenn ich die dort lösche, den gibt es leider nicht. Man kann Löschungen derart
     * ignorieren, dass die nicht in die Cloud zurückgespiegelt werden. Aber nicht sicherstellen, dass die dann gelöscht
     * bleiben lokal. Was ja iwie auch verständlich ist für ne Backup / Sicherungslösung. Aber für mich gerade etwas doof.
     */
    private void copyAttachments() {
        for (AttachmentCopy copy : pendingCopies) {
            try {
                Files.createDirectories(copy.target().getParent());
                Files.copy(copy.source(), copy.target(), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                throw new IllegalStateException(
                    "Attachment-Kopie fehlgeschlagen — das hätte nie passieren dürfen. " +
                    "Bitte Konsistenz-Check durchführen. Quelle: " + copy.source() +
                    " Ziel: " + copy.target(), e);
            }
        }
    }

    private record AttachmentCopy(Path source, Path target) {}

    // -------------------------------------------------------------------------
    // Abschluss-Alert
    // -------------------------------------------------------------------------

    private void showSummaryAlert() {
    	Alerts.show("WhatsApp-Import abgeschlossen", importedMessages + " Nachrichten und " + importedAttachments + " Anhänge importiert.", ButtonEnum.OK);
    }

    // -------------------------------------------------------------------------
    // Hilfsmethoden
    // -------------------------------------------------------------------------


    private String computeHash(Path file) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = Files.readAllBytes(file);
            byte[] hash  = digest.digest(bytes);
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("SHA-256-Hash der crypt15-Datei konnte nicht berechnet werden", e);
        }
    }

    private String formatTs(long unixMs) {
        return java.time.LocalDateTime
            .ofInstant(java.time.Instant.ofEpochMilli(unixMs), java.time.ZoneId.systemDefault())
            .toString();
    }
}