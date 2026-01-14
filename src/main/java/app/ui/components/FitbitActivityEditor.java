package app.ui.components;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import app.fitbit.json.Activity;
import app.fitbit.json.ActivityDaySummary;
import app.ui.components.SimpleTextAreaTableDialog;
import javafx.stage.Window;

/**
 * Fitbit-spezifischer Wrapper um AutoSizedTableInDialog.
 * 
 * Zeigt Activities eines Tages in editierbarer Tabelle an. Fügt automatisch eine synthetische "Steps"-Zeile hinzu mit den Gesamtschritten des Tages.
 * 
 * Der User kann: - Activity-Namen ändern - Distanzen korrigieren - Steps anpassen (auch die Gesamtschritte!)
 * 
 * ❌ Woran wir gescheitert sind: TableView Auto-Sizing Ziel: TableView die sich automatisch an Inhalt anpasst - ohne Magic Numbers, funktioniert mit allen
 * Skins/Fonts/Auflösungen Versuch 1: Geminis MVP (AutoSizedTableInDialog)
 * 
 * Ansatz: Text-Node für Messung, Magic Numbers für TextField-Padding (35px) Problem: Magic Numbers für verschiedene Konstanten (CELL_PADDING, ROW_HEIGHT,
 * TEXT_FIELD_PADDING, HEADER_HEIGHT) Ergebnis: Funktionierte, aber gegen Prinzip "keine Magic Numbers"
 * 
 * Versuch 2: TextField.prefWidth() für Padding-Messung
 * 
 * Ansatz: TextField erstellen, prefWidth(-1) abfragen, mit Text-Breite vergleichen Problem: prefWidth(-1) gab Default-Breite (256px) statt content-basierte
 * Breite zurück Ergebnis: TextField-Padding = 245px (viel zu groß), alle Spalten gleich breit, horizontaler Scrollbalken
 * 
 * Versuch 3: TextField.getInsets() + getBorder()
 * 
 * Ansatz: CSS-Padding und Border-Width direkt abfragen Problem: Funktionierte teilweise (textFieldPadding = 44px korrekt), ABER horizontaler Scrollbalken blieb
 * Ursache: Teufelskreis - falsche Höhenberechnung → vertikaler Scrollbalken → nimmt Breite weg → horizontaler Scrollbalken Ergebnis: Breite fast korrekt (561px
 * vs 600px bei Gemini), aber Scrollbalken-Problem unlösbar
 * 
 * Versuch 4: Echte Label/TextField-Komponenten messen (Geminis Ansatz)
 * 
 * Ansatz: Label für Header, TextField für Zellen, in dummy Scene mit SkinService stylen, dann prefHeight() und prefWidth() abfragen Problem Header:
 * prefWidth(-1) funktionierte für Label Problem Zellen: prefWidth(-1) gab wieder Default-Breite (256px) statt Text-basiert Ergebnis: Zurück zu Text-Messung +
 * Insets, aber Problem mit Höhe/Scrollbalken blieb
 * 
 * Versuch 5: Höhe nicht setzen
 * 
 * Ansatz: Keine setPrefHeight() mehr, TableView zeigt Default-Anzahl Zeilen Problem: TableView zeigt ~10-15 Zeilen statt nur der tatsächlichen Zeilen, Dialog
 * zu groß Ergebnis: Vertikaler Scrollbalken weg, aber horizontaler blieb (nur wenige Pixel zu schmal)
 * 
 * 
 * Kern-Probleme mit TableView Auto-Sizing
 * 
 * TextField.prefWidth(-1) unzuverlässig: Gibt Default-Breite statt content-basierte Breite Versteckte Paddings/Borders: Nicht alle Abstände sind über
 * Insets/Border messbar Scrollbalken-Teufelskreis: Vertikaler Scrollbalken → horizontaler Scrollbalken wegen Breitenverlust Parent-Window-Problem: Mit
 * Parent-Window wird Dialog zu schmal (Ursache unklar) TableView-Komplexität: Viele versteckte Layout-Mechanismen, schwer zu debuggen
 * 
 * 
 * Lessons Learned
 * 
 * TableView ist zu komplex für "einfache" Auto-Sizing-Anforderungen Magic Numbers vermeiden ist richtig, aber extrem schwierig bei JavaFX-Komponenten
 * TextField-Komponenten haben Default-Größen die nichts mit Inhalt zu tun haben Measurement-Ansätze: Text-Nodes funktionieren gut, UI-Komponenten (TextField,
 * Label) nicht zuverlässig für prefWidth
 * 
 * 
 * Nächste Schritte (Tendenz) GridPane mit TextFields:
 * 
 * Volle Kontrolle über Layout Explizite Spaltenbreiten basierend auf gemessenem Text + konstantem Padding Eigenes Edit-Handling (onClick → TextField aktiv)
 * Keine versteckten TableView-Mechanismen
 * 
 * Alternative (aktuell):
 * 
 * TextArea mit " | " Trenner (funktioniert, sieht aber "scheiße aus") Später verschönern wenn Nerven vorhanden
 * 
 */
