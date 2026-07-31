package app.shared.ui;

import java.util.List;

import app.shared.model.CardData;
import app.shared.model.MovieStyle;
import app.shared.model.ScreenView;
import app.shared.skin.SkinService;
import app.shared.ui.components.SuiteBackground;
import app.shared.ui.components.MovieCard;
import app.shared.ui.components.SuiteSuggestionTextField;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class MovieViewerScreenView implements ScreenView {

    public interface SelectionListener {
        void onDirectorSelected(String name);
        void onActorSelected(String name);
        void onTitleSelected(String name);
    }

    private SelectionListener selectionListener = new SelectionListener() {
        public void onDirectorSelected(String name) {}
        public void onActorSelected(String name) {}
        public void onTitleSelected(String name) {}
    };

    private List<String> directorNames = List.of();
    private List<String> actorNames = List.of();
    private List<String> titleNames = List.of();

    private VBox view;
    private SuiteSuggestionTextField directorField;
    private SuiteSuggestionTextField actorField;
    private SuiteSuggestionTextField titleField;
    private VBox resultBox;

    /** Was gerade in den Kacheln steht — gemerkt, damit ein Neuaufbau die Anzeige wiederherstellen kann. */
    private List<CardData> shownCards = List.of();

    public void setSelectionListener(SelectionListener l) { this.selectionListener = l; }

    public void setNames(List<String> directors, List<String> actors, List<String> titles) {
        this.directorNames = directors;
        this.actorNames = actors;
        this.titleNames = titles;
    }

    @Override
    public Pane getPane() {
        if (view == null) {
            view = new VBox();
            VBox.setVgrow(view, Priority.ALWAYS);
            build();
        }
        return view;
    }

    /**
     * Kompletter Neuaufbau nach einem Skinwechsel — auch Maße, Schriften und Kacheln kommen aus dem
     * Skin, ein bloßer Hintergrundtausch griffe also zu kurz.
     *
     * <p>Die laufende Suche überlebt: die drei Feldinhalte werden still zurückgeschrieben (kein
     * Callback, kein Popup) und die zuletzt gezeigten Kacheln neu gebaut. Eine erneute Abfrage
     * braucht es dafür nicht.</p>
     *
     * <p>Vor dem ersten {@link #getPane()} gibt es nichts aufzubauen — dann baut ohnehin erst der
     * Mount, und zwar bereits mit dem neuen Skin.</p>
     */
    public void rebuild() {
        if (view == null)
            return;

        String director = directorField.getText();
        String actor = actorField.getText();
        String title = titleField.getText();
        List<CardData> cards = shownCards;

        build();

        directorField.setTextSilent(director);
        actorField.setTextSilent(actor);
        titleField.setTextSilent(title);
        showCards(cards);
    }

    private void build() {
        view.getChildren().clear();
        view.setBackground(SuiteBackground.of(SkinService.get().emptyWallpaperPath()));

        MovieStyle style = SkinService.get().movieStyle();

        // SWYT-Felder
        directorField = new SuiteSuggestionTextField("Choose Director...");
        actorField = new SuiteSuggestionTextField("Choose Actor...");
        titleField = new SuiteSuggestionTextField("Choose Title...");

        // Labels für die Felder
        Label dirLabel = new Label("Director:");
        Label actLabel = new Label("Actor:");
        Label titLabel = new Label("Title:");

        // SWYT-Felder mit Labels in einer VBox
        double swytWidth = style.contentWidth() * 0.2;
        VBox swytPane = new VBox(style.font().getSize() * 0.8);
        swytPane.getStyleClass().add("movie-viewer-swyt");
        swytPane.setPrefWidth(swytWidth);
        swytPane.setMinWidth(swytWidth);
        swytPane.setMaxWidth(swytWidth);
        swytPane.getChildren().addAll(
                dirLabel, directorField,
                actLabel, actorField,
                titLabel, titleField);

        // Ergebnisbereich
        resultBox = new VBox(style.font().getSize() * 0.5);
        resultBox.getStyleClass().add("movie-viewer-results");

        // ScrollPane für Ergebnisse
        ScrollPane scrollPane = new ScrollPane(resultBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(false);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.getStyleClass().add("movie-viewer-scroll");

        // Hauptlayout: SWYT links (20%), Kacheln rechts (70%), je 5% Rand
        HBox contentBox = new HBox(style.font().getSize());
        contentBox.getStyleClass().add("movie-viewer-content");
        HBox.setHgrow(scrollPane, Priority.ALWAYS);
        contentBox.getChildren().addAll(swytPane, scrollPane);

        // Äußerer Wrapper mit Padding
        VBox root = new VBox();
        root.getStyleClass().add("movie-viewer-root");
        root.getChildren().add(contentBox);
        VBox.setVgrow(contentBox, Priority.ALWAYS);

        directorField.setAllItems(directorNames);
        actorField.setAllItems(actorNames);
        titleField.setAllItems(titleNames);

        directorField.setOnSelected(name -> {
            actorField.clearSilent();
            titleField.clearSilent();
            selectionListener.onDirectorSelected(name);
        });
        actorField.setOnSelected(name -> {
            directorField.clearSilent();
            titleField.clearSilent();
            selectionListener.onActorSelected(name);
        });
        titleField.setOnSelected(name -> {
            directorField.clearSilent();
            actorField.clearSilent();
            selectionListener.onTitleSelected(name);
        });

        VBox.setVgrow(root, Priority.ALWAYS);
        view.getChildren().add(root);
    }

    public void showCards(List<CardData> cards) {
        shownCards = cards;
        resultBox.getChildren().clear();
        MovieStyle style = SkinService.get().movieStyle();
        for (CardData card : cards)
            resultBox.getChildren().add(
                    new MovieCard(card, style, this::onDirectorClicked, this::onActorClicked));
    }

    private void onDirectorClicked(String directorName) {
        actorField.clearSilent();
        titleField.clearSilent();
        directorField.setTextAndTrigger(directorName);
    }

    private void onActorClicked(String actorName) {
        directorField.clearSilent();
        titleField.clearSilent();
        actorField.setTextAndTrigger(actorName);
    }
}
