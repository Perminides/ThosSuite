package app.shared.ui;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import app.shared.model.DiaryCardData;
import app.shared.model.ScreenView;
import app.shared.skin.SkinService;
import app.shared.ui.components.DiaryCard;
import app.shared.ui.components.SuiteBackground;
import app.shared.ui.components.SuiteCardList;
import app.shared.ui.components.SuiteDatePicker;
import app.shared.ui.components.SuiteTextField;
import javafx.application.Platform;
import javafx.css.PseudoClass;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class DiaryScreenView implements ScreenView {

    private static final PseudoClass INVALID_QUERY = PseudoClass.getPseudoClass("invalid-query");
    private static final double SPALTENANTEIL = 0.63; // Breite der Kartenspalte, Anteil an der Contentbreite. Obergrenze für die Zeilenlänge des Fließtextes; ein Skin-Feld daraus wird erst, wenn ein Skin einen anderen Wert braucht.

    public interface SearchListener {
        void onSearch(String query, LocalDate from, LocalDate to);
    }

    private SearchListener searchListener = (_, _, _) -> {};
    private Consumer<DiaryCardData> editListener = _ -> {};

    private VBox view;
    private TextField queryField;
    private SuiteDatePicker fromPicker;
    private SuiteDatePicker toPicker;
    private Label hinweis;
    private SuiteCardList kartenListe;

    public void setSearchListener(SearchListener l) { this.searchListener = l; }
    public void setEditListener(Consumer<DiaryCardData> l) { this.editListener = l; }

    @Override
    public Pane getPane() {
        if (view == null) {
            view = new VBox();
            view.setAlignment(Pos.TOP_CENTER);
            view.getStyleClass().add("diary-viewer-root"); // Hier und nicht in build(): das läuft bei jedem Skinwechsel erneut
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

        double spaltenBreite = SkinService.get().getContentSize().getWidth() * SPALTENANTEIL;

        // Filterleiste
        fromPicker = new SuiteDatePicker(LocalDate.now().minusMonths(1));
        toPicker = new SuiteDatePicker(LocalDate.now());

        queryField = new SuiteTextField();
        queryField.setPromptText("(x or y) and tag:z");
        queryField.setMaxWidth(Double.MAX_VALUE); // Nimmt den Platz, den die Datumsfelder übrig lassen, damit die Zeile an der Scrollbar endet

        HBox filterBar = new HBox();
        filterBar.setAlignment(Pos.CENTER_LEFT);
        filterBar.getStyleClass().add("diary-viewer-filter-bar");
        filterBar.setMaxWidth(spaltenBreite);
        filterBar.getChildren().addAll(
                new Label("Von:"), fromPicker,
                new Label("Bis:"), toPicker,
                queryField);
        HBox.setHgrow(queryField, Priority.ALWAYS);

        // Hinweis auf zu viele Treffer — steht über der Liste und scrollt deshalb nicht weg
        hinweis = new Label();
        hinweis.getStyleClass().add("diary-viewer-hint");
        hinweis.setMaxWidth(spaltenBreite);
        zeigeHinweis(null);

        // Ergebnisbereich
        kartenListe = new SuiteCardList();
        kartenListe.setMaxWidth(spaltenBreite); // Dieselbe Zahl wie die Filterleiste — die beiden sind zusammen die Spalte

        queryField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) fireSearch();
        });
        fromPicker.setOnAction(_ -> fireSearch());
        toPicker.setOnAction(_ -> fireSearch());

        view.getChildren().addAll(filterBar, hinweis, kartenListe);
        VBox.setVgrow(kartenListe, Priority.ALWAYS);
        Platform.runLater(() -> queryField.requestFocus());
        fireSearch();
    }

    private void fireSearch() {
        searchListener.onSearch(queryField.getText(), fromPicker.getValue(), toPicker.getValue());
    }

    public void showResults(List<DiaryCardData> cards, boolean truncated, int maxResults) {
        zeigeHinweis(truncated ? "Mehr als " + maxResults + " Treffer — bitte Suche verfeinern." : null);

        List<DiaryCard> inhalt = new ArrayList<>();
        for (DiaryCardData c : cards) {
            DiaryCard card = new DiaryCard(c);
            card.setOnMouseClicked(_ -> {
                editListener.accept(c); // Screen öffnet Edit-Dialog (blockierend)
                fireSearch();           // nach Edit neu suchen
            });
            inhalt.add(card);
        }
        kartenListe.setCards(inhalt);
    }

    /** {@code null} blendet aus — auch aus dem Layout, sonst bliebe die leere Zeile stehen. */
    private void zeigeHinweis(String text) {
        hinweis.setText(text == null ? "" : text);
        hinweis.setVisible(text != null);
        hinweis.setManaged(text != null);
    }

    public void setQueryValid(boolean valid) {
        queryField.pseudoClassStateChanged(INVALID_QUERY, !valid);
    }
}
