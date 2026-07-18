package app.diary;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import app.diary.repository.Repository;
import app.shared.Config;
import app.shared.model.DiaryAttachment;
import app.shared.model.DiaryCardData;
import app.shared.model.InvasiveConfig;
import app.shared.ui.surfaces.dialogs.DiaryEditor;

/**
 * Framework-freie Hälfte des Tagebuch-Editors. Öffnet die modale {@link DiaryEditor}-View
 * (shared) für genau einen Eintrag, verdrahtet Speichern/Löschen als Callbacks und macht
 * die Domäne: Anlegen/Updaten, Attachment-Kopieren/Thumbnail/Diff, Invasiv-Regel. Kein JavaFX.
 *
 * Erwartete DiaryEditor-API (gebaut in Schicht 2):
 *   new DiaryEditor(DiaryCardData initialEntry,   // createdAt==null => neuer Eintrag
 *                   List<String> allTags,
 *                   InvasiveConfig invasive,       // null => nicht invasiv (showEdit)
 *                   Consumer<DiaryCardData> onSave,
 *                   Consumer<DiaryCardData> onDelete) // null => kein Löschen (showNew)
 *   editor.showNew()   — Dialog bleibt nach jedem Save offen, View leert sich
 *   editor.showEdit()  — Dialog schließt nach Save/Delete
 */
public class DiaryEditorPresenter {

    private static final int DEFAULT_INVASIVE_AFTER_HOURS = 18;
    private static final int DEFAULT_INVASIVE_SECONDS = 120;
    private static final int DEFAULT_MIN_CHARS = 20;
    private static final int DEFAULT_MIN_TAGS = 1;
    private static final int DEFAULT_THUMBNAIL_HEIGHT = 120;

    private final Repository repository = new Repository();

    // ---- Einstiege ----------------------------------------------------------

    /** Neuen Eintrag erfassen (Controller: PostTask + Menü). Ggf. invasiv. */
    public void showNew() {
        DiaryCardData empty = new DiaryCardData(null, LocalDate.now(), "", List.of(), List.of());
        InvasiveConfig invasive = computeInvasive();  // null, wenn nicht invasiv
        DiaryEditor editor = new DiaryEditor(empty, repository.loadAllTags(), invasive, this::save, null);
        editor.showNew();
    }

    /** Bestehenden Eintrag bearbeiten (Klick auf eine Karte im DiaryScreen). Nie invasiv. */
    public void showEdit(DiaryCardData clicked) {
        DiaryCardData full = withAttachments(clicked);  // Attachments frisch aus DB, absolut
        DiaryEditor editor = new DiaryEditor(full, repository.loadAllTags(), null, this::save, this::delete);
        editor.showEdit();
    }

    // ---- Callbacks (von der View gerufen) ----------------------------------

    /** createdAt == null => neuer Eintrag; sonst Update. */
    private void save(DiaryCardData entry) {
        if (entry.createdAt() == null)
            create(entry);
        else
            update(entry);
    }

    private void delete(DiaryCardData entry) {
        LocalDateTime createdAt = entry.createdAt();
        for (String rel : repository.loadAttachments(createdAt))
            deleteAttachmentWithFile(createdAt, rel);
        repository.deleteEntry(createdAt);
    }

    // ---- Domäne -------------------------------------------------------------

    private void create(DiaryCardData entry) {
        LocalDateTime createdAt = LocalDateTime.now();
        repository.saveEntry(createdAt, entry.entryDate(), entry.text().trim(), new ArrayList<>(entry.tags()));
        for (DiaryAttachment att : entry.attachments())
            linkOrCopy(createdAt, att.imagePath());
    }

    private void update(DiaryCardData entry) {
        LocalDateTime createdAt = entry.createdAt();
        repository.updateEntry(createdAt, entry.entryDate(), entry.text().trim(), new ArrayList<>(entry.tags()));

        Path diaryFolder = diaryFolder();
        List<String> wantedRel = new ArrayList<>();
        for (DiaryAttachment att : entry.attachments())
            wantedRel.add(relativeName(diaryFolder, att.imagePath()));

        for (String oldRel : repository.loadAttachments(createdAt))
            if (!wantedRel.contains(oldRel))
                deleteAttachmentWithFile(createdAt, oldRel);

        for (DiaryAttachment att : entry.attachments())
            linkOrCopy(createdAt, att.imagePath());   // INSERT OR IGNORE => idempotent
    }

