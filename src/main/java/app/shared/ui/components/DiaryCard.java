package app.shared.ui.components;

import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.List;

import app.shared.Config;
import app.shared.model.DiaryAttachment;
import app.shared.model.DiaryCardData;
import app.shared.model.DiaryStyle;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.stage.Screen;
import javafx.util.Duration;

/**
 * Ein Tagebuch-Eintrag als Karte: Datum, Tags, Thumbnails der Anhänge, Text.
 *
 * <p>Passiver Baustein — bekommt Daten und Maße herein, holt sich nichts.</p>
 */
public class DiaryCard extends VBox {

	private final double tooltipMargin;

	public DiaryCard(DiaryCardData data, DiaryStyle style) {
		this.tooltipMargin = style.tooltipMargin();

		Label dateLabel = new Label(data.entryDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")));
		dateLabel.getStyleClass().add("diary-card-date");

		String tagsText = data.tags().isEmpty() ? "" : String.join(" · ", data.tags());
		Label tagsLabel = new Label(tagsText);
		tagsLabel.getStyleClass().add("diary-card-tags");
		tagsLabel.managedProperty().bind(tagsLabel.visibleProperty());
		tagsLabel.setVisible(!data.tags().isEmpty());

		Label textLabel = new Label(data.text());
		textLabel.getStyleClass().add("diary-card-text");
		textLabel.setWrapText(true);
		textLabel.setMaxWidth(Double.MAX_VALUE);

		setSpacing(6);
		getChildren().addAll(dateLabel, tagsLabel);

		if (!data.attachments().isEmpty())
			getChildren().add(buildThumbnails(data.attachments()));

		getChildren().add(textLabel);
		getStyleClass().add("diary-card");
		setMaxWidth(Double.MAX_VALUE);
	}

	/**
	 * !Sofort: Das muss eine eigene Komponente werden. Ein Thumbnail, das die vergrößerte Version
	 * als MouseOver anzeigt, ist generisch genug und wird sicher nochmal wiederverwendet.
	 */
	private FlowPane buildThumbnails(List<DiaryAttachment> attachments) {
		int thumbHeight = Config.getInt("diary.thumbnailHeight", 120);

		FlowPane thumbPane = new FlowPane(8, 8);
		thumbPane.getStyleClass().add("diary-card-thumbs");

		Tooltip tooltip = new Tooltip();
		tooltip.setShowDelay(Duration.millis(300));
		tooltip.setShowDuration(Duration.INDEFINITE);
		tooltip.setHideDelay(Duration.ZERO);
		tooltip.setAutoFix(false);
		tooltip.setStyle("-fx-padding: 0;");

		for (DiaryAttachment attachment : attachments) {
			Path thumbPath = Path.of(attachment.thumbnailPath());
			Path originalPath = Path.of(attachment.imagePath());

			ImageView iv = new ImageView(new Image(thumbPath.toUri().toString(), -1, thumbHeight, true, true));

			iv.setOnMouseEntered(e -> showEnlarged(tooltip, iv, originalPath, e.getScreenX(), e.getScreenY()));
			iv.setOnMouseExited(_ -> tooltip.hide());

			thumbPane.getChildren().add(iv);
		}
		return thumbPane;
	}

	/**
	 * Sucht die größte Fläche für das Originalbild: erst der bessere Halbraum links/rechts, dann
	 * der bessere oben/unten, dann gewinnt der mit der größeren Bildfläche.
	 */
	private void showEnlarged(Tooltip tooltip, ImageView anchor, Path originalPath, double mouseX, double mouseY) {
		Rectangle2D screen = Screen.getPrimary().getVisualBounds();

		// Bild einmal laden
		Image original = new Image(originalPath.toUri().toString());
		double naturalW = original.getWidth();
		double naturalH = original.getHeight();
		double aspectRatio = naturalW / naturalH;

		// Schritt 1: Gewinner links/rechts
		double leftW = mouseX - screen.getMinX() - 2 * tooltipMargin;
		double rightW = screen.getMaxX() - mouseX - 2 * tooltipMargin;
		double hemiH = screen.getHeight() - 2 * tooltipMargin;

		boolean useRight = rightW >= leftW;
		double hWinner_W = useRight ? rightW : leftW;

		double hLR_imgW, hLR_imgH;
		if (naturalW <= hWinner_W && naturalH <= hemiH) {
			hLR_imgW = naturalW; hLR_imgH = naturalH;
		} else if (naturalW / hWinner_W >= naturalH / hemiH) {
			hLR_imgW = hWinner_W; hLR_imgH = hWinner_W / aspectRatio;
		} else {
			hLR_imgH = hemiH; hLR_imgW = hemiH * aspectRatio;
		}
		double areaLR = hLR_imgW * hLR_imgH;

		// Schritt 2: Gewinner oben/unten
		double topH = mouseY - screen.getMinY() - 2 * tooltipMargin;
		double botH = screen.getMaxY() - mouseY - 2 * tooltipMargin;
		double hemiW = screen.getWidth() - 2 * tooltipMargin;

		boolean useBottom = botH >= topH;
		double hWinner_H = useBottom ? botH : topH;

		double hTB_imgW, hTB_imgH;
		if (naturalW <= hemiW && naturalH <= hWinner_H) {
			hTB_imgW = naturalW; hTB_imgH = naturalH;
		} else if (naturalW / hemiW >= naturalH / hWinner_H) {
			hTB_imgW = hemiW; hTB_imgH = hemiW / aspectRatio;
		} else {
			hTB_imgH = hWinner_H; hTB_imgW = hWinner_H * aspectRatio;
		}
		double areaTB = hTB_imgW * hTB_imgH;

		// Schritt 3: Finale Entscheidung
		double imgW, imgH, tooltipX, tooltipY;

		if (areaLR >= areaTB) {
			imgW = hLR_imgW; imgH = hLR_imgH;
			tooltipX = useRight ? mouseX + tooltipMargin : mouseX - tooltipMargin - imgW;
			tooltipY = mouseY - imgH / 2.0;
			tooltipY = Math.max(screen.getMinY() + tooltipMargin, tooltipY);
			tooltipY = Math.min(screen.getMaxY() - tooltipMargin - imgH, tooltipY);
		} else {
			imgW = hTB_imgW; imgH = hTB_imgH;
			tooltipY = useBottom ? mouseY + tooltipMargin : mouseY - tooltipMargin - imgH;
			tooltipX = mouseX - imgW / 2.0;
			tooltipX = Math.max(screen.getMinX() + tooltipMargin, tooltipX);
			tooltipX = Math.min(screen.getMaxX() - tooltipMargin - imgW, tooltipX);
		}

		// ImageView mit Originalbild, bei Bedarf via setFitWidth/setFitHeight skaliert
		ImageView imageView = new ImageView(original);
		if (naturalW > imgW || naturalH > imgH) {
			imageView.setFitWidth(imgW);
			imageView.setFitHeight(imgH);
			imageView.setPreserveRatio(true);
			imageView.setSmooth(true);
		}

		tooltip.setGraphic(imageView);
		tooltip.show(anchor, tooltipX, tooltipY);
	}
}
