package app.shared.ui.surfaces;

import java.time.LocalDate;
import java.util.List;
import java.util.function.Consumer;

import app.shared.model.DiaryCardData;
import app.shared.skin.Skin.DiaryViewerComponents;
import app.shared.ui.contracts.ScreenView;
import app.shared.skin.SkinService;
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

public class DiaryScreenView implements ScreenView {

    private static final PseudoClass INVALID_QUERY = PseudoClass.getPseudoClass("invalid-query");

    public interface SearchListener {
        void onSearch(String query, LocalDate from, LocalDate to);
    }

    private SearchListener searchListener = (_, _, _) -> {};
    private Consumer<DiaryCardData> editListener = _ -> {};

    private VBox view;
    private TextField queryField;
    private DatePicker fromPicker;
    private DatePicker toPicker;
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
        view.setBackground(new Background(SkinService.get().getEmptyBackgroundImage()));

        DiaryViewerComponents components = SkinService.get().createDiaryViewer();
        fromPicker = components.fromPicker();
        toPicker = components.toPicker();
        queryField = components.queryField();
        resultBox = components.resultBox();

        queryField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) fireSearch();
        });
        fromPicker.setOnAction(_ -> fireSearch());
        toPicker.setOnAction(_ -> fireSearch());

        VBox.setVgrow(components.root(), Priority.ALWAYS);
        view.getChildren().add(components.root());
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
            Pane card = SkinService.get().createDiaryCard(
                    c.createdAt(), c.entryDate(), c.text(), c.tags(), c.attachments());
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