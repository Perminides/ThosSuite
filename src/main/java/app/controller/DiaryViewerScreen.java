package app.controller;

import java.time.LocalDate;
import java.util.List;

import app.config.Config;
import app.data.Deck;
import app.data.DiaryEntry;
import app.data.SessionSwitchStrategy;
import app.data.persistence.DiaryRepository;
import app.ui.components.DiaryDialog;
import app.ui.skin.Skin.DiaryViewerComponents;
import app.ui.skin.SkinService;
import javafx.application.Platform;
import javafx.css.PseudoClass;
import javafx.geometry.Pos;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Background;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class DiaryViewerScreen implements Screen {

    private static final PseudoClass INVALID_QUERY = PseudoClass.getPseudoClass("invalid-query");
    private static final int DEFAULT_MAX_RESULTS = 100;

    private final DiaryRepository repository = new DiaryRepository();

    private VBox view;
    private TextField queryField;
    private DatePicker fromPicker;
    private DatePicker toPicker;
    private VBox resultBox;

    @Override
    public Pane getView() {
        if (view == null) {
            view = new VBox();
            view.setBackground(new Background(SkinService.get().getBackgroundImage(Deck.WORLD_CARDS)));
            buildView();
        }
        return view;
    }

    @Override
    public void refresh() {
        view.setBackground(new Background(SkinService.get().getBackgroundImage(Deck.WORLD_CARDS)));
    }

    @Override
    public SessionSwitchStrategy getSwitchStrategy() {
        return SessionSwitchStrategy.IMMEDIATE;
    }

    private void buildView() {
        view.getChildren().clear();
        view.setBackground(new Background(SkinService.get().getBackgroundImage(Deck.WORLD_CARDS)));
        view.setAlignment(Pos.TOP_CENTER);
        VBox.setVgrow(view, Priority.ALWAYS);

        DiaryViewerComponents components = SkinService.get().createDiaryViewer();
        fromPicker = components.fromPicker();
        toPicker = components.toPicker();
        queryField = components.queryField();
        resultBox = components.resultBox();

        queryField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                runSearch();
            }
        });

        fromPicker.setOnAction(_ -> runSearch());
        toPicker.setOnAction(_ -> runSearch());

        VBox.setVgrow(components.root(), Priority.ALWAYS);
        view.getChildren().add(components.root());
        Platform.runLater(() -> queryField.requestFocus());
        runSearch();
    }

    private void runSearch() {
        String raw = queryField.getText();
        String whereFragment;

        if (raw == null || raw.isBlank()) {
            // Keine Einschränkung auf Text/Tags — alle Einträge im Zeitraum
            whereFragment = "1=1";
            queryField.pseudoClassStateChanged(INVALID_QUERY, false);
        } else {
            try {
                whereFragment = new QueryParser().parse(raw);
                queryField.pseudoClassStateChanged(INVALID_QUERY, false);
            } catch (QueryParser.InvalidQueryException e) {
                queryField.pseudoClassStateChanged(INVALID_QUERY, true);
                return;
            }
        }

        int maxResults = Config.getInt("diary.maxResults", DEFAULT_MAX_RESULTS);
        LocalDate from = fromPicker.getValue();
        LocalDate to = toPicker.getValue();

        List<DiaryEntry> entries =
                repository.search(whereFragment, from, to, maxResults + 1);

        resultBox.getChildren().clear();

        if (entries.size() > maxResults) {
            entries = entries.subList(0, maxResults);
            Label hint = new Label("Mehr als " + maxResults + " Treffer — bitte Suche verfeinern.");
            hint.getStyleClass().add("diary-viewer-hint");
            resultBox.getChildren().add(hint);
        }

        for (DiaryEntry entry : entries) {
        	Pane card = SkinService.get().createDiaryCard(
                    entry.createdAt(),
                    entry.entryDate(),
                    entry.text(),
                    entry.tags(),
                    entry.attachmentPaths());

            card.setOnMouseClicked(_ -> {
                DiaryDialog editDialog = new DiaryDialog();
                editDialog.showEdit(
                        view.getScene().getWindow(),
                        entry.createdAt(),
                        entry.entryDate(),
                        entry.text(),
                        entry.tags());
                runSearch(); // Ergebnisse nach Edit aktualisieren
            });

            resultBox.getChildren().add(card);
        }
    }

    // -------------------------------------------------------------------------
    // Innere Klasse: QueryParser
    // -------------------------------------------------------------------------

    private static class QueryParser {

        private String input;
        private int pos;

        static class InvalidQueryException extends RuntimeException {
			private static final long serialVersionUID = 1L;

			InvalidQueryException(String message) {
                super(message);
            }
        }

        String parse(String raw) {
            this.input = raw.trim().toLowerCase();
            this.pos = 0;

            if (this.input.isEmpty()) {
                throw new InvalidQueryException("Leere Eingabe");
            }

            String result = parseExpr();

            skipWhitespace();
            if (pos < this.input.length()) {
                throw new InvalidQueryException("Unerwartetes Zeichen an Position " + pos);
            }

            return result;
        }

        private String parseExpr() {
            String left = parseTerm();
            while (true) {
                skipWhitespace();
                if (matchKeyword("or")) {
                    String right = parseTerm();
                    left = "(" + left + " OR " + right + ")";
                } else {
                    break;
                }
            }
            return left;
        }

        private String parseTerm() {
            String left = parseFactor();
            while (true) {
                skipWhitespace();
                if (matchKeyword("and")) {
                    String right = parseFactor();
                    left = "(" + left + " AND " + right + ")";
                } else {
                    break;
                }
            }
            return left;
        }

        private String parseFactor() {
            skipWhitespace();

            if (pos >= input.length()) {
                throw new InvalidQueryException("Unerwartetes Ende des Ausdrucks");
            }

            if (input.charAt(pos) == '(') {
                pos++;
                String inner = parseExpr();
                skipWhitespace();
                if (pos >= input.length() || input.charAt(pos) != ')') {
                    throw new InvalidQueryException("Schließende Klammer fehlt");
                }
                pos++;
                return inner;
            }

            if (input.startsWith("tag:", pos)) {
                pos += 4;
                String tagName = parseWord();
                if (tagName.isEmpty()) {
                    throw new InvalidQueryException("Tag-Name fehlt nach 'tag:'");
                }
                return "EXISTS (SELECT 1 FROM diary_entry_tag det WHERE det.entry_created_at = de.created_at AND lower(det.tag_name) = '" + escapeSql(tagName) + "')";
            }

            String word = parseWord();
            if (word.isEmpty()) {
                throw new InvalidQueryException("Leeres Wort an Position " + pos);
            }
            return "lower(de.text) LIKE '%" + escapeSql(word) + "%'";
        }

        private String parseWord() {
            int start = pos;
            while (pos < input.length()
                    && input.charAt(pos) != ' '
                    && input.charAt(pos) != '('
                    && input.charAt(pos) != ')') {
                pos++;
            }
            String word = input.substring(start, pos);
            if (word.equals("and") || word.equals("or")) {
                pos = start;
                throw new InvalidQueryException("Operator '" + word + "' an ungültiger Stelle");
            }
            return word;
        }

        private boolean matchKeyword(String keyword) {
            if (!input.startsWith(keyword, pos)) {
                return false;
            }
            int after = pos + keyword.length();
            if (after < input.length()) {
                char next = input.charAt(after);
                if (next != ' ' && next != '(') {
                    return false;
                }
            }
            pos += keyword.length();
            return true;
        }

        private void skipWhitespace() {
            while (pos < input.length() && input.charAt(pos) == ' ') {
                pos++;
            }
        }

        private String escapeSql(String value) {
            return value.replace("'", "''");
        }
    }
}