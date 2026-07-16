package app.shared.ui.components.fitbit;

import java.time.LocalDate;
import java.util.List;
import java.util.function.BiConsumer;

import app.shared.ScreenView;
import app.shared.skin.SkinService;
import app.shared.ui.components.fitbit.WeekPointsChartData.State;
import app.shared.ui.components.fitbit.WeekPointsChartData.Week;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.scene.Node;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
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

/**
 * Framework-gebundene Hälfte: zeichnet eine {@link WeekPointsChartData} als Balken plus
 * Ziellinie. Kennt kein Fitbit, keine Repository, keine Fachlogik — die Daten holt sie
 * über einen {@link WeekPointsDataProvider}.
 * 
 * !Sofort: Bitte das Naming überall gerade ziehen. Soll die wirklich ...Pane heißen?
 * 
 */
public class WeekPointsChartPane implements ScreenView {
	
	private StackPane pane = new StackPane(); 

    private static final PseudoClass ACHIEVED = PseudoClass.getPseudoClass("achieved");
    private static final PseudoClass FAILED = PseudoClass.getPseudoClass("failed");
    private static final PseudoClass IN_PROGRESS = PseudoClass.getPseudoClass("in-progress");

	private DatePicker fromPicker;
    private DatePicker toPicker;
    private WeekPointsChartData currentChartData;
    private Spinner<Integer> gapSpinner;
    private BiConsumer<LocalDate, LocalDate> onDateChange;
    
    public WeekPointsChartPane() {
    	pane.getStyleClass().add("chart-root");	
    }

    public void setData(WeekPointsChartData chartData) {
    	this.currentChartData = chartData;
    	buildView(chartData);
    }
    
    public void onDateChange(BiConsumer<LocalDate, LocalDate> consumer) {
    	this.onDateChange = consumer;
    }
    
    public WeekPointsChartPane getView() {
		return this;
	}

    private void buildView(WeekPointsChartData chartData) {
    	pane.getChildren().clear();
    	// !Sofort: Sollte der Screen nicht entscheiden, welchen Hintergrund seine View bekommen soll?
    	// An sich vielleicht schon, aber da diese Komponente sich ja eh eigenständig zusammenbaut, finde
    	// ich es auch konsequent, wenn sie den Background setzt tbh
    	pane.setBackground(new Background(SkinService.get().getEmptyBackgroundImage()));

        VBox container = new VBox();
        container.getStyleClass().add("chart-container");
        container.getChildren().add(createControlsBar());

        StackPane chartStack = createChart(chartData);
        container.getChildren().add(chartStack);
        VBox.setVgrow(chartStack, Priority.ALWAYS);

        pane.getChildren().add(container);
    }

    private HBox createControlsBar() {
        HBox controls = new HBox();
        controls.getStyleClass().add("chart-controls");

        Label fromLabel = new Label("Von:");
        fromPicker = SkinService.get().createDatePicker(java.time.LocalDate.now().minusYears(2));
        fromPicker.setOnAction(_ -> onDateChange.accept(fromPicker.getValue(), toPicker.getValue()));

        Label toLabel = new Label("Bis:");
        toPicker = SkinService.get().createDatePicker(java.time.LocalDate.now());
        toPicker.setOnAction(_ -> onDateChange.accept(fromPicker.getValue(), toPicker.getValue()));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label gapLabel = new Label("Balkenabstand:");
        gapSpinner = new Spinner<>(0, 20, 0);
        gapSpinner.setEditable(true);
        gapSpinner.valueProperty().addListener((_, _, _) -> updateChart());

        controls.getChildren().addAll(fromLabel, fromPicker, toLabel, toPicker, spacer, gapLabel, gapSpinner);
        return controls;
    }

    private StackPane createChart(WeekPointsChartData chartData) {
        List<Week> weeks = chartData.weeks();
        int categoryGap = gapSpinner.getValue();

        if (weeks.isEmpty()) {
            return createEmptyChartStack();
        }

        CategoryAxis xAxis = new CategoryAxis();
        ObservableList<String> categories = FXCollections.observableArrayList(
            weeks.stream().map(w -> w.weekStart().toString()).toList()
        );
        xAxis.setCategories(categories);
        xAxis.setTickLabelRotation(-45);

        NumberAxis yAxis = new NumberAxis(0, chartData.yMax(), 500);

        BarChart<String, Number> barChart = new BarChart<>(xAxis, yAxis);
        barChart.setLegendVisible(false);
        barChart.setCategoryGap(categoryGap);
        barChart.setHorizontalGridLinesVisible(false);
        barChart.setVerticalGridLinesVisible(false);

        XYChart.Series<String, Number> barSeries = new XYChart.Series<>();
        for (Week week : weeks) {
            barSeries.getData().add(new XYChart.Data<>(week.weekStart().toString(), week.points()));
        }
        barChart.getData().add(barSeries);

        LineChart<String, Number> lineChart = new LineChart<>(xAxis, yAxis);
        lineChart.setCreateSymbols(false);
        lineChart.setLegendVisible(false);
        lineChart.setMouseTransparent(true);
        lineChart.setHorizontalGridLinesVisible(false);
        lineChart.setVerticalGridLinesVisible(false);

        XYChart.Series<String, Number> lineSeries = new XYChart.Series<>();
        for (Week week : weeks) {
            lineSeries.getData().add(new XYChart.Data<>(week.weekStart().toString(), week.goal()));
        }
        lineChart.getData().add(lineSeries);

        lineChart.setStyle("-fx-background-color: transparent;");
        lineChart.lookup(".chart-plot-background").setStyle("-fx-background-color: transparent;");

        barChart.layout();

        for (int i = 0; i < barSeries.getData().size(); i++) {
            Node barNode = barSeries.getData().get(i).getNode();
            if (barNode == null) {
                continue;
            }
            Week week = weeks.get(i);
            barNode.pseudoClassStateChanged(ACHIEVED, week.state() == State.ACHIEVED);
            barNode.pseudoClassStateChanged(FAILED, week.state() == State.FAILED);
            barNode.pseudoClassStateChanged(IN_PROGRESS, week.state() == State.IN_PROGRESS);

            String tooltipText = week.weekStart() + " : " + week.points();
            if (week.remark() != null && !week.remark().isEmpty()) {
                tooltipText = tooltipText + "\n" + week.remark();
            }
            Tooltip.install(barNode, new Tooltip(tooltipText));
        }

        StackPane stack = new StackPane();
        stack.getChildren().addAll(barChart, lineChart);
        return stack;
    }

    private void updateChart() {
        VBox container = (VBox) pane.getChildren().get(0);
        if (container.getChildren().size() > 1) {
            container.getChildren().remove(1);
        }
        StackPane chartStack = createChart(currentChartData);
        container.getChildren().add(chartStack);
        VBox.setVgrow(chartStack, Priority.ALWAYS);
    }

    private StackPane createEmptyChartStack() {
        BarChart<String, Number> emptyChart = new BarChart<>(new CategoryAxis(), new NumberAxis());
        StackPane stack = new StackPane();
        stack.getChildren().add(emptyChart);
        return stack;
    }

	@Override
	public Pane getPane() {
		return pane;
	}
}