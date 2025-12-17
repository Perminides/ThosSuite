package app.ui.components;

import java.io.File;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import javafx.scene.paint.Color;

public class ImagePane extends StackPane {

    // Wir nutzen Shapes (Rectangle), weil die sauber clippen!
    private final Rectangle backgroundRect;
    private final Rectangle imageRect;

    public ImagePane(double width, double height, double arcDiameter) {
        // 1. Container-Größe fixieren
        setPrefSize(width, height);
        setMinSize(width, height);
        setMaxSize(width, height);

        // 2. Hintergrund-Layer (Shape für die Farbe Gold)
        backgroundRect = new Rectangle(width, height);
        backgroundRect.setArcWidth(arcDiameter);
        backgroundRect.setArcHeight(arcDiameter);
        // Weist dem Shape die CSS-Klasse für die Füllfarbe zu
        backgroundRect.getStyleClass().add("image-background-layer");

        // 3. Bild-Layer (Shape für das PNG)
        imageRect = new Rectangle(width, height);
        imageRect.setArcWidth(arcDiameter);
        imageRect.setArcHeight(arcDiameter);
        imageRect.setFill(Color.TRANSPARENT); // Erstmal leer/durchsichtig
        
        // WICHTIG: Kein Rahmen am Bild-Rect, damit es nicht doppelt gemalt wird.
        // Der Rahmen kommt entweder an den backgroundRect oder an den StackPane.
        backgroundRect.getStyleClass().add("image-border-layer");

        // 4. Stapeln: Gold unten, Bild oben
        getChildren().addAll(backgroundRect, imageRect);
    }

    public void setImage(String imagePath) {
        if (imagePath == null) {
            imageRect.setStyle(""); 
            imageRect.setFill(Color.TRANSPARENT);
        } else {
            String uri = new File(imagePath).toURI().toString();
            // Hier nutzen wir CSS beim Shape. JavaFX macht daraus intern ein ImagePattern.
            // Das Bild wird auf die Größe des Rectangles gestreckt (Stretch).
            // Da das Rectangle die Ecken abschneidet, wird auch das Bild abgeschnitten.
            imageRect.setStyle("-fx-fill: url('" + uri + "');");
        }
    }
    
    // Getter für den Skin (falls nötig)
    public Rectangle getBackgroundRect() { return backgroundRect; }
}