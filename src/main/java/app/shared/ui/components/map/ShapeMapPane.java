package app.shared.ui.components.map;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

import app.shared.model.ShapeGeometry;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.css.PseudoClass;
import javafx.geometry.Rectangle2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Shape;
import javafx.scene.transform.Scale;
import app.shared.model.ShapeMapState;

/**
 * Eine interaktive Karte, die ausschließlich aus Shapes besteht. Einige sind interaktiv, andere nicht.
 *
 * <p>Sie kennt <b>keinen Lern-Typ</b>: die Shapes kommen als {@link ShapeGeometry}-Liste herein (jede trägt id
 * und type), Klicks meldet sie über einen {@link Consumer} als reine id zurück. Aus jeder Geometrie baut sie den
 * Node ({@link MapNodeBuilder}); zIndex und „interaktiv?" leitet sie über {@link ShapeLayer} aus dem type ab —
 * dieselbe Ableitung, die der Builder für Layer-Klasse und mouseTransparent nutzt.</p>
 *
 * <p>Sie <b>ist</b> der sichtbare Node — eine StackPane, die ihren Inhalt zentriert. Ihr Feld bekommt sie
 * übergeben: welche Karte gerade gebaut wird, weiß der Aufrufer, nicht sie.</p>
 *
 * <p>Warum skaliert wird: die Shapes tragen ihre Originalkoordinaten aus dem GeoJSON, das Feld im Skin ist
 * dagegen frei konfigurierbar. Gemessen wird <b>vor</b> dem Einhängen in eine Scene, damit die UA-Defaults
 * gelten und nicht schon das Skin-CSS. Die Skalierung trifft auch die Strichdicken — die werden deshalb
 * repariert, sobald die Scene (und damit das echte CSS) da ist. Die Bild-Karte braucht davon nichts: Bitmap,
 * keine Vektor-Striche.</p>
 *
 * <p>CSS (vom Skin gesetzt):
 * Karte = {@code .my-shape-map-pane}, {@code :paused};
 * Shape = {@code .my-map-shape}, {@code :correct/:incorrect/:marked/:active/:inactive}.</p>
 */
public class ShapeMapPane extends StackPane implements LearnMap {

	/** Was die Pane pro Shape festhält: den gebauten Node und ob er interaktiv ist (für Klick + moveAllToActive). */
	private record ShapeNode(Node node, boolean interactive) {}


	private static final PseudoClass PAUSED = PseudoClass.getPseudoClass("paused");

	// Visuelle Zustands-Vokabel der Shapes.
	// Achtung!!! Alle müssen in resetShapeState() zurückgesetzt werden. Wenn du hier eine hinzufügst, dann denk daran!
	private static final PseudoClass CORRECT = PseudoClass.getPseudoClass("correct");
	private static final PseudoClass INCORRECT = PseudoClass.getPseudoClass("incorrect");
	private static final PseudoClass MARKED = PseudoClass.getPseudoClass("marked");
	private static final PseudoClass ACTIVE = PseudoClass.getPseudoClass("active");
	private static final PseudoClass INACTIVE = PseudoClass.getPseudoClass("inactive");

	// Alle Shapes im Zugriff für Zustands-Updates per id.
	private final Map<String, ShapeNode> shapes = new HashMap<>();

	private Consumer<String> clickListener;
	private boolean isInteractive = false;

	/**
	 * @param geometries Die Shapes der Karte (jede mit id und type). zIndex/interaktiv/Layer-Klasse leiten sich
	 *                   aus dem type ab; die Pane baut die Nodes selbst.
	 * @param bounds     Das Feld, in das die Karte skaliert und gesetzt wird. Löst der Aufrufer auf — er weiß,
	 *                   um welche Karte es geht, die Pane nicht.
	 */
	public ShapeMapPane(List<ShapeGeometry> geometries, Rectangle2D bounds) {
		Group contentGroup = new Group();

		// Nach zIndex sortiert einhängen — die Reihenfolge in der Group ist die Zeichenreihenfolge.
		List<ShapeGeometry> nachEbene = new ArrayList<>(geometries);
		nachEbene.sort(Comparator.comparingInt(g -> ShapeLayer.fromJsonId(g.type()).zIndex()));

		for (ShapeGeometry geometry : nachEbene) {
			boolean interactive = ShapeLayer.fromJsonId(geometry.type()).interactive();
			Node node = MapNodeBuilder.buildShapeMapNode(geometry);
			shapes.put(geometry.id(), new ShapeNode(node, interactive));

			if (interactive)
				node.setOnMouseClicked(_ -> {
					if (clickListener != null)
						clickListener.accept(geometry.id());
				});

			contentGroup.getChildren().add(node);
		}

		resetAllStates();

		// Messen, solange noch keine Scene da ist — dann gelten die UA-Defaults und nicht schon das Skin-CSS.
		contentGroup.applyCss();
		contentGroup.layout();
		double scaleFactor = bounds.getHeight() / contentGroup.getBoundsInLocal().getHeight();

		Scale scale = new Scale(scaleFactor, scaleFactor);
		scale.setPivotX(0);
		scale.setPivotY(0);
		contentGroup.getTransforms().add(scale);

		getChildren().add(contentGroup); // StackPane zentriert.
		getStyleClass().add("my-shape-map-pane");
		setLayoutX(bounds.getMinX());
		setLayoutY(bounds.getMinY());

		fixStrokeWidthsOnceStyled(contentGroup, scaleFactor);
	}

