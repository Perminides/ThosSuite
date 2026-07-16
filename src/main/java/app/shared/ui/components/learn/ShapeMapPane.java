package app.shared.ui.components.learn;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

import app.shared.skin.SkinService;
import app.shared.ui.components.UiComponent;
import app.shared.ui.components.learn.model.ShapeGeometry;
import javafx.css.PseudoClass;
import javafx.scene.Node;
import javafx.scene.layout.Region;

/**
 * Eine interaktive Karte, die ausschließlich aus Shapes besteht. Einige sind interaktiv, andere nicht.
 *
 * <p>Framework-gebundene UI-Komponente (lebt bewusst in shared.ui.components). Sie kennt <b>keinen Lern-Typ</b>:
 * das Feature reicht die Shapes als {@link ShapeGeometry}-Liste rein (jede trägt id und type), Klicks meldet sie
 * über einen {@link Consumer} als reine id zurück. Aus jeder Geometrie baut sie selbst den Node
 * ({@link MapNodeBuilder}); zIndex und "interaktiv?" leitet sie über {@link ShapeLayer} aus dem type ab —
 * dieselbe Ableitung, die der Builder für Layer-Klasse/mouseTransparent nutzt. Symmetrisch zur Bild-Karte, die
 * ebenfalls nur Geometrien bekommt; das frühere {@code ShapeInput}-Transportobjekt (mit fertigem Node) ist weg.</p>
 *
 * <p>Sie ist selbst <b>kein Node</b>. Den sichtbaren, gemessenen, skalierten Wrapper baut der Skin
 * ({@code buildShapeMapWrapper}). Diese Klasse hält ihn nur als opakes {@link Region} und reicht ihn via
 * {@link #getView()} nach außen. Skin-Werte (Höhe, Skalierung) erreichen diese Klasse nie.</p>
 *
 * <p>CSS (vom Skin gesetzt):
 * Wrapper = {@code .my-shape-map-pane}, {@code :paused};
 * Shape = {@code .my-map-shape}, {@code :correct/:incorrect/:marked/:active/:inactive}.</p>
 */
public class ShapeMapPane implements UiComponent{

	/** Was die Pane pro Shape festhält: den gebauten Node und ob er interaktiv ist (für Klick + moveAllToActive). */
	private record ShapeNode(Node node, boolean interactive) {}

	public static record ShapeMapState(
			Set<String> correctShapes,
			Set<String> incorrectShapes,
			Set<String> markedShapes,
			Set<String> activeShapes,
			Set<String> inactiveShapes,
			boolean interactive) {}

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

	// Der vom Skin gebaute, sichtbare Wrapper. Opak — diese Klasse kennt nur Region.
	// !Sofort: Schau halt in Skin beim shapemapwrapper. Das Konstrukt ist so kompliziert und ich weiß auch nicht
	// mehr genau warum wir das so gebaut hatten. Was ganz schlecht ist.
	private final Region view;

	private Consumer<String> clickListener;
	private boolean isInteractive = false;

	/**
	 * @param geometries Die Shapes der Karte (jede mit id und type). zIndex/interaktiv/Layer-Klasse leiten sich
	 *                   aus dem type ab; die Pane baut die Nodes selbst.
	 * @param mapName    Layout-Schlüssel des Skins (spezifisch).
	 * @param category   Layout-Schlüssel des Skins (Fallback).
	 */
	public ShapeMapPane(List<ShapeGeometry> geometries, String mapName, String category) {
		List<Node> orderedShapeNodes = new ArrayList<>();

		geometries.stream()
				.sorted(Comparator.comparingInt(g -> ShapeLayer.fromJsonId(g.type()).zIndex()))
				.forEach(geometry -> {
					boolean interactive = ShapeLayer.fromJsonId(geometry.type()).interactive();
					Node node = MapNodeBuilder.buildShapeMapNode(geometry);
					shapes.put(geometry.id(), new ShapeNode(node, interactive));

					if (interactive)
						node.setOnMouseClicked(_ -> {
							if (clickListener != null)
								clickListener.accept(geometry.id());
						});

					orderedShapeNodes.add(node);
				});

		resetAllStates();

		// Skin baut den sichtbaren Wrapper: misst, skaliert, fixt Strichdicken, setzt CSS-Klasse, positioniert.
		this.view = SkinService.get().buildShapeMapWrapper(orderedShapeNodes, mapName, category);
	}

	/** Der sichtbare Node. Wird von der SessionPane in den Szenengraphen gehängt. */
	public Region getView() {
		return view;
	}

	public void setClickListener(Consumer<String> listener) {
		this.clickListener = Objects.requireNonNull(listener, "Wolltest Du ernsthaft gerade null als Listener setzen?");
	}

	// --- Zustand via Pseudo-Klasse ---

	public void setInteractive(boolean interactive) {
		this.isInteractive = interactive;
		view.pseudoClassStateChanged(PAUSED, !interactive);
	}

	public void addToCorrect(Set<String> ids) {
		for (String id : ids)
			updateShapeState(id, CORRECT);
	}

	public void addToIncorrect(String id) {
		updateShapeState(id, INCORRECT);
	}

	public void addToMarked(Set<String> ids) {
		for (String id : ids)
			updateShapeState(id, MARKED);
	}

	public void makeActive(Set<String> ids) {
		for (String id : ids)
			updateShapeState(id, ACTIVE);
	}

	public void makeInactive(Set<String> ids) {
		for (String id : ids)
			updateShapeState(id, INACTIVE);
	}

	public void moveCorrectToActive() {
		shapes.values().forEach(shape -> {
			Node node = shape.node();
			if (node.getPseudoClassStates().contains(CORRECT)) {
				node.pseudoClassStateChanged(CORRECT, false);
				node.pseudoClassStateChanged(ACTIVE, true);
			}
		});
	}

	/** Resettet alle interaktiven Shapes und setzt sie auf ACTIVE. */
	public void moveAllToActive() {
		shapes.values().forEach(shape -> {
			resetShapeState(shape.node());
			if (shape.interactive())
				shape.node().pseudoClassStateChanged(ACTIVE, true);
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
		addToCorrect(state.correctShapes());
		state.incorrectShapes().forEach(this::addToIncorrect);
		addToMarked(state.markedShapes());
		makeInactive(state.inactiveShapes());
		makeActive(state.activeShapes());
		setInteractive(state.interactive());
	}
}