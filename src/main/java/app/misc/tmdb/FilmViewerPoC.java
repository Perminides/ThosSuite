package app.misc.tmdb;

import java.util.List;

import app.config.Config;
import app.ui.skin.Skin;
import app.ui.skin.SkinService;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import javafx.stage.Screen;
import javafx.stage.Stage;

public class FilmViewerPoC extends Application {

    private static final double POSTER_WIDTH  = 154;
    private static final double POSTER_HEIGHT = 231;

    private static final double OVERLAY_WIDTH   = 400;
    private static final double OVERLAY_PADDING = 12;

    // ── Fake-Daten ────────────────────────────────────────────────────────────

    record Actor(String name) {}
    record Director(String name) {}

    record FilmEntry(
        String titleOriginal,
        String titleLocalized,
        int year,
        String ratedDate,
        int rating,
        Director director,
        List<Actor> actors,
        String synopsis,
        String comment
    ) {}

    private static final List<FilmEntry> FAKE_FILMS = List.of(

        new FilmEntry(
            "The Zone of Interest", null, 2023, "2024-03-10", 6,
            new Director("Jonathan Glazer"),
            List.of(new Actor("Christian Friedel"), new Actor("Sandra Hüller"),
                    new Actor("Johann Karthaus"), new Actor("Luis Noah Witte"),
                    new Actor("Nele Ahrensmeier")),
            "The commandant of Auschwitz, Rudolf Höss, and his wife Hedwig strive to build a dream life for their family in a house next to the camp.",
            "Mit Jörn im Hochhauskino. Sicherlich cineastisch und vom Konzept nicht uninteressant und in der Bildgestaltung teilweise meisterhaft. Am besten hat mir die Szene mit der Mutter im Schlafzimmer gefallen, die die Schreie der Opfer und das Widerspiegeln des Feuerscheins von den Gasöfen doch nicht ganz so gut wegstecken kann. Aber am Ende ließ mich der Film auch recht kalt."
        ),

        new FilmEntry(
            "Anatomie d'une chute", "Anatomie eines Falls", 2023, "2023-12-06", 7,
            new Director("Justine Triet"),
            List.of(new Actor("Sandra Hüller"), new Actor("Swann Arlaud"),
                    new Actor("Milo Machado-Graner"), new Actor("Antoine Reinartz"),
                    new Actor("Saadia Bentaïeb")),
            "A woman is suspected of her husband's murder, and their blind son faces a moral dilemma as the sole witness.",
            null
        ),

        new FilmEntry(
            "In the Aisles", "In den Gängen", 2018, "2018-02-25", 5,
            new Director("Thomas Stuber"),
            List.of(new Actor("Franz Rogowski"), new Actor("Sandra Hüller"),
                    new Actor("Peter Kurth"), new Actor("Henning Peker"),
                    new Actor("Ramona Kunze-Libnow")),
            "Christian, a reclusive young man from Leipzig, gets a job working the night shift at a big-box store and develops a quiet infatuation with a co-worker named Marion.",
            "Stille, zarte Studie über Einsamkeit und unausgesprochene Gefühle. Franz Rogowski trägt den Film auf den Schultern.\n\nDie Großmarkt-Atmosphäre wirkt erstaunlich poetisch. Kein großes Kino, aber ein sehr ehrliches."
        ),

        new FilmEntry(
            "Toni Erdmann", null, 2016, "2016-07-16", 7,
            new Director("Maren Ade"),
            List.of(new Actor("Sandra Hüller"), new Actor("Peter Simonischek"),
                    new Actor("Michael Wittenborn"), new Actor("Thomas Loibl"),
                    new Actor("Trystan Pütter")),
            "Without warning a father comes to visit his daughter abroad. He believes that she lost her humor and therefore surprises her with a rampage of jokes.",
            null
        )
    );

    // ── Application Entry ─────────────────────────────────────────────────────

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        Config.init("C:/Users/permi/Documents/Gedächtnis Lernen und so/ThosSuite/");
        Skin skin = SkinService.get();

        VBox resultBox = new VBox(12);
        resultBox.setPadding(new Insets(12, 0, 12, 0));

        for (FilmEntry film : FAKE_FILMS) {
            resultBox.getChildren().add(createFilmCard(film, primaryStage));
        }

