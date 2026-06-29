package scripts.tmdb;

import java.util.List;

import app.movie.repository.TmdbViewerRepository;
import app.shared.Config;
import app.shared.model.CardData;
import app.shared.skin.SkinService;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * Wegwerf: zeigt alle Episodenbewertungen untereinander in einer scrollbaren
 * VBox. Nutzt repository.loadAllEpisodes() — keine eigene Build-Logik, damit
 * die Anzeige identisch zum echten Viewer ist.
 */
public class EpisodeRatingReview extends Application {

    private final TmdbViewerRepository repository = new TmdbViewerRepository();

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
    	Config.init("C:/Users/permi/Documents/Gedächtnis Lernen und so/ThosSuite/");
        SkinService.setOwnerWindow(stage);

        List<CardData> cards = repository.loadAllEpisodes();

        VBox box = new VBox(10);
        for (CardData card : cards)
            box.getChildren().add(SkinService.get().createCard(card, _ -> {}, _ -> {}));

        ScrollPane scroll = new ScrollPane(box);
        scroll.setFitToWidth(true);

        Scene scene = new Scene(scroll, 900, 1000);
        SkinService.get().styleScene(scene);
        stage.setScene(scene);
        stage.setTitle("Episodenbewertungen — Sichtkontrolle (" + cards.size() + ")");
        stage.show();
    }
}