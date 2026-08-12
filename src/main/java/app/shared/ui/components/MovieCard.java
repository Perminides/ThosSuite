package app.shared.ui.components;

import java.io.File;
import java.util.List;
import java.util.function.Consumer;

import app.shared.Config;
import app.shared.UiUtils;
import app.shared.model.CardData;
import app.shared.model.MovieStyle;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Popup;
import javafx.stage.Screen;

/**
 * Eine Film-Kachel: Poster, Rating-Zahl, Infoblock mit klickbaren Namen.
 *
 * <p>Passiver Baustein — bekommt Daten, Maße und die beiden Klick-Callbacks herein.</p>
 */
public class MovieCard extends HBox implements Card {

	private final MovieStyle style;

	public MovieCard(CardData data, MovieStyle style,
			Consumer<String> onDirectorClicked,
			Consumer<String> onActorClicked) {
		this.style = style;

		boolean hasComment = data.comment() != null
				&& !data.comment().isEmpty()
				&& !".".equals(data.comment());

		StackPane posterPane = buildPoster(data, hasComment);

		// Kommentar-Popup auf dem Poster
		if (hasComment && posterPane != null)
			setupCommentPopup(posterPane, data.comment());

		// === Rating-Zahl ===
		Label ratingLabel = new Label(String.valueOf(data.rating()));
		ratingLabel.getStyleClass().add("movie-card-rating");
		Text widestRating = new Text("10");
		widestRating.setFont(Font.font(style.font().getFamily(), FontWeight.BOLD, style.posterWidth() * 0.5));
		ratingLabel.setPrefWidth(widestRating.getLayoutBounds().getWidth());
		ratingLabel.setMinWidth(Region.USE_PREF_SIZE);

		VBox infoBox = buildInfoBox(data, onDirectorClicked, onActorClicked);

		// === Kachel zusammenbauen ===
		// Die Style-Klasse heißt "diary-card" — die Film-Kachel benutzt bewusst dasselbe CSS
		// wie die Tagebuch-Karte. Unverändert übernommen.
		setSpacing(style.font().getSize() * 0.5);
		getStyleClass().add("diary-card");
		if (posterPane != null)
			getChildren().add(posterPane);
		getChildren().addAll(ratingLabel, infoBox);
		ratingLabel.setMaxHeight(Double.MAX_VALUE);
	}

	/**
	 * Das +-Zeichen wird direkt auf das Bild gemalt ({@code UiUtils.addPlusSign}), es braucht also
	 * keinen eigenen Badge-Node.
	 *
	 * @return das Poster, oder {@code null} wenn es keine Bilddatei gibt
	 */
	private StackPane buildPoster(CardData data, boolean hasComment) {
		File imageFile = data.imageFilename() != null
				? Config.getPath("imageFolder").resolve("tmdb").resolve(data.imageFilename()).toFile()
				: null;

		if (imageFile == null || !imageFile.exists())
			imageFile = Config.getPath("imageFolder").resolve("tmdb").resolve("None_available_en-US_154_231.jpg").toFile();

		Image posterImage = new Image(imageFile.toURI().toString());
		if (hasComment)
			posterImage = UiUtils.addPlusSign(posterImage, style.posterWidth());

		ImageView posterView = new ImageView(posterImage);
		posterView.setFitWidth(style.posterWidth());
		posterView.setPreserveRatio(true);
		posterView.setSmooth(true);

		return new StackPane(posterView);
	}

