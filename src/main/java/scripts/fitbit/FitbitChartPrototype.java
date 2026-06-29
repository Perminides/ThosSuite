package scripts.fitbit;

import app.shared.Config;
import app.shared.skin.SkinService;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class FitbitChartPrototype extends Application {
    
    @Override
    public void start(Stage stage) {
    	
    	Config.init("C:/Users/Markgraf/OneDrive/ThosSuite/");
    	
        // GEMEINSAME Achsen (wichtig!)
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis(0, 5000, 500);
        
        // BarChart für die Balken
        BarChart<String, Number> barChart = new BarChart<>(xAxis, yAxis);
        barChart.setLegendVisible(false);
        
        XYChart.Series<String, Number> barSeries = new XYChart.Series<>();
        barSeries.getData().add(new XYChart.Data<>("W1", 3500));
        barSeries.getData().add(new XYChart.Data<>("W2", 4200));
        barSeries.getData().add(new XYChart.Data<>("W3", 3800));
        barSeries.getData().add(new XYChart.Data<>("W4", 4500));
        barSeries.getData().add(new XYChart.Data<>("W5", 3900));
        barSeries.getData().add(new XYChart.Data<>("W6", 4100));
        barSeries.getData().add(new XYChart.Data<>("W7", 3700));
        barSeries.getData().add(new XYChart.Data<>("W8", 4300));
        barSeries.getData().add(new XYChart.Data<>("W9", 3600));
        barSeries.getData().add(new XYChart.Data<>("W10", 4400));
        barChart.getData().add(barSeries);
        
        // LineChart für die Ziellinie
        //CategoryAxis xAxis2 = new CategoryAxis();
        //NumberAxis yAxis2 = new NumberAxis(0, 5000, 500);
        xAxis.setTickLabelRotation(-45);
        //xAxis2.setTickLabelRotation(-45);
        
        
        LineChart<String, Number> lineChart = new LineChart<>(xAxis, yAxis);
        lineChart.setCreateSymbols(false); // Keine Punkte auf der Linie
        lineChart.setLegendVisible(false);
        lineChart.setMouseTransparent(true); // Maus-Events durchreichen
        
        barChart.setHorizontalGridLinesVisible(false);
        barChart.setVerticalGridLinesVisible(false);
        lineChart.setHorizontalGridLinesVisible(false);
        lineChart.setVerticalGridLinesVisible(false);
        
        XYChart.Series<String, Number> lineSeries = new XYChart.Series<>();
        lineSeries.getData().add(new XYChart.Data<>("2025-01-08", 4000));
        lineSeries.getData().add(new XYChart.Data<>("W2", 4000));
        lineSeries.getData().add(new XYChart.Data<>("W3", 4000));
        lineSeries.getData().add(new XYChart.Data<>("W4", 4000));
        lineSeries.getData().add(new XYChart.Data<>("W5", 4000));
        lineSeries.getData().add(new XYChart.Data<>("W6", 4500)); // Sprung!
        lineSeries.getData().add(new XYChart.Data<>("W7", 4500));
        lineSeries.getData().add(new XYChart.Data<>("W8", 4500));
        lineSeries.getData().add(new XYChart.Data<>("W9", 4500));
        lineSeries.getData().add(new XYChart.Data<>("W10", 4500));
        lineChart.getData().add(lineSeries);
        
        // LineChart transparent machen (nur Linie sichtbar)
        lineChart.setStyle("-fx-background-color: transparent;");
        lineChart.lookup(".chart-plot-background").setStyle("-fx-background-color: transparent;");
        
        // Beide Charts übereinander
        StackPane stack = new StackPane(barChart, lineChart);
        
        Scene scene = new Scene(stack, 800, 600);
        stage.setScene(scene);
        stage.setTitle("Fitbit Chart Prototype");
        SkinService.get().styleScene(scene);
        stage.show();
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}