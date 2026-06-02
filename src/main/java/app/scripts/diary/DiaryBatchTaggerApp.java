package app.scripts.diary;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import app.shared.Config;
import app.shared.DB;
import app.ui.skin.SkinService;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Skin;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import javafx.stage.Stage;

/**
 * Standalone Tool zum manuellen Nach-Taggen migrierter Tagebuch-Einträge.
 *
 * Usage:
 *   Run with program arguments:
 *     2020-01-01
 *   or:
 *     --start=2020-01-01
 *
 * Controls:
 *   Ctrl+Enter  -> Save + Next
 *   Ctrl+Right  -> Skip
 *   Esc         -> Exit
 */
public class DiaryBatchTaggerApp extends Application {

    private static final DateTimeFormatter TS = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final DateTimeFormatter D = DateTimeFormatter.ISO_LOCAL_DATE;

    private static final PseudoClass HIGHLIGHTED = PseudoClass.getPseudoClass("highlighted");
    private static final String TAG_REMOVE_BUTTON = "tag-chip-remove";

    // --- Repo ---
    private final Repo repo = new Repo();

    // --- State ---
    private LocalDate startDate;
    private DiaryEntry current;

    private List<String> allTags = new ArrayList<>();

    // --- UI ---
    private Label lblEntryDate;
    private Label lblCreatedAt;
    private Label lblTagsInline;

    private TextArea textArea;

    private final ObservableList<String> selectedTags = FXCollections.observableArrayList();
    private FlowPane chipPane;
    private TextField tagInput;

    private Button btnSaveNext;
    private Button btnSkip;

    // Autocomplete popup
    private final List<String> currentMatches = new ArrayList<>();
    private Popup suggestionPopup;
    private VBox suggestionBox;
    private int activeIndex = -1;
    private boolean suppressSuggestions = false;

    @Override
    public void start(Stage stage) {
        this.startDate = parseStartDate(getParameters().getRaw());
        this.allTags = repo.loadAllTags();

        var root = buildUi(stage);
        var scene = new Scene(root, 1100, 750);
        SkinService.get().styleScene(scene);

        // Keyboard shortcuts
        scene.getAccelerators().put(
                new KeyCodeCombination(KeyCode.ENTER, KeyCombination.CONTROL_DOWN),
                this::onSaveNext
        );
        scene.getAccelerators().put(
                new KeyCodeCombination(KeyCode.RIGHT, KeyCombination.CONTROL_DOWN),
                this::onSkip
        );
        scene.getAccelerators().put(
                new KeyCodeCombination(KeyCode.ESCAPE),
                stage::close
        );

        stage.setTitle("Diary Batch Tagger (Wegwerf-Tool) — start=" + startDate);
        stage.setScene(scene);
        stage.show();

        loadNextAndDisplay();
        Platform.runLater(() -> textArea.requestFocus());
    }

