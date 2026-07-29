package app.shared.ui.components;

import javafx.scene.control.Label;
import javafx.scene.layout.HeaderBar;
import javafx.scene.layout.HeaderDragType;

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

	public SuiteHeaderBar(String title) {
		getStyleClass().add("my-header-bar");

		titleLabel = new Label(title);
		titleLabel.getStyleClass().add("my-dialog-title");

		setCenter(titleLabel);
		HeaderBar.setDragType(titleLabel, HeaderDragType.DRAGGABLE_SUBTREE);
	}

	/**
	 * Wechselt den Titel im laufenden Betrieb (der Kontakt-Dialog tut das).
	 *
	 * <p>Vorher lief das über {@code getHeader().lookup(".my-dialog-title")} — ein CSS-Lookup in einen
	 * fremden Dialog hinein. Die Beschriftung liegt jetzt als Feld vor.</p>
	 */
	public void setTitleText(String title) {
		titleLabel.setText(title);
	}
}
