package app.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import app.alc.AlcStatisticsScreen;
import app.alc.StartupService;
import app.controller.model.AnkiPlayItem;
import app.controller.model.PlayMenuNode;
import app.controller.model.RegionPlayItem;
import app.diary.DiaryEditorPresenter;
import app.diary.DiaryScreen;
import app.fitbit.DataFetcher;
import app.fitbit.DataReviewService;
import app.fitbit.FitbitStatisticsScreen;
import app.learn.ImageScaler;
import app.learn.anki.AnkiDeckService;
import app.learn.anki.AnkiDeckSession;
import app.learn.anki.AnkiPlaySetup;
import app.learn.anki.model.AnkiLearnSessionInfo;
import app.learn.anki.model.Card;
import app.learn.model.Deck;
import app.learn.model.DeckCategory;
import app.learn.model.LearnSessionInfo;
import app.learn.model.MapShape;
import app.learn.region.RegionDeckService;
import app.learn.region.RegionPlaySetup;
import app.learn.region.RegionSession;
import app.learn.region.model.RegionLearnSessionInfo;
import app.learn.region.model.RegionPlaySelection;
import app.mattress.MattressTurnDialog;
import app.messaging.signal.SignalIncrementalImport;
import app.messaging.whatsapp.WhatsAppIncrementalImport;
import app.movie.MovieCleanup;
import app.movie.MovieImporter;
import app.movie.MovieViewerScreen;
import app.movie.SeriesImporter;
import app.shared.Config;
import app.shared.Log;
import app.shared.UiUtils;
import app.shared.model.ButtonEnum;
import app.shared.model.Screen;
import app.shared.skin.Skin;
import app.shared.skin.SkinImageCache;
import app.shared.skin.SkinService;
import app.shared.ui.Alerts;
import app.tmp.Comparison;
import app.weekday.WeekdayDialog;
import javafx.application.Platform;

/**
 * Orchestrates session lifecycle:
 * 1. Creates session (which creates presenter + view container and progress)
 * 2. Shows view container in MainWindow once
 * 3. Starts session logic
 * 
 * On skin change: session.refresh() rebuilds the pane inside the container.
 * MainWindow continues showing the same container - no re-wiring needed.
 */
public class Controller{
	private final AnkiDeckService ankiDeckService;
	private final RegionDeckService regionDeckService;
	
    private MainWindow mainWindow;
    private Screen currentScreen;
    private DataFetcher fitbitDataFetcher;
    // Fehler der drei Start-Importe. Sie werden im PreTask nur gemerkt und erst im PostTask
    // gemeldet — während des Splashs gibt es kein Hauptfenster, und über den Splash gehört kein
    // Dialog. Siehe runPreTasks.
    private Exception fitbitError;
    private Exception movieImportError;
    private Comparison comparison;        // !tmp
    private Exception comparisonError;    // !tmp

    
    public enum SessionSwitchAction {
        SAVE_AND_SWITCH,
        DISCARD_AND_SWITCH,
        CANCEL
    }
    
