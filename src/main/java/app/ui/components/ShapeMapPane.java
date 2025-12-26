package app.ui.components;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
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
 * Das neue Herzstück der Karten-Anzeige.
 * Nutzt JavaFX Scene Graph statt manuellem Painting.
 */
public class ShapeMapPane extends StackPane { // StackPane zentriert den Inhalt automatisch!
	
	public static record ShapeMapState(
		    Set<String> correctShapes,
		    Set<String> incorrectShapes,
		    Set<String> markedShapes,
		    Set<String> activeShapes,
		    boolean interactive
		) {}

    // CSS Pseudo-Klassen für den State-Transfer zum Skin
	// Achtung!!! Die müssen alle gleich auf false gesetzt werden. Wenn du hier eine hinzufügst, dann denk daran!
    private static final PseudoClass CORRECT = PseudoClass.getPseudoClass("correct");
    private static final PseudoClass INCORRECT = PseudoClass.getPseudoClass("incorrect");
    private static final PseudoClass MARKED = PseudoClass.getPseudoClass("marked");
    private static final PseudoClass ACTIVE_GAME = PseudoClass.getPseudoClass("active-game");
    
    // Hält alle Shapes im Zugriff für Updates per ID
    private final Map<String, Node> shapeMap = new HashMap<>();
    
    // Container für alle Shapes (wird skaliert)
    private final Group contentGroup;
    
    private MapElementListener listener;
    private boolean isInteractive = false;

    public ShapeMapPane(GeoMap map, int targetHeight) {
    	this.contentGroup = new Group();
    	
    	// !Sofort: Das ist alles etwas unschön gewachsen. Lieber ein neues Attribut "Layer" einführen? Und zumindest "fixedColorId" umbenennen in "decorationID" oder so? Denk dir ws schönes aus!
    	// Layer könntest DU dann gleich flexibel gestalten und solange durchloopen un immer den nächsthöheren Layer nehmen, bis alle drin sind...!
        
        // 1. Shapes initialisieren
        // HIER IST DIE ÄNDERUNG: Aufteilung in zwei Phasen für korrekte Z-Sortierung.
        
        // Phase 1: Alles zeichnen, was KEINE Overlay-Grenze ("2") ist.
        // Das sind die normalen Spiel-Shapes und Hintergründe.
        for (MapShape mapShape : map.getShapes()) {
        	mapShape.shape().pseudoClassStateChanged(CORRECT, false);
        	mapShape.shape().pseudoClassStateChanged(INCORRECT, false);
        	mapShape.shape().pseudoClassStateChanged(MARKED, false);
        	mapShape.shape().pseudoClassStateChanged(ACTIVE_GAME, false);
            if (!"2".equals(mapShape.fixedColorSet())) {
                initShape(mapShape);
            }
        }
        
        // Phase 2: Die Overlay-Grenzen ("2") nachträglich zeichnen.
        // Da sie als letztes in die 'contentGroup' kommen, liegen sie visuell obenauf.
        for (MapShape mapShape : map.getShapes()) {
            if ("2".equals(mapShape.fixedColorSet())) {
                initShape(mapShape);
            }
        }
        
        // 2. Inhalt zusammenbauen
        this.getChildren().add(contentGroup);
        
        // 3. Skalierung (Autopilot dank normierter GeoJSONs)
        // Wir warten bis das Layout steht, oder berechnen es initial basierend auf den Bounds der Shapes
        // Da StackPane zentriert, müssen wir nur den Scale-Faktor setzen.
        
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
        
        /**contentGroup.getChildren().forEach(node -> {
            if (node instanceof Shape shape) {
                System.out.println("Shape: " + shape.getUserData() + 
                                  " | Fill: " + shape.getFill() + 
                                  " | Classes: " + shape.getStyleClass() +
                                  " | PseudoClasses: " + shape.getPseudoClassStates())
                ;
            }
        });**/
    }
    
    private void initShape(MapShape mapShape) {
        Node shape = mapShape.shape();
        String id = mapShape.id();
        
        // ID für Lookup speichern
        shapeMap.put(id, shape);
        
        // UserData für Click-Listener
        shape.setUserData(id);
        
        // Deko-Logik
        if (!mapShape.isDecoration()) {
            // Interaktion nur für spielbare Shapes
            setupInteractions(shape);
        }
        
        contentGroup.getChildren().add(shape);
    }
    
    private void setupInteractions(Node shape) {
        shape.setOnMouseClicked(e -> {
            if (listener != null) {
                String id = (String) shape.getUserData();
                listener.mouseClicked(id);
            }
        });
    }

    public void setListener(MapElementListener listener) {
        this.listener = listener;
    }
    
    // --- State Management via Pseudo-Klassen ---
    public void setInteractive(boolean interactive) {
        this.isInteractive = interactive;
        
        if (interactive) {
            this.getStyleClass().remove("game-paused");
        } else {
            if (!this.getStyleClass().contains("game-paused")) {
                this.getStyleClass().add("game-paused");
            }
        }
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
       // Logik: Suche alle CORRECT shapes und mache sie ACTIVE
       // Da wir den State nicht speichern, iterieren wir über alle Shapes und prüfen PseudoClassState?
       // Einfacher: Wir iterieren über alle und resetten, das Presenter-Modell weiß ja, was aktiv sein soll.
       // Aber hier sollen wir ja nur Visuals machen.
       // Swing-Logik war: activeShapes.addAll(correctShapes); correctShapes.clear();
       
       shapeMap.values().forEach(shape -> {
           if (shape.getPseudoClassStates().contains(CORRECT)) {
               shape.pseudoClassStateChanged(CORRECT, false);
               shape.pseudoClassStateChanged(ACTIVE_GAME, true);
           }
       });
    }
    
    public void moveAllToActive() {
        shapeMap.values().forEach(shape -> {
            resetShapeState(shape);
            // !Sofort: Das ist ein undurchdachter Hack. Der funktioniert, aber sauber durchgedacht ist das noch nicht...
            if (!shape.getStyleClass().contains("decoration-2")) {
                shape.pseudoClassStateChanged(ACTIVE_GAME, true);
            }
        });
    }

    public void makeEveryShapeActive() {
        moveAllToActive();
    }
    
    // --- State Helpers ---
    
    private void updateShapeState(String id, PseudoClass state) {
        Node shape = shapeMap.get(id);
        if (shape != null) {
            // Exklusiv-Logik: Ein Shape hat idealerweise nur einen dominanten State
            // oder CSS regelt die Priorität. Hier resetten wir sicherheitshalber die anderen.
            resetShapeState(shape);
            shape.pseudoClassStateChanged(state, true);
        }
    }
    
    private void resetAllStates() {
        shapeMap.values().forEach(this::resetShapeState);
    }
    
    private void resetShapeState(Node shape) {
        shape.pseudoClassStateChanged(CORRECT, false);
        shape.pseudoClassStateChanged(INCORRECT, false);
        shape.pseudoClassStateChanged(MARKED, false);
        shape.pseudoClassStateChanged(ACTIVE_GAME, false);
    }
    
    // Für Resume/Restore State (ShapeMapState Record)
    public ShapeMapState getState() {
        // Wir müssen aus den Pseudo-Classes den State rekonstruieren
        Set<String> correct = new HashSet<>();
        Set<String> incorrect = new HashSet<>();
        Set<String> marked = new HashSet<>();
        Set<String> active = new HashSet<>();
        
        shapeMap.forEach((id, shape) -> {
            Set<PseudoClass> states = shape.getPseudoClassStates();
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