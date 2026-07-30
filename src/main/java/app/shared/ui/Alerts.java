package app.shared.ui;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import app.shared.UiUtils;
import app.shared.model.AlertOptions;
import app.shared.model.ButtonEnum;
import app.shared.model.DialogStyle;
import app.shared.model.DismissEnum;
import app.shared.skin.SkinService;
import app.shared.ui.components.SuiteHeaderBar;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HeaderBar;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.WindowEvent;

/**
 * Zeigt einen Alert und liefert die Antwort des Benutzers.
 *
 * <p>Der Einstiegspunkt für die ganze Suite: <b>Features rufen ausschließlich hier</b> an, nicht
 * beim Skin. Deshalb liegt die Klasse in der Wurzel von {@code shared.ui} und nicht bei den
 * Bausteinen — sie ist Teil der Außenfläche.</p>
 *
 * <p>Keine Fabrik: sie baut nichts zusammen, das der Aufrufer weiterverwendet. Sie führt eine
 * modale Interaktion und gibt deren Ergebnis zurück.</p>
 *
 * <p>Die einzige Stelle der Suite, die {@code javafx.scene.control.ButtonType} kennt — nach außen
 * gibt es nur {@link ButtonEnum}. Dismiss und X bedeuten immer {@link ButtonEnum#CANCEL}; der
 * Aufrufer interpretiert das (etwa als „später").</p>
 */
public class Alerts {

	private Alerts() {
	}

	public static ButtonEnum show(String title, String message, ButtonEnum... buttons) {
		return show(title, message, new AlertOptions(), buttons);
	}

	/**
	 * 
	 * Wenn ein ImagePath mitgegeben wird, so werden die nicht transparenten Teile mit der Textfarbe eingefärbt.
	 * Das ist sehr speziell für den Matratze wenden Dialog gebaut und gehört verbessert, wenn auch andere Bilder
	 * angezeigt werden sollen.
	 * 
	 * @param title
	 * @param message
	 * @param options
	 * @param buttons
	 * @return
	 */
	public static ButtonEnum show(String title, String message, AlertOptions options, ButtonEnum... buttons) {
		DialogStyle style = SkinService.get().dialogStyle();
		Alert alert = new Alert(Alert.AlertType.NONE);

		if (options.dismiss() == DismissEnum.NO_ESC || options.dismiss() == DismissEnum.MANDATORY)
			UiUtils.inactivateEscPress(alert);
		if (options.dismiss() == DismissEnum.MANDATORY)
			installCloseBlocker(alert);

		alert.initOwner(UiUtils.getOwnerWindow()); // Pflicht — sonst bleibt der Alert ungestylt
		alert.initStyle(StageStyle.EXTENDED);

		SuiteHeaderBar headerBar = new SuiteHeaderBar(title);
		alert.getDialogPane().setHeader(headerBar);

		alert.getDialogPane().setContent(buildContent(message, options.image(), options.isCentered(), style));
		alert.setGraphic(null);

		Map<ButtonType, ButtonEnum> reverse = new LinkedHashMap<>(); // JavaFX-Button → unser ButtonEnum
		for (ButtonEnum b : buttons)
			reverse.put(toButtonType(b), b);
		alert.getButtonTypes().setAll(reverse.keySet());

		headerBar.heightProperty().addListener((_, _, newVal) -> {
			if (alert.getDialogPane().getScene().getWindow() instanceof Stage stage)
				HeaderBar.setPrefButtonHeight(stage, newVal.doubleValue());
		});

		Optional<ButtonType> result = alert.showAndWait();
		return result.map(reverse::get).orElse(ButtonEnum.CANCEL);
	}

	/**
	 * Blockiert das Schließen per Fenster-X. Beim Alert entstehen Scene und Window erst spät
	 * (lazy), darum über Property-Listener einhängen statt sofort.
	 */
	private static void installCloseBlocker(Alert alert) {
		DialogPane pane = alert.getDialogPane();
		if (pane.getScene() != null && pane.getScene().getWindow() != null) {
			pane.getScene().getWindow().addEventFilter(WindowEvent.WINDOW_CLOSE_REQUEST, WindowEvent::consume);
			return;
		}
		pane.sceneProperty().addListener((_, _, scene) -> {
			if (scene == null)
				return;
			if (scene.getWindow() != null)
				scene.getWindow().addEventFilter(WindowEvent.WINDOW_CLOSE_REQUEST, WindowEvent::consume);
			else
				scene.windowProperty().addListener((_, _, win) -> {
					if (win != null)
						win.addEventFilter(WindowEvent.WINDOW_CLOSE_REQUEST, WindowEvent::consume);
				});
		});
	}

	private static Node buildContent(String message, Path image, boolean centered, DialogStyle style) {
		Node textNode = null;
		if (message != null && !message.isBlank()) {
			Label label = new Label(message);
			label.setWrapText(true);
			label.setMaxWidth(Double.MAX_VALUE);
			if (centered) {
				label.setAlignment(Pos.CENTER);
				label.setTextAlignment(TextAlignment.CENTER);
			}

			ScrollPane scroll = new ScrollPane(label) {
				@Override
				protected double computePrefHeight(double width) {
					return Math.min(super.computePrefHeight(width), 1000);
				}
			};
			scroll.setFitToWidth(true);
			scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
			scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
			scroll.getStyleClass().add("my-dialog-scrollpane");
			textNode = scroll;
		}

		ImageView imageNode = null;
		if (image != null) {
			Image img = new Image(image.toUri().toString());
			imageNode = new ImageView(UiUtils.tintImage(img, style.textColor()));
			imageNode.setPreserveRatio(true);
			imageNode.setFitWidth(200);
		}

		if (imageNode != null && textNode != null) {
			VBox box = new VBox(15, imageNode, textNode);
			box.setAlignment(Pos.CENTER);
			return box;
		}
		if (imageNode != null) {
			VBox box = new VBox(imageNode);
			box.setAlignment(Pos.CENTER);
			return box;
		}
		if (textNode != null)
			return textNode;

		return new Label("");
	}

	private static ButtonType toButtonType(ButtonEnum b) {
		ButtonBar.ButtonData data = switch (b) {
			case OK, YES, SAVE, IMPORT, END_ANYHOW, WHITELIST -> ButtonBar.ButtonData.YES;
			case CANCEL, DISCARD, LATER -> ButtonBar.ButtonData.CANCEL_CLOSE; // wichtig: macht ESC zum Abbrechen
			case NO, BLACKLIST -> ButtonBar.ButtonData.NO;
			case GREEN, RED, YELLOW, RESUME, EPISODE, WHOLE_SEASON, DONE, OTHER_DIRECTION,
					MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY -> ButtonBar.ButtonData.OTHER;
		};
		return new ButtonType(b.getText(), data);
	}
}
