package app.controller;

import java.util.List;

import app.data.Deck;
import app.data.SessionSwitchStrategy;
import app.data.persistence.TmdbViewerRepository;
import app.tmdb.CardData;
import app.ui.components.SuggestionTextField;
import app.ui.skin.Skin.MovieViewerComponents;
import app.ui.skin.SkinService;
import javafx.scene.layout.Background;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class MovieViewerSession implements Session {

    private final TmdbViewerRepository repository = new TmdbViewerRepository();

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

        directorField.setAllItems(repository.loadAllDirectorNames());
        actorField.setAllItems(repository.loadAllActorNames());
        titleField.setAllItems(repository.loadAllTitles());

        directorField.setOnSelected(name -> {
            actorField.clearSilent();
            titleField.clearSilent();
            showCards(repository.loadByDirector(name));
        });

        actorField.setOnSelected(name -> {
            directorField.clearSilent();
            titleField.clearSilent();
            showCards(repository.loadByActor(name));
        });

        titleField.setOnSelected(name -> {
            directorField.clearSilent();
            actorField.clearSilent();
            showCards(repository.loadByTitle(name));
        });

        VBox.setVgrow(components.root(), Priority.ALWAYS);
        view.getChildren().add(components.root());
    }

    private void showCards(List<CardData> cards) {
        resultBox.getChildren().clear();

        for (CardData card : cards) {
            Pane cardPane = SkinService.get().createCard(
                    card,
                    this::onDirectorClicked,
                    this::onActorClicked);
            resultBox.getChildren().add(cardPane);
        }
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