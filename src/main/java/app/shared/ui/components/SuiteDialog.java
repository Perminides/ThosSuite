package app.shared.ui.components;

import app.shared.UiUtils;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.layout.HeaderBar;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

/**
 * Ein Dialog im Suite-Look: Owner gesetzt, {@code StageStyle.EXTENDED}, eigene Titelleiste.
 *
 * <p>Unterklasse statt Fabrik — JavaFX macht es selbst so ({@code Alert extends Dialog}). Der
 * Typparameter erspart den unchecked Cast an den Aufrufstellen: {@code new SuiteDialog<Foo>(titel)}
 * liefert direkt einen {@code Dialog<Foo>}.</p>
 *
 * <p>Komponierer, kein Blatt: liest die skin-abhängigen Werte selbst und reicht sie in die
 * {@link SuiteHeaderBar} hinein. Das ist abwärts ({@code shared.ui} → {@code shared.skin}) und
 * damit regelkonform.</p>
 *
 * @param <R> der Ergebnistyp des Dialogs
 */
public class SuiteDialog<R> extends Dialog<R> {

	private final SuiteHeaderBar headerBar;

	public SuiteDialog(String title) {
		Window owner = UiUtils.getOwnerWindow();
		if (owner != null)
			initOwner(owner); // Pflicht — ohne Owner bleibt der Dialog ungestylt, siehe UiUtils.setOwnerWindow

		initStyle(StageStyle.EXTENDED);

		headerBar = new SuiteHeaderBar(title);
		DialogPane pane = getDialogPane();
		pane.setHeader(headerBar);

		// Minimize- und Close-Button sollen die volle Höhe der Titelleiste nutzen.
		// Einigermaßen heikel, weil aus der Dialog-Doku: "this essentially means that the
		// DialogPane is shown to users inside a Stage, but future releases may offer alternative
		// options (such as 'lightweight' or 'internal' dialogs)."
		headerBar.heightProperty().addListener((_, _, newVal) -> {
			if (getDialogPane().getScene().getWindow() instanceof Stage stage)
				HeaderBar.setPrefButtonHeight(stage, newVal.doubleValue());
		});
	}

	/**
	 * Eine leere, gestylte Inhalts-Box. Der Aufrufer füllt sie und hängt sie selbst ein
	 * ({@code getDialogPane().setContent(box)}) — so wie bisher auch.
	 */
	public VBox contentBox() {
		VBox vbox = new VBox(15); // nur Spacing, alles andere über die Style-Klasse
		vbox.getStyleClass().add("my-dialog-vbox");
		return vbox;
	}

	/** Wechselt den Titel im laufenden Betrieb. */
	public void setSuiteTitle(String title) {
		headerBar.setTitleText(title);
	}
}
