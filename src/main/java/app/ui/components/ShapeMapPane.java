package app.ui.components;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import app.data.GeoMap;
import app.data.MapShape;
import app.ui.MapElementListener;
import javafx.css.PseudoClass;
import javafx.geometry.Bounds;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import javafx.scene.transform.Scale;

/**
 * An interactive map that consists entirely of shapes. Some are interactive, some are not.
 * 
 * CSS:
 * 		ShapeMapPane	= ".shape-map-pane"
 * 		ShapeMapPane	= ":paused"
 * 		Shape			= ".map-shape"
 * 		Shape			= ":corrrect", ":incorrect", ":marked", ":active-game"
 */
public class ShapeMapPane extends StackPane { // StackPane zentriert den Inhalt automatisch!
	
	public static record ShapeMapState(
		    Set<String> correctShapes,
		    Set<String> incorrectShapes,
		    Set<String> markedShapes,
		    Set<String> activeShapes,
		    boolean interactive
		) {}

	private static final PseudoClass PAUSED = PseudoClass.getPseudoClass("paused");
	
    // CSS Pseudo-Klassen für den State-Transfer zum Skin
	// Achtung!!! Die müssen alle gleich auf false gesetzt werden. Wenn du hier eine hinzufügst, dann denk daran!
    private static final PseudoClass CORRECT = PseudoClass.getPseudoClass("correct");
    private static final PseudoClass INCORRECT = PseudoClass.getPseudoClass("incorrect");
    private static final PseudoClass MARKED = PseudoClass.getPseudoClass("marked");
    private static final PseudoClass ACTIVE_GAME = PseudoClass.getPseudoClass("active-game");
    
    // Hält alle Shapes im Zugriff für Updates per ID
    private final Map<String, MapShape> shapeMap = new HashMap<>();
    
    // Container für alle Shapes (wird skaliert)
    private final Group contentGroup;
    
    private MapElementListener listener;
    private boolean isInteractive = false; // Benötigt für den ShapeMapState

    public ShapeMapPane(GeoMap map, int targetHeight) {
    	this.contentGroup = new Group();
    	getStyleClass().add("shape-map-pane");
        
        map.getShapes().stream()
           // 1. Sortieren nach Z-Index (niedrig zuerst = unten)
           .sorted(Comparator.comparingInt(s -> s.getZIndex()))
           .forEach(mapShape -> {
               Node node = mapShape.shape();
               String id = mapShape.id();
               
               // 1. Lookup füllen
               shapeMap.put(id, mapShape);
               
               // 2. Listener auf interaktive Shapes
               if (mapShape.isInteractive())
            	   node.setOnMouseClicked(_ -> {
            		   if (listener != null) {
            			   listener.mouseClicked(id);
            		   }
            	   });
               
               // 3. In Z-Order-Reihenfolge die shapes hinzufügen
               contentGroup.getChildren().add(node);
          });
        
        // Inhalt zusammenbauen
        this.getChildren().add(contentGroup);
    
        // Initialer Layout-Pass für Bounds
        contentGroup.applyCss(); 
        contentGroup.layout();
        Bounds bounds = contentGroup.getBoundsInLocal();
        
        double scaleFactor = targetHeight / bounds.getHeight();
        
        Scale scale = new Scale(scaleFactor, scaleFactor);
        // Pivot auf 0,0 (Links oben der Group) - StackPane zentriert das Ergebnis dann
        scale.setPivotX(0);
        scale.setPivotY(0);
        
        contentGroup.getTransforms().add(scale);
    }

    public void setListener(MapElementListener listener) {
    	// Ich wollte das mal ausprobieren. Aber sind wir ehrlich: this.listener = listener ist ja mal 3x einfacher zu verstehen...!
        this.listener = Objects.requireNonNull(listener, "Wolltest Du ernsthaft gerade null als Listener setzen?");
    }
    
    // --- State Management via Pseudo-Klasse ---
    public void setInteractive(boolean interactive) {
        this.isInteractive = interactive;
        this.pseudoClassStateChanged(PAUSED, !interactive);
    }

    public void addToCorrect(Set<String> ids) {
        for (String id : ids) {
            updateShapeState(id, CORRECT);
        }
    }

    public void addToIncorrect(String id) {
        updateShapeState(id, INCORRECT);
    }

    public void addToMarked(Set<String> ids) {
        for (String id : ids) {
            updateShapeState(id, MARKED);
        }
    }
    
    public void makeActive(Set<String> ids) {
        for (String id : ids) {
            updateShapeState(id, ACTIVE_GAME);
        }
    }
    
    public void moveCorrectToActive() {
       shapeMap.values().forEach(shape -> {
           if (shape.shape().getPseudoClassStates().contains(CORRECT)) {
               shape.shape().pseudoClassStateChanged(CORRECT, false);
               shape.shape().pseudoClassStateChanged(ACTIVE_GAME, true);
           }
       });
    }
    
    public void moveAllToActive() {
        shapeMap.values().forEach(mapShape -> {
            resetShapeState(mapShape);
            // !Sofort: Das ist ein undurchdachter Hack. Der funktioniert, aber sauber durchgedacht ist das noch nicht...
            if (mapShape.isInteractive()) {
                mapShape.shape().pseudoClassStateChanged(ACTIVE_GAME, true);
            }
        });
    }
    
    // --- State Helpers ---
    
    private void updateShapeState(String id, PseudoClass state) {
        MapShape mapShape = shapeMap.get(id);
        if (mapShape != null) {
            // Exklusiv-Logik: Ein Shape hat idealerweise nur einen dominanten State
            // oder CSS regelt die Priorität. Hier resetten wir sicherheitshalber die anderen.
            resetShapeState(mapShape);
            mapShape.shape().pseudoClassStateChanged(state, true);
        }
    }
    
    public void resetAllStates() {
        shapeMap.values().forEach(this::resetShapeState);
    }
    
    private void resetShapeState(MapShape shape) {
        shape.shape().pseudoClassStateChanged(CORRECT, false);
        shape.shape().pseudoClassStateChanged(INCORRECT, false);
        shape.shape().pseudoClassStateChanged(MARKED, false);
        shape.shape().pseudoClassStateChanged(ACTIVE_GAME, false);
    }
    
    // Für Resume/Restore State (ShapeMapState Record)
    public ShapeMapState getState() {
        // Wir müssen aus den Pseudo-Classes den State rekonstruieren
        Set<String> correct = new HashSet<>();
        Set<String> incorrect = new HashSet<>();
        Set<String> marked = new HashSet<>();
        Set<String> active = new HashSet<>();
        
        shapeMap.forEach((id, mapShape) -> {
            Set<PseudoClass> states = mapShape.shape().getPseudoClassStates();
            if (states.contains(CORRECT)) correct.add(id);
            if (states.contains(INCORRECT)) incorrect.add(id);
            if (states.contains(MARKED)) marked.add(id);
            if (states.contains(ACTIVE_GAME)) active.add(id);
        });
        
        return new ShapeMapState(correct, incorrect, marked, active, isInteractive);
    }
    
    public void setState(ShapeMapState state) {
        resetAllStates();
        addToCorrect(state.correctShapes());
        // Einzel-Add für Incorrect (meist nur einer)
        state.incorrectShapes().forEach(this::addToIncorrect); 
        addToMarked(state.markedShapes());
        makeActive(state.activeShapes());
        setInteractive(state.interactive());
    }
}