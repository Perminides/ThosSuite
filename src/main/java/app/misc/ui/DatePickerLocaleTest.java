package app.misc.ui;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.DatePicker;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import java.util.Locale;

public class DatePickerLocaleTest extends Application {
    
    @Override
    public void start(Stage stage) {
        System.out.println("Start: " + Locale.getDefault());
        
        // VARIANTE 1: Ohne setDefault
        // DatePicker picker = new DatePicker();
        
        // VARIANTE 2: Mit setDefault
        //Locale.setDefault(Locale.GERMANY);
        DatePicker picker = new DatePicker();
        
        VBox root = new VBox(picker);
        Scene scene = new Scene(root, 300, 200);
        
        stage.setScene(scene);
        stage.show();
        
        picker.show(); // Popup sofort öffnen
    }
    
    public static void main(String[] args) {

        launch(args);
    }
}