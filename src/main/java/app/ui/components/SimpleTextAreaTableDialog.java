package app.ui.components;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import app.ui.skin.SkinService;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

/**
 * Editierbare Tabelle als TextArea mit " | "-getrennten Werten.
 * 
 * <h3>Warum TextArea statt TableView?</h3>
 * <p>Nach stundenlangem Kampf mit JavaFX TableView aufgegeben:
 * 
 * <h4>Versuch 1: Text-Node Messung + TextField-Padding (Magic Number 35px)</h4>
 * <ul>
 *   <li>Problem: Magic Number funktioniert nicht für alle Skins/Fonts/Auflösungen</li>
 * </ul>
 * 
 * <h4>Versuch 2: TextField.prefWidth(-1) für Padding-Messung</h4>
 * <ul>
 *   <li>Problem: prefWidth(-1) gab Default-Breite (256px) statt content-basiert</li>
 *   <li>Resultat: Alle Spalten gleich breit, horizontaler Scrollbalken</li>
 * </ul>
 * 
 * <h4>Versuch 3: TextField.getInsets() + getBorder()</h4>
 * <ul>
 *   <li>Problem: Padding korrekt gemessen (44px), aber Scrollbalken-Teufelskreis</li>
 *   <li>Ursache: Falsche Höhe → vertikaler Scrollbalken → nimmt Breite → horizontaler Scrollbalken</li>
 * </ul>
 * 
 * <h4>Versuch 4: Label/TextField-Komponenten mit applyCss() messen (Geminis Ansatz)</h4>
 * <ul>
 *   <li>Problem: prefWidth(-1) für TextField gibt wieder Default-Breite statt Text-basiert</li>
 *   <li>Resultat: Alle Spalten 256px breit</li>
 * </ul>
 * 
 * <h4>Kern-Probleme mit TableView Auto-Sizing:</h4>
 * <ul>
 *   <li>TextField.prefWidth(-1) ist unzuverlässig (gibt Default-Breite statt content-basiert)</li>
 *   <li>Versteckte Paddings/Borders nicht alle über Insets/Border messbar</li>
 *   <li>Scrollbalken-Teufelskreis (vertikal → horizontal wegen Breitenverlust)</li>
 *   <li>TableView-Komplexität mit vielen versteckten Layout-Mechanismen</li>
 * </ul>
 * 
 * <h4>Mögliche zukünftige Lösung: Binäre Suche</h4>
 * <p>PostLayoutPulseListener + binäre Suche nach optimaler Breite:
 * <ul>
 *   <li>Start bei 500px, prüfe ob Scrollbar sichtbar</li>
 *   <li>Scrollbar sichtbar → zu schmal → erhöhe lowerBound</li>
 *   <li>Keine Scrollbar → passt → senke upperBound</li>
 *   <li>Wiederhole bis Differenz < 10px</li>
 *   <li>Opacity 0 während Suche, dann 1 → User sieht nur perfektes Ergebnis</li>
 * </ul>
 * 
 * <h4>Aktuelle Lösung: TextArea</h4>
 * <p>Simple, funktioniert, keine Layout-Probleme. User editiert Text direkt.
 * Bei kaputten Daten (falsche Spaltenanzahl) wird Dialog wiederholt.
 * 
 * <p><b>Lessons Learned:</b>
 * <ul>
 *   <li>TableView ist zu komplex für einfaches Auto-Sizing</li>
 *   <li>TextField-Komponenten haben Default-Größen unabhängig vom Inhalt</li>
 *   <li>Manchmal ist die "primitive" Lösung (TextArea) besser als die "richtige" (TableView)</li>
 * </ul>
 */
public class SimpleTextAreaTableDialog {
    
    private final Window parent;
    private final String title;
    private final int expectedColumnCount;
    
    public SimpleTextAreaTableDialog(Window parent, String title) {
        this.parent = parent;
        this.title = title;
        this.expectedColumnCount = -1; // Wird beim ersten Aufruf gesetzt
    }
    
