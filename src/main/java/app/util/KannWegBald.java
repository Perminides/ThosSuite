package app.util;

import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.text.Text;

/**
 * Utility-Klasse für UI-Helfer-Methoden.
 */
public class KannWegBald {
    
    /**
     * Padding-Wert für TableColumn-Breiten.
     * Beinhaltet Cell-Padding (links + rechts) und Platz für Sortier-Pfeil.
     */
    private static final double COLUMN_PADDING = 25.0;
    
    /**
     * Berechnet die optimale Breite für alle Spalten einer TableView basierend auf Header und Inhalt.
     * Die Spalten werden so breit, dass der komplette Inhalt lesbar ist.
     * 
     * @param table Die TableView, deren Spalten angepasst werden sollen
     */
    public static void autoSizeColumns(TableView<?> table) {
        // Erlaubt, dass die Tabelle breiter wird als der sichtbare Bereich (Scrollbar erscheint)
        table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        
        for (TableColumn<?, ?> column : table.getColumns()) {
            double maxWidth = 0;
            
            // 1. Breite des Headers
            Text headerText = new Text(column.getText());
            maxWidth = headerText.getLayoutBounds().getWidth();
            
            // 2. Breite der Zellen (Text-Node nutzt automatisch den CSS-Font)
            for (int i = 0; i < table.getItems().size(); i++) {
                Object cellData = column.getCellData(i);
                if (cellData != null) {
                    Text cellText = new Text(cellData.toString());
                    double width = cellText.getLayoutBounds().getWidth();
                    maxWidth = Math.max(maxWidth, width);
                }
            }
            
            // 3. Setze die berechnete Breite mit Padding
            column.setPrefWidth(maxWidth + COLUMN_PADDING);
        }
    }
}