	private VBox buildInfoBox(CardData data,
			Consumer<String> onDirectorClicked,
			Consumer<String> onActorClicked) {
		VBox infoBox = new VBox(0);
		infoBox.getStyleClass().add("movie-card-info");
		HBox.setHgrow(infoBox, Priority.ALWAYS);

		Label headerLabel = new Label(String.join("\n", data.headerLines()));
		headerLabel.getStyleClass().add("movie-card-header");
		infoBox.getChildren().add(headerLabel);

		// Abstand nach Header-Block
		Region headerSpacer = new Region();
		headerSpacer.setPrefHeight(style.font().getSize());
		infoBox.getChildren().add(headerSpacer);

		String detailString = String.join("\n", data.detailLines());
		if (detailString != null && !detailString.isEmpty()) {
			Label detailLabel = new Label(detailString);
			detailLabel.getStyleClass().add("movie-card-text");
			infoBox.getChildren().add(detailLabel);
		}

		if (data.ratedAt() != null) {
			Label ratedLabel = new Label("Rated: " + data.ratedAt());
			ratedLabel.getStyleClass().add("movie-card-text");
			infoBox.getChildren().add(ratedLabel);
		}

		if (!data.directors().isEmpty())
			infoBox.getChildren().add(linkedPersonLine("Director: ", data.directors(), onDirectorClicked));

		if (!data.actors().isEmpty())
			infoBox.getChildren().add(linkedPersonLine("Stars: ", data.actors(), onActorClicked));

		if (data.overview() != null && !data.overview().isEmpty()) {
			Region overviewSpacer = new Region();
			overviewSpacer.setPrefHeight(style.font().getSize());
			infoBox.getChildren().add(overviewSpacer);

			Label overviewLabel = new Label(data.overview());
			overviewLabel.setWrapText(true);
			overviewLabel.getStyleClass().add("movie-card-text");
			infoBox.getChildren().add(overviewLabel);
		}
		return infoBox;
	}

	/**
	 * Eine Zeile mit Prefix ("Director: " / "Stars: ") und klickbaren Namen. Die Namen liegen als
	 * einzelne Labels in einer FlowPane, damit sie bei Bedarf umbrechen.
	 */
	private Pane linkedPersonLine(String prefix, List<String> names, Consumer<String> onClick) {
		FlowPane flow = new FlowPane();
		flow.setHgap(0);
		flow.setVgap(2);
		flow.getStyleClass().add("movie-card-person-line");

		Label prefixLabel = new Label(prefix);
		prefixLabel.getStyleClass().add("movie-card-text");
		flow.getChildren().add(prefixLabel);

		for (int i = 0; i < names.size(); i++) {
			String name = names.get(i);
			Label link = new Label(name);
			link.getStyleClass().add("movie-card-link");
			link.setOnMouseClicked(_ -> onClick.accept(name));
			flow.getChildren().add(link);

			if (i < names.size() - 1) {
				Label comma = new Label(", ");
				comma.getStyleClass().add("movie-card-text");
				flow.getChildren().add(comma);
			}
		}

		return flow;
	}

	/**
	 * Zeigt einen Kommentar als Popup auf dem Poster-Image.
	 *
	 * <p>Wir nutzen ein Popup statt eines Tooltips, weil der JavaFX-Tooltip bei
	 * {@code setGraphic()} die maxWidth des Graphic-Nodes nicht zuverlässig respektiert:
	 * einzeilige Texte (ohne \n) werden nicht umgebrochen, mehrzeilige schon. Das ist eine
	 * Eigenheit des Tooltip-Layouts, kein dokumentierter Bug, aber reproduzierbar (getestet mit
	 * JavaFX 25). Ein Popup mit einem Label darin bricht zuverlässig um.</p>
	 */
	private void setupCommentPopup(Pane target, String commentText) {
		Popup popup = new Popup();

		target.setOnMouseEntered(e -> {
			popup.getContent().clear();

			double tooltipWidth = target.getScene().getWindow().getWidth() * 0.5;

			Label content = new Label(commentText);
			content.setWrapText(true);
			content.setMaxWidth(tooltipWidth);
			content.getStyleClass().add("movie-comment-popup");

			popup.getContent().add(content);

			// Links oder rechts vom Mauszeiger, je nachdem wo mehr Platz ist
			Rectangle2D screen = Screen.getPrimary().getVisualBounds();
			double mouseX = e.getScreenX();
			double mouseY = e.getScreenY();

			double leftSpace = mouseX - screen.getMinX();
			double rightSpace = screen.getMaxX() - mouseX;

			double popupX;
			if (rightSpace >= leftSpace)
				popupX = mouseX + style.tooltipMargin();
			else
				popupX = mouseX - style.tooltipMargin() - tooltipWidth;

			double popupY = Math.max(screen.getMinY() + style.tooltipMargin(), mouseY);

			popup.show(target, popupX, popupY);
		});

		target.setOnMouseExited(_ -> popup.hide());
	}
}
