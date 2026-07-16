package app.shared.ui.components;

import java.util.List;

import app.shared.ScreenView;
import app.shared.model.CardData;
import app.shared.skin.Skin.MovieViewerComponents;
import app.shared.skin.SkinService;
import app.shared.ui.SuggestionTextField;
import javafx.scene.layout.Background;
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
    private SuggestionTextField directorField;
    private SuggestionTextField actorField;
    private SuggestionTextField titleField;
    private VBox resultBox;

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

    public void reapplyBackground() {
        if (view != null)
            view.setBackground(new Background(SkinService.get().getEmptyBackgroundImage()));
    }

    private void build() {
        view.getChildren().clear();
        view.setBackground(new Background(SkinService.get().getEmptyBackgroundImage()));

        MovieViewerComponents components = SkinService.get().createMovieViewer();
        directorField = components.directorField();
        actorField = components.actorField();
        titleField = components.titleField();
        resultBox = components.resultBox();

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

        VBox.setVgrow(components.root(), Priority.ALWAYS);
        view.getChildren().add(components.root());
    }

    public void showCards(List<CardData> cards) {
        resultBox.getChildren().clear();
        for (CardData card : cards) {
            Pane cardPane = SkinService.get().createCard(card, this::onDirectorClicked, this::onActorClicked);
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