    public Controller(MainWindow mainWindow) throws InterruptedException {
    	this.mainWindow = mainWindow;
    	//Wenn sich herausstellt, dass eh nur der Controller die ganzen Menü-Events erhält,
    	//dann darf das MainWindow auch den Controller kennen und die Methoden direkt aufrufen.
    	//Außer Claude erklärt mir, was an dieser zirkulären Beziehung nun so gefährlich sein soll...
    	//Claude meinte raus damit, aber ich habe mich mittlerweile so dran gewöhnt *lol*
    	mainWindow.setEscPressedRunnable(this::escPressed);
    	mainWindow.setPausePressedRunnable(this::pausePressed);
    	mainWindow.setCloseRunnable(this::closeSelected);
    	mainWindow.setQuitRunnable(() -> requestSessionSwitch(Platform::exit));
    	mainWindow.setLearnSessionConsumer(this::onLearnMenuItemSelected);
    	mainWindow.setSortChangedRunnable(this::sortOrderChanged);
    	mainWindow.setSkinChangeConsumer(this::newSkinSelected);
    	mainWindow.setReloadSkinRunnable(this::triggerSkinRefresh);
    	mainWindow.setStatisticsConsumer(this::onStatisticsMenuItemSelected);
    	mainWindow.setDiaryCreateRunnable(this::diaryCreateSelected);
    	mainWindow.setDiaryViewRunnable(this::diaryViewSelected);
    	mainWindow.setWeekdayRunnable(this::weekdaySelected);
    	mainWindow.setMattressRunnable(this::mattressSelected);
    	mainWindow.setExportRunnable(this::exportSelected);
    	mainWindow.setMovieRunnable(this::movieSelected);
    	mainWindow.setExtraTmdbImportRunnable(this::additionalTmdbImportSelected);
    	mainWindow.setPlayItemConsumer(this::onPlayMenuItemSelected);	
    	showStartScreen();
    	
    	ankiDeckService = new AnkiDeckService();

    	// Die großen Kartenbilder vorwärmen, solange der Splash noch steht. Das Feature nennt nur die
    	// Kartennamen; Pfade und Bilder bleiben auf der Skin-Seite.
    	for (String mapName : ankiDeckService.getImageMapNames())
    		SkinImageCache.getInstance().warmMapImages(mapName);
    	regionDeckService = new RegionDeckService();
    	
    	setLearnMenuItemLabels();
    	setPlayMenuItemLabels();
    }
    /**
     * Wird VOR initializeMainWindow aufgerufen (Splash noch sichtbar). Holt Daten im UI-Thread, blockiert aber die App - Splash bleibt sichtbar.
     *
     * <p><b>Ein toter Dienst darf den Start nicht reißen.</b> Fitbit, TMDB und Health sind fremde
     * Server; dass einer gerade nicht erreichbar ist, ist ein alltäglicher Zustand und kein
     * Programmierfehler — dieselbe Begründung wie beim fehlenden Attachment im Architekturdokument.
     * Alle drei werfen deshalb ehrlich, und hier, an der Orchestrierungs-Grenze, wird einmal
     * entschieden, das auszuhalten. Die bewusste Ausnahme von FailFast steht also sichtbar dort, wo
     * die Regel gemacht wird, statt in den Datenklassen vergraben zu sein.</p>
     *
     * <p>Gemeldet wird nichts von hier: über dem Splash gehört kein Dialog hin. Die Fehler werden
     * gemerkt und in {@link #runPostTasks()} zu <b>einer</b> Meldung zusammengefasst.</p>
     */
    public void runPreTasks() {
        fitbitDataFetcher = new DataFetcher(); // Muss Instanzvariable sein, weil wir Daten für den PostTask übergeben. Das macht tmdb sauberer, wie ich finde...
        if (Config.get("offline", "false").equals("false")) {
            try {
                fitbitDataFetcher.fetch();
            } catch (Exception e) {
                fitbitError = e;
                Log.error(this.getClass(), "Fitbit-Abruf fehlgeschlagen", e);
            }

            try {
                new MovieImporter().run();
            } catch (Exception e) {
                movieImportError = e;
                Log.error(this.getClass(), "TMDB-Import fehlgeschlagen", e);
            }

            // !tmp: Health-Vergleich mitlaufen lassen (Übergang bis Fitbit-Abschaltung).
            try {
                comparison = new Comparison();
                comparison.fetch(fitbitDataFetcher.getProjection());
            } catch (Exception e) {
                comparisonError = e;
                Log.error(this.getClass(), "Health-Vergleich fehlgeschlagen", e);
            }
        }
    }
     
     
    /**
     * Wird NACH splashStage.close() aufgerufen (Splash weg, MainWindow sichtbar).
     * Registriert das MainWindow im SkinService, zeigt Dialoge und speichert Daten.
     */
    public void runPostTasks() {
        // Owner-Stage registrieren VOR allen Dialogen
    	UiUtils.setOwnerWindow(mainWindow.getStage());

        // Die Fehler der drei Start-Importe in EINER Meldung. An einem Tag ohne Netz wären es sonst
        // drei Alerts hintereinander, die alle dasselbe sagen.
        List<String> failed = new ArrayList<>();
        if (fitbitError != null)
            failed.add("Fitbit: " + fitbitError.getMessage());
        if (movieImportError != null)
            failed.add("TMDB: " + movieImportError.getMessage());
        if (comparisonError != null)
            failed.add("Health-Vergleich: " + comparisonError.getMessage());

        if (!failed.isEmpty())
            Alerts.show("Importe fehlgeschlagen", String.join("\n\n", failed), ButtonEnum.OK);

        // Jeder Folgeschritt hängt daran, ob SEIN Import geklappt hat.
        if (fitbitError == null && fitbitDataFetcher.hasData())
            new DataReviewService(fitbitDataFetcher).showDialogsAndSave();

        // !tmp: Bei Fehler kein Popup — comparison wäre nur teilbefüllt.
        if (comparisonError == null && comparison != null) {
            comparison.showPopup();
        }
     
        StartupService alcoholService = new StartupService();
        alcoholService.checkAndPrompt();
     
        new DiaryEditorPresenter().showNew();
     
        new WeekdayDialog().showForDaily();
     
        new MattressTurnDialog().showIfDue();
     
        new MovieCleanup().run();
     
        try {
            new SignalIncrementalImport().run();
        } catch (Exception e) {
            Log.error(this.getClass(), "", e);
            Alerts.show("Signal", "Beim Signalimport ist was schiefgelaufen.\nEs wurde nichts in die DB geschrieben.\nBitte anschauen.", ButtonEnum.OK);
        }
     
        try {
            new WhatsAppIncrementalImport().run();
        } catch (Exception e) {
            Log.error(this.getClass(), "", e);
            Alerts.show("WhatsApp", "Beim WhatsApp-Import ist was schiefgelaufen.\nEs wurde nichts in die DB geschrieben.\nBitte anschauen.", ButtonEnum.OK);
        }
     
        ImageScaler.processImages();
    }
    
