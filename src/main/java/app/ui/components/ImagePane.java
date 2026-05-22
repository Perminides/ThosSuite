package app.ui.components;

import java.io.File;
import java.net.MalformedURLException;

import app.config.Config;
import javafx.scene.image.Image;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Rectangle;


/**
 * Used to show an image. Consists of three Rectangles:
 * 		- backgroundRect	-> Used when no image is shown.
 * 		- imageRect			-> The image to display
 * 		- borderRect		-> Optional border
 * 
 * CSS-classes:
 * 		backgroundRect	= "my-image-background-layer"
 * 		borderRect		= "my-image-border-layer"
 * 
 */
public class ImagePane extends StackPane {

    // Layer 1: Hintergrundfarbe (wenn kein Bild da ist oder Transparenz im Bild)
    private final Rectangle backgroundRect;
    // Layer 2: Das eigentliche Bild
    private final Rectangle imageRect;
    // Layer 3: Der Rahmen (liegt ganz oben)
    private final Rectangle borderRect;

    /**
     * Ja. Leider brauchen wir diese Parameter. Man kann mittels CSS kein Bild beschneiden.
     * Das würde sonst also unter abgerundeten Ecken hervorlugen... Siehe RoundedImageTest, falls noch da...
     * Und nein, das ist jetzt auch kein Riesenproblem: Wir müssen eh das Spielfeld neu aufbauen bei Skinwechsel,
     * weil sich ja auch Positionen ändern können von Komponenten
     * 
     * @param width
     * @param height
     * @param arcDiameter
     */
    public ImagePane(double width, double height, double arcDiameter) {
        // 1. Container-Größe fixieren
        setPrefSize(width, height);
        setMinSize(width, height);
        setMaxSize(width, height);
        
        // !Sofort: Hier  auch noch den -fx-background-insets Fix einführen. Analog ImageMapPane 

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
        if (imagePath == null || imagePath.equals(Config.get("learnImageFolder"))) {
            imageRect.setStyle(""); 
            imageRect.setFill(Color.TRANSPARENT);
            return;
        }
        

        File imageFile = new File(imagePath);
        if (!imageFile.exists()) {
            throw new RuntimeException("Konnte das Bild nicht finden: " + imagePath);
        }

        try {
            String url = imageFile.toURI().toURL().toExternalForm(); // sauberer als toString()
            Image img = new Image(url, false); // backgroundLoading=false => lädt synchron
            if (img.isError()) {
                throw new RuntimeException("Fehler beim Laden des Bildes: " + imagePath, img.getException());
            }
            imageRect.setStyle(""); // CSS weg, falls vorher gesetzt
            imageRect.setFill(new ImagePattern(img));
        } catch (MalformedURLException e) {
            throw new RuntimeException("Ungültige Bild-URL: " + imagePath, e);
        }
    }
    
    // Getter: Falls jemand Zugriff auf den Hintergrund braucht
    public Rectangle getBackgroundRect() { return backgroundRect; }
    
    // Falls du später mal Zugriff auf den Rahmen brauchst, könntest du das hier ergänzen:
    public Rectangle getBorderRect() { return borderRect; }
}