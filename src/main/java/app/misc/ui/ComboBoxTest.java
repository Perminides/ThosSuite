package app.misc.ui;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class ComboBoxTest extends Application {

    private static final List<String> ALL_ITEMS = List.of("Erstens", "Zweitens", "Drittens");

    private String inkompatibel = "Zweitens";

    @Override
    public void start(Stage stage) {
        ComboBox<String> comboBox = new ComboBox<>();
        comboBox.setValue("Erstens");
        updateItems(comboBox);

        comboBox.setOnAction(_ -> {
            String selected = comboBox.getValue();
            if (selected != null) {
                // Nächstes nach selected inkompatibel machen
                int idx = ALL_ITEMS.indexOf(selected);
                inkompatibel = ALL_ITEMS.get((idx + 1) % ALL_ITEMS.size());
                updateItems(comboBox);
            }
        });

        Button button = new Button("Wechseln");
        button.setOnAction(_ -> {
            String current = comboBox.getValue();
            String naechster = comboBox.getItems().stream()
                .filter(i -> !i.equals(current))
                .findFirst()
                .orElse(null);
            if (naechster != null) {
                comboBox.setValue(naechster);
            }
        });

        VBox root = new VBox(20, comboBox, button);

        String css = ".combo-box-popup .list-view .list-cell { -fx-text-fill: #000000; }";
        String encodedCss = URLEncoder.encode(css, StandardCharsets.UTF_8);
        Scene scene = new Scene(root, 300, 200);
        scene.getStylesheets().add("data:text/css," + encodedCss);
        stage.setScene(scene);
        stage.show();
    }

    private void updateItems(ComboBox<String> comboBox) {
        String current = comboBox.getValue();
        ObservableList<String> kompatibel = FXCollections.observableArrayList(
            ALL_ITEMS.stream().filter(i -> !i.equals(inkompatibel)).toList()
        );
        comboBox.setItems(kompatibel);
        // Aktuelle Auswahl beibehalten wenn noch kompatibel, sonst ersten wählen
        if (kompatibel.contains(current)) {
            comboBox.setValue(current);
        } else {
            comboBox.setValue(kompatibel.get(0));
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}