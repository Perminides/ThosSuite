package app.misc.alc;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import app.config.Config;
import app.ui.skin.SkinService;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * Standalone-Prototyp für AlcoholChartSession.
 * 
 * Features:
 * - DatePicker "Von" / "Bis" für Zeitraum-Auswahl
 * - Spinner für category-gap (Balkenabstand) 1-20
 * - BarChart mit Mock-Daten (Grün/Gelb/Rot)
 * - Dynamisches Neuladen bei Änderungen
 */
public class AlcoholChartPrototype extends Application {
    
    // Mock-Datenklasse
    private record AlcoholDayData(LocalDate date, String status, int balance) {}
    
    // PseudoClasses für Balken-Farben
    private static final PseudoClass GREEN = PseudoClass.getPseudoClass("green");
    private static final PseudoClass YELLOW = PseudoClass.getPseudoClass("yellow");
    private static final PseudoClass RED = PseudoClass.getPseudoClass("red");
    
    private DatePicker fromPicker;
    private DatePicker toPicker;
    private Spinner<Integer> gapSpinner;
    private StackPane chartContainer;
    
    @Override
    public void start(Stage stage) {
        VBox root = new VBox(20);
        root.setPadding(new Insets(20));
        root.setAlignment(Pos.TOP_CENTER);
        
        // === Controls oben ===
        HBox controls = createControlsBar();
        
        // === Chart Container ===
        chartContainer = new StackPane();
        VBox.setVgrow(chartContainer, Priority.ALWAYS);
        
        root.getChildren().addAll(controls, chartContainer);
        
        // Initiales Chart laden
        updateChart();
        
        Scene scene = new Scene(root, 1200, 800);
        
        //applyCss(scene);
        
        Config.init("C:/Users/Markgraf/OneDrive/ThosSuite/");
        SkinService.get().styleScene(scene);
        
        stage.setScene(scene);
        stage.setTitle("Alkohol-Tracker Chart Prototyp");
        stage.show();
    }
    
    private HBox createControlsBar() {
        HBox controls = new HBox(15);
        controls.setAlignment(Pos.CENTER_LEFT);
        controls.setPadding(new Insets(10));
        controls.setStyle("-fx-background-color: #2b2b2b; -fx-background-radius: 10px;");
        
        // DatePicker "Von"
        Label fromLabel = new Label("Von:");
        fromLabel.setStyle("-fx-text-fill: white;");
        fromPicker = new DatePicker(LocalDate.now().minusDays(1500));
        fromPicker.setOnAction(e -> updateChart());
        
        // DatePicker "Bis"
        Label toLabel = new Label("Bis:");
        toLabel.setStyle("-fx-text-fill: white;");
        toPicker = new DatePicker(LocalDate.now());
        toPicker.setOnAction(e -> updateChart());
        
        // Spacer (drückt Spinner nach rechts)
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        // Spinner für category-gap
        Label gapLabel = new Label("Balkenabstand:");
        gapLabel.setStyle("-fx-text-fill: white;");
        gapSpinner = new Spinner<>(1, 20, 3);
        gapSpinner.setEditable(true);
        gapSpinner.setPrefWidth(Region.USE_COMPUTED_SIZE);
        gapSpinner.valueProperty().addListener((obs, old, newVal) -> updateChart());
        
        controls.getChildren().addAll(
            fromLabel, fromPicker,
            toLabel, toPicker,
            spacer,
            gapLabel, gapSpinner
        );
        
        return controls;
    }
    
    private void updateChart() {
        chartContainer.getChildren().clear();
        
        LocalDate from = fromPicker.getValue();
        LocalDate to = toPicker.getValue();
        int categoryGap = gapSpinner.getValue();
        
        // Mock-Daten generieren
        List<AlcoholDayData> data = generateMockData(from, to);
        
        // Chart erstellen
        BarChart<String, Number> chart = createChart(data, categoryGap);
        
        chartContainer.getChildren().add(chart);
    }
    
