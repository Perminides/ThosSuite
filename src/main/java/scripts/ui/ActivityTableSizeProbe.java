package scripts.ui;

import javafx.application.Application;
import javafx.animation.PauseTransition;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;
import javafx.util.converter.DoubleStringConverter;
import javafx.util.converter.IntegerStringConverter;

/**
 * Messwerkzeug zu {@code app.shared.ui.ActivityTableDialog} — der Fitbit-Aktivitätentabelle.
 *
 * <h3>Wofür</h3>
 * <p>Der Dialog rechnet seine Größe selbst aus, weil {@code TableView} über ihre Wunschbreite lügt.
 * Diese Rechnung geht um Bruchteile eines Pixels auf: 0,8 px zu wenig, und eine Bildlaufleiste
 * erscheint oder unten bleibt ein angeschnittener Streifen stehen. <b>Solche Unterschiede sieht man
 * nicht, die muss man messen</b> — und genau dafür ist dieses Programm da. Es baut den Dialog
 * nach und gibt für eine Kombination aus Schriftgröße und Zeilenzahl aus, was hinten herauskommt:</p>
 *
 * <ul>
 *   <li><b>Füllerbreite</b> — die „leere Spalte" rechts. Soll 1 bis 2 px sein; große Werte heißen,
 *       dass die Tabelle breiter ist als ihre Spalten.</li>
 *   <li><b>Rest unten</b> — was unterhalb der letzten vollen Zeile übrig bleibt. Soll 0 sein.</li>
 *   <li><b>Bildlaufleisten</b> — beide sollen aus sein, solange die Zeilen passen.</li>
 * </ul>
 *
 * <h3>Aufruf</h3>
 * <pre>
 * java --module-path &lt;javafx-lib&gt; --add-modules javafx.controls \
 *      scripts/ui/ActivityTableSizeProbe.java &lt;schriftgroesse&gt; &lt;zeilenzahl&gt;
 * </pre>
 * <p>Also etwa {@code 23 4} für den Skin <i>Tiles</i> mit vier Zeilen, oder {@code 20 4} für alle
 * übrigen Skins, die auf Schriftgröße 20 stehen. Sinnvoll ist, die Zeilenzahlen 1 bis 6 und einen
 * Wert oberhalb von {@code MAX_VISIBLE_ROWS} durchzugehen — die Fehler treten nur bei einzelnen
 * Zeilenzahlen auf, nicht durchgängig.</p>
 *
 * <p>Mit {@code --add-opens javafx.controls/javafx.scene.control.skin=ALL-UNNAMED} kommen
 * zusätzlich die Entscheidungsgrößen aus {@code VirtualFlow.computeBarVisiblity} dazu. Die braucht
 * man, wenn eine Bildlaufleiste erscheint, die niemand bestellt hat — sie zeigen, ob der Flow im
 * bistabilen Zustand hängt, der im Javadoc von {@code ActivityTableDialog.snapToContent}
 * beschrieben ist. Ohne das Flag läuft alles andere trotzdem.</p>
 *
 * <h3>Zwei bewusste Abweichungen vom Original</h3>
 * <ul>
 *   <li>Ein blanker {@code Dialog} statt {@code SuiteDialog}, damit das Programm ohne Config, DB
 *       und Besitzerfenster startet. Die Fensterdekoration ist dadurch etwas anders, die Geometrie
 *       der Tabelle nicht.</li>
 *   <li>Ein eigenes Stylesheet mit den Regeln aus {@code Skin.java}, die die Geometrie bestimmen
 *       (Schrift, Padding der Inhaltsbox, Rahmen der Tabelle). Farben fehlen — dies misst, es malt
 *       nicht.</li>
 * </ul>
 *
 * <p><b>Beim Ändern des Dialogs mitziehen.</b> Die Größenlogik unten ist eine Kopie aus
 * {@code ActivityTableDialog}; misst sie etwas anderes als der Dialog, misst sie nichts.</p>
 */
public class ActivityTableSizeProbe extends Application {

    /** Wie in ActivityTableDialog. */
    private static final int MAX_VISIBLE_ROWS = 6;
    private static final int MAX_SNAP_PULSES = 8;

    private static double fontSize;
    private static int rowCount;

