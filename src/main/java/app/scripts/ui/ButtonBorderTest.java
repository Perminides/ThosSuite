package app.scripts.ui;

import app.shared.Config;
import app.shared.skin.SkinService;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * 
 * https://stackoverflow.com/questions/18154110/set-border-size
 * 
 * 
 */
public class ButtonBorderTest extends Application {
    
    @Override
    public void start(Stage stage) {
    	Button button = new Button("Test Button");
    	Button button2 = new Button("Noch ein Button");
    	TextField textFiled = new TextField("Und ein Textfeld");

        // 1. Der Button braucht eine Scene, um Zugriff auf die User-Agent-Styles (Modena) zu haben
        Scene dummyScene = new Scene(new StackPane(button));

        // 2. Jetzt CSS anwenden - JavaFX schaut nun in die Modena-Stylesheets
        button.applyCss();

        Background background = button.getBackground();
		if (background != null && !background.getFills().isEmpty()) {
		    // Wir nehmen den ersten BackgroundFill (wie in ScenicView zu sehen)
		    BackgroundFill firstFill = background.getFills().get(0);
		    
		    javafx.geometry.Insets insets = firstFill.getInsets();
		    
		    double top = insets.getTop();
		    double right = insets.getRight();
		    double bottom = insets.getBottom();
		    double left = insets.getLeft();

		    System.out.println("Bottom Inset ist: " + bottom);
		}
        
        VBox root = new VBox(20);
        root.getChildren().add(button);
        root.getChildren().add(button2);
        root.getChildren().add(textFiled);
        root.setStyle("-fx-padding: 50px; -fx-background-color: yellow;");
        
        Scene scene = new Scene(root, 400, 200);
        
        //Exakt deine Styles:
        /**String css = """
            .button {
        		-fx-font-size: 20px;
                -fx-background-radius: 8px;
                -fx-border-radius: 8px;
                -fx-border-width: 1px;
                -fx-border-color: black;
                -fx-background-color: #ffffff;
            }
            .button:hover{
        		-fx-background-color: #888888;
            }
            """;
        
        scene.getStylesheets().add("data:text/css," + css.replace("#", "%23"));**/
        
        Config.init("C:/Users/Markgraf/OneDrive/ThosSuite/");
        SkinService.get().styleScene(scene);
        
        stage.setScene(scene);
        stage.setTitle("Button Border Test");
        stage.show();
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}