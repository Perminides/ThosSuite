package app.controller;

import java.text.NumberFormat;
import java.util.Locale;

import app.alc.repository.AlcRepository;
import app.fitbit.DashboardService;
import app.mattress.repository.MattressRepository;
import app.messaging.repository.MessageRepository;
import app.shared.AppClock;
import app.shared.Config;
import app.shared.model.SessionSwitchStrategy;
import app.shared.skin.SkinService;
import app.shared.ui.components.DashboardTile;
import app.shared.ui.contracts.Screen;
import app.shared.ui.contracts.ScreenView;
import app.shared.ui.surfaces.DashboardScreenView;
import app.weekday.model.WeekdayStats;
import app.weekday.repository.WeekdayRepository;

public class DashboardScreen implements Screen {
    
    private DashboardScreenView view = new DashboardScreenView();
    
    public DashboardScreen() {
        buildContent();
    }
    
    private void buildContent() {
     
        DashboardService fitbitService = new DashboardService();
        int stepsNeeded = fitbitService.calculateRemainingDailySteps(AppClock.TODAY);
        String formattedSteps = NumberFormat.getInstance(Locale.GERMANY).format(stepsNeeded);
        DashboardTile fitbitTile1 = SkinService.get().createDashboardTile(
            	formattedSteps, 
                "Schritte pro Tag noch nötig"
            );
        
        DashboardTile fitbitTile2 = SkinService.get().createDashboardTile(
            	"" + fitbitService.calculateCurrentStreak(AppClock.TODAY), 
                "Aktueller Fitbit-Streak in Wochen (Rekord: " + fitbitService.calculateRecordStreak() + ")"
            );
        
        AlcRepository alcoholRepo = new AlcRepository();
        int balance = alcoholRepo.getCurrentBalance();
        DashboardTile alcTile = SkinService.get().createDashboardTile(
                "" + balance, 
                "Aktueller Alkoholkontostand"
            );
        
        WeekdayRepository wr = new WeekdayRepository();
        WeekdayStats ws = wr.getWeekdayStats();
        DashboardTile weekDayTile = SkinService.get().createDashboardTile(
            	"" + ws.currentStreak(), 
                "Aktueller Wochentags-Streak in Tagen (Rekord: " + ws.maxStreak() + ")"
            );
        
        MattressRepository mr = new MattressRepository();
        long daysUntilMattressTurn = mr.getDaysUntilNextTurn();
        DashboardTile mattressTile = SkinService.get().createDashboardTile(
                "" + daysUntilMattressTurn, 
                "Tage bis zum Wenden der Matratze"
            );
        
        MessageRepository sr = new MessageRepository();
        int messagesToday = sr.getMessageCountToday();
        DashboardTile messageTile = SkinService.get().createDashboardTile(
            	"" + messagesToday, 
                "Heute importierte Nachrichten");
        
        int daysSinceLastAdditionalTmdbImport = Config.getDaysSince("tmdb.lastAdditionalImportRun");
        DashboardTile movieTile = SkinService.get().createDashboardTile(
            	"" + daysSinceLastAdditionalTmdbImport, 
                "Tage seit letztem Extra-TMDB-Import");
        
        view.build(fitbitTile1, fitbitTile2, alcTile, weekDayTile, mattressTile, messageTile, movieTile);
        
    }
    
    @Override
    public ScreenView getView() {
    	return view;
    }
    
    @Override
    public void refresh() {
    	buildContent();
    }
    
    @Override
    public SessionSwitchStrategy getSwitchStrategy() {
        return SessionSwitchStrategy.IMMEDIATE;
    }
}