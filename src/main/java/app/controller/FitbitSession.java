package app.controller;

import java.time.LocalDate;
import java.util.List;

import app.config.Config;
import app.data.Deck;
import app.data.SessionSwitchStrategy;
import app.data.persistence.FitbitRepository;
import app.fitbit.FitbitGoalHistoryEntry;
import app.fitbit.FitbitWeekData;
import app.ui.skin.SkinService;
import app.util.Log;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.scene.Node;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Background;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;

/**
 * !Architektur: Also im controller-Paket so heavy UI-Krams? Really?
 * !Architektur: Sollte der Großteil des UI-Codes nicht in einer create-methode im Skin liegen und Du übergibst dem nur Daten? Andererseits, musst Du die Daten ja auch ändern können. Muss man mal überlegen. Ist schon ein Designbruch hier!
 * 
 * Zeigt ein Diagramm mit den Fitbit-Wochenpunkten der letzten N Wochen.
 * 
 * - BarChart zeigt die erreichten Punkte pro Woche
 * - LineChart zeigt das jeweils gültige Wochenziel als durchgezogene Linie
 * - Balken werden via PseudoClass :achieved eingefärbt (Ziel erreicht/nicht erreicht)
 * - Beide Charts teilen sich dieselben Achsen für perfekte Überlagerung
 * !Sofort: Sollten DashboardSession und FitbitSession nicht analog funktionieren? Und / Oder sollte das hier eine genrische DiagramSession werden?
 */
public class FitbitSession implements Session {

    private static final PseudoClass ACHIEVED = PseudoClass.getPseudoClass("achieved");
    private static final PseudoClass FAILED = PseudoClass.getPseudoClass("failed");
    
    private final FitbitRepository repository;
    private final int weeksToShow;
    
    private StackPane view;

