package app.shared.ui.components;

import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.List;

import app.shared.Config;
import app.shared.model.DiaryAttachment;
import app.shared.model.DiaryCardData;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

/**
 * Ein Tagebuch-Eintrag als Karte: Datum, Tags, Thumbnails der Anhänge, Text.
 *
 * <p>Passiver Baustein — bekommt Daten und Maße herein, holt sich nichts.</p>
 */
public class DiaryCard extends VBox {

	public DiaryCard(DiaryCardData data) {
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

	/** Nur noch das Anordnen — das Bild samt Vergrößerung kann {@link SuiteThumbnail}. */
	private FlowPane buildThumbnails(List<DiaryAttachment> attachments) {
		int thumbHeight = Config.getInt("diary.thumbnailHeight", 120);

		FlowPane thumbPane = new FlowPane(8, 8);
		thumbPane.getStyleClass().add("diary-card-thumbs");

		for (DiaryAttachment attachment : attachments)
			thumbPane.getChildren().add(new SuiteThumbnail(
					Path.of(attachment.thumbnailPath()),
					Path.of(attachment.imagePath()),
					thumbHeight));

		return thumbPane;
	}
}
