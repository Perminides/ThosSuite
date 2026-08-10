package app.shared.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import app.shared.model.ActivityTableRow;
import app.shared.ui.components.SuiteTabCommitTextFieldTableCell;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import javafx.util.converter.DoubleStringConverter;
import javafx.util.converter.IntegerStringConverter;
import app.shared.ui.components.SuiteDialog;

/**
 * Editierbare Tabelle in einem Dialog, der sich auf seinen Inhalt einschnappt. Fünf feste typisierte
 * Spalten, DELETE entfernt löschbare Zeilen. Rein/raus über framework-freie {@link ActivityTableRow}.
 *
 * <h3>Warum die Größe von Hand gerechnet wird</h3>
 * <p>{@code TableViewSkinBase.computePrefWidth} summiert die <b>Wunsch</b>breiten der Spalten. Das
 * Ausmessen der Spalten auf ihren Inhalt, das JavaFX beim ersten Layout selbst anstößt, setzt aber
 * nur deren <b>Ist</b>breite ({@code TableColumnBaseHelper.setWidth}) — die Wunschbreite bleibt auf
 * dem Default 80 stehen. Die Tabelle meldet dadurch rund 400 px, während ihre Spalten das Doppelte
 * belegen, und jeder gewöhnliche Größenmechanismus liefert einen viel zu schmalen Dialog. Die
 * Überschreibung summiert deshalb die Ist-Breiten.</p>
 *
 * <p>Die Höhe hat denselben Ursprung: {@code computePrefHeight} liefert eine fest verdrahtete 400,
 * die kein Vielfaches der Zeilenhöhe ist — unten bleibt ein angeschnittener Streifen stehen, dessen
 * Höhe mit jeder Schriftgröße anders ausfällt. Gerechnet wird deshalb in ganzen Zeilen.</p>
 *
 * <p>{@code snapSizeX}/{@code snapSizeY} runden auf, und das ist Pflicht: Spaltensummen sind krumm,
 * und ein abgerundetes Ergebnis holt genau die Bildlaufleiste zurück, um die es in
 * {@code docs/ScrollPane-Bug-JavaFX26} geht.</p>
 *
 * <h3>Bekannter Rest</h3>
 * <p>Bei manchen Zeilenzahlen bleiben unten <b>0,8 px</b> stehen (bei 125 % Skalierung ein
 * Gerätepixel) — gemessen bei Schrift 20 mit 1 und 3 Zeilen und bei Schrift 23 mit 3 und 4 Zeilen,
 * sonst nirgends. Ursache ist der einmal gemessene Überhang in {@link ContentSizedTable}, der in
 * diesen Fällen eine Snap-Einheit danebenliegt. <b>Das ist bekannt und kein neuer Fehler.</b> Der
 * naheliegende Ausweg — den Überhang nachträglich verkleinern — ist gemessen und funktioniert
 * nicht, siehe die Warnung an {@code ContentSizedTable.lockHeight}.</p>
 *
 * <p>Wer es doch angehen will: Statt zu schrumpfen auf die <i>nächste volle Zeile zu wachsen</i>
 * bringt den Rest in fast allen Fällen auf null, hängt dafür manchmal eine komplette Leerzeile an.
 * Bewusst nicht gemacht, weil die Höhe sich an den Inhalt anpassen soll.</p>
 *
 * <h3>Womit man das nachmisst</h3>
 * <p>{@code scripts.ui.ActivityTableSizeProbe}. Baut diesen Dialog nach und gibt Füllerbreite, Rest
 * unten und Bildlaufleisten für eine beliebige Kombination aus Schriftgröße und Zeilenzahl aus.
 * <b>Vor jeder Änderung an der Größenrechnung dort messen</b> — es geht um Bruchteile eines Pixels,
 * die man am Bildschirm nicht sieht, und die Fehler treten nur bei einzelnen Zeilenzahlen auf. Die
 * Größenlogik ist dort kopiert und muss bei Änderungen hier mitgezogen werden.</p>
 */
public final class ActivityTableDialog {

    /** Mehr Zeilen zeigt die Tabelle nicht, darüber hinaus wird gescrollt. Die Summenzeile zählt mit. */
    private static final int MAX_VISIBLE_ROWS = 6;

    /**
     * Notbremse für das Einschnappen, falls die Wunschgröße wider Erwarten nie zur Ruhe kommt.
     * Regulär sind vier bis fünf Pulse nötig (Spalten ausmessen, Breite setzen, Höhe freigeben,
     * Höhe setzen); acht sind Luft nach oben, keine erwartete Zahl.
     */
    private static final int MAX_SNAP_PULSES = 8;

