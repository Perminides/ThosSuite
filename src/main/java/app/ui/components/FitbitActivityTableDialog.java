package app.ui.components;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import app.fitbit.json.Activity;
import app.fitbit.json.ActivityDaySummary;
import app.ui.skin.SkinService;
import app.util.Log;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.converter.DoubleStringConverter;
import javafx.util.converter.IntegerStringConverter;

/**
 * Zeigt Fitbit-Activities in einer auto-sized TableView.
 * Benutzt eine eigene Stage für perfekte Kontrolle.
 * 
 * !Sofort (Das ist hingerotzt hier, das kann nicht so bleiben!):
 * 		- Das Styling im Skin der Table-View ist nicht perfekt
 * 		- Wir sollten hier KEINE eigene Stage nutzen sondern einen Dialog. Ja, und irgendwie den Dialog an der Höhe der Tabelle ausrichten. Wenn wir das für eine Stage hinbekommen, warum nicht für einen Diallog auch???
 * 		- Und die Titelzeile sollte schon EXTENDED sein, was wir bei Dialogs geschenkt bekommen würden :)
 * !Später: Also ein Dashboard mit Kennzahlen. Nur ein Überblick, keine Diagramme. Z. B. Durchschnitliche Fitbitpunkte pro Tag für diese Woche noch. Die aktuelle Alk-Zahl
 */
public class FitbitActivityTableDialog {
    
    private final Window parent;
    private final LocalDate date;
    private final List<Activity> activities;
    private final ActivityDaySummary daySummary;
    
    private static final int TIMEOUT_SECONDS = 5;
    
    private DialogResult result = null;
    
    public FitbitActivityTableDialog(Window parent, LocalDate date, List<Activity> activities, ActivityDaySummary daySummary) {
        this.parent = parent;
        this.date = date;
        this.activities = activities;
        this.daySummary = daySummary;
    }
    
