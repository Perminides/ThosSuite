package app.shared.ui.surfaces.dialogs;

import java.io.File;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import app.shared.Config;
import app.shared.model.DiaryAttachment;
import app.shared.model.DiaryCardData;
import app.shared.model.InvasiveConfig;
import app.shared.skin.SkinService;
import app.shared.ui.components.DiaryTagInputComponent;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.binding.BooleanBinding;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
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
import javafx.stage.WindowEvent;
import javafx.util.Duration;

/**
 * Modale Editor-Ansicht für genau einen Tagebucheintrag. Framework-gebundene Hälfte:
 * baut die Widgets, besitzt den UI-Zustand (Text, Tags, Attachments) während des
 * Bearbeitens und meldet Speichern/Löschen als {@link DiaryCardData} über Callbacks.
 * Kennt keine Repository, keine Ordnerstruktur — nur absolute Pfade.
 *
 * <p>Zwei Modi:
 * <ul>
 *   <li>{@link #showNew()} — neuer Eintrag; Save feuert wiederholt, Dialog bleibt offen,
 *       View leert nach jedem Save. Optional invasiv (Close-Blocker + Timer + Save-disable).</li>
 *   <li>{@link #showEdit()} — bestehender Eintrag; Save/Delete feuern einmal, Dialog schließt.
 *       Nie invasiv.</li>
 * </ul>
 */
public class DiaryEditor {

    private static final int DEFAULT_THUMBNAIL_HEIGHT = 120;

    private final DiaryCardData initialEntry;
    private final List<String> allTags;
    private final InvasiveConfig invasive;              // null => nicht invasiv
    private final Consumer<DiaryCardData> onSave;
    private final Consumer<DiaryCardData> onDelete;     // null => kein Löschen (showNew)

    private final DiaryTagInputComponent tagInput = new DiaryTagInputComponent();

    // UI-Zustand während des Bearbeitens — die View besitzt ihn.
    private final List<DiaryAttachment> existing = new ArrayList<>();  // bestehende (haben Thumbnail)
    private final List<File> pendingOriginals = new ArrayList<>();     // neu hinzugefügt (kein Thumbnail)

    private TextArea textArea;
    private DatePicker datePicker;
    private FlowPane attachmentPane;
    private Button saveButton;

    // Invasiv-Mechanik
    private BooleanBinding requirementsNotMet;
    private EventHandler<WindowEvent> closeBlocker;

    public DiaryEditor(DiaryCardData initialEntry, List<String> allTags, InvasiveConfig invasive,
                       Consumer<DiaryCardData> onSave, Consumer<DiaryCardData> onDelete) {
        this.initialEntry = initialEntry;
        this.allTags = allTags;
        this.invasive = invasive;
        this.onSave = onSave;
        this.onDelete = onDelete;
    }

    // ---- Einstiege ----------------------------------------------------------

    /** Neuer Eintrag: Save feuert wiederholt, Dialog bleibt offen, View leert nach jedem Save. */
    public void showNew() {
        Dialog<?> dialog = buildDialog("Tagebuch", false);

        saveButton.addEventFilter(ActionEvent.ACTION, e -> {
            e.consume();
            boolean hasText = !textArea.getText().isBlank();
            boolean hasTags = !tagInput.getSelectedTags().isEmpty();
            if (hasText && hasTags) {
                onSave.accept(collect(null));   // createdAt null => neuer Eintrag
                clearForNext(dialog);
            } else if (!hasText && !hasTags) {
                dialog.close();                 // leer + Speichern => Abbruch (View-Logik)
            }
        });

        dialog.setOnShown(_ -> {
            textArea.requestFocus();
            if (invasive != null)
                makeInvasive(dialog);
        });
        dialog.showAndWait();
    }

    /** Bestehender Eintrag: Save/Delete feuern einmal, Dialog schließt. */
    public void showEdit() {
        Dialog<?> dialog = buildDialog("Eintrag bearbeiten", true);

        saveButton.addEventFilter(ActionEvent.ACTION, e -> {
            e.consume();
            onSave.accept(collect(initialEntry.createdAt()));
            dialog.close();
        });

        dialog.setOnShown(_ -> textArea.requestFocus());
        dialog.showAndWait();
    }

    // ---- Dialog- und Content-Aufbau ----------------------------------------