        ScrollPane scrollPane = new ScrollPane(resultBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.getStyleClass().add("diary-viewer-scroll");

        VBox root = new VBox(scrollPane);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        root.setPadding(new Insets(16));

        Scene scene = new Scene(root, 1200, 800);
        skin.styleScene(scene);

        primaryStage.setTitle("Film Viewer – PoC");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    // ── Kachel ────────────────────────────────────────────────────────────────

    private HBox createFilmCard(FilmEntry film, Stage stage) {
        StackPane posterPane = createPosterPane(film, stage);

        Label ratingLabel = new Label(String.valueOf(film.rating()));
        ratingLabel.getStyleClass().add("film-rating");
        ratingLabel.setMinWidth(Region.USE_PREF_SIZE);
        ratingLabel.setAlignment(Pos.CENTER);

        VBox infoBox = createInfoBox(film);
        HBox.setHgrow(infoBox, Priority.ALWAYS);

        HBox card = new HBox(16, posterPane, ratingLabel, infoBox);
        card.setPadding(new Insets(12));
        card.setAlignment(Pos.CENTER_LEFT);
        card.getStyleClass().add("diary-card");

        return card;
    }

    // ── Poster ────────────────────────────────────────────────────────────────

    private StackPane createPosterPane(FilmEntry film, Stage stage) {
        javafx.scene.shape.Rectangle placeholder = new javafx.scene.shape.Rectangle(POSTER_WIDTH, POSTER_HEIGHT);
        placeholder.getStyleClass().add("film-poster-placeholder");

        Label posterTitle = new Label(film.titleOriginal());
        posterTitle.setWrapText(true);
        posterTitle.setMaxWidth(POSTER_WIDTH - 8);
        posterTitle.setAlignment(Pos.CENTER);
        posterTitle.getStyleClass().add("diary-card-text");

        StackPane posterPane = new StackPane(placeholder, posterTitle);
        posterPane.setMinSize(POSTER_WIDTH, POSTER_HEIGHT);
        posterPane.setMaxSize(POSTER_WIDTH, POSTER_HEIGHT);

        if (film.comment() != null && !film.comment().isBlank()) {
            Label plusLabel = new Label("+");
            plusLabel.getStyleClass().add("film-plus-indicator");
            StackPane.setAlignment(plusLabel, Pos.BOTTOM_LEFT);
            StackPane.setMargin(plusLabel, new Insets(0, 0, 4, 4));
            posterPane.getChildren().add(plusLabel);

            Popup commentPopup = buildCommentPopup(film.comment());

            posterPane.setOnMouseEntered(e ->
                showPopup(commentPopup, e.getScreenX(), e.getScreenY(), stage));
            posterPane.setOnMouseExited(_ -> commentPopup.hide());
        }

        return posterPane;
    }

    // ── Kommentar-Overlay ─────────────────────────────────────────────────────

    private Popup buildCommentPopup(String comment) {
        Label label = new Label(comment);
        label.setWrapText(true);
        label.setPrefWidth(OVERLAY_WIDTH - OVERLAY_PADDING * 2);
        label.getStyleClass().add("diary-card-text");

        VBox container = new VBox(label);
        container.setPadding(new Insets(OVERLAY_PADDING));
        container.setPrefWidth(OVERLAY_WIDTH);
        container.getStyleClass().add("film-comment-popup");

        Popup popup = new Popup();
        popup.getContent().add(container);
        popup.setAutoFix(false);
        return popup;
    }

    private void showPopup(Popup popup, double mouseX, double mouseY, Stage stage) {
        // Erstmal anzeigen damit JavaFX die Höhe berechnet
        popup.show(stage, mouseX + 16, mouseY);

        double screenMinY  = Screen.getPrimary().getVisualBounds().getMinY();
        double screenMaxY  = Screen.getPrimary().getVisualBounds().getMaxY();
        double availableH  = screenMaxY - screenMinY - 16;

        // Popup-Höhe auf verfügbare Bildschirmhöhe deckeln
        double popupH = Math.min(popup.getHeight(), availableH);

        // Oberkante: idealerweise auf Maus-Y, aber hochschieben wenn nötig
        double anchorY = mouseY;
        if (anchorY + popupH > screenMaxY - 8) {
            anchorY = screenMaxY - 8 - popupH;
        }
        if (anchorY < screenMinY + 8) {
            anchorY = screenMinY + 8;
        }

        popup.setAnchorX(mouseX + 16);
        popup.setAnchorY(anchorY);
    }

    // ── Info-Box ──────────────────────────────────────────────────────────────

    private VBox createInfoBox(FilmEntry film) {
        VBox box = new VBox(6);

        Label originalTitleLabel = new Label(film.titleOriginal() + " (" + film.year() + ")");
        originalTitleLabel.getStyleClass().add("film-title");

        VBox titleBox = new VBox(2, originalTitleLabel);
        if (film.titleLocalized() != null) {
            Label localizedLabel = new Label(film.titleLocalized());
            localizedLabel.getStyleClass().add("film-title");
            titleBox.getChildren().add(localizedLabel);
        }

        Label ratedLabel = new Label("Bewertet: " + film.ratedDate());
        ratedLabel.getStyleClass().add("diary-card-text");

        Label dirLabel = new Label("Regie: ");
        dirLabel.getStyleClass().add("diary-card-text");
        Hyperlink dirLink = new Hyperlink(film.director().name());
        dirLink.getStyleClass().add("film-hyperlink");
        dirLink.setOnAction(_ -> onDirectorClicked(film.director()));
        HBox directorRow = new HBox(0, dirLabel, dirLink);
        directorRow.setAlignment(Pos.CENTER_LEFT);

        HBox actorRow = new HBox(0);
        actorRow.setAlignment(Pos.CENTER_LEFT);
        Label mitLabel = new Label("Mit: ");
        mitLabel.getStyleClass().add("diary-card-text");
        actorRow.getChildren().add(mitLabel);
        for (int i = 0; i < film.actors().size(); i++) {
            Actor actor = film.actors().get(i);
            Hyperlink link = new Hyperlink(actor.name());
            link.getStyleClass().add("film-hyperlink");
            link.setOnAction(_ -> onActorClicked(actor));
            actorRow.getChildren().add(link);
            if (i < film.actors().size() - 1) {
                Label comma = new Label(", ");
                comma.getStyleClass().add("diary-card-text");
                actorRow.getChildren().add(comma);
            }
        }

        Label synopsisLabel = new Label(film.synopsis());
        synopsisLabel.getStyleClass().add("diary-card-text");
        synopsisLabel.setWrapText(true);
        synopsisLabel.setMaxWidth(400);

        box.getChildren().addAll(titleBox, ratedLabel, directorRow, actorRow, synopsisLabel);
        return box;
    }

    // ── Klick-Handler ─────────────────────────────────────────────────────────

    private void onDirectorClicked(Director director) {
        System.out.println("Suche nach Regisseur: " + director.name());
    }

    private void onActorClicked(Actor actor) {
        System.out.println("Suche nach Schauspieler: " + actor.name());
    }
}