package app.scripts.ui;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HeaderBar;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * TEST: Opacity-Trick mit 1 Sekunde Wartezeit
 */
public class WhiteFlashTest extends Application {

    @Override
    public void start(Stage stage) {
        stage.initStyle(StageStyle.EXTENDED);
        stage.setOpacity(0); // UNSICHTBAR!
        
        BorderPane root = new BorderPane();
        root.getStyleClass().add("my-root");
        
        HeaderBar headerBar = new HeaderBar();
        headerBar.setCenter(new Label("Test Header"));
        headerBar.getStyleClass().add("my-header");
        root.setTop(headerBar);
        
        StackPane content = new StackPane();
        content.getStyleClass().add("my-content");
        root.setCenter(content);
        
        Scene scene = new Scene(root, 800, 600);
        
        String css = ".my-root { -fx-background-color: #6DA6BB; } " +
                     ".my-header { -fx-background-color: #6DA6BB; } " +
                     ".my-content { -fx-background-color: #6DA6BB; }";
        String encoded = css.replace("#", "%23");
        scene.getStylesheets().add("data:text/css," + encoded);
        
        stage.setScene(scene);
        stage.setTitle("Opacity Test");
        stage.show(); // ZEIGEN (aber unsichtbar)
        
        // Nach 1 Sekunde sichtbar machen
        new Thread(() -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {}
            
            Platform.runLater(() -> {
                System.out.println("Setze Opacity auf 1");
                stage.setOpacity(1);
            });
        }).start();
    }

    public static void main(String[] args) {
        launch(args);
    }
}