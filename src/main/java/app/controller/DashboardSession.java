package app.controller;

import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Locale;

import app.data.AppClock;
import app.data.Deck;
import app.data.SessionSwitchStrategy;
import app.fitbit.FitbitDashboardService;
import app.ui.skin.SkinService;
import app.util.Log;
import javafx.geometry.Pos;
import javafx.scene.layout.Background;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Pane;

public class DashboardSession implements Session {
    
    private FlowPane view;
    
    public DashboardSession() {
        view = new FlowPane();
        view.setHgap(20);
        view.setVgap(20);
        view.setAlignment(Pos.CENTER);
        buildContent(); // Füllt die View
    }
    
    private void buildContent() {
        view.getChildren().clear();
        view.setBackground(new Background(SkinService.get().getBackgroundImage(Deck.WORLD_CARDS)));
        
     // Fitbit-Service erstellen
        FitbitDashboardService fitbitService = new FitbitDashboardService();
        
        // Echte Berechnung
        int stepsNeeded = fitbitService.calculateRemainingDailySteps(AppClock.TODAY);
        
        // Mit Tausenderpunkt formatieren
        String formattedSteps = NumberFormat.getInstance(Locale.GERMANY).format(stepsNeeded);
        
        // Tile erstellen
        view.getChildren().add(
            SkinService.get().createDashboardTile(
            	formattedSteps, 
                "Schritte pro Tag noch nötig"
            )
        );
        
        // Tile erstellen
        view.getChildren().add(
            SkinService.get().createDashboardTile(
            	"" + fitbitService.calculateCurrentStreak(AppClock.TODAY), 
                "Aktueller Fitbit-Streak in Wochen (Rekord: " + fitbitService.calculateRecordStreak() + ")"
            )
        );
    }
    
    @Override
    public void start() {
    }
    
    @Override
    public Pane getView() {
        return view;
    }
    
    @Override
    public void refresh() {
        // Dashboard neu bauen bei Skin-Wechsel
    	buildContent();
    }
    
    @Override
    public void closeSilent(boolean save) {
        // Nichts zu speichern
    }
    
    @Override
    public void endGracefully() {
        // Keine Summary nötig
    }
    
    @Override
    public void escClicked() {
        // Nichts zu canceln
    }
    
    @Override
    public void sort(app.data.CardSortOrder order) {
        // Keine Sortierung
    }
    
    @Override
    public void endPause() {
        // Keine Pause
    }
    
    @Override
    public SessionSwitchStrategy getSwitchStrategy() {
        return SessionSwitchStrategy.IMMEDIATE;
    }
}