    public List<ActivityTableRow> show(String title, List<ActivityTableRow> input) {
    	// 1. Dialog erstellen
        SuiteDialog<ButtonType> dialog = new SuiteDialog<>(title);

        // 2. TableView erstellen
        ContentSizedTable tableView = createTableView();
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

        // 6. Fenster auf die Tabelle einschnappen lassen
        snapToContent(dialog, tableView);

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

    /**
     * Passt das Fenster an die Tabelle an, sobald deren Spalten ausgemessen sind.
     *
     * <p>Ein Puls-Listener bleibt nötig, weil das Ausmessen der Spalten erst beim ersten Layout
     * läuft — vorher meldet die Tabelle ihre Default-Breite.</p>
     *
     * <p><b>Erst die Breite, dann die Höhe.</b> Beides gleichzeitig zu setzen führt zu einem
     * bistabilen Zustand in {@code VirtualFlow}: Reicht die Breite der Zellen exakt aus, ist „beide
     * Bildlaufleisten an" genauso selbstkonsistent wie „keine" — jede Leiste nimmt der anderen so
     * viel Platz weg, dass die andere nötig bleibt. {@code VirtualFlow.computeBarVisiblity} rechnet
     * nur bei einer Änderung neu und bleibt sonst im einmal eingerasteten Zustand stehen. Kommt die
     * Höhe erst, wenn die Breite schon steht, entscheidet der Flow einmal mit der endgültigen
     * Breite, und die Frage stellt sich nicht.</p>
     *
     * <p><b>Vergeblich probiert</b>, um den eingerasteten Zustand nachträglich aufzulösen — bitte
     * nicht noch einmal: {@code requestLayout} auf Tabelle und Flow, synchrone {@code layout()},
     * {@code TableView.refresh()}, {@code fixedCellSize} auf 0 und zurück, und die Endgröße von
     * unten anzufahren (macht es schlimmer, dann hängt die horizontale Leiste überall).</p>
     *
     * <p>Bis dahin bleibt das Fenster durchsichtig, damit niemand die Zwischengrößen sieht.</p>
     */
    private void snapToContent(SuiteDialog<ButtonType> dialog, ContentSizedTable tableView) {
        Scene scene = dialog.getDialogPane().getScene();
        Window window = scene.getWindow();
        window.setOpacity(0);

        double[] lastWidth  = { -1 };
        double[] lastHeight = { -1 };
        int[] pulses = { 0 };
        Runnable[] listener = new Runnable[1];
        listener[0] = () -> {
            tableView.lockRowHeight();
            double width  = tableView.prefWidth(-1);
            double height = tableView.prefHeight(-1);
            boolean settled = Math.abs(width - lastWidth[0]) < 0.5 && Math.abs(height - lastHeight[0]) < 0.5;
            lastWidth[0]  = width;
            lastHeight[0] = height;
            window.sizeToScene();

            if (settled && !tableView.isHeightLocked()) {
                tableView.lockHeight();
                lastWidth[0] = lastHeight[0] = -1;
                return;
            }
            if ((settled && tableView.isHeightLocked()) || ++pulses[0] >= MAX_SNAP_PULSES) {
                scene.removePostLayoutPulseListener(listener[0]);
                window.setOpacity(1);
            }
        };
        scene.addPostLayoutPulseListener(listener[0]);
    }

    @SuppressWarnings("unchecked")
    private ContentSizedTable createTableView() {
        ContentSizedTable tableView = new ContentSizedTable();
        tableView.setMaxWidth(Region.USE_PREF_SIZE);   // sonst zieht die VBox die Tabelle auf Dialogbreite
        tableView.setMaxHeight(Region.USE_PREF_SIZE);
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

    /**
     * Die Tabelle, die ihre Größe selbst kennt. Warum überhaupt, steht im Klassen-Javadoc.
     *
     * <p>Zwei Feinheiten, die beide teuer erkauft sind:</p>
     * <ul>
     *   <li><b>Jede Spalte wird einzeln gesnappt.</b> Das Layout rundet jede Spaltenbreite für sich
     *       auf; wer nur die Summe der krummen Rohbreiten aufrundet, ist bei fünf Spalten bis zu
     *       vier Pixel zu schmal, und die horizontale Bildlaufleiste erscheint.</li>
     *   <li><b>Die Zeilenhöhe wird festgenagelt</b> ({@code setFixedCellSize}). Ohne das rechnet
     *       {@code VirtualFlow} mit gemessenen Bruchteilen, und die Höhe verfehlt das Vielfache der
     *       Zeilenhöhe um ein Gerätepixel — genau der angeschnittene Streifen, um den es geht.</li>
     * </ul>
     *
     * <p>Beide Werte werden aus dem gerenderten Baum gelesen, nicht aus dem Skin. Damit wächst die
     * Tabelle automatisch mit der Schriftgröße mit — der Auslöser der ganzen Übung war, dass der
     * Skin <i>Tiles</i> als einziger nicht mit Schriftgröße 20 läuft.</p>
     */
    private static final class ContentSizedTable extends TableView<Row> {

        private boolean heightLocked;

        /**
         * Kopfzeile plus Rahmen, als Differenz gemessen statt aus den Einzelteilen gerechnet: die
         * Höhe von {@code .column-header-background} weicht um eine Snap-Einheit von der ab, mit
         * der das Layout tatsächlich arbeitet, mal nach oben, mal nach unten.
         */
        private double overhead = -1;

        /**
         * Nagelt die Zeilenhöhe auf die gemessene Anzeigezeile fest.
         *
         * <p><b>Hängt an einer Skin-Regel.</b> Eine feste Zeilenhöhe heißt, dass die Zeile beim
         * Editieren nicht mehr wachsen kann — vorher tat sie das, von 38,6 auf 61 px. Dass der
         * Editor trotzdem hineinpasst, liegt an {@code .my-table-view .text-field} in
         * {@code Skin.java}: Die Regel nimmt dem Textfeld Polsterung, Rahmen und Zustands-Ring, so
         * dass es genau eine Schriftzeile hoch ist. Fällt die Regel weg, wird der Editor
         * abgeschnitten, ohne dass hier etwas kaputtgeht.</p>
         */
        void lockRowHeight() {
            if (getFixedCellSize() > 0)
                return;
            double rowHeight = heightOf(this, ".table-row-cell");
            if (rowHeight > 0)
                setFixedCellSize(snapSizeY(rowHeight));
        }

        /**
         * Übernimmt die Höhensteuerung; vorher gilt der JavaFX-Default.
         *
         * <p><b>Der Überhang wird genau einmal gemessen und danach nie wieder angefasst — das ist
         * Absicht.</b> Naheliegend wäre, ihn nachzuziehen, sobald der Flow steht: Ist der Flow ein
         * paar Zehntel zu hoch, den Überhang um die Differenz verkleinern. Genau das ist gemessen
         * worden und macht es <i>schlechter</i>: Eine nachträglich <b>schrumpfende</b> Tabellenhöhe
         * lässt {@code VirtualFlow} zuverlässig in den bistabilen Leisten-Zustand fallen, der in
         * {@code snapToContent} beschrieben ist — statt 0,8 px Rest gibt es dann zwei
         * Bildlaufleisten, die keine sind. Wachsen ist unkritisch, Schrumpfen nie.</p>
         *
         * <p>Deshalb steht hier ein einmal gemessener Wert und keine Regelschleife. Der Preis ist
         * der im Klassen-Javadoc beschriebene Rest von 0,8 px bei einigen Zeilenzahlen.</p>
         */
        void lockHeight() {
            double flowHeight = heightOf(this, ".virtual-flow");
            if (flowHeight > 0 && getHeight() > 0)
                overhead = getHeight() - flowHeight;
            heightLocked = true;
            requestLayout();
        }

        boolean isHeightLocked() {
            return heightLocked;
        }

        private int visibleRows() {
            return getItems() == null ? 0 : Math.min(getItems().size(), MAX_VISIBLE_ROWS);
        }

        /**
         * Spaltenbreiten plus Rahmen, und bei mehr Zeilen als sichtbar die Bildlaufleiste obendrauf.
         *
         * <p>Dass die Leistenbreite dazukommt, ist kein Sicherheitsaufschlag:
         * {@code TableHeaderRow.layoutChildren} rechnet den Füller als
         * {@code tableWidth - headerWidth - controlInsets} und zieht die Leistenbreite nur ab, wenn
         * der Spaltenmenü-Knopf sichtbar ist — den benutzen wir nicht. Die Kopfzeile spannt sich
         * also über die volle Breite <i>einschließlich</i> des Streifens über der Leiste. Ohne den
         * Zuschlag wäre der Zellbereich um die Leistenbreite zu schmal.</p>
         *
         * <p><b>Nicht</b> {@code verticalBar.isVisible()} abfragen: Ob die Leiste da ist, ist ein
         * Layout-Ergebnis, und es hier zu lesen macht die Wunschbreite von dem abhängig, was sie
         * selbst verursacht. Die Zeilenzahl ist dieselbe Information ohne Rückkopplung.</p>
         */
        @Override
        protected double computePrefWidth(double height) {
            double width = snappedLeftInset() + snappedRightInset();
            for (TableColumn<Row, ?> column : getColumns())
                if (column.isVisible())
                    width += snapSizeX(column.getWidth());

            ScrollBar verticalBar = scrollBar(this, Orientation.VERTICAL);
            if (verticalBar != null && getItems() != null && getItems().size() > MAX_VISIBLE_ROWS)
                width += snapSizeX(verticalBar.prefWidth(-1));

            return snapSizeX(width);
        }

        @Override
        protected double computePrefHeight(double width) {
            double rowHeight = getFixedCellSize();
            if (!heightLocked || overhead < 0 || rowHeight <= 0)
                return super.computePrefHeight(width);
            return snapSizeY(overhead + visibleRows() * rowHeight);
        }
    }

    /** Höhe des ersten Knotens mit dieser Stilklasse; 0, solange er noch nicht gebaut ist. */
    private static double heightOf(TableView<?> table, String selector) {
        return table.lookup(selector) instanceof Region region ? region.getHeight() : 0;
    }

    private static ScrollBar scrollBar(TableView<?> table, Orientation orientation) {
        for (Node n : table.lookupAll(".scroll-bar")) {
            if (n instanceof ScrollBar bar && bar.getOrientation() == orientation)
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