    private Dialog<?> buildDialog(String title, boolean withDelete) {
        Dialog<?> dialog = SkinService.get().createDialog(SkinService.getOwnerWindow(), title);
        dialog.getDialogPane().setContent(buildContent());
        dialog.setResultConverter(_ -> null);

        if (withDelete) {
            // Unsichtbarer CANCEL_CLOSE: erlaubt Schließen per X/ESC (kein invasiver Modus im Edit).
            ButtonType cancelType = new ButtonType("", ButtonBar.ButtonData.CANCEL_CLOSE);
            dialog.getDialogPane().getButtonTypes().add(cancelType);
            Button cancelBtn = (Button) dialog.getDialogPane().lookupButton(cancelType);
            cancelBtn.setVisible(false);
            cancelBtn.setManaged(false);
        }

        ButtonType saveType = new ButtonType("Speichern", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().add(saveType);
        saveButton = (Button) dialog.getDialogPane().lookupButton(saveType);

        if (withDelete) {
            ButtonType deleteType = new ButtonType("Löschen", ButtonBar.ButtonData.LEFT);
            dialog.getDialogPane().getButtonTypes().add(deleteType);
            Button deleteBtn = (Button) dialog.getDialogPane().lookupButton(deleteType);
            deleteBtn.addEventFilter(ActionEvent.ACTION, e -> {
                e.consume();
                onDelete.accept(initialEntry);   // createdAt gesetzt (nur showEdit)
                dialog.close();
            });
        }

        return dialog;
    }

    private VBox buildContent() {
        tagInput.setAllTags(allTags);
        for (String tag : initialEntry.tags())
            tagInput.addTag(tag);

        existing.clear();
        existing.addAll(initialEntry.attachments());   // bestehende: haben Thumbnail
        pendingOriginals.clear();

        datePicker = new DatePicker(initialEntry.entryDate());

        textArea = new TextArea(initialEntry.text());
        textArea.setId("diaryTextArea");
        textArea.setPromptText("Was beschäftigt dich?");
        textArea.setPrefColumnCount(80);
        textArea.setPrefRowCount(10);
        textArea.setWrapText(true);
        VBox.setVgrow(textArea, Priority.ALWAYS);

        textArea.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.TAB) {
                e.consume();
                if (e.isShiftDown())
                    tagInput.requestFocus();
                else if (saveButton != null)
                    saveButton.requestFocus();
            }
        });

        HBox topRow = new HBox(16, datePicker, tagInput.getTagInput());
        topRow.setAlignment(Pos.CENTER_LEFT);

        attachmentPane = new FlowPane(8, 8);
        attachmentPane.setPadding(new Insets(4, 0, 0, 0));
        rebuildAttachmentPane();

        VBox root = new VBox(12, topRow, tagInput.getChipPane(), attachmentPane, textArea);
        root.setPadding(new Insets(16));
        return root;
    }

    private void rebuildAttachmentPane() {
        attachmentPane.getChildren().clear();
        int thumbHeight = Config.getInt("diary.thumbnailHeight", DEFAULT_THUMBNAIL_HEIGHT);

        // Bestehende: Vorschau aus dem Thumbnail (Performance).
        for (DiaryAttachment att : existing) {
            ImageView iv = new ImageView(new Image(
                    Path.of(att.thumbnailPath()).toUri().toString(), -1, thumbHeight, true, true));
            attachmentPane.getChildren().add(buildTile(iv, () -> {
                existing.remove(att);
                rebuildAttachmentPane();
            }));
        }

        // Neu hinzugefügte: Vorschau aus dem skalierten Original (noch kein Thumbnail).
        for (File original : pendingOriginals) {
            ImageView iv = new ImageView(new Image(
                    original.toURI().toString(), -1, thumbHeight, true, true));
            attachmentPane.getChildren().add(buildTile(iv, () -> {
                pendingOriginals.remove(original);
                rebuildAttachmentPane();
            }));
        }

        Button addBtn = new Button("+");
        addBtn.setOnAction(_ -> pickAndAddImage());
        attachmentPane.getChildren().add(addBtn);
    }

    private StackPane buildTile(ImageView imageView, Runnable onRemove) {
        Button removeBtn = new Button("✕");
        removeBtn.setOnAction(_ -> onRemove.run());
        StackPane tile = new StackPane(imageView, removeBtn);
        StackPane.setAlignment(removeBtn, Pos.TOP_RIGHT);
        return tile;
    }

    private void pickAndAddImage() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Bild auswählen");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Bilder", "*.jpg", "*.jpeg", "*.png", "*.gif", "*.webp"));
        File chosen = chooser.showOpenDialog(SkinService.getOwnerWindow());
        if (chosen == null)
            return;

        String abs = chosen.getAbsolutePath();
        boolean alreadyThere =
                existing.stream().anyMatch(a -> a.imagePath().equals(abs))
                || pendingOriginals.stream().anyMatch(f -> f.getAbsolutePath().equals(abs));
        if (!alreadyThere) {
            pendingOriginals.add(chosen);
            rebuildAttachmentPane();
        }
    }

    // ---- aktueller UI-Zustand -> Grenzobjekt --------------------------------

    private DiaryCardData collect(LocalDateTime createdAt) {
        List<DiaryAttachment> attachments = new ArrayList<>(existing);
        for (File original : pendingOriginals)
            attachments.add(new DiaryAttachment(original.getAbsolutePath(), null)); // kein Thumbnail
        return new DiaryCardData(
                createdAt, datePicker.getValue(), textArea.getText().trim(),
                new ArrayList<>(tagInput.getSelectedTags()), attachments);
    }

    // ---- Invasiv-Mechanik (nur showNew) ------------------------------------

    private void makeInvasive(Dialog<?> dialog) {
        closeBlocker = WindowEvent::consume;
        dialog.getDialogPane().getScene().getWindow()
                .addEventFilter(WindowEvent.WINDOW_CLOSE_REQUEST, closeBlocker);

        requirementsNotMet = new BooleanBinding() {
            { bind(textArea.textProperty(), tagInput.getSelectedTags()); }
            @Override protected boolean computeValue() {
                int len = textArea.getText() == null ? 0 : textArea.getText().length();
                return len < invasive.minChars() || tagInput.getSelectedTags().size() < invasive.minTags();
            }
        };
        saveButton.disableProperty().bind(requirementsNotMet);

        Timeline timer = new Timeline(new KeyFrame(
                Duration.seconds(invasive.invasiveSeconds()), _ -> makeNonInvasive(dialog)));
        timer.setCycleCount(1);
        timer.play();
    }

    private void makeNonInvasive(Dialog<?> dialog) {
        if (closeBlocker != null) {
            dialog.getDialogPane().getScene().getWindow()
                    .removeEventFilter(WindowEvent.WINDOW_CLOSE_REQUEST, closeBlocker);
            closeBlocker = null;
        }
        saveButton.disableProperty().unbind();
        saveButton.setDisable(false);
        requirementsNotMet = null;
    }

    private void clearForNext(Dialog<?> dialog) {
        textArea.clear();
        tagInput.reset();
        existing.clear();
        pendingOriginals.clear();
        rebuildAttachmentPane();
        makeNonInvasive(dialog);
    }
}