public class FitbitActivityEditor {
    
    private final Window parent;
    private final LocalDate date;
    private final List<Activity> activities;
    private final ActivityDaySummary daySummary;
    
    public FitbitActivityEditor(Window parent, LocalDate date, List<Activity> activities, ActivityDaySummary daySummary) {
        this.parent = parent;
        this.date = date;
        this.activities = activities;
        this.daySummary = daySummary;
    }
    
    /**
     * Zeigt den Dialog mit editierbarer Tabelle.
     * 
     * @return Optional mit editierten Activities UND korrigierten Gesamtschritten,
     *         oder empty() bei Abbruch
     */
    public Optional<EditorResult> showAndWait() {
        // 1. Daten in String-Arrays konvertieren
        List<String[]> tableData = convertToTableData();
        
        // 2. TextArea-Dialog zeigen
        SimpleTextAreaTableDialog dialog = new SimpleTextAreaTableDialog(
            parent, 
            "Aktivitäten bearbeiten vom " + date
        );
        Optional<List<String[]>> resultOpt = dialog.showAndWait(tableData);
        
        if (resultOpt.isEmpty()) {
            return Optional.empty(); // User hat abgebrochen
        }
        
        // 3. String-Arrays zurück in Activities konvertieren
        return Optional.of(convertFromTableData(resultOpt.get()));
    }
    
    /**
     * Konvertiert Activities + DaySummary in String-Arrays für die Tabelle.
     * 
     * Format:
     * [0] = Header: ["Start Time", "Activity Name", "Distance Unit", "Distance", "Steps"]
     * [1] = Steps-Zeile: ["(gesamt)", "Steps", "", "", "14171"]
     * [2+] = Activities
     */
    private List<String[]> convertToTableData() {
        List<String[]> data = new ArrayList<>();
        
        // Header
        data.add(new String[]{
            "StartTime", 
            "ActivityName", 
            "DistanceUnit", 
            "Distance", 
            "Steps"
        });
        
        // Synthetische Steps-Zeile (immer erste Datenzeile)
        data.add(new String[]{
            "(gesamt)",
            "Steps",
            "",
            "",
            String.valueOf(daySummary.getSummary().getSteps())
        });
        
        // Echte Activities
        for (Activity activity : activities) {
            data.add(new String[]{
                activity.getStartTime(),
                activity.getActivityName(),
                activity.getDistanceUnit() != null ? activity.getDistanceUnit() : "",
                activity.getDistance() != null ? String.valueOf(activity.getDistance()) : "",
                String.valueOf(activity.getSteps())
            });
        }
        
        return data;
    }
    
    /**
     * Konvertiert String-Arrays zurück in Activities + korrigierte Gesamtschritte.
     * 
     * @param tableData Die Daten aus dem Dialog (Header + Zeilen)
     * @return EditorResult mit Activities und korrigierten Steps
     */
    private EditorResult convertFromTableData(List<String[]> tableData) {
        // tableData[0] = Header (ignorieren)
        // tableData[1] = Steps-Zeile
        // tableData[2+] = Activities
        
        // Steps aus erster Datenzeile extrahieren
        String[] stepsRow = tableData.get(1);
        int correctedTotalSteps = Integer.parseInt(stepsRow[4]);
        
        // Activities aus restlichen Zeilen
        List<Activity> editedActivities = new ArrayList<>();
        
        for (int i = 2; i < tableData.size(); i++) {
            String[] row = tableData.get(i);
            
            // Original-Activity finden (via Index)
            Activity original = activities.get(i - 2);
            
            // Original-Activity kopieren und editierte Werte setzen
            Activity edited = new Activity(
                original.getStartTime(),
                original.getActivityName(),
                original.getDistanceUnit(),
                original.getDistance(),
                original.getSteps()
            );
            
            // Editierte Werte überschreiben
            edited.setStartTime(row[0]);
            edited.setActivityName(row[1]);
            edited.setDistanceUnit(row[2].isEmpty() ? null : row[2]);
            edited.setDistance(row[3].isEmpty() ? null : Double.parseDouble(row[3]));
            edited.setSteps(Integer.parseInt(row[4]));
            edited.setOriginalStartTime(original.getOriginalStartTime());
            
            editedActivities.add(edited);
        }
        
        return new EditorResult(editedActivities, correctedTotalSteps);
    }
    
    /**
     * Ergebnis des Editors: Editierte Activities + korrigierte Gesamtschritte.
     */
    public record EditorResult(List<Activity> activities, int totalSteps) {}
}