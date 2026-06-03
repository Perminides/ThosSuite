package app.learn;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import app.learn.model.GeoMap;
import app.learn.model.MapElementListener;
import app.learn.model.ShapeMap;
import app.shared.skin.SkinService;
import javafx.css.PseudoClass;
import javafx.scene.Node;
import javafx.scene.layout.Region;

/**
 * Eine interaktive Karte, die ausschließlich aus Shapes besteht. Einige sind interaktiv, andere nicht.
 *
 * <p>Diese Klasse hält <b>Inhalt + Verhalten</b>: sie sammelt die Shape-Nodes aus der GeoMap, verdrahtet
 * Klicks und verwaltet den visuellen Zustand der Shapes (correct/incorrect/marked/active/inactive) über
 * Pseudo-Klassen.</p>
 *
 * <p>Sie ist selbst <b>kein Node</b>. Den sichtbaren, fertig gemessenen, skalierten und positionierten
 * Wrapper baut der Skin ({@code buildShapeMapWrapper}) — Messen, Skalieren, Strichdicken-Fix und Platzieren
 * passieren alle dort. Die ShapeMapPane hält diesen Wrapper nur als opakes {@link Region} und reicht ihn via
 * {@link #getView()} nach außen. Skin-Werte (Höhe, Skalierung) erreichen diese Klasse nie.</p>
 *
 * <p>CSS (vom Skin gesetzt):
 * Wrapper = {@code .my-shape-map-pane}, {@code :paused};
 * Shape = {@code .my-map-shape}, {@code :correct/:incorrect/:marked/:active/:inactive}.</p>
 */
public class ShapeMapPane {

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
	private final Map<String, ShapeMap> shapeMap = new HashMap<>();

	// Der vom Skin gebaute, sichtbare Wrapper. Opak — diese Klasse kennt nur Region, nicht StackPane/Group.
	private final Region view;

	private MapElementListener listener;
	private boolean isInteractive = false;

	/**
	 * @param map      Die Karte mit den Shapes.
	 * @param mapName  Layout-Schlüssel des Skins (spezifisch). Wird unten zum Nachschlagen der Bounds genutzt.
	 * @param category Layout-Schlüssel des Skins (Fallback), falls für mapName keine Bounds definiert sind.
	 */
	public ShapeMapPane(GeoMap map, String mapName, String category) {
		List<Node> orderedShapeNodes = new ArrayList<>();

		map.getShapes().stream()
				.sorted(Comparator.comparingInt(s -> s.getZIndex()))
				.forEach(mapShape -> {
					Node node = mapShape.shape();
					String id = mapShape.id();
					shapeMap.put(id, mapShape);

					if (mapShape.isInteractive())
						node.setOnMouseClicked(_ -> {
							if (listener != null)
								listener.mouseClicked(id);
						});

					orderedShapeNodes.add(node);
				});

		resetAllStates();

		// Skin baut den sichtbaren Wrapper: misst, skaliert, fixt die Strichdicken, setzt CSS-Klasse, positioniert.
		this.view = SkinService.get().buildShapeMapWrapper(orderedShapeNodes, mapName, category);
	}

	/** Der sichtbare Node. Wird von der SessionPane in den Szenengraphen gehängt. */
	public Region getView() {
		return view;
	}

	public void setListener(MapElementListener listener) {
		this.listener = Objects.requireNonNull(listener, "Wolltest Du ernsthaft gerade null als Listener setzen?");
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
		shapeMap.values().forEach(shape -> {
			if (shape.shape().getPseudoClassStates().contains(CORRECT)) {
				shape.shape().pseudoClassStateChanged(CORRECT, false);
				shape.shape().pseudoClassStateChanged(ACTIVE, true);
			}
		});
	}

	/** Resettet alle interaktiven Shapes und setzt sie auf ACTIVE. */
	public void moveAllToActive() {
		shapeMap.values().forEach(mapShape -> {
			resetShapeState(mapShape);
			if (mapShape.isInteractive())
				mapShape.shape().pseudoClassStateChanged(ACTIVE, true);
		});
	}

	// --- State Helpers ---

	private void updateShapeState(String id, PseudoClass state) {
		ShapeMap mapShape = shapeMap.get(id);
		if (mapShape != null) {
			// Exklusiv-Logik: ein Shape hat idealerweise nur einen dominanten State. Sicherheitshalber die anderen resetten.
			resetShapeState(mapShape);
			mapShape.shape().pseudoClassStateChanged(state, true);
		}
	}

	protected void resetAllStates() {
		shapeMap.values().forEach(this::resetShapeState);
	}

	private void resetShapeState(ShapeMap shape) {
		shape.shape().pseudoClassStateChanged(CORRECT, false);
		shape.shape().pseudoClassStateChanged(INCORRECT, false);
		shape.shape().pseudoClassStateChanged(MARKED, false);
		shape.shape().pseudoClassStateChanged(ACTIVE, false);
		shape.shape().pseudoClassStateChanged(INACTIVE, false);
	}

	// Für Resume/Restore State (ShapeMapState Record)
	public ShapeMapState getState() {
		Set<String> correct = new HashSet<>();
		Set<String> incorrect = new HashSet<>();
		Set<String> marked = new HashSet<>();
		Set<String> active = new HashSet<>();
		Set<String> inactive = new HashSet<>();

		shapeMap.forEach((id, mapShape) -> {
			Set<PseudoClass> states = mapShape.shape().getPseudoClassStates();
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