package app.fitbit;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import app.fitbit.model.json.Activity;
import app.fitbit.model.json.ActivityDaySummary;
import app.shared.Log;
import app.shared.skin.SkinService;
import app.shared.ui.TabCommitTextFieldTableCell;
import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import javafx.util.converter.DoubleStringConverter;
import javafx.util.converter.IntegerStringConverter;

/**
 * Zeigt Fitbit-Activities in einer auto-width-sized TableView innerhalb eines Dialogs.
 * Setzt die Höhe der Table hart auf 400 weil JavaFX das sonst sowieso tun würde. Die JavaFX-Höhe zu nehmen und den Dialog daran anzupassen, gestaltete sich als ein Nightmare. Da ich weiß, dass unabhängig von Inhalt und Schriftgröße etc. die Höhe eh immer auf 400 gesetzt wird, kann ich das auch hardcodieren..
 * 
 * !Sofort: Das Styling im Skin der Table-View ist nicht perfekt
 * !Später: Also ein Dashboard mit Kennzahlen. Nur ein Überblick, keine Diagramme. Z. B. Durchschnitliche Fitbitpunkte pro Tag für diese Woche noch. Die aktuelle Alk-Zahl
 */
public class ActivityTableDialog {
    
    private final LocalDate date;
    private final List<Activity> activities;
    private final ActivityDaySummary daySummary;
    
    private static final int TIMEOUT_SECONDS = 5;
    
    public ActivityTableDialog(LocalDate date, List<Activity> activities, ActivityDaySummary daySummary) {
        this.date = date;
        this.activities = activities;
        this.daySummary = daySummary;
    }
    