    private Parent buildUi(Stage stage) {
        // Header
        lblEntryDate = new Label("-");
        lblEntryDate.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        lblCreatedAt = new Label("-");
        lblCreatedAt.setStyle("-fx-opacity: 0.75;");

        lblTagsInline = new Label("-");
        lblTagsInline.setStyle("-fx-opacity: 0.85;");

        var headerLeft = new VBox(4, lblEntryDate, lblCreatedAt, lblTagsInline);
        headerLeft.setAlignment(Pos.CENTER_LEFT);

        btnSaveNext = new Button("Save + Next (Ctrl+Enter)");
        btnSkip = new Button("Skip (Ctrl+→)");
        var btnExit = new Button("Exit (Esc)");
        btnSaveNext.setOnAction(_ -> onSaveNext());
        btnSkip.setOnAction(_ -> onSkip());
        btnExit.setOnAction(_ -> stage.close());

        var headerRight = new HBox(10, btnSaveNext, btnSkip, btnExit);
        headerRight.setAlignment(Pos.CENTER_RIGHT);

        var header = new HBox(16, headerLeft, new Region(), headerRight);
        HBox.setHgrow(header.getChildren().get(1), Priority.ALWAYS);
        header.setPadding(new Insets(14));
        header.setStyle("-fx-background-color: rgba(0,0,0,0.04);");

        // Tag input + chips
        tagInput = new TextField();
        tagInput.setPromptText("Tag hinzufügen… (Enter) — Autocomplete tippen");
        tagInput.setPrefWidth(360);

        setupTagAutocomplete();

        chipPane = new FlowPane(6, 6);
        chipPane.setAlignment(Pos.CENTER_LEFT);
        chipPane.setPadding(new Insets(6));
        chipPane.managedProperty().bind(chipPane.visibleProperty());
        chipPane.setVisible(false);

        var tagsRow = new HBox(12, new Label("Tags:"), tagInput);
        tagsRow.setAlignment(Pos.CENTER_LEFT);

        var tagsBox = new VBox(8, tagsRow, chipPane);
        tagsBox.setPadding(new Insets(14, 14, 8, 14));

        // Text area
        textArea = new TextArea();
        textArea.setWrapText(true);
        textArea.setPrefColumnCount(100);
        textArea.setPrefRowCount(30);
        VBox.setVgrow(textArea, Priority.ALWAYS);

        // Optional: TAB focus jump (wie in deinem DiaryDialog)
        textArea.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.TAB) {
                e.consume();
                tagInput.requestFocus();
            }
        });

        var center = new VBox(10, tagsBox, textArea);
        center.setPadding(new Insets(0, 14, 14, 14));
        VBox.setVgrow(textArea, Priority.ALWAYS);

        var root = new BorderPane();
        root.setTop(header);
        root.setCenter(center);

        // Disable save/skip until first load
        btnSaveNext.setDisable(true);
        btnSkip.setDisable(true);

        return root;
    }

    private void loadNextAndDisplay() {
        current = repo.loadNextEntry(current, startDate);
        if (current == null) {
            btnSaveNext.setDisable(true);
            btnSkip.setDisable(true);
            lblEntryDate.setText("Fertig ✅");
            lblCreatedAt.setText("");
            lblTagsInline.setText("");
            textArea.setText("Keine weiteren Einträge ab " + startDate + ".");
            selectedTags.clear();
            rebuildChips();
            tagInput.setDisable(true);
            textArea.setDisable(true);
            return;
        }

        btnSaveNext.setDisable(false);
        btnSkip.setDisable(false);
        tagInput.setDisable(false);
        textArea.setDisable(false);

        lblEntryDate.setText("Entry date: " + current.entryDate());
        lblCreatedAt.setText("created_at: " + current.createdAt());
        lblTagsInline.setText("current tags: " + (current.tags().isEmpty() ? "(none)" : String.join(", ", current.tags())));

        textArea.setText(current.text() == null ? "" : current.text());
        selectedTags.setAll(current.tags());
        rebuildChips();

        Platform.runLater(() -> {
            textArea.requestFocus();
            textArea.positionCaret(textArea.getText().length());
        });
    }

    private void onSaveNext() {
        if (current == null) {
            return;
        }

        String newText = textArea.getText() == null ? "" : textArea.getText().trim();
        List<String> newTags = new ArrayList<>(selectedTags);

        repo.updateEntry(current.createdAt(), newText, newTags);

        // Autocomplete darf neue Tags kennen (wie in DiaryDialog)
        for (String t : newTags) {
            if (!allTags.contains(t)) {
                allTags.add(t);
            }
        }

        loadNextAndDisplay();
    }

    private void onSkip() {
        loadNextAndDisplay();
    }

    // ---------------- Tag UI ----------------

    private void setupTagAutocomplete() {
        suggestionBox = new VBox();
        suggestionBox.getStyleClass().add("suggestion-box");
        suggestionPopup = new Popup();
        suggestionPopup.setAutoHide(true);
        suggestionPopup.getContent().add(suggestionBox);

        tagInput.setOnAction(_ -> {
            String text = tagInput.getText().trim();
            if (!text.isEmpty()) {
                addTag(text);
            }
        });

        tagInput.setOnKeyPressed(e -> {
            if (!suggestionPopup.isShowing() || currentMatches.isEmpty()) {
                // support backspace-to-remove-last
                if (e.getCode() == KeyCode.BACK_SPACE && tagInput.getText().isEmpty() && !selectedTags.isEmpty()) {
                    selectedTags.remove(selectedTags.size() - 1);
                    rebuildChips();
                }
                return;
            }

            switch (e.getCode()) {
                case ENTER, TAB -> {
                    e.consume();
                    if (activeIndex >= 0 && activeIndex < currentMatches.size()) {
                        addTag(currentMatches.get(activeIndex));
                    }
                }
                case DOWN -> {
                    e.consume();
                    setActiveIndex(Math.min(activeIndex + 1, currentMatches.size() - 1));
                }
                case UP -> {
                    e.consume();
                    setActiveIndex(Math.max(activeIndex - 1, 0));
                }
                case ESCAPE -> {
                    e.consume();
                    hideSuggestions();
                }
                default -> {}
            }
        });

        // update suggestions on typing
        ChangeListener<String> listener = (_, _, newVal) -> {
            if (!suppressSuggestions) {
                updateSuggestions(newVal);
            }
        };
        tagInput.textProperty().addListener(listener);
    }

    private void addTag(String tag) {
        String trimmed = tag.trim();
        if (!trimmed.isEmpty() && !selectedTags.contains(trimmed)) {
            selectedTags.add(trimmed);
            rebuildChips();
        }
        suppressSuggestions = true;
        tagInput.clear();
        suppressSuggestions = false;
        hideSuggestions();
        tagInput.requestFocus();
    }

    private void rebuildChips() {
        chipPane.getChildren().clear();

        for (String tag : selectedTags) {
            chipPane.getChildren().add(createChip(tag));
        }

        chipPane.setVisible(!selectedTags.isEmpty());
    }

    private HBox createChip(String tag) {
        Label label = new Label(tag);
        Button removeBtn = new Button("×");
        removeBtn.getStyleClass().add(TAG_REMOVE_BUTTON);
        removeBtn.setFocusTraversable(false);
        removeBtn.setOnAction(_ -> {
            selectedTags.remove(tag);
            rebuildChips();
            tagInput.requestFocus();
        });

        HBox chip = new HBox(6, label, removeBtn);
        chip.setAlignment(Pos.CENTER_LEFT);
        chip.setPadding(new Insets(4, 8, 4, 8));
        chip.setStyle("-fx-background-radius: 14; -fx-background-color: rgba(0,0,0,0.07);");
        chip.setFocusTraversable(false);
        return chip;
    }

    private void updateSuggestions(String filter) {
        if (filter == null || filter.isBlank()) {
            hideSuggestions();
            return;
        }

        String lower = filter.toLowerCase();
        currentMatches.clear();
        currentMatches.addAll(allTags.stream()
                .filter(t -> t.toLowerCase().startsWith(lower))
                .filter(t -> !selectedTags.contains(t))
                .limit(30)
                .collect(Collectors.toList()));

        if (currentMatches.isEmpty()) {
            hideSuggestions();
            return;
        }

        rebuildSuggestionBox();
        activeIndex = 0;
        highlightActive();
        showPopupBelowInput();
    }

    private void rebuildSuggestionBox() {
        suggestionBox.getChildren().clear();

        for (int i = 0; i < currentMatches.size(); i++) {
            String match = currentMatches.get(i);
            Label label = new Label(match);
            label.setMaxWidth(Double.MAX_VALUE);
            label.setPadding(new Insets(6, 10, 6, 10));

            int index = i;
            label.setOnMouseEntered(_ -> setActiveIndex(index));
            label.setOnMouseClicked(_ -> addTag(match));

            suggestionBox.getChildren().add(label);
        }
        suggestionBox.setPrefWidth(tagInput.getWidth());
        suggestionBox.setStyle("-fx-background-color: white; -fx-border-color: rgba(0,0,0,0.2);");
    }

    private void setActiveIndex(int index) {
        activeIndex = index;
        highlightActive();
    }

    private void highlightActive() {
        var children = suggestionBox.getChildren();
        for (int i = 0; i < children.size(); i++) {
            Label label = (Label) children.get(i);
            label.pseudoClassStateChanged(HIGHLIGHTED, i == activeIndex);
            if (i == activeIndex) {
                label.setStyle("-fx-background-color: rgba(0,0,0,0.08);");
            } else {
                label.setStyle("");
            }
        }
    }

    private void showPopupBelowInput() {
        Bounds bounds = tagInput.localToScreen(tagInput.getBoundsInLocal());
        if (bounds != null) {
            suggestionPopup.show(tagInput, bounds.getMinX(), bounds.getMaxY() + 2);
        }
    }

    private void hideSuggestions() {
        suggestionPopup.hide();
        currentMatches.clear();
        activeIndex = -1;
    }

    // ---------------- Args parsing ----------------

    private static LocalDate parseStartDate(List<String> args) {
        if (args == null || args.isEmpty()) {
            throw new IllegalArgumentException("Startdatum fehlt. Übergib z.B. 2020-01-01 oder --start=2020-01-01");
        }
        String a0 = args.get(0).trim();
        if (a0.startsWith("--start=")) {
            a0 = a0.substring("--start=".length()).trim();
        }
        return LocalDate.parse(a0, D);
    }

    public static void main(String[] args) {
    	Config.init("C:/Users/Markgraf/OneDrive/ThosSuite/");
        launch(args);
    }

    // ---------------- Repo / DB ----------------
    // bewusst lokal in der Wegwerfklasse: keine produktiven API-Änderungen nötig.

    private static final class Repo {

        List<String> loadAllTags() {
            List<String> tags = new ArrayList<>();
            try (PreparedStatement ps = DB.getConnection().prepareStatement(
                    "SELECT name FROM diary_tag ORDER BY name");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    tags.add(rs.getString("name"));
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to load diary tags", e);
            }
            return tags;
        }

        /**
         * Lädt den nächsten Entry ab startDate.
         * Wenn current == null: den ersten >= startDate
         * Sonst: den nächsten nach (entry_date, created_at) von current.
         */
        DiaryEntry loadNextEntry(DiaryEntry current, LocalDate startDate) {
            String sql;
            boolean hasCurrent = current != null;

            if (!hasCurrent) {
                sql = """
                        SELECT created_at, entry_date, text
                        FROM diary_entry
                        WHERE entry_date >= ?
                        ORDER BY entry_date ASC, created_at ASC
                        LIMIT 1
                        """;
            } else {
                sql = """
                        SELECT created_at, entry_date, text
                        FROM diary_entry
                        WHERE entry_date >= ?
                          AND (entry_date > ? OR (entry_date = ? AND created_at > ?))
                        ORDER BY entry_date ASC, created_at ASC
                        LIMIT 1
                        """;
            }

            try (PreparedStatement ps = DB.getConnection().prepareStatement(sql)) {
                ps.setString(1, startDate.format(D));

                if (hasCurrent) {
                    ps.setString(2, current.entryDate().format(D));
                    ps.setString(3, current.entryDate().format(D));
                    ps.setString(4, current.createdAt().format(TS));
                }

                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        return null;
                    }

                    LocalDateTime createdAt = LocalDateTime.parse(rs.getString("created_at"), TS);
                    LocalDate entryDate = LocalDate.parse(rs.getString("entry_date"), D);
                    String text = rs.getString("text");
                    List<String> tags = loadTagsForEntry(createdAt);

                    return new DiaryEntry(createdAt, entryDate, text, tags);
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to load next diary entry", e);
            }
        }

        List<String> loadTagsForEntry(LocalDateTime createdAt) {
            List<String> tags = new ArrayList<>();
            try (PreparedStatement ps = DB.getConnection().prepareStatement(
                    "SELECT tag_name FROM diary_entry_tag WHERE entry_created_at = ? ORDER BY tag_name")) {
                ps.setString(1, createdAt.withNano(0).format(TS));
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        tags.add(rs.getString("tag_name"));
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to load tags for entry " + createdAt, e);
            }
            return tags;
        }

        /**
         * Update: Text in diary_entry + Tags neu setzen (ersetzen).
         * - diary_tag: INSERT OR IGNORE
         * - diary_entry_tag: delete then insert batch
         */
        void updateEntry(LocalDateTime createdAt, String newText, List<String> tags) {
            Connection conn = DB.getConnection();
            try {
                conn.setAutoCommit(false);

                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE diary_entry SET text = ? WHERE created_at = ?")) {
                    ps.setString(1, newText);
                    ps.setString(2, createdAt.withNano(0).format(TS));
                    int updated = ps.executeUpdate();
                    if (updated != 1) {
                        throw new RuntimeException("Expected to update exactly 1 row but updated " + updated);
                    }
                }

                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT OR IGNORE INTO diary_tag (name) VALUES (?)")) {
                    for (String tag : tags) {
                        ps.setString(1, tag);
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }

                try (PreparedStatement ps = conn.prepareStatement(
                        "DELETE FROM diary_entry_tag WHERE entry_created_at = ?")) {
                    ps.setString(1, createdAt.withNano(0).format(TS));
                    ps.executeUpdate();
                }

                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO diary_entry_tag (entry_created_at, tag_name) VALUES (?, ?)")) {
                    String ts = createdAt.withNano(0).format(TS);
                    for (String tag : tags) {
                        ps.setString(1, ts);
                        ps.setString(2, tag);
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }

                conn.commit();
            } catch (Exception e) {
                try { conn.rollback(); } catch (Exception ignored) {}
                throw new RuntimeException("Failed to update diary entry " + createdAt, e);
            } finally {
                try { conn.setAutoCommit(true); } catch (Exception ignored) {}
            }
        }
    }

    private record DiaryEntry(LocalDateTime createdAt, LocalDate entryDate, String text, List<String> tags) {}
}