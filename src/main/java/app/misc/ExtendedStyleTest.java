package app.misc;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HeaderBar;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class ExtendedStyleTest extends Application {

    @Override
    public void start(Stage primaryStage) {
        primaryStage.initStyle(StageStyle.EXTENDED);
        
        BorderPane root = new BorderPane();
        StackPane content = new StackPane();
        content.setPrefSize(800, 600);
        
        Button openDialogButton = new Button("Open Dialog");
        openDialogButton.setOnAction(e -> showTestDialog(primaryStage));
        content.getChildren().add(openDialogButton);
        
        root.setCenter(content);
        
        HeaderBar headerBar = new HeaderBar();
        Label titleLabel = new Label("Main Window");
        titleLabel.setPadding(new javafx.geometry.Insets(20, 20, 20, 20));
        headerBar.setCenter(titleLabel);
        headerBar.heightProperty().addListener((obs, oldVal, newVal) ->
            HeaderBar.setPrefButtonHeight(primaryStage, newVal.doubleValue())
        );
        
        root.setTop(headerBar);
        
        primaryStage.setScene(new Scene(root));
        primaryStage.show();
    }
    
    private void showTestDialog(Stage owner) {
        Alert alert = new Alert(Alert.AlertType.NONE);
        alert.initOwner(owner);
        alert.initStyle(StageStyle.EXTENDED);
        
        HeaderBar headerBar = new HeaderBar();
        Label titleLabel = new Label("Dialog");
        titleLabel.setPadding(new javafx.geometry.Insets(20, 0, 20, 0));
        headerBar.setCenter(titleLabel);
        alert.getDialogPane().setHeader(headerBar);
        alert.getDialogPane().setContent(new Label("Compare Close button widths!"));
        alert.getButtonTypes().add(ButtonType.OK);
        
        headerBar.heightProperty().addListener((obs, oldVal, newVal) -> {
            if (alert.getDialogPane().getScene().getWindow() instanceof Stage stage) {
                HeaderBar.setPrefButtonHeight(stage, newVal.doubleValue());
            }
        });
        
        alert.showAndWait();
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}