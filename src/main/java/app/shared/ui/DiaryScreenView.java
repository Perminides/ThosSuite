package app.shared.ui;

import java.time.LocalDate;
import java.util.List;
import java.util.function.Consumer;

import app.shared.model.DiaryCardData;
import app.shared.model.ScreenView;
import app.shared.skin.SkinService;
import app.shared.ui.components.SuiteBackground;
import app.shared.ui.components.DiaryCard;
import app.shared.ui.components.SuiteDatePicker;
import javafx.application.Platform;
import javafx.css.PseudoClass;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class DiaryScreenView implements ScreenView {

    private static final PseudoClass INVALID_QUERY = PseudoClass.getPseudoClass("invalid-query");

    public interface SearchListener {
        void onSearch(String query, LocalDate from, LocalDate to);
    }

    private SearchListener searchListener = (_, _, _) -> {};
    private Consumer<DiaryCardData> editListener = _ -> {};

    private VBox view;
    private TextField queryField;
    private SuiteDatePicker fromPicker;
    private SuiteDatePicker toPicker;
    private VBox resultBox;

    public void setSearchListener(SearchListener l) { this.searchListener = l; }
    public void setEditListener(Consumer<DiaryCardData> l) { this.editListener = l; }

    @Override
    public Pane getPane() {
        if (view == null) {
            view = new VBox();
            view.setAlignment(Pos.TOP_CENTER);
            VBox.setVgrow(view, Priority.ALWAYS);
            build();
        }
        return view;
    }

    public void rebuild() {
        if (view != null) build();
    }

    private void build() {
        view.getChildren().clear();
        view.setBackground(SuiteBackground.of(SkinService.get().emptyWallpaperPath()));

        // Filterleiste
        fromPicker = new SuiteDatePicker(LocalDate.now().minusMonths(1));
        toPicker = new SuiteDatePicker(LocalDate.now());

        queryField = new TextField();
        queryField.setPromptText("(x or y) and tag:z");

        HBox filterBar = new HBox();
        filterBar.setAlignment(Pos.CENTER_LEFT);
        filterBar.getStyleClass().add("diary-viewer-filter-bar");
        filterBar.getChildren().addAll(
                new Label("Von:"), fromPicker,
                new Label("Bis:"), toPicker,
                queryField);

        // Ergebnisbereich
        resultBox = new VBox();
        resultBox.getStyleClass().add("diary-viewer-results");

        // ScrollPane
        ScrollPane scrollPane = new ScrollPane(resultBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(false);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.getStyleClass().add("diary-viewer-scroll");

        // Äußerer Wrapper — zentriert alles
        VBox root = new VBox();
        root.setAlignment(Pos.TOP_CENTER);
        root.getStyleClass().add("diary-viewer-root");
        root.getChildren().addAll(filterBar, scrollPane);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        queryField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) fireSearch();
        });
        fromPicker.setOnAction(_ -> fireSearch());
        toPicker.setOnAction(_ -> fireSearch());

        VBox.setVgrow(root, Priority.ALWAYS);
        view.getChildren().add(root);
        Platform.runLater(() -> queryField.requestFocus());
        fireSearch();
    }

    private void fireSearch() {
        searchListener.onSearch(queryField.getText(), fromPicker.getValue(), toPicker.getValue());
    }

    public void showResults(List<DiaryCardData> cards, boolean truncated, int maxResults) {
        resultBox.getChildren().clear();
        if (truncated) {
            Label hint = new Label("Mehr als " + maxResults + " Treffer — bitte Suche verfeinern.");
            hint.getStyleClass().add("diary-viewer-hint");
            resultBox.getChildren().add(hint);
        }
        for (DiaryCardData c : cards) {
            DiaryCard card = new DiaryCard(c);
            card.setOnMouseClicked(_ -> {
                editListener.accept(c); // Screen öffnet Edit-Dialog (blockierend)
                fireSearch();           // nach Edit neu suchen
            });
            resultBox.getChildren().add(card);
        }
    }

    public void setQueryValid(boolean valid) {
        queryField.pseudoClassStateChanged(INVALID_QUERY, !valid);
    }
}