    /** Liegt das Bild schon im diaryFolder (inkl. Quelle==Ziel) => nur verknüpfen; sonst kopieren + Thumbnail. */
    private void linkOrCopy(LocalDateTime createdAt, String absoluteImagePath) {
        Path diaryFolder = diaryFolder();
        Path source = Path.of(absoluteImagePath);

        if (source.startsWith(diaryFolder)) {
            repository.saveAttachment(createdAt, diaryFolder.relativize(source).toString());
            return;
        }

        String cleanName = source.getFileName().toString().replace(",", "_");
        Path target = diaryFolder.resolve(cleanName);
        try {
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new RuntimeException("Failed to copy image", ex);
        }
        generateThumbnail(target, diaryFolder.resolve("thumbnails").resolve(cleanName));
        repository.saveAttachment(createdAt, cleanName);
    }

    // ---- Laden / Invasiv ----------------------------------------------------

    /** Ergänzt die geklickte Karte um ihre Attachments (frisch aus DB, absolute Paare). */
    private DiaryCardData withAttachments(DiaryCardData clicked) {
        Path diaryFolder = diaryFolder();
        Path thumbs = diaryFolder.resolve("thumbnails");
        List<DiaryAttachment> attachments = new ArrayList<>();
        for (String rel : repository.loadAttachments(clicked.createdAt())) {
            String fileName = Path.of(rel).getFileName().toString();
            attachments.add(new DiaryAttachment(
                    diaryFolder.resolve(rel).toString(),
                    thumbs.resolve(fileName).toString()));
        }
        return new DiaryCardData(clicked.createdAt(), clicked.entryDate(), clicked.text(),
                clicked.tags(), attachments);
    }

    /** 18h-Regel: invasiv, wenn der letzte Eintrag zu lange her ist (oder keiner existiert). */
    private InvasiveConfig computeInvasive() {
        int afterHours = Config.getInt("diary.invasiveAfterHours", DEFAULT_INVASIVE_AFTER_HOURS);
        LocalDateTime last = repository.findLastEntryTimestamp();
        boolean invasive = last == null || ChronoUnit.HOURS.between(last, LocalDateTime.now()) >= afterHours;
        if (!invasive)
            return null;
        return new InvasiveConfig(
                Config.getInt("diary.minChars", DEFAULT_MIN_CHARS),
                Config.getInt("diary.minTags", DEFAULT_MIN_TAGS),
                Config.getInt("diary.invasiveSeconds", DEFAULT_INVASIVE_SECONDS));
    }

    // ---- Attachment-Löschen / Thumbnail / Pfade ----------------------------

    private void deleteAttachmentWithFile(LocalDateTime createdAt, String relativePath) {
        repository.deleteAttachment(createdAt, relativePath);
        if (!repository.isPathReferencedElsewhere(relativePath)) {
            Path diaryFolder = diaryFolder();
            diaryFolder.resolve(relativePath).toFile().delete();
            diaryFolder.resolve("thumbnails").resolve(Path.of(relativePath).getFileName()).toFile().delete();
        }
    }

    private String relativeName(Path diaryFolder, String absoluteImagePath) {
        Path source = Path.of(absoluteImagePath);
        if (source.startsWith(diaryFolder))
            return diaryFolder.relativize(source).toString();
        return source.getFileName().toString().replace(",", "_");
    }

    private void generateThumbnail(Path sourcePath, Path thumbPath) {
        int thumbHeight = Config.getInt("diary.thumbnailHeight", DEFAULT_THUMBNAIL_HEIGHT);
        try {
            java.awt.image.BufferedImage original = javax.imageio.ImageIO.read(sourcePath.toFile());
            double ratio = (double) thumbHeight / original.getHeight();
            int thumbWidth = (int) (original.getWidth() * ratio);
            java.awt.image.BufferedImage scaled = org.imgscalr.Scalr.resize(
                    original, org.imgscalr.Scalr.Method.QUALITY, thumbWidth, thumbHeight);

            String filename = thumbPath.getFileName().toString().toLowerCase();
            String format;
            if (filename.endsWith(".png")) format = "png";
            else if (filename.endsWith(".jpg") || filename.endsWith(".jpeg")) format = "jpg";
            else throw new RuntimeException("Unsupported thumbnail format for: " + thumbPath);

            boolean written = javax.imageio.ImageIO.write(scaled, format, thumbPath.toFile());
            if (!written)
                throw new RuntimeException("ImageIO.write returned false for " + thumbPath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate thumbnail for " + sourcePath, e);
        }
    }

    private Path diaryFolder() {
        return Config.getPath("attachments.folder").resolve("diary");
    }
}