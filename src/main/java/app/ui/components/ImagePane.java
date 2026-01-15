package app.ui.components;

import java.io.File;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import javafx.scene.paint.Color;


/**
 * Used to show an image. Consists of three Rectangles:
 * 		- backgroundRect	-> Used when no image is shown.
 * 		- imageRect			-> The image to display
 * 		- borderRect		-> Optional border
 * 
 * CSS-classes:
 * 		backgroundRect	= "image-background-layer"
 * 		borderRect		= "image-border-layer"
 */
public class ImagePane extends StackPane {

    // Layer 1: Hintergrundfarbe (wenn kein Bild da ist oder Transparenz im Bild)
    private final Rectangle backgroundRect;
    // Layer 2: Das eigentliche Bild
    private final Rectangle imageRect;
    // Layer 3: Der Rahmen (liegt ganz oben)
    private final Rectangle borderRect;

    // !Sofort Wieso definieren wir die arcs und so nicht über CSS????
    public ImagePane(double width, double height, double arcDiameter) {
        // 1. Container-Größe fixieren
        setPrefSize(width, height);
        setMinSize(width, height);
        setMaxSize(width, height);

        // ---------------------------------------------------------
        // Layer 1: Hintergrund (Unten)
        // ---------------------------------------------------------
        backgroundRect = new Rectangle(width, height);
        backgroundRect.setArcWidth(arcDiameter);
        backgroundRect.setArcHeight(arcDiameter);
        backgroundRect.getStyleClass().add("my-image-background-layer");
        // HIER WICHTIG: Den Border-Style entfernen wir hier!

        // ---------------------------------------------------------
        // Layer 2: Bild (Mitte)
        // ---------------------------------------------------------
        imageRect = new Rectangle(width, height);
        imageRect.setArcWidth(arcDiameter);
        imageRect.setArcHeight(arcDiameter);
        imageRect.setFill(Color.TRANSPARENT);

        // ---------------------------------------------------------
        // Layer 3: Rahmen (Oben)
        // ---------------------------------------------------------
        borderRect = new Rectangle(width, height);
        borderRect.setArcWidth(arcDiameter);
        borderRect.setArcHeight(arcDiameter);
        borderRect.setFill(Color.TRANSPARENT); // Innen komplett durchsichtig!
        borderRect.setMouseTransparent(true);  // Klicks sollen durchgehen (optional, aber sauber)
        
        // Der Rahmen-Style wandert auf dieses neue Rechteck:
        borderRect.getStyleClass().add("my-image-border-layer");

        // ---------------------------------------------------------
        // Stapeln: Hintergrund -> Bild -> Rahmen
        // ---------------------------------------------------------
        getChildren().addAll(backgroundRect, imageRect, borderRect);
    }

    public void setImage(String imagePath) {
        if (imagePath == null) {
            imageRect.setStyle(""); 
            imageRect.setFill(Color.TRANSPARENT);
        } else {
        	File imageFile = new File(imagePath);
        	if (!imageFile.exists())
        		throw new RuntimeException("Konnte das Bild nicht finden: " + imagePath);
            String uri = imageFile.toURI().toString();
            imageRect.setStyle("-fx-fill: url('" + uri + "');");
        }
    }
    
    // Getter: Falls jemand Zugriff auf den Hintergrund braucht
    public Rectangle getBackgroundRect() { return backgroundRect; }
    
    // Falls du später mal Zugriff auf den Rahmen brauchst, könntest du das hier ergänzen:
    public Rectangle getBorderRect() { return borderRect; }
}