    /**
     * Zeigt den Dialog mit auto-sized TableView.
     */
    public Optional<DialogResult> showAndWait() {
        // 1. Dialog erstellen
        Dialog<?> dialog = SkinService.get().createDialog(null, "Aktivitäten bearbeiten vom " + date);
        
        // 2. TableView erstellen
        TableView<ActivityRow> tableView = createTableView();
        tableView.getStyleClass().add("my-table-view");
        
        // 3. Daten laden
        ObservableList<ActivityRow> data = convertToTableData();
        tableView.setItems(data);
        
        // 4. DELETE-Handler für TableView
        tableView.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.DELETE || event.getCode() == KeyCode.BACK_SPACE) {
                ActivityRow selected = tableView.getSelectionModel().getSelectedItem();
                if (selected != null && !selected.isStepsRow()) {
                    tableView.getItems().remove(selected);
                }
            }
        });
        
        // 5. Layout zusammenbauen
        VBox content = new VBox();
        content.getStyleClass().add("my-dialog-vbox");
        content.getChildren().add(tableView);
        
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        
        // 6. Auto-Sizing für Breite durchführen (Binary Search)
        // Höhe wird von JavaFX automatisch angepasst (TableView hat 400px Default)
        AtomicInteger currentWidth = new AtomicInteger(750);
        AtomicInteger upperBound = new AtomicInteger(2000);
        AtomicInteger lowerBound = new AtomicInteger(currentWidth.get() - 10);
        
        Window window = dialog.getDialogPane().getScene().getWindow();
        window.setWidth(currentWidth.get());
        content.layout();
        content.applyCss();
        window.setOpacity(0);
        
        final double[] lastTableViewWidth = {-2};
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        ScheduledFuture<?> timeout = executor.schedule(() -> {
            Platform.runLater(() -> {
                window.setOpacity(1);
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
                window.setWidth(upperBound.get());
                dialog.getDialogPane().getScene().removePostLayoutPulseListener(listenerRef[0]);
                window.setOpacity(1);
            } else {
                if (sb != null && sb.isVisible()) {
                    lowerBound.set(currentWidth.get());
                } else {
                    upperBound.set(currentWidth.get());
                }
                
                currentWidth.set((lowerBound.get() + upperBound.get()) / 2);
                window.setWidth(currentWidth.get());
            }
        };
        
        dialog.getDialogPane().getScene().addPostLayoutPulseListener(listenerRef[0]);
        
        // 7. Dialog zeigen und auf Schließen warten
        Optional<?> result = dialog.showAndWait();
        
        // 8. Ergebnis zurückgeben
        if (result.isPresent() && result.get() == ButtonType.OK) {
            return Optional.of(convertFromTableData(tableView.getItems()));
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
        tableView.setMinHeight(400);
        tableView.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        tableView.setEditable(true);
        tableView.getStyleClass().add("fitbit-activity-table");
        
        // Spalten erstellen
        TableColumn<ActivityRow, String> startTimeCol = new TableColumn<>("StartTime");
        startTimeCol.setCellValueFactory(cd -> cd.getValue().startTimeProperty());
        startTimeCol.setCellFactory(TabCommitTextFieldTableCell.forTableColumn());
        startTimeCol.setEditable(true);
        startTimeCol.setSortable(false);
        
        TableColumn<ActivityRow, String> nameCol = new TableColumn<>("ActivityName");
        nameCol.setCellValueFactory(cd -> cd.getValue().activityNameProperty());
        nameCol.setCellFactory(TabCommitTextFieldTableCell.forTableColumn());
        nameCol.setEditable(true);
        nameCol.setSortable(false);
        
        TableColumn<ActivityRow, String> unitCol = new TableColumn<>("DistanceUnit");
        unitCol.setCellValueFactory(cd -> cd.getValue().distanceUnitProperty());
        unitCol.setCellFactory(TabCommitTextFieldTableCell.forTableColumn());
        unitCol.setEditable(true);
        unitCol.setSortable(false);
        
        TableColumn<ActivityRow, Double> distanceCol = new TableColumn<>("Distance");
        distanceCol.setCellValueFactory(cd -> cd.getValue().distanceProperty().asObject());
        distanceCol.setCellFactory(TabCommitTextFieldTableCell.forTableColumn(new DoubleStringConverter()));
        distanceCol.setEditable(true);
        distanceCol.setSortable(false);
        
        TableColumn<ActivityRow, Integer> stepsCol = new TableColumn<>("Steps");
        stepsCol.setCellValueFactory(cd -> cd.getValue().stepsProperty().asObject());
        stepsCol.setCellFactory(TabCommitTextFieldTableCell.forTableColumn(new IntegerStringConverter()));
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
                original.getOriginalStartTime(),  // ← Vom Original
                row.getActivityName(),
                row.getDistanceUnit().isEmpty() ? null : row.getDistanceUnit(),
                row.getDistance(),
                row.getSteps()
            );
            edited.setStartTime(row.getStartTime());  // ← Explizit setzen!

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
        private final SimpleStringProperty startTime;
        private final SimpleStringProperty activityName;
        private final SimpleStringProperty distanceUnit;
        private final SimpleDoubleProperty distance;
        private final SimpleIntegerProperty steps;
        private final boolean isStepsRow;
        
        public ActivityRow(String startTime, String activityName, String distanceUnit, 
                          Double distance, Integer steps, boolean isStepsRow) {
            this.startTime = new SimpleStringProperty(startTime);
            this.activityName = new SimpleStringProperty(activityName);
            this.distanceUnit = new SimpleStringProperty(distanceUnit);
            this.distance = new SimpleDoubleProperty(distance != null ? distance : 0.0);
            this.steps = new SimpleIntegerProperty(steps != null ? steps : 0);
            this.isStepsRow = isStepsRow;
        }
        
        // --- Property-Accessors (für JavaFX-Binding) ---
        
        public SimpleStringProperty startTimeProperty() { return startTime; }
        public SimpleStringProperty activityNameProperty() { return activityName; }
        public SimpleStringProperty distanceUnitProperty() { return distanceUnit; }
        public SimpleDoubleProperty distanceProperty() { return distance; }
        public SimpleIntegerProperty stepsProperty() { return steps; }
        
        // --- Konventionelle Getter/Setter (für Zugriff aus Java-Code) ---
        
        public String getStartTime() { return startTime.get(); }
        public void setStartTime(String value) { startTime.set(value); }
        
        public String getActivityName() { return activityName.get(); }
        public void setActivityName(String value) { activityName.set(value); }
        
        public String getDistanceUnit() { return distanceUnit.get(); }
        public void setDistanceUnit(String value) { distanceUnit.set(value); }
        
        public double getDistance() { return distance.get(); }
        public void setDistance(double value) { distance.set(value); }
        
        public int getSteps() { return steps.get(); }
        public void setSteps(int value) { steps.set(value); }
        
        public boolean isStepsRow() { return isStepsRow; }
    }
    
    /**
     * Ergebnis des Dialogs.
     */
    public record DialogResult(List<Activity> activities, int totalSteps) {}
}