package app.ui.components;

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
import javafx.scene.layout.Border;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

/**
 * Generische, editierbare Tabelle in einem Dialog.
 * Passt Breite und Höhe automatisch an den Inhalt an - OHNE Magic Numbers!
 * Alles wird gemessen, nichts wird geraten.
 * 
 * <h3>Verwendung:</h3>
 * <pre>
 * List<String[]> data = new ArrayList<>();
 * data.add(new String[]{"Header1", "Header2"}); // Erste Zeile = Header
 * data.add(new String[]{"Value1", "Value2"});   // Datenzeilen
 * 
 * AutoSizedTableInDialog dialog = new AutoSizedTableInDialog();
 * Optional<List<String[]>> result = dialog.showAndWait(parent, "Titel", data);
 * </pre>
 */
public class AutoSizedTableInDialog {

    /**
     * Zeigt eine generische, editierbare Tabelle an.
     * 
     * @param parent Parent-Window für den Dialog
     * @param title Dialog-Titel
     * @param rawData Liste von String-Arrays. Index 0 MUSS der Header sein!
     * @return Die (ggf. modifizierte) Liste inklusive Header, oder empty() bei Abbruch
     */
    public Optional<List<String[]>> showAndWait(Window parent, String title, List<String[]> rawData) {
        if (rawData == null || rawData.isEmpty()) {
            return Optional.empty();
        }

        // 1. Daten trennen (Header vs. Inhalt)
        String[] headers = rawData.get(0);
        List<String[]> dataRows = new ArrayList<>(rawData.subList(1, rawData.size()));

        // 2. Dialog vom SkinService holen
        @SuppressWarnings("unchecked")
        Dialog<List<String[]>> dialog = (Dialog<List<String[]>>) SkinService.get().createDialog(parent, title);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        // 3. Tabelle bauen
        TableView<String[]> table = new TableView<>();
        table.setEditable(true);

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
                String[] row = e.getRowValue();
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
        content.getStyleClass().add("my-dialog-vbox");
        dialog.getDialogPane().setContent(content);

        // 7. Ergebnis zusammenbauen
        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                // Header wieder vorne dran
                List<String[]> result = new ArrayList<>();
                result.add(headers);
                result.addAll(table.getItems());
                return result;
            }
            return null;
        });

        // 8. Dialog zentrieren (muss nach Rendering passieren)
        Platform.runLater(() -> dialog.getDialogPane().getScene().getWindow().centerOnScreen());

        return dialog.showAndWait();
    }

    /**
     * Misst Header und Zellen-Inhalte anhand echter UI-Komponenten.
     * Keine Magic Numbers - Label und TextField werden gemessen!
     */
    private void makeTableFitContent(TableView<String[]> table, String[] headers) {
        // Dummy-Komponenten erzeugen, wie sie in der Table vorkommen
        javafx.scene.control.Label dummyHeader = new javafx.scene.control.Label();
        javafx.scene.control.TextField dummyCell = new javafx.scene.control.TextField();
        
        // In Scene packen und stylen, damit Font-Größe & Padding stimmen
        VBox dummyRoot = new VBox(dummyHeader, dummyCell);
        Scene dummyScene = new Scene(dummyRoot);
        SkinService.get().styleScene(dummyScene);
        dummyRoot.applyCss(); 
        dummyRoot.layout();

        // 1. Zeilenhöhe ermitteln
        // TextField fragen: "Wie hoch bist du inkl. Rahmen bei diesem Font?"
        double rowHeight = dummyCell.prefHeight(-1);
        table.setFixedCellSize(rowHeight);

        // 2. Headerhöhe ermitteln (plus Puffer für Sortier-Pfeil)
        dummyHeader.setText("Ag"); // Repräsentativer Text für Höhe
        double headerHeight = dummyHeader.prefHeight(-1) + 5; 

        // 3. Spaltenbreiten berechnen
        double totalWidth = 0;
        
        // TextField-Padding einmalig ermitteln (Insets + Border)
        dummyCell.setText("X"); // Irgendein Text, damit es gerendert wird
        Insets cellInsets = dummyCell.getInsets();
        double textFieldPadding = cellInsets.getLeft() + cellInsets.getRight();
        
        // Border-Width hinzufügen falls vorhanden
        if (dummyCell.getBorder() != null) {
            Insets borderInsets = dummyCell.getBorder().getInsets();
            System.out.println("borderInsets: " + borderInsets);
            textFieldPadding += borderInsets.getLeft() + borderInsets.getRight();
        }
        
        System.out.println("cellInsets: " + cellInsets);
        System.out.println("textFieldPadding: " + textFieldPadding);

        for (int i = 0; i < table.getColumns().size(); i++) {
            TableColumn<String[], ?> col = table.getColumns().get(i);
            
            // A. Header messen
            dummyHeader.setText(headers[i]);
            double maxW = dummyHeader.prefWidth(-1) + 10; // +10 für Sortier-Pfeil

            // B. Alle Inhalte messen (Text + Padding)
            for (String[] row : table.getItems()) {
                String text = (i < row.length) ? row[i] : "";
                javafx.scene.text.Text textMeasure = new javafx.scene.text.Text(text);
                dummyScene.setRoot(new Group(textMeasure));
                textMeasure.applyCss();
                double textWidth = textMeasure.getLayoutBounds().getWidth();
                maxW = Math.max(maxW, textWidth + textFieldPadding);
                System.out.println("Spalte 0 - textWidth: " + textWidth + ", mit padding: " + (textWidth + textFieldPadding));
            }

            col.setPrefWidth(maxW);
            col.setMinWidth(maxW);
            totalWidth += maxW;
            
            System.out.println("rowHeight: " + rowHeight);
            System.out.println("headerHeight: " + headerHeight);
            System.out.println("Anzahl Zeilen: " + table.getItems().size());
            
        }

        // 4. Tabellengröße setzen
        // Breite = Summe aller Spalten + 2px für Tabellen-Rahmen
        table.setPrefWidth(totalWidth + 2);
        System.out.println("totalWidth: " + totalWidth);
        
        // Höhe = Header + (Zeilen * Höhe) + 2px Rahmen
        double totalHeight = headerHeight + (table.getItems().size() * rowHeight) + 2;
        
        /**table.setPrefHeight(totalHeight);
        table.setMinHeight(totalHeight);
        table.setMaxHeight(totalHeight);**/
        
        System.out.println("totalHeight: " + totalHeight);
        
        // Header-Border entfernen, damit berechnete Höhe exakt passt
        table.setStyle("-fx-table-header-border-color: transparent;");
    }

}