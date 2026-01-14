package app.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import app.ui.skin.SkinService;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Window;

public class AutoSizedTableGemini {

    private static final double CELL_PADDING = 20.0;
    private static final double HEADER_HEIGHT = 35.0;
    private static final double ROW_HEIGHT = 30.0;

    /**
     * Zeigt eine generische, editierbare Tabelle an.
     * @param rawData Liste von String-Arrays. Index 0 MUSS der Header sein!
     * @return Die (ggf. modifizierte) Liste inklusive Header.
     */
    public Optional<List<String[]>> showAndWait(Window parent, String title, List<String[]> rawData) {
        if (rawData == null || rawData.isEmpty()) return Optional.empty();

        // 1. Daten trennen (Header vs. Inhalt)
        String[] headers = rawData.get(0);
        List<String[]> dataRows = new ArrayList<>(rawData.subList(1, rawData.size())); // Kopie für UI

        // 2. Dialog vom SkinService holen
        Dialog<List<String[]>> dialog = (Dialog<List<String[]>>) SkinService.get().createDialog(parent, title);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        // 3. Tabelle bauen
        TableView<String[]> table = new TableView<>();
        table.setEditable(true); // MVP: Alles ist editierbar!
        table.setFixedCellSize(ROW_HEIGHT);

        // 4. Spalten dynamisch erzeugen
        for (int i = 0; i < headers.length; i++) {
            final int colIndex = i;
            TableColumn<String[], String> col = new TableColumn<>(headers[i]);
            
            // Value: String aus Array holen
            col.setCellValueFactory(cellData -> {
                String[] row = cellData.getValue();
                return new SimpleStringProperty(colIndex < row.length ? row[colIndex] : "");
            });

            // Editing: Standard Textfeld
            col.setCellFactory(TextFieldTableCell.forTableColumn());
            col.setOnEditCommit(e -> {
                // Array in der Liste aktualisieren
                String[] row = e.getRowValue();
                // Sicherstellen, dass Array groß genug ist (falls du mal Spalten hinzufügst)
                if (colIndex < row.length) {
                    row[colIndex] = e.getNewValue();
                }
            });

            table.getColumns().add(col);
        }

        table.setItems(FXCollections.observableArrayList(dataRows));

        // 5. Auto-Size Magic (Messen statt Raten)
        makeTableFitContent(table, headers);

        // 6. Layout
        VBox content = new VBox(table);
        content.setPadding(new Insets(10));
        content.getStyleClass().add("my-dialog-vbox"); // Styling aus Skin übernehmen
        dialog.getDialogPane().setContent(content);

        // 7. Ergebnis zusammenbauen
        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                // Header wieder vorne dran kleben
                List<String[]> result = new ArrayList<>();
                result.add(headers);
                result.addAll(table.getItems());
                return result;
            }
            return null;
        });

        // Zentrieren Hack
        Platform.runLater(() -> dialog.getDialogPane().getScene().getWindow().centerOnScreen());

        return dialog.showAndWait();
    }

    /**
     * Misst den Inhalt aller Zellen und Header und passt die Tabellengröße exakt an.
     * Setzt PrefWidth und PrefHeight der Tabelle.
     */
    private void makeTableFitContent(TableView<String[]> table, String[] headers) {
        // Helper Text-Node für die Messung. 
        Text measureHelper = new Text();
        
        // Trick: Wir hängen ihn in eine Scene und wenden den Skin an!
        // So hat der Text exakt dieselbe Größe (z.B. 20px Aptos) wie die echte Tabelle.
        Scene dummyScene = new Scene(new Group(measureHelper));
        SkinService.get().styleScene(dummyScene); 
        
        measureHelper.applyCss();

        double totalWidth = 0;
        // TextFields brauchen mehr Platz als nackter Text (Ränder links/rechts)
        double textFieldPadding = 35.0; 

        for (int i = 0; i < table.getColumns().size(); i++) {
            TableColumn<String[], ?> col = table.getColumns().get(i);
            
            // A. Header messen (Der Header hat oft Fettschrift, geben wir ihm etwas Puffer)
            double maxW = getWidthOf(measureHelper, headers[i]) + 10;

            // B. Alle Zeilen messen
            for (String[] row : table.getItems()) {
                String text = (i < row.length) ? row[i] : "";
                maxW = Math.max(maxW, getWidthOf(measureHelper, text));
            }

            // C. Spaltenbreite setzen (Max + Padding für TextField-Ränder)
            double finalColW = maxW + textFieldPadding;
            
            col.setPrefWidth(finalColW);
            col.setMinWidth(finalColW);
            totalWidth += finalColW;
        }

        // Gesamthöhe berechnen: Header + Zeilen * Höhe + Puffer für Ränder
        double totalHeight = HEADER_HEIGHT + (table.getItems().size() * ROW_HEIGHT) + 10;
        
        // Tabelle fixieren
        table.setPrefWidth(totalWidth + 5); // +5 für Rahmen
        table.setPrefHeight(totalHeight);
        table.setMinHeight(totalHeight);
        table.setMaxHeight(totalHeight);
        
        table.setStyle("-fx-table-header-border-color: transparent;");
    }

    private double getWidthOf(Text helper, String text) {
        if (text == null || text.isEmpty()) return 0;
        helper.setText(text);
        return helper.getLayoutBounds().getWidth();
    }
}