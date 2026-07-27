package app.shared.ui.components;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.HeaderBar;
import javafx.scene.layout.HeaderDragType;
import javafx.scene.text.Font;

/**
 * Die Titelleiste der Suite-Fenster — für Dialoge wie für Alerts.
 *
 * <p>Nötig, weil Dialoge mit {@code StageStyle.EXTENDED} laufen: das Fenster zeichnet seine
 * Dekoration selbst und muss deshalb eine eigene HeaderBar setzen.</p>
 *
 * <p>Passiver Baustein — bekommt seine Werte herein, holt sie nie selbst.</p>
 */
public class SuiteHeaderBar extends HeaderBar {

	private final Label titleLabel;

	/**
	 * TODO: Die {@code font} wird ausschließlich für das vertikale Padding des Titels gebraucht.
	 *   Das gehört ins generierte CSS ({@code addDialogStyles}, Selektor {@code .my-title}) — dann
	 *   entfällt der Parameter, und mit ihm die {@code font} im {@code DialogStyle}. Bewusst nicht
	 *   im Zuge des Umzugs gemacht, damit 3a ein reiner Move bleibt.
	 */
	public SuiteHeaderBar(String title, Font font) {
		getStyleClass().add("my-header-bar");

		titleLabel = new Label(title);
		titleLabel.getStyleClass().add("my-title");

		double verticalPadding = font.getSize() * 0.3;
		titleLabel.setPadding(new Insets(verticalPadding, 0, verticalPadding, 0));

		setCenter(titleLabel);
		HeaderBar.setDragType(titleLabel, HeaderDragType.DRAGGABLE_SUBTREE);
	}

	/**
	 * Wechselt den Titel im laufenden Betrieb (der Kontakt-Dialog tut das).
	 *
	 * <p>Vorher lief das über {@code getHeader().lookup(".my-title")} — ein CSS-Lookup in einen
	 * fremden Dialog hinein. Die Beschriftung liegt jetzt als Feld vor.</p>
	 */
	public void setTitleText(String title) {
		titleLabel.setText(title);
	}
}