	/**
	 * Die Skalierung zieht die Strichdicken mit. Sobald die Scene da ist, steht die vom CSS gewollte Dicke fest
	 * und wird per Inline-Style durch den Faktor geteilt — danach hängt sich der Listener selbst wieder aus.
	 */
	private void fixStrokeWidthsOnceStyled(Group contentGroup, double scaleFactor) {
		sceneProperty().addListener(new ChangeListener<Scene>() {
			@Override
			public void changed(ObservableValue<? extends Scene> obs, Scene oldScene, Scene newScene) {
				if (newScene == null)
					return;

				contentGroup.applyCss();
				for (Node node : contentGroup.getChildren())
					if (node instanceof Shape s && s.getStrokeWidth() > 0)
						s.setStyle("-fx-stroke-width: "
								+ String.format(Locale.US, "%.4f", s.getStrokeWidth() / scaleFactor) + "px;");

				sceneProperty().removeListener(this);
			}
		});
	}

	/** Die Karte ist selbst der Node — siehe {@link LearnMap#getView()}. */
	@Override
	public Node getView() {
		return this;
	}

	public void setClickListener(Consumer<String> listener) {
		this.clickListener = Objects.requireNonNull(listener, "Wolltest Du ernsthaft gerade null als Listener setzen?");
	}

	// --- Das gemeinsame Vokabular (LearnMap) ---

	/** Alle interaktiven Shapes zurück auf Anfang und wieder aktiv. */
	@Override
	public void reset() {
		shapes.values().forEach(shape -> {
			resetShapeState(shape.node());
			if (shape.interactive())
				shape.node().pseudoClassStateChanged(ACTIVE, true);
		});
	}

	@Override
	public void setActive(boolean active) {
		this.isInteractive = active;
		pseudoClassStateChanged(PAUSED, !active);
	}

	@Override
	public void markCorrect(Set<String> ids) {
		for (String id : ids)
			updateShapeState(id, CORRECT);
	}

	@Override
	public void markIncorrect(String id) {
		updateShapeState(id, INCORRECT);
	}

	@Override
	public void mark(Set<String> ids) {
		for (String id : ids)
			updateShapeState(id, MARKED);
	}

	/**
	 * Leer: hier liegen alle Formen von Anfang an im Szenengraphen, und ob eine klickbar ist, hängt
	 * an ihrem Typ. Es gibt also nichts zu platzieren und nichts umzuschalten.
	 */
	@Override
	public void setClickTargets(Set<String> ids) {
	}

	// --- Darüber hinaus: was nur die Region-Session braucht ---

	public void markActive(Set<String> ids) {
		for (String id : ids)
			updateShapeState(id, ACTIVE);
	}

	public void markInactive(Set<String> ids) {
		for (String id : ids)
			updateShapeState(id, INACTIVE);
	}

	/**
	 * Alles Abgehakte verschwindet wieder: was richtig geklickt wurde ({@code CORRECT}) <b>und</b> was
	 * verpasst wurde ({@code INCORRECT}), fällt zurück auf {@code ACTIVE}. Für den „schwer"-Modus, in dem
	 * nichts stehenbleiben soll — sonst verrät eine rot markierte Region den Rest der Session, wo sie liegt.
	 */
	public void moveResolvedToActive() {
		shapes.values().forEach(shape -> {
			Node node = shape.node();
			Set<PseudoClass> states = node.getPseudoClassStates();
			if (states.contains(CORRECT) || states.contains(INCORRECT)) {
				node.pseudoClassStateChanged(CORRECT, false);
				node.pseudoClassStateChanged(INCORRECT, false);
				node.pseudoClassStateChanged(ACTIVE, true);
			}
		});
	}

	// --- State Helpers ---

	private void updateShapeState(String id, PseudoClass state) {
		ShapeNode shape = shapes.get(id);
		if (shape != null) {
			// Exklusiv-Logik: ein Shape hat idealerweise nur einen dominanten State. Sicherheitshalber die anderen resetten.
			resetShapeState(shape.node());
			shape.node().pseudoClassStateChanged(state, true);
		}
	}

	protected void resetAllStates() {
		shapes.values().forEach(shape -> resetShapeState(shape.node()));
	}

	private void resetShapeState(Node node) {
		node.pseudoClassStateChanged(CORRECT, false);
		node.pseudoClassStateChanged(INCORRECT, false);
		node.pseudoClassStateChanged(MARKED, false);
		node.pseudoClassStateChanged(ACTIVE, false);
		node.pseudoClassStateChanged(INACTIVE, false);
	}

	// Für Resume/Restore State (ShapeMapState Record)
	public ShapeMapState getState() {
		Set<String> correct = new HashSet<>();
		Set<String> incorrect = new HashSet<>();
		Set<String> marked = new HashSet<>();
		Set<String> active = new HashSet<>();
		Set<String> inactive = new HashSet<>();

		shapes.forEach((id, shape) -> {
			Set<PseudoClass> states = shape.node().getPseudoClassStates();
			if (states.contains(CORRECT)) correct.add(id);
			if (states.contains(INCORRECT)) incorrect.add(id);
			if (states.contains(MARKED)) marked.add(id);
			if (states.contains(ACTIVE)) active.add(id);
			if (states.contains(INACTIVE)) inactive.add(id);
		});

		return new ShapeMapState(correct, incorrect, marked, active, inactive, isInteractive);
	}

	public void setState(ShapeMapState state) {
		resetAllStates();
		markCorrect(state.correctShapes());
		state.incorrectShapes().forEach(this::markIncorrect);
		mark(state.markedShapes());
		markInactive(state.inactiveShapes());
		markActive(state.activeShapes());
		setActive(state.interactive());
	}
}