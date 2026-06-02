package app.scripts.ui;

import java.io.File;

import app.shared.Config;
import app.ui.skin.SkinService;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class RoundedImageTest extends Application {

    @Override
    public void start(Stage primaryStage) {
        Config.init("C:/Users/Markgraf/OneDrive/ThosSuite/");
        
        VBox root = new VBox(20);
        root.setPadding(new Insets(20));
        
        // Test 1: Region mit background-image
        Label label1 = new Label("Test 1: Region mit -fx-background-radius");
        RegionImagePane test1 = new RegionImagePane(500, 500, 20);
        test1.setImage("C:/Users/Markgraf/OneDrive/ThosSuite/data/images/500x500/AbuSimbel.jpg");
        
        // Test 2: Aktueller Rectangle-Ansatz zum Vergleich
        Label label2 = new Label("Test 2: Rectangle-Ansatz (aktuell)");
        RectangleImagePane test2 = new RectangleImagePane(500, 500, 40);
        test2.setImage("C:/Users/Markgraf/OneDrive/ThosSuite/data/images/500x500/AbuSimbel.jpg");
        
        root.getChildren().addAll(label1, test1, label2, test2);
        
        Scene scene = new Scene(root);
        SkinService.get().styleScene(scene);
        
        // Inline CSS für den Test
        scene.getStylesheets().add("data:text/css," + 
            ".test-region-bg { -fx-background-radius: 20px; }" +
            ".test-region-img { -fx-background-radius: 20px; -fx-background-size: cover; -fx-background-position: center; }" +
            ".test-region-border { -fx-border-color: red; -fx-border-width: 3px; -fx-border-radius: 20px; }"
        );
        
        primaryStage.setScene(scene);
        primaryStage.setTitle("Rounded Image Test");
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
    
    // ========== Test-Implementierung mit Region ==========
    
    static class RegionImagePane extends StackPane {
        private final Region backgroundLayer;
        private final Region imageLayer;
        private final Region borderLayer;

        public RegionImagePane(double width, double height, double radius) {
            setPrefSize(width, height);
            setMinSize(width, height);
            setMaxSize(width, height);
            
            backgroundLayer = new Region();
            backgroundLayer.setPrefSize(width, height);
            backgroundLayer.getStyleClass().add("test-region-bg");
            backgroundLayer.setStyle("-fx-background-color: black;"); // Fallback
            
            imageLayer = new Region();
            imageLayer.setPrefSize(width, height);
            imageLayer.getStyleClass().add("test-region-img");
            
            borderLayer = new Region();
            borderLayer.setPrefSize(width, height);
            borderLayer.getStyleClass().add("test-region-border");
            borderLayer.setMouseTransparent(true);
            
            getChildren().addAll(backgroundLayer, imageLayer, borderLayer);
        }
        
        public void setImage(String imagePath) {
            if (imagePath == null) {
                imageLayer.setStyle("");
            } else {
                File imageFile = new File(imagePath);
                String uri = imageFile.toURI().toString();
                imageLayer.setStyle("-fx-background-image: url('" + uri + "'); -fx-background-size: cover; -fx-background-position: center;");
            }
        }
    }
    
    // ========== Aktueller Rectangle-Ansatz zum Vergleich ==========
    
    static class RectangleImagePane extends StackPane {
        private final javafx.scene.shape.Rectangle backgroundRect;
        private final javafx.scene.shape.Rectangle imageRect;
        private final javafx.scene.shape.Rectangle borderRect;

        public RectangleImagePane(double width, double height, double arcDiameter) {
            setPrefSize(width, height);
            setMinSize(width, height);
            setMaxSize(width, height);

            backgroundRect = new javafx.scene.shape.Rectangle(width, height);
            backgroundRect.setArcWidth(arcDiameter);
            backgroundRect.setArcHeight(arcDiameter);
            backgroundRect.setFill(javafx.scene.paint.Color.BLACK);

            imageRect = new javafx.scene.shape.Rectangle(width, height);
            imageRect.setArcWidth(arcDiameter);
            imageRect.setArcHeight(arcDiameter);
            imageRect.setFill(javafx.scene.paint.Color.TRANSPARENT);

            borderRect = new javafx.scene.shape.Rectangle(width, height);
            borderRect.setArcWidth(arcDiameter);
            borderRect.setArcHeight(arcDiameter);
            borderRect.setFill(javafx.scene.paint.Color.TRANSPARENT);
            borderRect.setStroke(javafx.scene.paint.Color.RED);
            borderRect.setStrokeWidth(3);
            borderRect.setMouseTransparent(true);

            getChildren().addAll(backgroundRect, imageRect, borderRect);
        }
        
        public void setImage(String imagePath) {
            if (imagePath == null) {
                imageRect.setStyle("");
                imageRect.setFill(javafx.scene.paint.Color.TRANSPARENT);
            } else {
                File imageFile = new File(imagePath);
                String uri = imageFile.toURI().toString();
                imageRect.setStyle("-fx-fill: url('" + uri + "');");
            }
        }
    }
}