    /**
     * Zeigt editierbare Tabelle als Text.
     * 
     * @param rawData Liste von String-Arrays. Index 0 = Header, Rest = Daten
     * @return Editierte Daten oder empty() bei Abbruch
     */
    public Optional<List<String[]>> showAndWait(List<String[]> rawData) {
        if (rawData == null || rawData.isEmpty()) {
            return Optional.empty();
        }
        
        // Expected Column Count aus Header ermitteln
        int expectedCols = rawData.get(0).length;
        
        // Konvertiere zu Tab-getrenntem Text
        String text = convertToText(rawData);
        
        while (true) {
            // Dialog zeigen
            Optional<String> resultOpt = showTextDialog(text, rawData.size());
            
            if (resultOpt.isEmpty()) {
                return Optional.empty(); // User hat abgebrochen
            }
            
            String editedText = resultOpt.get();
            
            // Parse zurück zu String-Arrays
            try {
                List<String[]> parsed = parseText(editedText, expectedCols);
                return Optional.of(parsed);
                
            } catch (ValidationException e) {
                // Validation fehlgeschlagen - zeige Fehler und wiederhole
                Alert alert = SkinService.get().createAlert(
                    parent,
                    "Ungültige Daten",
                    e.getMessage() + "\n\nBitte Daten korrigieren.",
                    false,
                    false
                );
                alert.showAndWait();
            }
        }
    }
    
    /**
     * Zeigt TextArea-Dialog.
     */
    private Optional<String> showTextDialog(String text, int rowCount) {
        Dialog<String> dialog = (Dialog<String>) SkinService.get().createDialog(parent, title);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        
        // TextArea erstellen
        TextArea textArea = new TextArea(text);
        textArea.setWrapText(false);
        
        // Sizing basierend auf Inhalt
        int maxLineLength = Arrays.stream(text.split("\n"))
            .mapToInt(String::length)
            .max()
            .orElse(50);
        
        textArea.setPrefColumnCount(40); // Sollten reichen für den zu erwartenden Text
        textArea.setPrefRowCount(rowCount);
        
        // Tab-Handling: Tab einfügen statt Focus wechseln
        textArea.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.TAB) {
                textArea.insertText(textArea.getCaretPosition(), "\t");
                e.consume();
            }
        });
        
        VBox content = new VBox(textArea);
        content.getStyleClass().add("my-dialog-vbox");
        content.setPadding(new Insets(10));
        dialog.getDialogPane().setContent(content);
        
        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                return textArea.getText();
            }
            return null;
        });
        
        return dialog.showAndWait();
    }
    
    /**
     * Konvertiert String-Arrays zu Tab-getrenntem Text.
     */
    private String convertToText(List<String[]> data) {
        StringBuilder sb = new StringBuilder();
        
        for (String[] row : data) {
            sb.append(String.join(" | ", row));
            sb.append("\n");
        }
        
        return sb.toString().trim();
    }
    
    /**
     * Parst Tab-getrennten Text zurück zu String-Arrays.
     * 
     * @throws ValidationException wenn Spaltenanzahl nicht stimmt
     */
    private List<String[]> parseText(String text, int expectedColumnCount) throws ValidationException {
        String[] lines = text.split("\n");
        List<String[]> result = new ArrayList<>();
        
        if (lines.length == 0) {
            throw new ValidationException("Text ist leer!");
        }
        
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            
            if (line.isEmpty()) {
                continue; // Leere Zeilen überspringen
            }
            
            String[] columns = line.split(" \\| ", -1); // -1 um trailing empty strings zu behalten
            
            if (columns.length != expectedColumnCount) {
                throw new ValidationException(
                    String.format("Zeile %d hat %d Spalten, erwartet %d",
                        i + 1, columns.length, expectedColumnCount)
                );
            }
            
            result.add(columns);
        }
        
        if (result.isEmpty()) {
            throw new ValidationException("Keine Daten vorhanden!");
        }
        
        return result;
    }
    
    /**
     * Exception für Validierungs-Fehler.
     */
    private static class ValidationException extends Exception {
        public ValidationException(String message) {
            super(message);
        }
    }
}