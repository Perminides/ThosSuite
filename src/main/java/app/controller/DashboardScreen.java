package app.controller;

import java.text.NumberFormat;
import java.util.Locale;

import app.alc.repository.Repository;
import app.fitbit.DashboardService;
import app.mattress.repository.MattressRepository;
import app.messaging.repository.MessageRepository;
import app.shared.AppClock;
import app.shared.Config;
import app.shared.Screen;
import app.shared.model.SessionSwitchStrategy;
import app.shared.skin.SkinService;
import app.weekday.model.WeekdayStats;
import app.weekday.repository.WeekdayRepository;
import javafx.geometry.Pos;
import javafx.scene.layout.Background;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Pane;

public class DashboardScreen implements Screen {
    
    private FlowPane view;
    
    public DashboardScreen() {
        view = new FlowPane();
        view.setHgap(20);
        view.setVgap(20);
        view.setAlignment(Pos.CENTER);
        buildContent(); // Füllt die View
    }
    
    private void buildContent() {
        view.getChildren().clear();
        view.setBackground(new Background(SkinService.get().getEmptyBackgroundImage()));
        
     // Fitbit-Service erstellen
        DashboardService fitbitService = new DashboardService();
        
        // Echte Berechnung
        int stepsNeeded = fitbitService.calculateRemainingDailySteps(AppClock.TODAY);
        
        // Mit Tausenderpunkt formatieren
        String formattedSteps = NumberFormat.getInstance(Locale.GERMANY).format(stepsNeeded);
        
        // Erstes Fitbit-Tile erstellen
        view.getChildren().add(
            SkinService.get().createDashboardTile(
            	formattedSteps, 
                "Schritte pro Tag noch nötig"
            )
        );
        
        // Zweites Fitbit-Tile erstellen
        view.getChildren().add(
            SkinService.get().createDashboardTile(
            	"" + fitbitService.calculateCurrentStreak(AppClock.TODAY), 
                "Aktueller Fitbit-Streak in Wochen (Rekord: " + fitbitService.calculateRecordStreak() + ")"
            )
        );
        
        Repository alcoholRepo = new Repository();
        int balance = alcoholRepo.getCurrentBalance();
        
        // Alkohol-Tile erstellen
        view.getChildren().add(
            SkinService.get().createDashboardTile(
                "" + balance, 
                "Aktueller Alkoholkontostand"
            )
        );
        
        WeekdayRepository wr = new WeekdayRepository();
        WeekdayStats ws = wr.getWeekdayStats();
        
        // Wochentags-Tile erstellen
        view.getChildren().add(
                SkinService.get().createDashboardTile(
                	"" + ws.currentStreak(), 
                    "Aktueller Wochentags-Streak in Tagen (Rekord: " + ws.maxStreak() + ")"
                )
            );
        
        MattressRepository mr = new MattressRepository();
        long daysUntilMattressTurn = mr.getDaysUntilNextTurn();
        
        // Bett-Tile erstellen
        view.getChildren().add(
            SkinService.get().createDashboardTile(
                "" + daysUntilMattressTurn, 
                "Tage bis zum Wenden der Matratze"
            )
        );
        
        MessageRepository sr = new MessageRepository();
        int messagesToday = sr.getMessageCountToday();
        
        // Nachrichten-Tile erstellen
        view.getChildren().add(
                SkinService.get().createDashboardTile(
                	"" + messagesToday, 
                    "Heute importierte Nachrichten")
                );
        
        int daysSinceLastAdditionalTmdbImport = Config.getDaysSince("tmdb.lastAdditionalImportRun");
        
        // Tmdb-Tile erstellen
        view.getChildren().add(
                SkinService.get().createDashboardTile(
                	"" + daysSinceLastAdditionalTmdbImport, 
                    "Tage seit letztem Extra-TMDB-Import")
                );
        
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
    public SessionSwitchStrategy getSwitchStrategy() {
        return SessionSwitchStrategy.IMMEDIATE;
    }
}