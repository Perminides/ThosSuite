package app.diary;

import java.io.File;
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
import app.shared.skin.SkinService;
import app.shared.ui.TagInputComponent;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.binding.BooleanBinding;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import javafx.stage.WindowEvent;
import javafx.util.Duration;
import javafx.event.EventHandler;

/**
 * Dialog zum Erfassen und Bearbeiten von Tagebucheinträgen.
 *
 * <p>Bietet zwei Modi:
 * <ul>
 *   <li>{@link #showNew(Window)} — Neuen Eintrag erfassen, optional mit invasivem Verhalten
 *       (Fenster kann nicht geschlossen werden bis Mindestanforderungen erfüllt sind).</li>
 *   <li>{@link #showEdit(Window, LocalDateTime, LocalDate, String, List)} — Bestehenden Eintrag
 *       bearbeiten oder löschen, ohne invasives Verhalten.</li>
 * </ul>
 *
 * <p>Attachments werden als relative Pfade unterhalb des konfigurierten Diary-Ordners gespeichert.
 * Neu hinzugefügte Dateien werden erst beim Speichern in den Zielordner kopiert (pending-Mechanismus).
 * Für jedes Attachment wird automatisch ein Thumbnail generiert (Scalr, QUALITY).
 *
 * <p>Dateinamen-Bereinigung: Kommas werden beim Kopieren in den Zielordner durch Unterstriche ersetzt,
 * da Kommas in Dateinamen unser schlankes Select in der search im Repo kaputt macht. Der in der Datenbank
 * gespeicherte relative Pfad verwendet stets den bereinigten Namen.
 *
 * <p>Konfigurationsschlüssel (alle optional mit Defaults):
 * <ul>
 *   <li>{@code diary.mediaFolder} — Wurzelordner für Medien (kein Default, muss gesetzt sein)</li>
 *   <li>{@code diary.thumbnailHeight} — Thumbnail-Höhe in Pixeln (Default: 120)</li>
 *   <li>{@code diary.invasiveAfterHours} — Stunden seit letztem Eintrag bis invasiv (Default: 18)</li>
 *   <li>{@code diary.invasiveSeconds} — Sekunden bis invasiver Modus endet (Default: 120)</li>
 *   <li>{@code diary.minChars} — Mindestzeichen im Text für invasiven Modus (Default: 20)</li>
 *   <li>{@code diary.minTags} — Mindestanzahl Tags für invasiven Modus (Default: 1)</li>
 * </ul>
 */
public class DiaryDialog {

    private static final int DEFAULT_INVASIVE_AFTER_HOURS = 18;
    private static final int DEFAULT_INVASIVE_SECONDS = 120;
    private static final int DEFAULT_MIN_CHARS = 20;
    private static final int DEFAULT_MIN_TAGS = 1;
    private static final int DEFAULT_THUMBNAIL_HEIGHT = 120;

    private final Repository repository = new Repository();
    private final TagInputComponent tagInput = new TagInputComponent();
    private final ObservableList<String> currentAttachmentPaths = FXCollections.observableArrayList();
    // Neu hinzugefügte Originaldateien — noch nicht kopiert, erst bei Save
    private final List<File> pendingOriginals = new ArrayList<>();
    // Snapshot der Attachment-Pfade beim Dialog-Start (showEdit)
    private List<String> savedAttachmentPaths;

    // Dialog state
    private TextArea textArea;
    private Button saveButton;
    private FlowPane attachmentPane;
    private BooleanBinding requirementsNotMet;
    private EventHandler<WindowEvent> closeBlocker;
    private ListChangeListener<String> tagsListener;