    private List<AlcoholDayData> generateMockData(LocalDate from, LocalDate to) {
        List<AlcoholDayData> data = new ArrayList<>();
        int balance = 0;
        
        LocalDate current = from;
        while (!current.isAfter(to)) {
            // Zufälliger Status
            String status;
            int dayOfWeek = current.getDayOfWeek().getValue();
            
            if (dayOfWeek == 6 || dayOfWeek == 7) {
                // Wochenende: mehr Rot
                status = Math.random() < 0.6 ? "RED" : (Math.random() < 0.5 ? "YELLOW" : "GREEN");
            } else {
                // Wochentag: mehr Grün
                status = Math.random() < 0.7 ? "GREEN" : (Math.random() < 0.5 ? "YELLOW" : "RED");
            }
            
            // Balance berechnen (Ratio 2:3)
            balance += switch (status) {
                case "GREEN" -> 2;
                case "YELLOW" -> 0;
                case "RED" -> -3;
                default -> 0;
            };
            
            data.add(new AlcoholDayData(current, status, balance));
            current = current.plusDays(1);
        }
        
        return data;
    }
    
    private BarChart<String, Number> createChart(List<AlcoholDayData> data, int categoryGap) {
        // Achsen erstellen
        CategoryAxis xAxis = new CategoryAxis();
        ObservableList<String> categories = FXCollections.observableArrayList(
            data.stream().map(d -> d.date().toString()).toList()
        );
        xAxis.setCategories(categories);
        xAxis.setTickLabelRotation(-45);
        
        // Y-Achse: Balance von min bis max
        int minBalance = data.stream().mapToInt(AlcoholDayData::balance).min().orElse(-10);
        int maxBalance = data.stream().mapToInt(AlcoholDayData::balance).max().orElse(10);
        int range = Math.max(Math.abs(minBalance), Math.abs(maxBalance)) + 5;
        
        NumberAxis yAxis = new NumberAxis(-range, range, 5);
        yAxis.setLabel("Kontostand");
        
        // BarChart erstellen
        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.setLegendVisible(false);
        chart.setCategoryGap(categoryGap);
        chart.setHorizontalGridLinesVisible(true);
        chart.setVerticalGridLinesVisible(false);
        
        // Daten hinzufügen
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        for (AlcoholDayData day : data) {
            series.getData().add(new XYChart.Data<>(day.date().toString(), day.balance()));
        }
        chart.getData().add(series);
        
        // Layout-Pass erzwingen, dann PseudoClasses setzen
        chart.layout();
        
        for (int i = 0; i < series.getData().size(); i++) {
            XYChart.Data<String, Number> dataPoint = series.getData().get(i);
            AlcoholDayData day = data.get(i);
            
            Node barNode = dataPoint.getNode();
            if (barNode != null) {
                // PseudoClass basierend auf Status
                barNode.pseudoClassStateChanged(GREEN, "GREEN".equals(day.status()));
                barNode.pseudoClassStateChanged(YELLOW, "YELLOW".equals(day.status()));
                barNode.pseudoClassStateChanged(RED, "RED".equals(day.status()));
            }
        }
        
        return chart;
    }
    
    private void applyCss(Scene scene) {
        String css = """
            .root {
                -fx-font-family: 'Arial';
                -fx-font-size: 14px;
                -fx-background-color: #1e1e1e;
            }
            
            .chart {
                -fx-background-color: #2b2b2b;
            }
            
            .chart-bar:green {
                -fx-bar-fill: #4caf50;
            }
            
            .chart-bar:yellow {
                -fx-bar-fill: #ffc107;
            }
            
            .chart-bar:red {
                -fx-bar-fill: #f44336;
            }
            
            .chart .axis {
                -fx-tick-label-fill: white;
            }
            
            .chart .axis-label {
                -fx-text-fill: white;
            }
            
            .chart-plot-background {
                -fx-background-color: #1e1e1e;
            }
            
            .date-picker {
                -fx-background-color: #3c3c3c;
            }
            
            .spinner {
                -fx-background-color: #3c3c3c;
            }
            
            /* Month/Year Header im DatePicker-Popup */
.date-picker-popup .month-year-pane {
    -fx-background-color: #ff0000;  /* Knallrot zum Testen */
}

.date-picker-popup .month-year-pane .label {
    -fx-text-fill: #00ff00;  /* Knallgrün zum Testen */
    -fx-font-size: 20px;
    -fx-font-weight: bold;
}

.date-picker-popup .spinner {
    -fx-background-color: #0000ff;  /* Knallblau zum Testen */
}

.date-picker-popup .spinner .button {
    -fx-background-color: yellow;
}
            """;
        
        String encoded = css.replace("%", "%25").replace("#", "%23");
        scene.getStylesheets().add("data:text/css," + encoded);
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}