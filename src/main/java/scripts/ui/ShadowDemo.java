package scripts.ui;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class ShadowDemo extends Application {

    private static final String CSS = """
        .shadow-label,
        .shadow-textfield,
        .shadow-button {
            -fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.4), 10, 0.3, 3, 3);
        }
        .shadow-label {
            -fx-font-size: 20px;
            -fx-font-weight: bold;
            -fx-text-fill: #333333;
        }
        .shadow-textfield {
            -fx-font-size: 14px;
            -fx-padding: 8px;
            -fx-max-width: 250px;
        }
        .shadow-button {
            -fx-font-size: 14px;
            -fx-padding: 8 20;
            -fx-background-color: #2a6496;
            -fx-text-fill: white;
            -fx-background-radius: 4;
            -fx-cursor: hand;
        }
        .shadow-button:hover {
            -fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.6), 15, 0.4, 4, 4);
        }
        .shadow-button:pressed {
            -fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.3), 5, 0.2, 1, 1);
        }
        """;

    @Override
    public void start(Stage primaryStage) {
        Label label = new Label("Schatten-Demo");
        label.getStyleClass().add("shadow-label");

        TextField textField = new TextField();
        textField.setPromptText("Text eingeben...");
        textField.getStyleClass().add("shadow-textfield");

        Button button = new Button("Klick mich");
        button.getStyleClass().add("shadow-button");

        VBox root = new VBox(20, label, textField, button);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(40));

        Scene scene = new Scene(root, 400, 300);
        scene.getStylesheets().add("data:text/css," + CSS.replace(" ", "%20"));

        primaryStage.setTitle("DropShadow CSS Demo");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}