    /** Polsterung des Editier-Textfelds, oben/unten und seitlich — aus borderSmallComponent des Skins. */
    private static double fieldPaddingY = 11;
    private static double fieldPaddingX = 20;

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Aufruf: ActivityTableSizeProbe <schriftgroesse> <zeilenzahl> "
                    + "[<polsterung-oben-unten> <polsterung-seitlich>]");
            return;
        }
        fontSize = Double.parseDouble(args[0]);
        rowCount = Integer.parseInt(args[1]);
        if (args.length >= 4) {
            fieldPaddingY = Double.parseDouble(args[2]);
            fieldPaddingX = Double.parseDouble(args[3]);
        }
        launch(args);
    }

    @Override
    public void start(Stage primary) {
        // Unsichtbares Besitzerfenster, sonst hat der Dialog keinen Platz zum Erscheinen
        primary.setWidth(100);
        primary.setHeight(100);
        primary.setOpacity(0);
        primary.setScene(new Scene(new VBox()));
        primary.show();

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.initOwner(primary);
        dialog.setTitle("Aktivitaeten bearbeiten");

        SizedTable tableView = createTableView();
        tableView.getStyleClass().add("my-table-view");

        ObservableList<Row> data = FXCollections.observableArrayList();
        data.add(new Row("(gesamt)", "Steps", "", 0.0, 12345));
        for (int i = 1; i < rowCount; i++)
            data.add(new Row("07:1" + (i % 10), "Spaziergang", "km", 3.42, 4711));
        tableView.setItems(data);

        VBox content = new VBox();
        content.getStyleClass().add("my-dialog-vbox");
        content.getChildren().add(tableView);

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.getDialogPane().getScene().getStylesheets().add(geometryStylesheet());

        snapToContent(dialog, tableView);
        dialog.show();

        // Erst messen, wenn das Einschnappen durch ist; danach hart raus, sonst bleibt das Fenster stehen
        PauseTransition done = new PauseTransition(Duration.millis(700));
        done.setOnFinished(_ -> {
            report(dialog.getDialogPane().getScene().getWindow(), tableView);
            System.out.flush();
            Runtime.getRuntime().halt(0);
        });
        done.play();
    }

    // ------------------------------------------------------------------
    // Kopie der Größenlogik aus ActivityTableDialog
    // ------------------------------------------------------------------

    private void snapToContent(Dialog<ButtonType> dialog, SizedTable tableView) {
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
            System.out.printf("  Puls %d: %.1f x %.1f%s%n", pulses[0], width, height, settled ? "  stabil" : "");

            if (settled && !tableView.isHeightLocked()) {
                tableView.lockHeight();
                lastWidth[0] = lastHeight[0] = -1;
                return;
            }
            if ((settled && tableView.isHeightLocked()) || ++pulses[0] >= MAX_SNAP_PULSES) {
                scene.removePostLayoutPulseListener(listener[0]);
            }
        };
        scene.addPostLayoutPulseListener(listener[0]);
    }

    private static final class SizedTable extends TableView<Row> {

        private boolean heightLocked;
        private double overhead = -1;

        void lockRowHeight() {
            if (getFixedCellSize() > 0)
                return;
            double rowHeight = heightOf(this, ".table-row-cell");
            if (rowHeight > 0)
                setFixedCellSize(snapSizeY(rowHeight));
        }

        /** Nur einmal messen, nie nachziehen — Schrumpfen löst den Leisten-Latch aus, siehe Dialog. */
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

    // ------------------------------------------------------------------
    // Messen und ausgeben
    // ------------------------------------------------------------------

    private void report(Window window, SizedTable table) {
        System.out.println();
        System.out.printf("=== Schrift %.0f, %d Zeilen ===%n", fontSize, rowCount);
        System.out.printf("Fenster %.2f | Tabelle %.2f x %.2f%n",
                window.getWidth(), table.getWidth(), table.getHeight());

        double sum = 0;
        for (TableColumn<Row, ?> column : table.getColumns()) {
            System.out.printf("  Spalte %-14s %8.3f%n", column.getText(), column.getWidth());
            sum += column.getWidth();
        }
        System.out.printf("  Spalten zusammen      %8.3f%n", sum);

        ScrollBar horizontal = scrollBar(table, Orientation.HORIZONTAL);
        ScrollBar vertical   = scrollBar(table, Orientation.VERTICAL);

        // Mit senkrechter Leiste ist der Füller zu Recht so breit wie sie — er sitzt darüber
        Node filler = table.lookup(".filler");
        boolean barExpected = rowCount > MAX_VISIBLE_ROWS;
        System.out.printf("  FUELLER               %8.3f   (soll: %s)%n",
                filler instanceof Region region ? region.getWidth() : -1,
                barExpected && vertical != null
                        ? String.format("%.1f = Leistenbreite", vertical.getWidth())
                        : "1 bis 2");

        System.out.printf("  Leisten               waagerecht %s, senkrecht %s   (soll: false / %s)%n",
                horizontal != null && horizontal.isVisible(),
                vertical != null && vertical.isVisible(),
                barExpected);

        double rowHeight  = table.getFixedCellSize();
        double flowHeight = heightOf(table, ".virtual-flow");
        System.out.printf("  Zeilenhoehe %.2f | Flow %.2f%n", rowHeight, flowHeight);
        if (rowHeight > 0 && flowHeight > 0) {
            double whole = Math.floor(flowHeight / rowHeight + 0.0001);
            double rest = flowHeight - whole * rowHeight;
            System.out.printf("  REST UNTEN            %8.3f   (soll: 0)%n", Math.abs(rest) < 0.001 ? 0 : rest);
        }
        dumpFlow(table);
        reportEditor(table);
    }

    /**
     * Wird beim Editieren etwas abgeschnitten?
     *
     * <p>Ohne die Regeln {@code .my-table-view .text-field} aus {@code Skin.java} wäre der Editor
     * rund 12 px höher als die Zeile: Ein {@code TextField} bringt die Polsterung aus
     * {@code borderSmallComponent} mit, und im Fokus — beim Editieren immer — zusätzlich den
     * Zustands-Ring. Das Stylesheet oben bildet beides nach, damit hier auch wirklich der harte Fall
     * gemessen wird.</p>
     *
     * <p><b>Zur Breite nicht {@code prefWidth} fragen.</b> Die richtet sich bei einem
     * {@code TextField} nach {@code prefColumnCount} (zwölf Zeichen) und nicht nach dem Inhalt — sie
     * ist in einer schmalen Spalte immer größer als der Platz, ohne dass etwas fehlt. Die Frage, auf
     * die es ankommt, ist, ob der <i>gerenderte Text</i> in den Innenraum passt; gemessen wird er am
     * {@code .text}-Knoten im Editor.</p>
     */
    private void reportEditor(SizedTable table) {
        TableColumn<Row, ?> lastColumn = table.getColumns().getLast();
        table.edit(0, lastColumn);
        table.layout();

        Node editor = table.lookup(".text-field");
        if (!(editor instanceof Region field)) {
            System.out.println("  EDITOR: kein Textfeld gefunden");
            return;
        }
        double rowHeight = table.getFixedCellSize();
        double needed = field.prefHeight(-1);
        System.out.printf("  EDITOR hoch           %8.3f   in Zeile von %.3f   %s%n",
                needed, rowHeight, needed <= rowHeight ? "passt" : "ABGESCHNITTEN");

        Node textNode = field.lookup(".text");
        if (textNode != null) {
            double textWidth = textNode.getLayoutBounds().getWidth();
            System.out.printf("  EDITOR Text breit     %8.3f   in Feld von %.3f    %s%n",
                    textWidth, field.getWidth(),
                    textWidth <= field.getWidth() ? "passt" : "ROLLT (laenger als die Spalte)");
        }
        table.edit(-1, null);
    }

    /**
     * Die Größen, aus denen {@code VirtualFlow.computeBarVisiblity} entscheidet. Braucht
     * {@code --add-opens javafx.controls/javafx.scene.control.skin=ALL-UNNAMED}; ohne das Flag
     * bleibt es bei einem Hinweis.
     */
    private void dumpFlow(TableView<Row> table) {
        try {
            Object flow = table.lookup(".virtual-flow");
            Class<?> type = flow.getClass();

            java.lang.reflect.Field cellsField = type.getDeclaredField("cells");
            cellsField.setAccessible(true);
            Object cells = cellsField.get(flow);
            java.lang.reflect.Method size = cells.getClass().getDeclaredMethod("size");
            size.setAccessible(true);

            System.out.printf("  FLOW cells=%s cellCount=%s position=%s laenge=%s breite=%s maxPrefBreadth=%s%n",
                    size.invoke(cells), call(flow, "getCellCount"), call(flow, "getPosition"),
                    call(flow, "getViewportLength"), call(flow, "getViewportBreadth"),
                    field(flow, "maxPrefBreadth"));
        } catch (Exception e) {
            System.out.println("  FLOW: nicht lesbar (--add-opens fehlt?) — " + e.getClass().getSimpleName());
        }
    }

    private static Object call(Object target, String name) throws Exception {
        java.lang.reflect.Method method = target.getClass().getDeclaredMethod(name);
        method.setAccessible(true);
        return method.invoke(target);
    }

    private static Object field(Object target, String name) throws Exception {
        java.lang.reflect.Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        return f.get(target);
    }

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

    // ------------------------------------------------------------------
    // Aufbau
    // ------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private SizedTable createTableView() {
        SizedTable tableView = new SizedTable();
        tableView.setMaxWidth(Region.USE_PREF_SIZE);
        tableView.setMaxHeight(Region.USE_PREF_SIZE);
        tableView.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        tableView.setEditable(true);

        TableColumn<Row, String> startTimeCol = new TableColumn<>("StartTime");
        startTimeCol.setCellValueFactory(cd -> cd.getValue().startTime);
        startTimeCol.setCellFactory(TextFieldTableCell.forTableColumn());

        TableColumn<Row, String> nameCol = new TableColumn<>("ActivityName");
        nameCol.setCellValueFactory(cd -> cd.getValue().activityName);
        nameCol.setCellFactory(TextFieldTableCell.forTableColumn());

        TableColumn<Row, String> unitCol = new TableColumn<>("DistanceUnit");
        unitCol.setCellValueFactory(cd -> cd.getValue().distanceUnit);
        unitCol.setCellFactory(TextFieldTableCell.forTableColumn());

        TableColumn<Row, Double> distanceCol = new TableColumn<>("Distance");
        distanceCol.setCellValueFactory(cd -> cd.getValue().distance.asObject());
        distanceCol.setCellFactory(TextFieldTableCell.forTableColumn(new DoubleStringConverter()));

        TableColumn<Row, Integer> stepsCol = new TableColumn<>("Steps");
        stepsCol.setCellValueFactory(cd -> cd.getValue().steps.asObject());
        stepsCol.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter()));

        tableView.getColumns().addAll(startTimeCol, nameCol, unitCol, distanceCol, stepsCol);
        return tableView;
    }

    /** Nur die Regeln aus Skin.java, die die Geometrie bestimmen. Keine Farben. */
    private String geometryStylesheet() {
        String css = """
            .root { -fx-font-family: 'Aptos'; -fx-font-size: %spx; }
            .my-dialog-vbox { -fx-padding: %spx; -fx-alignment: top-center; }
            .my-table-view .column-header, .my-table-view .filler {
                -fx-border-color: transparent #000000 #000000 transparent; }
            .my-table-view .table-cell { -fx-border-color: transparent #000000 transparent transparent; }
            .my-table-view { -fx-border-color: #000000; -fx-border-width: 1;
                -fx-background-insets: 0; -fx-padding: 0; }
            .text-field { -fx-padding: %spx %spx %spx %spx; -fx-alignment: center;
                -fx-border-color: #000000; -fx-border-width: 1px; -fx-background-insets: 1px; }
            .text-field:focused { -fx-border-color: #f2951f; -fx-border-width: 2px;
                -fx-background-insets: 2px; }
            .my-table-view .text-field { -fx-padding: 0; -fx-border-width: 0;
                -fx-background-insets: 0; }
            .my-table-view .text-field:focused { -fx-border-width: 0; -fx-background-insets: 0; }
            """.formatted(fontSize, fontSize * 0.5,
                          fieldPaddingY, fieldPaddingX, fieldPaddingY, fieldPaddingX);
        return "data:text/css;charset=utf-8,"
                + java.net.URLEncoder.encode(css, java.nio.charset.StandardCharsets.UTF_8).replace("+", "%20");
    }

    /** Zeilenmodell wie im Dialog, nur ohne deletable und carry — die spielen für die Größe keine Rolle. */
    private static final class Row {
        final SimpleStringProperty  startTime;
        final SimpleStringProperty  activityName;
        final SimpleStringProperty  distanceUnit;
        final SimpleDoubleProperty  distance;
        final SimpleIntegerProperty steps;

        Row(String startTime, String activityName, String distanceUnit, double distance, int steps) {
            this.startTime    = new SimpleStringProperty(startTime);
            this.activityName = new SimpleStringProperty(activityName);
            this.distanceUnit = new SimpleStringProperty(distanceUnit);
            this.distance     = new SimpleDoubleProperty(distance);
            this.steps        = new SimpleIntegerProperty(steps);
        }
    }
}
