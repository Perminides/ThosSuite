package app.shared.ui.surfaces;

import java.time.LocalDate;

import app.shared.model.BarChartData;
import app.shared.model.BarChartData.Bar;
import app.shared.model.BarChartData.State;
import app.shared.model.BarChartData.YAxis;
import app.shared.model.BarChartDataProvider;
import app.shared.skin.SkinService;
import app.shared.ui.contracts.ScreenView;
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
 * Framework-gebundene, feature-neutrale Hälfte: zeichnet eine {@link BarChartData}
 * als Balken plus optionale Ziellinie. Kennt kein Feature, keine Repository, keine
 * Fachlogik — die Daten holt sie über einen {@link BarChartDataProvider}.
 */
public class BarChartScreenView implements ScreenView {

    private static final PseudoClass ACHIEVED    = PseudoClass.getPseudoClass("achieved");
    private static final PseudoClass FAILED      = PseudoClass.getPseudoClass("failed");
    private static final PseudoClass IN_PROGRESS = PseudoClass.getPseudoClass("in-progress");

    private final StackPane pane = new StackPane();
    private final BarChartDataProvider provider;

    private DatePicker fromPicker;
    private DatePicker toPicker;
    private Spinner<Integer> gapSpinner;
    private BarChartData currentData;
    private final LocalDate initialFrom;
    private final LocalDate initialTo;

    public BarChartScreenView(BarChartDataProvider provider, LocalDate initialFrom, LocalDate initialTo) {
        this.provider = provider;
        this.initialFrom = initialFrom;
        this.initialTo = initialTo;
        pane.getStyleClass().add("chart-root");
    }

    /** Lädt neu für den aktuellen Zeitraum und zeichnet. Vom Screen bei refresh gerufen. */
    public void reload() {
        LocalDate from = fromPicker != null ? fromPicker.getValue() : initialFrom;
        LocalDate to   = toPicker   != null ? toPicker.getValue()   : initialTo;
        currentData = provider.get(from, to);
        buildView();
    }

    private void buildView() {
        pane.getChildren().clear();
        pane.setBackground(new Background(SkinService.get().getEmptyBackgroundImage()));

        VBox container = new VBox();
        container.getStyleClass().add("chart-container");
        container.getChildren().add(createControlsBar());

        StackPane chartStack = createChart();
        container.getChildren().add(chartStack);
        VBox.setVgrow(chartStack, Priority.ALWAYS);

        pane.getChildren().add(container);
    }

    private HBox createControlsBar() {
        HBox controls = new HBox();
        controls.getStyleClass().add("chart-controls");

        Label fromLabel = new Label("Von:");
        if (fromPicker == null)
            fromPicker = SkinService.get().createDatePicker(initialFrom);
        fromPicker.setOnAction(_ -> reloadFromControls());

        Label toLabel = new Label("Bis:");
        if (toPicker == null)
            toPicker = SkinService.get().createDatePicker(initialTo);
        toPicker.setOnAction(_ -> reloadFromControls());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label gapLabel = new Label("Balkenabstand:");
        if (gapSpinner == null) {
            gapSpinner = new Spinner<>(0, 20, 0);
            gapSpinner.setEditable(true);
            gapSpinner.valueProperty().addListener((_, _, _) -> redrawChartOnly());
        }

        controls.getChildren().addAll(fromLabel, fromPicker, toLabel, toPicker, spacer, gapLabel, gapSpinner);
        return controls;
    }

    /** DatePicker geändert → Daten neu holen (Zeitraum hat sich geändert). */
    private void reloadFromControls() {
        currentData = provider.get(fromPicker.getValue(), toPicker.getValue());
        redrawChartOnly();
    }

    /** Nur den Chart neu zeichnen (Gap geändert, oder nach reloadFromControls). */
    private void redrawChartOnly() {
        VBox container = (VBox) pane.getChildren().get(0);
        if (container.getChildren().size() > 1)
            container.getChildren().remove(1);
        StackPane chartStack = createChart();
        container.getChildren().add(chartStack);
        VBox.setVgrow(chartStack, Priority.ALWAYS);
    }

    private StackPane createChart() {
        if (currentData == null || currentData.bars().isEmpty())
            return emptyChartStack();

        CategoryAxis xAxis = new CategoryAxis();
        ObservableList<String> categories = FXCollections.observableArrayList(
            currentData.bars().stream().map(Bar::label).toList());
        xAxis.setCategories(categories);
        xAxis.setTickLabelRotation(-45);

        NumberAxis yAxis = yAxisFor(currentData.yAxis());

        BarChart<String, Number> barChart = new BarChart<>(xAxis, yAxis);
        barChart.setLegendVisible(false);
        barChart.setCategoryGap(gapSpinner.getValue());
        barChart.setHorizontalGridLinesVisible(false);
        barChart.setVerticalGridLinesVisible(false);

        XYChart.Series<String, Number> barSeries = new XYChart.Series<>();
        for (Bar bar : currentData.bars())
            barSeries.getData().add(new XYChart.Data<>(bar.label(), bar.value()));
        barChart.getData().add(barSeries);

        barChart.layout();

        for (int i = 0; i < barSeries.getData().size(); i++) {
            Node barNode = barSeries.getData().get(i).getNode();
            if (barNode == null)
                continue;
            Bar bar = currentData.bars().get(i);
            barNode.pseudoClassStateChanged(ACHIEVED,    bar.state() == State.ACHIEVED);
            barNode.pseudoClassStateChanged(FAILED,      bar.state() == State.FAILED);
            barNode.pseudoClassStateChanged(IN_PROGRESS, bar.state() == State.IN_PROGRESS);
            Tooltip.install(barNode, new Tooltip(bar.tooltip()));
        }

        StackPane stack = new StackPane();
        stack.getChildren().add(barChart);

        // Optionale Ziellinie als transparentes LineChart-Overlay
        if (currentData.target() != null) {
            LineChart<String, Number> lineChart = new LineChart<>(xAxis, yAxis);
            lineChart.setCreateSymbols(false);
            lineChart.setLegendVisible(false);
            lineChart.setMouseTransparent(true);
            lineChart.setHorizontalGridLinesVisible(false);
            lineChart.setVerticalGridLinesVisible(false);

            XYChart.Series<String, Number> lineSeries = new XYChart.Series<>();
            var yValues = currentData.target().yValues();
            for (int i = 0; i < currentData.bars().size(); i++)
                lineSeries.getData().add(new XYChart.Data<>(currentData.bars().get(i).label(), yValues.get(i)));
            lineChart.getData().add(lineSeries);

            lineChart.setStyle("-fx-background-color: transparent;");
            lineChart.lookup(".chart-plot-background").setStyle("-fx-background-color: transparent;");
            stack.getChildren().add(lineChart);
        }

        return stack;
    }

    private NumberAxis yAxisFor(YAxis spec) {
        if (spec.autoRanging()) {
            NumberAxis axis = new NumberAxis();
            axis.setAutoRanging(true);
            return axis;
        }
        return new NumberAxis(0, spec.max(), spec.tick());
    }

    private StackPane emptyChartStack() {
        StackPane stack = new StackPane();
        stack.getChildren().add(new BarChart<>(new CategoryAxis(), new NumberAxis()));
        return stack;
    }

    @Override
    public Pane getPane() {
        return pane;
    }
}