    public void sessionEnded() {
    	//!Idee: Endbedingungen integrieren...
    	setLearnMenuItemLabels();
    	showStartScreen();
    }
	
	public void onLearnMenuItemSelected(LearnSessionInfo info) {
	    requestSessionSwitch(() -> {
	    	if (info instanceof AnkiLearnSessionInfo anki) {
				List<Card> dueCards = ankiDeckService.getDueCards(anki.getDeckType()); // Analog dem freien Spiel liegt das Holen der Karten hier noch im Controller. Könnte man diskutieren, aber warum?
				currentScreen = AnkiDeckSession.forLearning(dueCards, this::sessionEnded, ankiDeckService, anki.getDeckType());
		    } else if (info instanceof RegionLearnSessionInfo region) {
		    	Set<MapShape> regions = regionDeckService.getRegions(region.getSpec());
		        currentScreen = new RegionSession(region.getSpec(), regions, this::sessionEnded, regionDeckService);
		    }
	        mainWindow.showScreenView(currentScreen.getView());
	        currentScreen.start();
	    });
	}
	
	public void onPlayMenuItemSelected(PlayMenuNode item) {
	    switch (item) {
	    case AnkiPlayItem ankiItem -> {
	        Deck deckType = ankiItem.deck();
	        List<Card> cards = AnkiPlaySetup.show(deckType, ankiDeckService); // Läuft im Menü-Kontext, bevor eine Session existiert — ob überhaupt gespielt wird, entscheidet sich hier.
	        if (cards == null) return;

	        requestSessionSwitch(() -> {
	            // Session starten
	            currentScreen = AnkiDeckSession.forFreePlay(cards, this::sessionEnded, ankiDeckService, deckType);
	            mainWindow.showScreenView(currentScreen.getView());
	            currentScreen.start();
	        });
	    }
	    case RegionPlayItem _ -> {
	        RegionPlaySelection selection = RegionPlaySetup.show(regionDeckService); // Läuft im Menü-Kontext, bevor eine Session existiert — ob überhaupt gespielt wird, entscheidet sich hier.
	        if (selection == null) return;

	        requestSessionSwitch(() -> {
	            // Session starten
	            currentScreen = new RegionSession(selection.spec(), selection.regions(), this::sessionEnded, regionDeckService);
	            mainWindow.showScreenView(currentScreen.getView());
	            currentScreen.start();
	        });
	    }
	    }
	}

	public void onStatisticsMenuItemSelected(String item) {
	    requestSessionSwitch(() -> {
	        if ("Dashboard".equals(item)) {
	            currentScreen = new DashboardScreen();
	        } else if ("Fitbit".equals(item)) {
	            currentScreen = new FitbitStatisticsScreen();
	        }  else if ("Alkohol".equals(item)) {
	            currentScreen = new AlcStatisticsScreen();
	        }
            mainWindow.showScreenView(currentScreen.getView());
            currentScreen.start();
	    });
	}
	
	public void saveMenuItemSelected() {
		currentScreen.closeLoud();
	}
	
	public void escPressed() {
		currentScreen.escClicked();
	}
	
	public void pausePressed() {
		currentScreen.reactOnPausePressed();
	}

	public void sortOrderChanged() {   
	    currentScreen.sortOrderChanged();
	}
	
	public void closeSelected() {
	    requestSessionSwitch(this::showStartScreen);
	}
	
    private void triggerSkinRefresh() {
        SkinService.refresh();
        updateUiAfterSkinChange();
    }

    public void newSkinSelected(Skin newSkin) {
        if (newSkin == SkinService.get())
            return;
        
        SkinService.set(newSkin);
        updateUiAfterSkinChange();
    }
    
    public void diaryCreateSelected() {
    	new DiaryEditorPresenter().showNew();
    }
    
    public void diaryViewSelected() {
    	requestSessionSwitch(() -> {
    		currentScreen = new DiaryScreen();
    		mainWindow.showScreenView(currentScreen.getView());
    		currentScreen.start();
    	});
    }
    
    public void movieSelected() {
    	requestSessionSwitch(() -> {
    		currentScreen = new MovieViewerScreen();
    		mainWindow.showScreenView(currentScreen.getView());
    		currentScreen.start();
    	});
    }
    
    public void weekdaySelected() {
    	new WeekdayDialog().showForPractice();
    }
    
