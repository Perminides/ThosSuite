package app.shared.ui;

import java.awt.image.BufferedImage;

import app.shared.model.SelectionEnum;
import app.shared.ui.components.SuiteDialog;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * Stellt zwei Bilder nebeneinander und lässt den Benutzer eines wählen.
 *
 * <p>Jedes Bild hat seinen eigenen OK-Knopf darunter — die Knopfleiste des Dialogs ist deshalb
 * flachgelegt und der Abbrechen-Knopf unsichtbar. Ein {@code ButtonType} mit
 * {@code CANCEL_CLOSE} muss trotzdem existieren, sonst lässt sich der Dialog gar nicht
 * schließen.</p>
 *
 * <p>Kennt nicht, wo die Bilder herkommen oder was mit der Wahl geschieht. Genutzt wird er heute
 * vom Lern-Bildimport, der zwei Skalierverfahren gegeneinander antreten lässt.</p>
 */
public class ImageComparisonDialog {

	/**
	 * @return {@link SelectionEnum#ZERO} für das linke Bild, {@link SelectionEnum#ONE} für das
	 *         rechte — oder {@code null}, wenn abgebrochen wurde.
	 */
	public static SelectionEnum show(BufferedImage left, BufferedImage right) {
		SuiteDialog<SelectionEnum> dialog = new SuiteDialog<>("Bild auswählen");

		ButtonType cancelType = new ButtonType("", ButtonBar.ButtonData.CANCEL_CLOSE);
		dialog.getDialogPane().getButtonTypes().add(cancelType);

		ButtonBar buttonBar = (ButtonBar) dialog.getDialogPane().lookup(".button-bar");
		if (buttonBar != null) {
			buttonBar.setMinHeight(0);
			buttonBar.setPrefHeight(0);
			buttonBar.setMaxHeight(0);
			var cancelBtn = dialog.getDialogPane().lookupButton(cancelType);
			if (cancelBtn != null) {
				cancelBtn.setVisible(false);
				cancelBtn.setManaged(false);
			}
		}

		DialogPane pane = dialog.getDialogPane();

		Button okLeft = new Button("OK");
		Button okRight = new Button("OK");
		okLeft.setOnAction(_ -> dialog.setResult(SelectionEnum.ZERO));
		okRight.setOnAction(_ -> dialog.setResult(SelectionEnum.ONE));

		ImageView viewLeft = new ImageView(SwingFXUtils.toFXImage(left, null));
		ImageView viewRight = new ImageView(SwingFXUtils.toFXImage(right, null));
		viewLeft.setPreserveRatio(true);
		viewRight.setPreserveRatio(true);

		VBox leftBox = new VBox(10, viewLeft, okLeft);
		VBox rightBox = new VBox(10, viewRight, okRight);
		leftBox.setAlignment(Pos.CENTER);
		rightBox.setAlignment(Pos.CENTER);

		HBox root = new HBox(20, leftBox, rightBox);
		root.setAlignment(Pos.CENTER);
		root.setPadding(new Insets(20));

		pane.setContent(root);
		return dialog.showAndWait().orElse(null);
	}
}
