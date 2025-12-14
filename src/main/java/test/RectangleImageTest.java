package test;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeType;
import javafx.stage.Stage;

import java.io.File;

public class RectangleImageTest extends Application {

    // TODO: Hier deinen Bildpfad eintragen! (z.B. "C:/Bilder/test.jpg" oder "/Users/Name/test.png")
    private static final String IMAGE_PATH = "C:/Users/Markgraf/OneDrive/ThosSuite/data/images/500x500/afghanistan-flag-square-small.png";

    @Override
    public void start(Stage primaryStage) {
        
        // 1. Bild laden
        Image image = null;
        try {
            File file = new File(IMAGE_PATH);
            if (file.exists()) {
                image = new Image(file.toURI().toString());
            } else {
                System.err.println("Bild nicht gefunden unter: " + IMAGE_PATH);
                // Fallback: Ein Platzhalterbild aus dem Netz, damit der Test trotzdem läuft
                image = new Image("https://via.placeholder.com/500"); 
            }
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        // 2. Das Vektor-Rechteck erstellen (500x500)
        Rectangle vectorShape = new Rectangle(500, 500);

        // 3. Füllung setzen (Das ist der Trick!)
        // Das Bild wird als "Tapete" (Pattern) in die Form gemalt.
        // Da das Bild 500x500 ist und das Rechteck auch, passt es 1:1.
        vectorShape.setFill(new ImagePattern(image));

        // 4. Runde Ecken definieren
        // WICHTIG: JavaFX erwartet den DURCHMESSER (Diameter), nicht den Radius.
        // Radius 20px  =>  ArcWidth/Height 40px
        vectorShape.setArcWidth(40);
        vectorShape.setArcHeight(40);

        // 5. Rahmen konfigurieren (3px, halbtransparentes Schwarz)
        vectorShape.setStroke(Color.rgb(255, 0, 0, 1)); // 0.5 = 50% Transparenz
        vectorShape.setStrokeWidth(3);

        // 6. StrokeType.INSIDE (Entscheidend für saubere Kanten!)
        // Der Rahmen wird nach INNEN gezeichnet. Dadurch:
        // a) Bleibt die Gesamtgröße exakt 500x500.
        // b) Werden pixelige Kanten des Bildes sauber vom Rahmen überdeckt.
        vectorShape.setStrokeType(StrokeType.INSIDE);

        // --- Layout Setup zum Anzeigen ---
        StackPane root = new StackPane(vectorShape);
        
        // Hintergrund der Scene farbig machen, um zu beweisen, dass die Ecken transparent sind
        root.setStyle("-fx-background-color: #f0f0f0;"); // Hellgrau

        Scene scene = new Scene(root, 600, 600);
        primaryStage.setTitle("JavaFX Perfect Border Clipping Test");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}