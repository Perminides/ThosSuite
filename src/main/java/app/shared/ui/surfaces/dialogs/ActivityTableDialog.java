package app.shared.ui.surfaces.dialogs;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import app.shared.Log;
import app.shared.model.ActivityTableRow;
import app.shared.skin.SkinService;
import app.shared.ui.components.SuiteTabCommitTextFieldTableCell;
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
 * Editierbare Tabelle in einem auto-width-sized Dialog. Fünf feste typisierte Spalten,
 * DELETE entfernt löschbare Zeilen. Rein/raus über framework-freie {@link ActivityTableRow}.
 *
 * Höhe hart auf 400 (JavaFX setzt sie ohnehin dorthin; die JavaFX-Höhe abzufragen und den
 * Dialog daran anzupassen war ein Nightmare, daher hardcodiert). Breite via Binärsuche.
 */
public final class ActivityTableDialog {

    private static final int TIMEOUT_SECONDS = 5;

    public List<ActivityTableRow> show(String title, List<ActivityTableRow> input) {
    	// 1. Dialog erstellen
        Dialog<?> dialog = SkinService.get().createDialog(SkinService.getOwnerWindow(), title);

        // 2. TableView erstellen
        TableView<Row> tableView = createTableView();
        tableView.getStyleClass().add("my-table-view");

        // 3. Daten laden
        ObservableList<Row> data = FXCollections.observableArrayList();
        for (ActivityTableRow r : input)
            data.add(new Row(r));
        tableView.setItems(data);

        // 4. DELETE-Handler für TableView
        tableView.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.DELETE || event.getCode() == KeyCode.BACK_SPACE) {
                Row selected = tableView.getSelectionModel().getSelectedItem();
                if (selected != null && selected.deletable)
                    tableView.getItems().remove(selected);
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
        AtomicInteger upperBound   = new AtomicInteger(2000);
        AtomicInteger lowerBound   = new AtomicInteger(currentWidth.get() - 10);

        Window window = dialog.getDialogPane().getScene().getWindow();
        window.setWidth(currentWidth.get());
        content.layout();
        content.applyCss();
        window.setOpacity(0);

        final double[] lastTableViewWidth = { -2 };
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
            if (Math.abs(currentTableViewWidth - lastTableViewWidth[0]) < 1)
                return;

            lastTableViewWidth[0] = currentTableViewWidth;
            ScrollBar sb = getHorizontalScrollbar(tableView);

            if (upperBound.get() - lowerBound.get() <= 2) {
                timeout.cancel(false);
                executor.shutdown();
                window.setWidth(upperBound.get());
                dialog.getDialogPane().getScene().removePostLayoutPulseListener(listenerRef[0]);
                window.setOpacity(1);
            } else {
                if (sb != null && sb.isVisible())
                    lowerBound.set(currentWidth.get());
                else
                    upperBound.set(currentWidth.get());

                currentWidth.set((lowerBound.get() + upperBound.get()) / 2);
                window.setWidth(currentWidth.get());
            }
        };
        dialog.getDialogPane().getScene().addPostLayoutPulseListener(listenerRef[0]);

        // 7. Dialog zeigen und auf Schließen warten
        Optional<?> result = dialog.showAndWait();
        
        // 8. Ergebnis zurückgeben
        if (result.isPresent() && result.get() == ButtonType.OK) {
            List<ActivityTableRow> out = new ArrayList<>();
            for (Row row : tableView.getItems())
                out.add(row.toRecord());
            return out;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private TableView<Row> createTableView() {
        TableView<Row> tableView = new TableView<>();
        tableView.setPrefWidth(TableView.USE_COMPUTED_SIZE);
        tableView.setMinHeight(400);
        tableView.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        tableView.setEditable(true);
        tableView.getStyleClass().add("fitbit-activity-table");

        TableColumn<Row, String> startTimeCol = new TableColumn<>("StartTime");
        startTimeCol.setCellValueFactory(cd -> cd.getValue().startTime);
        startTimeCol.setCellFactory(SuiteTabCommitTextFieldTableCell.forTableColumn());
        startTimeCol.setEditable(true);
        startTimeCol.setSortable(false);

        TableColumn<Row, String> nameCol = new TableColumn<>("ActivityName");
        nameCol.setCellValueFactory(cd -> cd.getValue().activityName);
        nameCol.setCellFactory(SuiteTabCommitTextFieldTableCell.forTableColumn());
        nameCol.setEditable(true);
        nameCol.setSortable(false);

        TableColumn<Row, String> unitCol = new TableColumn<>("DistanceUnit");
        unitCol.setCellValueFactory(cd -> cd.getValue().distanceUnit);
        unitCol.setCellFactory(SuiteTabCommitTextFieldTableCell.forTableColumn());
        unitCol.setEditable(true);
        unitCol.setSortable(false);

        TableColumn<Row, Double> distanceCol = new TableColumn<>("Distance");
        distanceCol.setCellValueFactory(cd -> cd.getValue().distance.asObject());
        distanceCol.setCellFactory(SuiteTabCommitTextFieldTableCell.forTableColumn(new DoubleStringConverter()));
        distanceCol.setEditable(true);
        distanceCol.setSortable(false);

        TableColumn<Row, Integer> stepsCol = new TableColumn<>("Steps");
        stepsCol.setCellValueFactory(cd -> cd.getValue().steps.asObject());
        stepsCol.setCellFactory(SuiteTabCommitTextFieldTableCell.forTableColumn(new IntegerStringConverter()));
        stepsCol.setEditable(true);
        stepsCol.setSortable(false);

        tableView.getColumns().addAll(startTimeCol, nameCol, unitCol, distanceCol, stepsCol);
        return tableView;
    }

    private static ScrollBar getHorizontalScrollbar(TableView<?> table) {
        for (Node n : table.lookupAll(".scroll-bar")) {
            if (n instanceof ScrollBar bar && bar.getOrientation() == Orientation.HORIZONTAL)
                return bar;
        }
        return null;
    }

    /** JavaFX-property-gebundenes Zeilenmodell; deletable + carry reisen unverändert mit. */
    private static final class Row {
        final SimpleStringProperty  startTime;
        final SimpleStringProperty  activityName;
        final SimpleStringProperty  distanceUnit;
        final SimpleDoubleProperty  distance;
        final SimpleIntegerProperty steps;
        final boolean deletable;
        final String  carry;

        Row(ActivityTableRow src) {
            this.startTime    = new SimpleStringProperty(src.startTime());
            this.activityName = new SimpleStringProperty(src.activityName());
            this.distanceUnit = new SimpleStringProperty(src.distanceUnit());
            this.distance     = new SimpleDoubleProperty(src.distance() != null ? src.distance() : 0.0);
            this.steps        = new SimpleIntegerProperty(src.steps() != null ? src.steps() : 0);
            this.deletable    = src.deletable();
            this.carry        = src.carry();
        }

        ActivityTableRow toRecord() {
            return new ActivityTableRow(startTime.get(), activityName.get(), distanceUnit.get(),
                    distance.get(), steps.get(), deletable, carry);
        }
    }
}