    public void mattressSelected() {
    	new MattressTurnDialog().show();
    }
    
    public void exportSelected() {
    	new SuiteExporter().export();
    }
    
    public void additionalTmdbImportSelected() {
    	new SeriesImporter().run();
    }
    
    /**
     * Nötig wegen meines mächtigen Skinsystems mit Platzierungen und Größen...
     * Und wegen der ImagePane, aber naja, vor allem wegen der oben genannten.
     */
    private void updateUiAfterSkinChange() {
    	Log.info(this, "=== SKIN CHANGE === currentSession=" 
    	        + (currentScreen == null ? "null" : "Session@" + System.identityHashCode(currentScreen)));
        mainWindow.buildStyledUi();        
        currentScreen.refresh();
    }
	
	private void setPlayMenuItemLabels() {
	    List<PlayMenuNode> items = new ArrayList<>();
	    
	    // Anki-Decks aus Enum holen
	    for (Deck type : Deck.values()) {
	        if (type.getCategory() == DeckCategory.ANKI_DECK) {
	            items.add(new AnkiPlayItem(type.getDisplayName(), type));
	        }
	    }

	    // Region-Config-Eintrag hinzufügen
	    items.add(new RegionPlayItem("Regionen"));
	    
	    mainWindow.setPlayItems(items);
	}
	
	/**
	 * Man könnte das einigermaßen kompliziert finden, wieso nicht einfach die Session übergeben?
	 * Nun, dann müsste die neue Session ja komplett aufgebaut werden. Was umsonst wäre, wenn der
	 * User gleich Abbrechen klickt...
	 * 
	 * @param startNewSessionRoutine
	 */
	public void requestSessionSwitch(Runnable startNewSessionRoutine) {
		Log.info(this, "=== REQUEST SESSION SWITCH === currentSession=" 
		        + (currentScreen == null ? "null" : "Session@" + System.identityHashCode(currentScreen)));

	    switch (currentScreen.getSwitchStrategy()) {
	        case IMMEDIATE:
	            currentScreen.closeSilent(false);
	            startNewSessionRoutine.run();
	            break;

	        case OFFER_SAVE:
	            // Der komplexe Dialog: Speichern / Verwerfen / Abbrechen
	            currentScreen.suspend();
	            var decision = showSaveDiscardCancelDialog();
	            if (decision == SessionSwitchAction.SAVE_AND_SWITCH) {
	                currentScreen.closeSilent(true);
	                setLearnMenuItemLabels();
	                startNewSessionRoutine.run();
	            } else if (decision == SessionSwitchAction.DISCARD_AND_SWITCH) {
	                currentScreen.closeSilent(false);
	                startNewSessionRoutine.run();
	            } else {
	                currentScreen.resume(); // CANCEL — die Session lebt weiter
	            }
	            break;

	        case CONFIRM_DISCARD:
	            // Der simple Dialog: "Achtung, Fortschritt geht verloren! OK / Abbrechen"
	            currentScreen.suspend();
	            boolean reallyQuit = showConfirmDiscardDialog();
	            if (reallyQuit) {
	                currentScreen.closeSilent(false); // Nicht speichern, nur schließen
	                startNewSessionRoutine.run();
	            } else {
	                currentScreen.resume();
	            }
	            break;
	    }
	}
	
	private SessionSwitchAction showSaveDiscardCancelDialog() {
	    ButtonEnum result = Alerts.show(
	        "Ungespeicherte Änderungen", 
	        "Du hast ungespeicherten Lernfortschritt...\nSpeichern?", 
	        ButtonEnum.SAVE, ButtonEnum.DISCARD, ButtonEnum.CANCEL
	    );

	    if (result == ButtonEnum.CANCEL) return SessionSwitchAction.CANCEL;
	    if (result == ButtonEnum.SAVE)      return SessionSwitchAction.SAVE_AND_SWITCH;
	    if (result == ButtonEnum.DISCARD)   return SessionSwitchAction.DISCARD_AND_SWITCH;	    
	    return SessionSwitchAction.CANCEL;
	}
	
	private boolean showConfirmDiscardDialog() {
	    ButtonEnum result = Alerts.show( 
	        "Sitzung abbrechen?", 
	        "Achtung: Der Fortschritt geht verloren.", 
	        ButtonEnum.END_ANYHOW, ButtonEnum.CANCEL
	    );

	    return result == ButtonEnum.END_ANYHOW;
	}
	
    private void setLearnMenuItemLabels() {
        mainWindow.setLearnItems(ankiDeckService.getDueGameInfos());
        mainWindow.addLearnItems(regionDeckService.getDueGameInfos());
    }
    
    private void showStartScreen() {
        currentScreen = new StartScreen();
        mainWindow.showScreenView(currentScreen.getView());
    }
}