    /**
     * Zeigt den Dialog mit auto-sized TableView.
     */
    public Optional<DialogResult> showAndWait() {
        // 1. TableView erstellen
        TableView<ActivityRow> tableView = createTableView();
        tableView.getStyleClass().add("my-table-view");
        
        // 2. Daten laden
        ObservableList<ActivityRow> data = convertToTableData();
        tableView.setItems(data);
        
        // 3. Stage erstellen
        Stage stage = new Stage();
        stage.setTitle("Aktivitäten bearbeiten vom " + date);
        stage.initModality(Modality.APPLICATION_MODAL);
        if (parent != null) {
            stage.initOwner(parent);
        }
        
        // 4. Buttons erstellen
        Button btnOk = new Button("OK");
        Button btnCancel = new Button("Abbrechen");
        
        btnOk.setOnAction(e -> {
            result = convertFromTableData(tableView.getItems());
            stage.close();
        });
        
        btnCancel.setOnAction(e -> {
            result = null;
            stage.close();
        });
        
        // Button-Layout
        HBox buttonBar = new HBox(10);
        buttonBar.setAlignment(Pos.CENTER_RIGHT);
        buttonBar.setPadding(new Insets(10));
        buttonBar.getChildren().addAll(btnOk, btnCancel);
        
        // 5. DELETE-Handler für TableView
        tableView.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.DELETE || event.getCode() == KeyCode.BACK_SPACE) {
                ActivityRow selected = tableView.getSelectionModel().getSelectedItem();
                if (selected != null && !selected.isStepsRow()) {
                    tableView.getItems().remove(selected);
                }
            }
        });
        
        // 6. Layout zusammenbauen
        VBox root = new VBox();
        root.getStyleClass().add("my-dialog-vbox");
        VBox.setVgrow(tableView, Priority.ALWAYS);
        root.getChildren().addAll(tableView, buttonBar);
        
        Scene scene = new Scene(root, -1, -1);
        
        // Styling anwenden
        SkinService.get().styleScene(scene);
        
        stage.setScene(scene);
        
        // 7. Auto-Sizing durchführen (Binary Search)
        AtomicInteger currentWidth = new AtomicInteger(750);
        AtomicInteger upperBound = new AtomicInteger(2000);
        AtomicInteger lowerBound = new AtomicInteger(currentWidth.get() - 10);
        
        stage.setWidth(currentWidth.get());
        root.layout();
        root.applyCss();
        stage.setOpacity(0);
        
        final long start = System.currentTimeMillis();
        
        final double[] lastTableViewWidth = {-2};
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        ScheduledFuture<?> timeout = executor.schedule(() -> {
            Platform.runLater(() -> {
                stage.close();
                Log.warn(this, "Auto-Sizing Timeout nach " + TIMEOUT_SECONDS + " Sekunden");
                executor.shutdown();
            });
        }, TIMEOUT_SECONDS, TimeUnit.SECONDS);
        
        Runnable[] listenerRef = new Runnable[1];
        listenerRef[0] = () -> {
            double currentTableViewWidth = tableView.getWidth();
            
            if (Math.abs(currentTableViewWidth - lastTableViewWidth[0]) < 1) {
                return;
            }
            
            lastTableViewWidth[0] = currentTableViewWidth;
            ScrollBar sb = getHorizontalScrollbar(tableView);
            
            if (upperBound.get() - lowerBound.get() <= 2) {
                timeout.cancel(false);
                executor.shutdown();
                stage.setWidth(upperBound.get());
                scene.removePostLayoutPulseListener(listenerRef[0]);
                stage.setOpacity(1);
            } else {
                if (sb != null && sb.isVisible()) {
                    lowerBound.set(currentWidth.get());
                } else {
                    upperBound.set(currentWidth.get());
                }
                
                currentWidth.set((lowerBound.get() + upperBound.get()) / 2);
                stage.setWidth(currentWidth.get());
            }
        };
        
        scene.addPostLayoutPulseListener(listenerRef[0]);
        
        // 8. Stage zeigen und auf Schließen warten
        stage.showAndWait();
        
        // 9. Ergebnis zurückgeben
        if (result != null) {
            return Optional.of(result);
        }
        return Optional.empty();
    }
    
    /**
     * Erstellt die TableView mit allen Spalten.
     */
    @SuppressWarnings("unchecked")
    private TableView<ActivityRow> createTableView() {
        TableView<ActivityRow> tableView = new TableView<>();
        tableView.setPrefWidth(TableView.USE_COMPUTED_SIZE);
        tableView.setPrefHeight(TableView.USE_COMPUTED_SIZE);
        tableView.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        tableView.setEditable(true);
        tableView.getStyleClass().add("fitbit-activity-table");
        
        // Spalten erstellen
        TableColumn<ActivityRow, String> startTimeCol = new TableColumn<>("StartTime");
        startTimeCol.setCellValueFactory(new PropertyValueFactory<>("startTime"));
        startTimeCol.setCellFactory(TextFieldTableCell.forTableColumn());
        startTimeCol.setEditable(true);
        startTimeCol.setSortable(false);
        
        TableColumn<ActivityRow, String> nameCol = new TableColumn<>("ActivityName");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("activityName"));
        nameCol.setCellFactory(TextFieldTableCell.forTableColumn());
        nameCol.setEditable(true);
        nameCol.setSortable(false);
        
        TableColumn<ActivityRow, String> unitCol = new TableColumn<>("DistanceUnit");
        unitCol.setCellValueFactory(new PropertyValueFactory<>("distanceUnit"));
        unitCol.setCellFactory(TextFieldTableCell.forTableColumn());
        unitCol.setEditable(true);
        unitCol.setSortable(false);
        
        TableColumn<ActivityRow, Double> distanceCol = new TableColumn<>("Distance");
        distanceCol.setCellValueFactory(new PropertyValueFactory<>("distance"));
        distanceCol.setCellFactory(TextFieldTableCell.forTableColumn(new DoubleStringConverter()));
        distanceCol.setEditable(true);
        distanceCol.setSortable(false);
        
        TableColumn<ActivityRow, Integer> stepsCol = new TableColumn<>("Steps");
        stepsCol.setCellValueFactory(new PropertyValueFactory<>("steps"));
        stepsCol.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter()));
        stepsCol.setEditable(true);
        stepsCol.setSortable(false);
        
        tableView.getColumns().addAll(startTimeCol, nameCol, unitCol, distanceCol, stepsCol);
        
        return tableView;
    }
    
    /**
     * Konvertiert Activities + DaySummary in TableView-Datenmodell.
     */
    private ObservableList<ActivityRow> convertToTableData() {
        ObservableList<ActivityRow> data = FXCollections.observableArrayList();
        
        // Steps-Zeile (synthetisch)
        data.add(new ActivityRow(
            "(gesamt)",
            "Steps",
            "",
            null,
            daySummary.getSummary().getSteps(),
            true
        ));
        
        // Echte Activities
        for (Activity activity : activities) {
            data.add(new ActivityRow(
                activity.getStartTime(),
                activity.getActivityName(),
                activity.getDistanceUnit() != null ? activity.getDistanceUnit() : "",
                activity.getDistance(),
                activity.getSteps(),
                false
            ));
        }
        
        return data;
    }
    
    /**
     * Konvertiert TableView-Daten zurück in Activities + korrigierte Gesamtschritte.
     */
    private DialogResult convertFromTableData(ObservableList<ActivityRow> rows) {
        ActivityRow stepsRow = rows.get(0);
        int correctedTotalSteps = stepsRow.getSteps();
        
        List<Activity> editedActivities = new ArrayList<>();
        
        for (int i = 1; i < rows.size(); i++) {
            ActivityRow row = rows.get(i);
            Activity original = activities.get(i - 1);
            
            Activity edited = new Activity(
                row.getStartTime(),
                row.getActivityName(),
                row.getDistanceUnit().isEmpty() ? null : row.getDistanceUnit(),
                row.getDistance(),
                row.getSteps()
            );
            edited.setOriginalStartTime(original.getOriginalStartTime());
            
            editedActivities.add(edited);
        }
        
        return new DialogResult(editedActivities, correctedTotalSteps);
    }
    
    /**
     * Findet die horizontale ScrollBar in der TableView.
     */
    private static ScrollBar getHorizontalScrollbar(TableView<?> table) {
        for (Node n : table.lookupAll(".scroll-bar")) {
            if (n instanceof ScrollBar bar) {
                if (bar.getOrientation() == Orientation.HORIZONTAL) {
                    return bar;
                }
            }
        }
        return null;
    }
    
    /**
     * Datenmodell für eine Zeile in der TableView.
     */
    public static class ActivityRow {
        private String startTime;
        private String activityName;
        private String distanceUnit;
        private Double distance;
        private Integer steps;
        private final boolean isStepsRow;
        
        public ActivityRow(String startTime, String activityName, String distanceUnit, 
                          Double distance, Integer steps, boolean isStepsRow) {
            this.startTime = startTime;
            this.activityName = activityName;
            this.distanceUnit = distanceUnit;
            this.distance = distance;
            this.steps = steps;
            this.isStepsRow = isStepsRow;
        }
        
        public String getStartTime() { return startTime; }
        public void setStartTime(String startTime) { this.startTime = startTime; }
        
        public String getActivityName() { return activityName; }
        public void setActivityName(String activityName) { this.activityName = activityName; }
        
        public String getDistanceUnit() { return distanceUnit; }
        public void setDistanceUnit(String distanceUnit) { this.distanceUnit = distanceUnit; }
        
        public Double getDistance() { return distance; }
        public void setDistance(Double distance) { this.distance = distance; }
        
        public Integer getSteps() { return steps; }
        public void setSteps(Integer steps) { this.steps = steps; }
        
        public boolean isStepsRow() { return isStepsRow; }
    }
    
    /**
     * Ergebnis des Dialogs.
     */
    public record DialogResult(List<Activity> activities, int totalSteps) {}
}