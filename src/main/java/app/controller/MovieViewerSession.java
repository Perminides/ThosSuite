package app.controller;

import java.util.List;

import app.data.Deck;
import app.data.SessionSwitchStrategy;
import app.data.persistence.TmdbMovieViewerRepository;
import app.tmdb.MovieCardData;
import app.ui.components.SuggestionTextField;
import app.ui.skin.Skin.MovieViewerComponents;
import app.ui.skin.SkinService;
import javafx.scene.layout.Background;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class MovieViewerSession implements Session {

    private final TmdbMovieViewerRepository repository = new TmdbMovieViewerRepository();

    private VBox view;
    private SuggestionTextField directorField;
    private SuggestionTextField actorField;
    private SuggestionTextField titleField;
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
    public void start() {
    }

    @Override
    public void refresh() {
        view.setBackground(new Background(SkinService.get().getBackgroundImage(Deck.WORLD_CARDS)));
    }

    @Override
    public SessionSwitchStrategy getSwitchStrategy() {
        return SessionSwitchStrategy.IMMEDIATE;
    }

    @Override
    public void escClicked() {
    }

    @Override
    public void closeSilent(boolean save) {
    }

    private void buildView() {
        view.getChildren().clear();
        view.setBackground(new Background(SkinService.get().getBackgroundImage(Deck.WORLD_CARDS)));
        VBox.setVgrow(view, Priority.ALWAYS);

        MovieViewerComponents components = SkinService.get().createMovieViewer();
        directorField = components.directorField();
        actorField = components.actorField();
        titleField = components.titleField();
        resultBox = components.resultBox();

        // SWYT-Listen vorladen (sortiert nach Relevanz)
        directorField.setAllItems(repository.loadAllDirectorNames());
        actorField.setAllItems(repository.loadAllActorNames());
        titleField.setAllItems(repository.loadAllTitles());

        // Callbacks: Bei Auswahl die anderen Felder leeren und Suche starten
        directorField.setOnSelected(name -> {
            actorField.clearSilent();
            titleField.clearSilent();
            showMovies(repository.loadMoviesByDirector(name));
        });

        actorField.setOnSelected(name -> {
            directorField.clearSilent();
            titleField.clearSilent();
            showMovies(repository.loadMoviesByActor(name));
        });

        titleField.setOnSelected(name -> {
            directorField.clearSilent();
            actorField.clearSilent();
            showMovies(repository.loadMoviesByTitle(name));
        });

        VBox.setVgrow(components.root(), Priority.ALWAYS);
        view.getChildren().add(components.root());
    }

    private void showMovies(List<MovieCardData> movies) {
        resultBox.getChildren().clear();

        for (MovieCardData movie : movies) {
            Pane card = SkinService.get().createMovieCard(
                    movie,
                    this::onDirectorClicked,
                    this::onActorClicked);
            resultBox.getChildren().add(card);
        }
    }

    /**
     * Link-Callback: Klick auf einen Regisseur-Namen in einer Filmkachel.
     * Setzt das Director-SWYT-Feld und löst die Suche aus.
     */
    private void onDirectorClicked(String directorName) {
        actorField.clearSilent();
        titleField.clearSilent();
        directorField.setTextAndTrigger(directorName);
    }

    /**
     * Link-Callback: Klick auf einen Schauspieler-Namen in einer Filmkachel.
     * Setzt das Actor-SWYT-Feld und löst die Suche aus.
     */
    private void onActorClicked(String actorName) {
        directorField.clearSilent();
        titleField.clearSilent();
        actorField.setTextAndTrigger(actorName);
    }
}