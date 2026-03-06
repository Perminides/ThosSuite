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
 * 		Shape			= ":corrrect", ":incorrect", ":marked", ":active"
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
    private static final PseudoClass ACTIVE = PseudoClass.getPseudoClass("active");
    private static final PseudoClass INACTIVE = PseudoClass.getPseudoClass("inactive");
    
    // Hält alle Shapes im Zugriff für Updates per ID
    private final Map<String, MapShape> shapeMap = new HashMap<>();
    
    // Container für alle Shapes (wird skaliert)
    private final Group contentGroup;
    
    private MapElementListener listener;
    private boolean isInteractive = false; // Benötigt für den ShapeMapState

    public ShapeMapPane(GeoMap map, double targetHeight) {
    	this.getStyleClass().add("my-shape-map-pane");
    	this.contentGroup = new Group();        
        map.getShapes().stream()
           .sorted(Comparator.comparingInt(s -> s.getZIndex()))
           .forEach(mapShape -> {
               Node node = mapShape.shape();
               String id = mapShape.id();
               
               // OPTIMIERUNG: Altlasten direkt entfernen!
               // Da die Node-Objekte wiederverwendet werden, entfernen wir hier 
               // eventuelle Inline-Styles (z.B. fette Ränder) aus der letzten Session.
               node.setStyle(""); 
               
               shapeMap.put(id, mapShape);
               
               if (mapShape.isInteractive())
            	   node.setOnMouseClicked(_ -> {
            		   if (listener != null) {
            			   listener.mouseClicked(id);
            		   }
            	   });
               
               contentGroup.getChildren().add(node);
          });
        
        resetAllStates();
        this.getChildren().add(contentGroup);
    
        // 1. Initiales Layout für Bounds-Berechnung
        // Hier greift meist nur der Default-Style (z.B. 1.0px), da noch keine Scene existiert.
        // Das ist aber okay, da wir dank node.setStyle("") oben "sauber" messen.
        contentGroup.applyCss(); 
        contentGroup.layout();
        Bounds bounds = contentGroup.getBoundsInLocal();
        
        // final für Zugriff im Listener
        final double scaleFactor = targetHeight / bounds.getHeight();
        
        Scale scale = new Scale(scaleFactor, scaleFactor);
        scale.setPivotX(0);
        scale.setPivotY(0);
        contentGroup.getTransforms().add(scale);
        
        // FIX: "Inverse Scaling" für konstante Strichdicken
        // Problem: Durch die Skalierung der Gruppe (z.B. auf 10%) werden auch die Linien 10x dünner.
        // Lösung: Wir warten, bis die Scene da ist (und damit der Skin), lesen die gewünschte Dicke
        // und setzen sie "künstlich" hoch (z.B. mal 10), damit sie optisch gleich bleibt.
        // WICHTIG: Inline-Style blockiert zukünftige CSS-Änderungen für stroke-width.
        // Falls später dynamische Stroke-Anpassungen nötig werden, muss diese Lösung überdacht werden.
        this.sceneProperty().addListener(new javafx.beans.value.ChangeListener<javafx.scene.Scene>() {
            @Override
            public void changed(javafx.beans.value.ObservableValue<? extends javafx.scene.Scene> observable, javafx.scene.Scene oldValue, javafx.scene.Scene newScene) {
                if (newScene != null) {
                    // 2. Skin-Werte laden (jetzt wo die Scene da ist)
                    // Zwingend nötig, um z.B. die "1.8px" aus dem CSS zu erfahren.
                    contentGroup.applyCss();
                    
                    // 3. Strichdicken anpassen
                    for (Node node : contentGroup.getChildren()) {
                        if (node instanceof javafx.scene.shape.Shape s) {
                            double originalWidth = s.getStrokeWidth();
                            if (originalWidth > 0) {
                                // Berechnung: Skin-Wert / Skalierung = Nötige technische Dicke
                                double newWidth = originalWidth / scaleFactor;
                                
                                // Wir nutzen setStyle (Inline-CSS), da dies Priorität vor dem Skin-CSS hat.
                                // Sonst würde JavaFX den Wert beim nächsten Pulse wieder überschreiben.
                                s.setStyle("-fx-stroke-width: " + String.format(java.util.Locale.US, "%.4f", newWidth) + "px;");
                            }
                        }
                    }
                    
                    // Listener entfernen, da der Job erledigt ist.
                    ShapeMapPane.this.sceneProperty().removeListener(this);
                }
            }
        });
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
            updateShapeState(id, ACTIVE);
        }
    }
    
    public void makeInactive(Set<String> ids) {
        for (String id : ids) {
            updateShapeState(id, INACTIVE);
        }
    }
    
    public void moveCorrectToActive() {
       shapeMap.values().forEach(shape -> {
           if (shape.shape().getPseudoClassStates().contains(CORRECT)) {
               shape.shape().pseudoClassStateChanged(CORRECT, false);
               shape.shape().pseudoClassStateChanged(ACTIVE, true);
           }
       });
    }
    
    /**
     * Resettet alle interaktiven Shapes und setzt sie auf ACTIVE
     */
    public void moveAllToActive() {
        shapeMap.values().forEach(mapShape -> {
            resetShapeState(mapShape);
            if (mapShape.isInteractive()) {
                mapShape.shape().pseudoClassStateChanged(ACTIVE, true);
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
    
    protected void resetAllStates() {
        shapeMap.values().forEach(this::resetShapeState);
    }
    
    private void resetShapeState(MapShape shape) {
        shape.shape().pseudoClassStateChanged(CORRECT, false);
        shape.shape().pseudoClassStateChanged(INCORRECT, false);
        shape.shape().pseudoClassStateChanged(MARKED, false);
        shape.shape().pseudoClassStateChanged(ACTIVE, false);
        shape.shape().pseudoClassStateChanged(INACTIVE, false);
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
            if (states.contains(ACTIVE)) active.add(id);
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