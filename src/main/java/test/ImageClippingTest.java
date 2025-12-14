package test;

import java.io.File;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class ImageClippingTest {

    public void start(Stage stage) {
        // PASSE DIESEN PFAD AN - 500x500 Bild!
        String imagePath = "C:/Users/Markgraf/OneDrive/ThosSuite/data/images/500x500/5 Dollar.png";
        Image image = new Image(new File(imagePath).toURI().toString());
        
        System.out.println("Bild: " + image.getWidth() + "x" + image.getHeight());
        
        int size = 500;
        int borderWidth = 2;
        int arc = 20;
        /**
        // Test 1: Label mit Graphic (ImageView)
        Label labelTest = new Label();
        ImageView iv1 = new ImageView(image);
        iv1.setFitWidth(size);
        iv1.setFitHeight(size);
        iv1.setPreserveRatio(false); // Exakt 500x500
        labelTest.setGraphic(iv1);
        labelTest.setPrefSize(size, size);
        labelTest.setMinSize(size, size);
        labelTest.setMaxSize(size, size);
        labelTest.setStyle(
            "-fx-background-radius: " + arc + "px;" +
            "-fx-border-radius: " + arc + "px;" +
            "-fx-border-width: " + borderWidth + "px;" +
            "-fx-border-color: red;" +
            "-fx-padding: 0;" +
            "-fx-background-color: transparent;"
        );
        
        // Test 2: Pane mit Background-Image (CSS)
        Pane paneTest = new Pane();
        paneTest.setPrefSize(size, size);
        paneTest.setMinSize(size, size);
        paneTest.setMaxSize(size, size);
        String uri = new File(imagePath).toURI().toString();
        paneTest.setStyle(
            "-fx-background-image: url('" + uri + "');" +
            "-fx-background-size: " + size + "px " + size + "px;" +
            "-fx-background-position: center;" +
            "-fx-background-repeat: no-repeat;" +
            "-fx-background-radius: " + arc + "px;" +
            "-fx-border-radius: " + arc + "px;" +
            "-fx-border-width: " + borderWidth + "px;" +
            "-fx-border-color: blue;"
        );**/
        
     // Test 3: Button mit Graphic (ImageView)
        Button buttonTest = new Button();
        ImageView iv3 = new ImageView(image);
        iv3.setFitWidth(size);
        iv3.setFitHeight(size);
        iv3.setPreserveRatio(false);
        buttonTest.setGraphic(iv3);
        buttonTest.setPrefSize(size, size);
        buttonTest.setMinSize(size, size);
        buttonTest.setMaxSize(size, size);
        buttonTest.setStyle(
            "-fx-background-radius: " + arc + "px;" +
            "-fx-border-radius: " + arc + "px;" +
            "-fx-border-width: " + borderWidth + "px;" +
            "-fx-border-color: green;" +
            "-fx-padding: 0;" +
            "-fx-background-color: transparent;" +
            "-fx-background-insets: 0;" +
            "-fx-border-insets: 0;"
        );
        
        // Layout mit Beschriftung
        VBox root = new VBox(30,
            new Label("=== DEIN USE-CASE: 500x500 Bild, exakt 500x500 Component ==="),
            /**new VBox(10,
                new Label("Label + setGraphic(ImageView) - ROTE BORDER:"),
                labelTest,
                new Label("Größe: " + labelTest.getPrefWidth() + "x" + labelTest.getPrefHeight())
            ),
            new VBox(10,
                new Label("Pane + background-image (CSS) - BLAUE BORDER:"),
                paneTest,
                new Label("Größe: " + paneTest.getPrefWidth() + "x" + paneTest.getPrefHeight())
            ),**/
            new VBox(10,
            	    new Label("Button + setGraphic(ImageView) - GRÜNE BORDER:"),
            	    buttonTest
            	),
            new Label("→ Vergleiche: Gucken die Ecken raus?")
        );
        root.setPadding(new Insets(20));
        
        Scene scene = new Scene(root, 600, 800);
        stage.setScene(scene);
        stage.setTitle("Image Clipping - 500x500 Test");
        stage.show();
    }

    public static void main(String[] args) {
        Platform.startup(() -> {
            Stage stage = new Stage();
            new ImageClippingTest().start(stage);
        });
    }
}