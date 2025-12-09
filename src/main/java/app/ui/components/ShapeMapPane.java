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
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Shape;
import javafx.scene.transform.Scale;

/**
 * Das neue Herzstück der Karten-Anzeige.
 * Nutzt JavaFX Scene Graph statt manuellem Painting.
 */
public class ShapeMapPane extends StackPane { // StackPane zentriert den Inhalt automatisch!

    // CSS Pseudo-Klassen für den State-Transfer zum Skin
    private static final PseudoClass CORRECT = PseudoClass.getPseudoClass("correct");
    private static final PseudoClass INCORRECT = PseudoClass.getPseudoClass("incorrect");
    private static final PseudoClass MARKED = PseudoClass.getPseudoClass("marked");
    private static final PseudoClass ACTIVE_GAME = PseudoClass.getPseudoClass("active-game");
    
    // Hält alle Shapes im Zugriff für Updates per ID
    private final Map<String, Shape> shapeMap = new HashMap<>();
    
    // Container für alle Shapes (wird skaliert)
    private final Group contentGroup;
    
    private MapElementListener listener;
    private boolean isInteractive = false;

    public ShapeMapPane(GeoMap map, int targetHeight) {
        this.contentGroup = new Group();
        
        // 1. Shapes initialisieren
        for (MapShape mapShape : map.getShapes()) {
            initShape(mapShape);
        }
        
        // !Später: Bundesländergrenzen (Hier würden wir die Overlay-Shapes laden und als nicht-interaktiv markieren)
        
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
        
        // !Sofort: Den neuen 3D-Hover mal ausprobieren
        // Ein DropShadow ist performant und sieht schick aus. Farbe kann man via CSS anpassen, wenn man will.
        DropShadow dropShadow = new DropShadow();
        dropShadow.setRadius(5.0);
        dropShadow.setColor(Color.rgb(0, 0, 0, 0.4)); // Leicht transparentes Schwarz
        // Wir binden den Effekt nur an den Hover-Zustand, um Performance zu sparen? 
        // Oder global für 3D-Look? Hier erstmal global für den "Retro-Hover" Ersatz.
        // Besser: Wir aktivieren ihn per CSS nur bei Hover! (Siehe Skin-Erweiterung später)
        // Aber da du sagtest "mit einer Zeile Code":
        // contentGroup.setEffect(dropShadow); // Würde alles schattieren.
        
        // Alternativ: Per CSS .map-shape:hover { -fx-effect: dropshadow(...) } -> Das ist der JavaFX Weg!
    }
    
    private void initShape(MapShape mapShape) {
        Shape shape = mapShape.shape();
        String id = mapShape.id();
        
        // ID für Lookup speichern
        shapeMap.put(id, shape);
        
        // UserData für Click-Listener
        shape.setUserData(id);
        
        // CSS-Klassen setzen
        shape.getStyleClass().add("map-shape");
        
        // Deko-Logik
        if (mapShape.isDecoration()) {
            shape.getStyleClass().add("decoration");
            // Spezifische Deko-Klassen für 0 und 1
            if ("0".equals(mapShape.fixedColorSet())) {
                shape.getStyleClass().add("decoration-0");
            } else {
                shape.getStyleClass().add("decoration-1");
            }
            shape.setMouseTransparent(true); // Deko fängt keine Klicks ab
        } else {
            // Interaktion nur für spielbare Shapes
            setupInteractions(shape);
        }
        
        contentGroup.getChildren().add(shape);
    }
    
    private void setupInteractions(Shape shape) {
        shape.setOnMouseClicked(e -> {
            if (isInteractive && listener != null) {
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
        // Optional: Visuelles Feedback für "Karte ist aktiv"
        // contentGroup.setOpacity(interactive ? 1.0 : 0.8); 
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
        resetAllStates(); // Erstmal alles sauber machen
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
            shape.pseudoClassStateChanged(ACTIVE_GAME, true);
        });
    }

    public void makeEveryShapeActive() {
        moveAllToActive();
    }
    
    // --- State Helpers ---
    
    private void updateShapeState(String id, PseudoClass state) {
        Shape shape = shapeMap.get(id);
        if (shape != null) {
            // Exklusiv-Logik: Ein Shape hat idealerweise nur einen dominanten State
            // oder CSS regelt die Priorität. Hier resetten wir sicherheitshalber die anderen.
            resetShapeState(shape);
            shape.pseudoClassStateChanged(state, true);
            shape.toFront(); // Wichtig: Markierte/Richtige nach vorne holen (wegen Overlap)!
        }
    }
    
    private void resetAllStates() {
        shapeMap.values().forEach(this::resetShapeState);
    }
    
    private void resetShapeState(Shape shape) {
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