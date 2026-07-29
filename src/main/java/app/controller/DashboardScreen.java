package app.controller;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import app.alc.repository.AlcRepository;
import app.fitbit.DashboardService;
import app.mattress.repository.MattressRepository;
import app.messaging.repository.MessageRepository;
import app.shared.AppClock;
import app.shared.Config;
import app.shared.model.DashboardTileData;
import app.shared.model.Screen;
import app.shared.model.ScreenView;
import app.shared.model.SessionSwitchStrategy;
import app.shared.ui.DashboardScreenView;
import app.weekday.model.WeekdayStats;
import app.weekday.repository.WeekdayRepository;

public class DashboardScreen implements Screen {

    private DashboardScreenView view = new DashboardScreenView();

    public DashboardScreen() {
        buildContent();
    }

    private void buildContent() {
        List<DashboardTileData> tiles = new ArrayList<>();

        DashboardService fitbitService = new DashboardService();
        int stepsNeeded = fitbitService.calculateRemainingDailySteps(AppClock.TODAY);
        if (stepsNeeded < 0)
        	stepsNeeded = 0;
        String formattedSteps = NumberFormat.getInstance(Locale.GERMANY).format(stepsNeeded);
        tiles.add(new DashboardTileData(
            	formattedSteps,
                "Schritte pro Tag noch nötig"
            ));

        tiles.add(new DashboardTileData(
            	"" + fitbitService.calculateCurrentStreak(AppClock.TODAY),
                "Aktueller Fitbit-Streak in Wochen (Rekord: " + fitbitService.calculateRecordStreak() + ")"
            ));

        AlcRepository alcoholRepo = new AlcRepository();
        int balance = alcoholRepo.getCurrentBalance();
        tiles.add(new DashboardTileData(
                "" + balance,
                "Aktueller Alkoholkontostand"
            ));

        WeekdayRepository wr = new WeekdayRepository();
        WeekdayStats ws = wr.getWeekdayStats();
        tiles.add(new DashboardTileData(
            	"" + ws.currentStreak(),
                "Aktueller Wochentags-Streak in Tagen (Rekord: " + ws.maxStreak() + ")"
            ));

        MattressRepository mr = new MattressRepository();
        long daysUntilMattressTurn = mr.getDaysUntilNextTurn();
        tiles.add(new DashboardTileData(
                "" + daysUntilMattressTurn,
                "Tage bis zum Wenden der Matratze"
            ));

        MessageRepository sr = new MessageRepository();
        int messagesToday = sr.getMessageCountToday();
        tiles.add(new DashboardTileData(
            	"" + messagesToday,
                "Heute importierte Nachrichten"));

        int daysSinceLastAdditionalTmdbImport = Config.getDaysSince("tmdb.lastAdditionalImportRun");
        tiles.add(new DashboardTileData(
            	"" + daysSinceLastAdditionalTmdbImport,
                "Tage seit letztem Extra-TMDB-Import"));

        view.build(tiles);

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