    // showNew: Neuen Eintrag erfassen, mit invasiver Logik
    public void showNew() {
        int invasiveAfterHours = Config.getInt("diary.invasiveAfterHours", DEFAULT_INVASIVE_AFTER_HOURS);
        LocalDateTime lastEntry = repository.findLastEntryTimestamp();
        boolean invasive = lastEntry == null
                || ChronoUnit.HOURS.between(lastEntry, LocalDateTime.now()) >= invasiveAfterHours;

        tagInput.setAllTags(repository.loadAllTags());
        currentAttachmentPaths.clear();
        
        Window owner = SkinService.getOwnerWindow();

        Dialog<?> dialog = SkinService.get().createDialog(owner, "Tagebuch");
        DatePicker datePicker = new DatePicker(LocalDate.now());
        dialog.getDialogPane().setContent(buildContent(datePicker, owner));

        ButtonType saveButtonType = new ButtonType("Speichern", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().add(saveButtonType);
        saveButton = (Button) dialog.getDialogPane().lookupButton(saveButtonType);

        saveButton.addEventFilter(javafx.event.ActionEvent.ACTION, e -> {
            e.consume();
            boolean hasText = textArea.getText() != null && !textArea.getText().isBlank();
            boolean hasTags = !tagInput.getSelectedTags().isEmpty();

            if (hasText && hasTags) {
                LocalDateTime createdAt = LocalDateTime.now();
                Path mediaRoot = Config.getPath("attachments.folder");
                Path diaryFolder = mediaRoot.resolve("diary");

                repository.saveEntry(
                        createdAt,
                        datePicker.getValue(),
                        textArea.getText().trim(),
                        new ArrayList<>(tagInput.getSelectedTags()));

                for (String path : currentAttachmentPaths) {
                    repository.saveAttachment(createdAt, path);
                }
                for (File original : pendingOriginals) {
                    String cleanName = original.getName().replace(",", "_");
                    Path target = diaryFolder.resolve(cleanName);
                    try { Files.copy(original.toPath(), target, StandardCopyOption.REPLACE_EXISTING); }
                    catch (IOException ex) { throw new RuntimeException("Failed to copy image", ex); }
                    generateThumbnail(target, diaryFolder.resolve("thumbnails").resolve(cleanName));
                    repository.saveAttachment(createdAt, cleanName);
                }

                tagInput.setAllTags(repository.loadAllTags());
                tagInput.reset();
                textArea.clear();
                currentAttachmentPaths.clear();
                pendingOriginals.clear();
                rebuildAttachmentPane(owner);
                makeNonInvasive(dialog);
            } else if (!hasText && !hasTags) {
                dialog.close();
            }
        });

        dialog.setResultConverter(_ -> null);

        if (invasive) {
            makeInvasive(dialog);
        }

        Platform.runLater(() -> textArea.requestFocus());
        dialog.showAndWait();
    }

    // showEdit: Bestehenden Eintrag bearbeiten, kein invasives Verhalten
    public void showEdit(LocalDateTime createdAt, LocalDate entryDate, String text, List<String> tags) {
        tagInput.setAllTags(repository.loadAllTags());
        for (String tag : tags) {
            tagInput.addTag(tag);
        }
        currentAttachmentPaths.clear();
        currentAttachmentPaths.addAll(repository.loadAttachments(createdAt));
        savedAttachmentPaths = List.copyOf(currentAttachmentPaths);

        Dialog<?> dialog = SkinService.get().createDialog(SkinService.getOwnerWindow(), "Eintrag bearbeiten");

        ButtonType cancelType = new ButtonType("", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().add(cancelType);
        var cancelBtn = dialog.getDialogPane().lookupButton(cancelType);
        cancelBtn.setVisible(false);
        cancelBtn.setManaged(false);

        DatePicker datePicker = new DatePicker(entryDate);
        dialog.getDialogPane().setContent(buildContent(datePicker, SkinService.getOwnerWindow()));
        textArea.setText(text);

        ButtonType saveButtonType = new ButtonType("Speichern", ButtonBar.ButtonData.OK_DONE);
        ButtonType deleteButtonType = new ButtonType("Löschen", ButtonBar.ButtonData.LEFT);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, deleteButtonType);

        saveButton = (Button) dialog.getDialogPane().lookupButton(saveButtonType);
        Button deleteButton = (Button) dialog.getDialogPane().lookupButton(deleteButtonType);

        saveButton.addEventFilter(javafx.event.ActionEvent.ACTION, e -> {
            e.consume();
            Path mediaRoot = Config.getPath("attachments.folder");
            Path diaryFolder = mediaRoot.resolve("diary");

            repository.updateEntry(
                    createdAt,
                    datePicker.getValue(),
                    textArea.getText().trim(),
                    new ArrayList<>(tagInput.getSelectedTags()));

            // Entfernte Attachments löschen (Diff gegen Snapshot vom Dialog-Start)
            for (String savedPath : savedAttachmentPaths) {
                if (!currentAttachmentPaths.contains(savedPath)) {
                    deleteAttachmentWithFile(createdAt, savedPath);
                }
            }
            // Bestehende Attachments (bereits im Zielordner) sichern
            for (String path : currentAttachmentPaths) {
                repository.saveAttachment(createdAt, path);
            }
            // Neue Originaldateien kopieren, Thumbnail generieren, speichern
            for (File original : pendingOriginals) {
                String cleanName = original.getName().replace(",", "_");
                Path target = diaryFolder.resolve(cleanName);
                try { Files.copy(original.toPath(), target, StandardCopyOption.REPLACE_EXISTING); }
                catch (IOException ex) { throw new RuntimeException("Failed to copy image", ex); }
                generateThumbnail(target, diaryFolder.resolve("thumbnails").resolve(cleanName));
                repository.saveAttachment(createdAt, cleanName);
            }

            dialog.close();
        });

        deleteButton.addEventFilter(javafx.event.ActionEvent.ACTION, e -> {
            e.consume();
            for (String path : repository.loadAttachments(createdAt)) {
                deleteAttachmentWithFile(createdAt, path);
            }
            repository.deleteEntry(createdAt);
            dialog.close();
        });

        dialog.setResultConverter(_ -> null);

        Platform.runLater(() -> textArea.requestFocus());
        dialog.showAndWait();
    }

    private VBox buildContent(DatePicker datePicker, Window owner) {
        VBox root = new VBox(12);
        root.setPadding(new Insets(16));

        HBox topRow = new HBox(16);
        topRow.setAlignment(Pos.CENTER_LEFT);
        topRow.getChildren().addAll(datePicker, tagInput.getTagInput());

        textArea = new TextArea();
        textArea.setId("diaryTextArea");
        textArea.setPromptText("Was beschäftigt dich?");
        textArea.setPrefColumnCount(80);
        textArea.setPrefRowCount(10);
        textArea.setWrapText(true);
        VBox.setVgrow(textArea, Priority.ALWAYS);

        textArea.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.TAB) {
                e.consume();
                if (e.isShiftDown()) {
                    tagInput.requestFocus();
                } else if (saveButton != null) {
                    saveButton.requestFocus();
                }
            }
        });

        attachmentPane = new FlowPane(8, 8);
        attachmentPane.setPadding(new Insets(4, 0, 0, 0));
        rebuildAttachmentPane(owner);

        root.getChildren().addAll(topRow, tagInput.getChipPane(), attachmentPane, textArea);
        return root;
    }

    private void rebuildAttachmentPane(Window owner) {
        attachmentPane.getChildren().clear();
        int thumbHeight = Config.getInt("diary.thumbnailHeight", DEFAULT_THUMBNAIL_HEIGHT);
        Path mediaRoot = Config.getPath("attachments.folder");
        Path diaryFolder = mediaRoot.resolve("diary");

        // Bestehende Attachments — Thumbnail von Platte laden
        for (String relativePath : currentAttachmentPaths) {
            Path thumbPath = diaryFolder.resolve("thumbnails").resolve(Path.of(relativePath).getFileName());
            ImageView imageView = new ImageView(new Image(thumbPath.toUri().toString(), -1, thumbHeight, true, true));
            attachmentPane.getChildren().add(buildTile(imageView, () -> {
                currentAttachmentPaths.remove(relativePath);
                rebuildAttachmentPane(owner);
            }));
        }

        // Pending Originale — in-memory Vorschau direkt aus Originaldatei
        for (File original : pendingOriginals) {
            ImageView imageView = new ImageView(new Image(original.toURI().toString(), -1, thumbHeight, true, true));
            attachmentPane.getChildren().add(buildTile(imageView, () -> {
                pendingOriginals.remove(original);
                rebuildAttachmentPane(owner);
            }));
        }

        Button addBtn = new Button("+");
        addBtn.setOnAction(_ -> pickAndAddImage(owner, diaryFolder));
        attachmentPane.getChildren().add(addBtn);
    }

    private StackPane buildTile(ImageView imageView, Runnable onRemove) {
        Button removeBtn = new Button("✕");
        removeBtn.setOnAction(_ -> onRemove.run());
        StackPane tile = new StackPane(imageView, removeBtn);
        StackPane.setAlignment(removeBtn, Pos.TOP_RIGHT);
        return tile;
    }

    private void pickAndAddImage(Window owner, Path diaryFolder) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Bild auswählen");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Bilder", "*.jpg", "*.jpeg", "*.png", "*.gif", "*.webp"));
        File chosen = chooser.showOpenDialog(owner);
        if (chosen == null) return;

        if (chosen.toPath().startsWith(diaryFolder)) {
            String relativePath = diaryFolder.relativize(chosen.toPath()).toString();
            if (!currentAttachmentPaths.contains(relativePath)) {
                currentAttachmentPaths.add(relativePath);
                rebuildAttachmentPane(owner);
            }
        } else {
            if (pendingOriginals.stream().noneMatch(f -> f.equals(chosen))) {
                pendingOriginals.add(chosen);
                rebuildAttachmentPane(owner);
            }
        }
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
            if (filename.endsWith(".png")) {
                format = "png";
            } else if (filename.endsWith(".jpg") || filename.endsWith(".jpeg")) {
                format = "jpg";
            } else {
                throw new RuntimeException("Unsupported thumbnail format for: " + thumbPath);
            }

            boolean written = javax.imageio.ImageIO.write(scaled, format, thumbPath.toFile());
            if (!written) {
                throw new RuntimeException("ImageIO.write returned false for " + thumbPath
                        + " (format: " + format + ", imageType: " + scaled.getType() + ")");
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate thumbnail for " + sourcePath, e);
        }
    }

    private void deleteAttachmentWithFile(LocalDateTime createdAt, String relativePath) {
        repository.deleteAttachment(createdAt, relativePath);
        if (!repository.isPathReferencedElsewhere(relativePath)) {
            Path mediaRoot = Config.getPath("attachments.folder");
            Path diaryFolder = mediaRoot.resolve("diary");
            Path file = diaryFolder.resolve(relativePath);
            Path thumb = diaryFolder.resolve("thumbnails").resolve(Path.of(relativePath).getFileName());
            file.toFile().delete();
            thumb.toFile().delete();
        }
    }

    private void makeInvasive(Dialog<?> dialog) {
        closeBlocker = WindowEvent::consume;
        dialog.getDialogPane().getScene().getWindow().addEventFilter(
                WindowEvent.WINDOW_CLOSE_REQUEST, closeBlocker);

        int minChars = Config.getInt("diary.minChars", DEFAULT_MIN_CHARS);
        int minTags = Config.getInt("diary.minTags", DEFAULT_MIN_TAGS);

        requirementsNotMet = new BooleanBinding() {
            { bind(textArea.textProperty(), tagInput.getSelectedTags()); }
            @Override
            protected boolean computeValue() {
                int textLen = textArea.getText() == null ? 0 : textArea.getText().length();
                return textLen < minChars || tagInput.getSelectedTags().size() < minTags;
            }
        };

        saveButton.disableProperty().bind(requirementsNotMet);

        int invasiveSeconds = Config.getInt("diary.invasiveSeconds", DEFAULT_INVASIVE_SECONDS);
        Timeline timer = new Timeline(new KeyFrame(
                Duration.seconds(invasiveSeconds),
                _ -> makeNonInvasive(dialog)));
        timer.setCycleCount(1);
        timer.play();
    }

    private void makeNonInvasive(Dialog<?> dialog) {
        if (closeBlocker != null) {
            dialog.getDialogPane().getScene().getWindow().removeEventFilter(
                    WindowEvent.WINDOW_CLOSE_REQUEST, closeBlocker);
            closeBlocker = null;
        }
        if (tagsListener != null) {
            tagInput.getSelectedTags().removeListener(tagsListener);
            tagsListener = null;
        }
        saveButton.disableProperty().unbind();
        saveButton.setDisable(false);
        requirementsNotMet = null;
    }
}