    public FitbitSession() {
        this.repository = new FitbitRepository();
        this.weeksToShow = Config.get("fitbitWeeksToShow") == null ? 30 : Integer.parseInt(Config.get("fitbitWeeksToShow")); // Default: 30 Wochen
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
    public void start() {
        Log.info(this, "FitbitSession gestartet");
    }

    @Override
    public void refresh() {
    	view.setBackground(new Background(SkinService.get().getBackgroundImage(Deck.WORLD_CARDS)));
        //view = buildView();
    }

    @Override
    public SessionSwitchStrategy getSwitchStrategy() {
        return SessionSwitchStrategy.IMMEDIATE;
    }

    @Override
    public void escClicked() {
        // Keine Aktion - Session kann einfach verlassen werden
    }
    
    private StackPane buildView() {
    	view.getChildren().clear(); // WICHTIG: Alte Inhalte entfernen
    	
        // 1. Daten laden
        List<FitbitWeekData> weeks = repository.getLastNWeeks(weeksToShow);
        List<FitbitGoalHistoryEntry> goalHistory = repository.getAllGoalHistory();
        
        if (weeks.isEmpty()) {
            Log.warn(this, "Keine Fitbit-Daten vorhanden");
            return createEmptyView();
        }
        
     // 2. Gemeinsame Achsen erstellen
        CategoryAxis xAxis = new CategoryAxis();

        // NEU: Kategorien explizit setzen
        ObservableList<String> categories = FXCollections.observableArrayList(
            weeks.stream().map(w -> w.weekStart().toString()).toList()
        );
        xAxis.setCategories(categories);
        xAxis.setTickLabelRotation(-45); // Gibt es so in der Form nicht als CSS. 

        int maxPoints = weeks.stream().mapToInt(FitbitWeekData::points).max().orElse(5000);
        int maxGoal = goalHistory.stream().mapToInt(FitbitGoalHistoryEntry::weeklyGoal).max().orElse(4000);
        int yMax = Math.max(maxPoints, maxGoal) + 500;

        NumberAxis yAxis = new NumberAxis(0, yMax, 500);
        
        // 3. BarChart erstellen
        BarChart<String, Number> barChart = new BarChart<>(xAxis, yAxis);
        barChart.setLegendVisible(false);
        //barChart.setCategoryGap(2);  // Hier im Java-Code setzen
        barChart.setHorizontalGridLinesVisible(false);
        barChart.setVerticalGridLinesVisible(false);
        
        XYChart.Series<String, Number> barSeries = new XYChart.Series<>();
        for (FitbitWeekData week : weeks) {
            barSeries.getData().add(new XYChart.Data<>(week.weekStart().toString(), week.points()));
        }
        barChart.getData().add(barSeries);
        
        // 4. LineChart erstellen (Ziellinie)
        LineChart<String, Number> lineChart = new LineChart<>(xAxis, yAxis);
        lineChart.setCreateSymbols(false);
        lineChart.setLegendVisible(false);
        lineChart.setMouseTransparent(true);
        lineChart.setHorizontalGridLinesVisible(false);
        lineChart.setVerticalGridLinesVisible(false);
        
        XYChart.Series<String, Number> lineSeries = new XYChart.Series<>();
        for (FitbitWeekData week : weeks) {
            int goalForWeek = findGoalForDate(week.weekStart(), goalHistory);
            lineSeries.getData().add(new XYChart.Data<>(week.weekStart().toString(), goalForWeek));
        }
        lineChart.getData().add(lineSeries);
        
        // 5. LineChart transparent machen (nur Linie sichtbar)
        lineChart.setStyle("-fx-background-color: transparent;");
        lineChart.lookup(".chart-plot-background").setStyle("-fx-background-color: transparent;"); // !Sofort. Äh, was geschieht hier? Das sieht aber fishy aus!
        
        // 6. PseudoClass auf Balken setzen (nach Rendering)
        barChart.layout(); // Layout-Pass erzwingen
        
        for (int i = 0; i < barSeries.getData().size(); i++) {
            XYChart.Data<String, Number> data = barSeries.getData().get(i);
            FitbitWeekData week = weeks.get(i);
            int goalForWeek = findGoalForDate(week.weekStart(), goalHistory);
            
            Node barNode = data.getNode();
            if (barNode != null) {
                boolean achieved = week.points() >= goalForWeek;
                barNode.pseudoClassStateChanged(ACHIEVED, achieved);
                barNode.pseudoClassStateChanged(FAILED, !achieved);
            }
            
            String tooltipText = week.weekStart().toString() + " : " + week.points();
            Tooltip tooltip = new Tooltip(tooltipText);
            Tooltip.install(barNode, tooltip);
        }
        
        // 7. Stack zusammenbauen
        view.getChildren().addAll(barChart, lineChart);
        Platform.runLater(() -> xAxis.setTickLabelRotation(-45));
        view.setBackground(new Background(SkinService.get().getBackgroundImage(Deck.WORLD_CARDS)));
        System.out.println("Rotation gesetzt auf: " + xAxis.getTickLabelRotation());
        
        return view;
    }
    
    /**
     * Findet das gültige Wochenziel für ein bestimmtes Datum.
     * Sucht den neuesten Eintrag mit validFrom <= date.
     */
    private int findGoalForDate(LocalDate date, List<FitbitGoalHistoryEntry> history) {
        return history.stream()
            .filter(entry -> !entry.validFrom().isAfter(date))
            .reduce((_, second) -> second) // Letzter passender Eintrag
            .map(FitbitGoalHistoryEntry::weeklyGoal)
            .orElseThrow(() -> new RuntimeException("Kein Fitbit-Ziel gefunden für " + date));
    }
    
    private StackPane createEmptyView() {
        StackPane pane = new StackPane();
        pane.setBackground(new Background(SkinService.get().getBackgroundImage(null)));
        return pane;
    }

	@Override
	public void closeSilent(boolean save) {
		// TODO Auto-generated method stub
	}
}