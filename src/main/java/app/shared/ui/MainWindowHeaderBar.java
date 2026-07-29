package app.shared.ui;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.binding.ObjectBinding;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.MenuBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.HeaderBar;
import javafx.scene.layout.HeaderDragType;
import javafx.stage.Stage;

/**
 * Die Titelleiste des Hauptfensters: Anwendungssymbol, Menüleiste und Titel.
 *
 * <p>Das Fenster bekommt sie fertig und hängt sie oben ein. Welche Menüs es gibt, entscheidet es
 * selbst und reicht die fertige {@link MenuBar} herein — das ist Navigation, nicht Aussehen.</p>
 *
 * <p>Das Symbol wächst mit der Leiste: es hängt an deren Höhe und wechselt dabei auf die
 * Auflösung, die am besten passt. Deshalb braucht die Leiste die {@link Stage} — die hält die
 * Symbole in mehreren Größen.</p>
 *
 * <p>CSS: die Leiste selbst {@code .my-header-bar}, der linke Block
 * {@code .my-header-leading} (trägt den Abstand zum Fensterrand).</p>
 */
public class MainWindowHeaderBar extends HeaderBar {

	/** Anteil der Leistenhöhe, den das Symbol einnimmt. */
	private static final double ICON_HEIGHT_RATIO = 0.55;

	public MainWindowHeaderBar(Stage stage, MenuBar menuBar) {
		getStyleClass().add("my-header-bar");

		Label titleLabel = new Label("Thos Suite (FX)");
		HeaderBar.setDragType(titleLabel, HeaderDragType.DRAGGABLE_SUBTREE);
		setCenter(titleLabel);

		HBox leading = new HBox(0);
		leading.getStyleClass().add("my-header-leading");
		leading.setAlignment(Pos.CENTER_LEFT);

		ImageView icon = buildResponsiveIcon(stage);
		if (icon != null)
			leading.getChildren().add(icon);
		leading.getChildren().add(menuBar);

		setLeading(leading);
	}

	/**
	 * Ein Symbol, das an der Höhe dieser Leiste hängt und dabei jeweils die Auflösung zeigt, die
	 * gerade am besten passt. Ohne Bindings müsste man nach jedem Layout-Durchlauf nachfassen.
	 *
	 * @return {@code null}, wenn die Stage keine Symbole hat.
	 */
	private ImageView buildResponsiveIcon(Stage stage) {
		ObservableList<Image> icons = stage.getIcons();
		if (icons.isEmpty())
			return null;

		ImageView view = new ImageView();
		view.setPreserveRatio(true);
		view.setSmooth(true);

		DoubleBinding zielHoehe = heightProperty().multiply(ICON_HEIGHT_RATIO);
		view.fitHeightProperty().bind(zielHoehe);

		ObjectBinding<Image> bestesSymbol = Bindings.createObjectBinding(
				() -> passendstes(icons, zielHoehe.get()), zielHoehe);
		view.imageProperty().bind(bestesSymbol);

		return view;
	}

	/**
	 * Das kleinste Symbol, das mindestens so hoch ist wie gebraucht — damit nie hochskaliert und so
	 * wenig wie möglich herunterskaliert wird. Ist keines groß genug, das letzte der Liste; die
	 * Symbole liegen aufsteigend vor.
	 */
	private static Image passendstes(ObservableList<Image> icons, double gebrauchteHoehe) {
		if (gebrauchteHoehe <= 0) // erster Layout-Durchlauf
			return icons.get(0);

		Image bestes = null;
		for (Image kandidat : icons)
			if (kandidat.getHeight() >= gebrauchteHoehe && (bestes == null || kandidat.getHeight() < bestes.getHeight()))
				bestes = kandidat;

		return bestes != null ? bestes : icons.get(icons.size() - 1);
	}
}
