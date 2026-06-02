package app.alc;

import java.time.LocalDate;
import java.util.List;

import app.alc.model.DayEntry;
import app.alc.model.RatioEntry;
import app.alc.repository.Repository;
import app.shared.Log;
import app.shared.Screen;
import app.shared.model.SessionSwitchStrategy;
import app.ui.skin.SkinService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.scene.Node;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Background;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class AlcStatisticsScreen implements Screen {

	   private static final PseudoClass ACHIEVED = PseudoClass.getPseudoClass("achieved");
	   private static final PseudoClass FAILED = PseudoClass.getPseudoClass("failed");
    
    private final Repository repository;
    
    private StackPane view;
    private DatePicker fromPicker;
    private DatePicker toPicker;
    private Spinner<Integer> gapSpinner;
    private BarChart<String, Number> chart;

    public AlcStatisticsScreen() {
        this.repository = new Repository();
    }

    @Override
    public Pane getView() {
        if (view == null) {
            view = new StackPane();
            view.getStyleClass().add("chart-root");
            buildView();
        }
        return view;
    }

    @Override
    public void refresh() {
        buildView();
    }

    @Override
    public SessionSwitchStrategy getSwitchStrategy() {
        return SessionSwitchStrategy.IMMEDIATE;
    }

    private void buildView() {
        view.getChildren().clear();
        view.setBackground(new Background(SkinService.get().getBackgroundImage()));
        
        VBox container = new VBox();
        container.getStyleClass().add("chart-container");
        
        // Controls oben
        HBox controls = createControlsBar();
        container.getChildren().add(controls);
        
        // Chart unten
        chart = createChart();
        container.getChildren().add(chart);
        VBox.setVgrow(chart, Priority.ALWAYS);
        
        view.getChildren().add(container);
    }

    private HBox createControlsBar() {
        HBox controls = new HBox();
        controls.getStyleClass().add("chart-controls");
        
        // DatePicker "Von"
        Label fromLabel = new Label("Von:");
        fromPicker = SkinService.get().createDatePicker(LocalDate.now().minusDays(365));
        fromPicker.setOnAction(_ -> updateChart());
        
        // DatePicker "Bis"
        Label toLabel = new Label("Bis:");
        toPicker = SkinService.get().createDatePicker(LocalDate.now());
        toPicker.setOnAction(_ -> updateChart());
        
        // Spacer
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        // Spinner für category-gap
        Label gapLabel = new Label("Balkenabstand:");
        gapSpinner = new Spinner<>(0, 20, 0);
        gapSpinner.setEditable(true);
        gapSpinner.valueProperty().addListener((_, _, _) -> updateChart());
        
        controls.getChildren().addAll(
            fromLabel, fromPicker,
            toLabel, toPicker,
            spacer,
            gapLabel, gapSpinner
        );
        
        return controls;
    }

    private BarChart<String, Number> createChart() {
        LocalDate from = fromPicker.getValue();
        LocalDate to = toPicker.getValue();
        int categoryGap = gapSpinner.getValue();
        
        // Daten laden
        List<DayEntry> data = repository.getDaysInRange(from, to);
        List<RatioEntry> ratios = repository.getAllRatios();
        
        if (data.isEmpty()) {
            Log.warn(this, "Keine Alkohol-Daten vorhanden");
            return createEmptyChart();
        }
        
        // Achsen erstellen
        CategoryAxis xAxis = new CategoryAxis();
        // Kategorien explizit setzen, ohne hat die Drehung nicht funktioniert...
        ObservableList<String> categories = FXCollections.observableArrayList(
            data.stream().map(d -> d.date().toString()).toList()
        );
        xAxis.setCategories(categories);
        xAxis.setTickLabelRotation(-45);  // JETZT funktioniert es!
        
        NumberAxis yAxis = new NumberAxis();
        yAxis.setAutoRanging(true);
        yAxis.setLabel("Kontostand");
        
        // BarChart erstellen
        BarChart<String, Number> newChart = new BarChart<>(xAxis, yAxis);
        newChart.setLegendVisible(false);
        newChart.setCategoryGap(categoryGap);
        newChart.setHorizontalGridLinesVisible(true);
        newChart.setVerticalGridLinesVisible(false);
        
        // Daten hinzufügen
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        for (DayEntry day : data) {
            series.getData().add(new XYChart.Data<>(day.date().toString(), day.balance()));
        }
        newChart.getData().add(series);
        
        
        // Layout-Pass erzwingen, dann PseudoClasses + Tooltips setzen
        newChart.layout();
        
        for (int i = 0; i < series.getData().size(); i++) {
            XYChart.Data<String, Number> dataPoint = series.getData().get(i);
            DayEntry day = data.get(i);
            
            Node barNode = dataPoint.getNode();
            if (barNode != null) {
            	   boolean isPositive = day.balance() > 0;
            	   boolean isNegative = day.balance() < 0;
            	   barNode.pseudoClassStateChanged(ACHIEVED, isPositive);
            	   barNode.pseudoClassStateChanged(FAILED, isNegative);
                
                // Tooltip mit Ratio
                RatioEntry ratio = getRatioForDate(day.date(), ratios);
                String tooltipText = String.format("%s | %+d | %d:%d:%d",
                    day.date(), 
                    day.balance(),
                    ratio.greenPoints(),
                    ratio.yellowPoints(),
                    ratio.redPoints()
                );
                Tooltip tooltip = new Tooltip(tooltipText);
                Tooltip.install(barNode, tooltip);
            }
        }
        return newChart;
    }

    private void updateChart() {
        if (chart != null && view != null) {
            // Alten Chart entfernen
            VBox container = (VBox) view.getChildren().get(0);
            container.getChildren().remove(chart);
            
            // Neuen Chart erstellen
            chart = createChart();
            container.getChildren().add(chart);
            VBox.setVgrow(chart, Priority.ALWAYS);
        }
    }

    private BarChart<String, Number> createEmptyChart() {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        return new BarChart<>(xAxis, yAxis);
    }

    /**
     * Findet die gültige Ratio für ein bestimmtes Datum.
     */
    private RatioEntry getRatioForDate(LocalDate date, List<RatioEntry> ratios) {
        RatioEntry current = null;
        
        for (RatioEntry ratio : ratios) {
            if (!ratio.validFrom().isAfter(date)) {
                current = ratio;
            } else {
                break;
            }
        }
        
        if (current == null) {
            throw new RuntimeException("Keine Ratio gefunden für " + date);
        }
        
        return current;
    }
}