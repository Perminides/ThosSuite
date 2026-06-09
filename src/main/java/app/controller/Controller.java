package app.controller;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import app.alc.AlcStatisticsScreen;
import app.alc.StartupService;
import app.diary.DiaryDialog;
import app.diary.DiaryViewerScreen;
import app.fitbit.DataFetcher;
import app.fitbit.DataReviewService;
import app.fitbit.FitbitStatisticsScreen;
import app.learn.ImageScaler;
import app.learn.anki.AnkiDeckService;
import app.learn.anki.AnkiDeckSession;
import app.learn.anki.AnkiPlayConfigDialog;
import app.learn.anki.AnkiPlayConfigDialog.AnkiPlayConfig;
import app.learn.anki.model.AnkiLearnSessionInfo;
import app.learn.anki.model.Card;
import app.learn.model.Deck;
import app.learn.model.DeckCategory;
import app.learn.model.LearnSessionInfo;
import app.learn.model.ShapeMap;
import app.learn.region.RegionDeckService;
import app.learn.region.RegionPlayConfigDialog;
import app.learn.region.RegionPlayConfigDialog.RegionPlayConfig;
import app.learn.region.RegionSession;
import app.learn.region.model.Mode;
import app.learn.region.model.RegionLearnSessionInfo;
import app.learn.region.model.SessionSpec;
import app.mattress.TurnDialog;
import app.messaging.signal.SignalIncrementalImport;
import app.messaging.whatsapp.WhatsAppIncrementalImport;
import app.movie.Cleanup;
import app.movie.Importer;
import app.movie.MovieViewerScreen;
import app.movie.SeriesImporter;
import app.shared.Config;
import app.shared.Log;
import app.shared.Screen;
import app.shared.model.CardSortOrder;
import app.shared.skin.Skin;
import app.shared.skin.SkinService;
import app.weekday.WeekdayDialog;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.Background;
import javafx.scene.layout.Pane;

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
    private Screen currentScreen; //!Später natürlich nicht mehr nur MapDeckSessions...
    private CardSortOrder currentSortOrder = CardSortOrder.BY_WRONG_COUNT_DESC; //!Architektur Sofort: Momentan muss die Anfangssortorder an 2 Stellen gesetzt werden. Gruselig!
    private DataFetcher fitbitDataFetcher;

    
    public enum SessionSwitchAction {
        SAVE_AND_SWITCH,
        DISCARD_AND_SWITCH,
        CANCEL
    }
    
    public Controller(MainWindow mainWindow) throws InterruptedException {
    	this.mainWindow = mainWindow;
    	//!Später: Wenn sich herausstellt, dass eh nur der Controller die ganzen Menü-Events erhält, dann darf das MainWindow auch den Controller kennen und die Methoden direkt aufrufen. Außer Claude erklärt mir, was an dieser zirkulären Beziehung nun so gefährlich sein soll...
    	mainWindow.setEscPressedRunnable(this::escPressed);
    	mainWindow.setPausePressedRunnable(this::pausePressed);
    	mainWindow.setSaveRunnable(this::saveMenuItemSelected);
    	mainWindow.setLearnSessionConsumer(this::onLearnMenuItemSelected);
    	mainWindow.setSortConsumer(this::cardSortOrderSelected);
    	mainWindow.setSkinChangeConsumer(this::newSkinSelected);
    	mainWindow.setReloadSkinRunnable(this::triggerSkinRefresh);
    	mainWindow.setStatisticsConsumer(this::onStatisticsMenuItemSelected);
    	mainWindow.setSortOrderSupplier(this::getCardSortOrder);
    	mainWindow.setDiaryCreateRunnable(this::diaryCreateSelected);
    	mainWindow.setDiaryViewRunnable(this::diaryViewSelected);
    	mainWindow.setWeekdayRunnable(this::weekdaySelected);
    	mainWindow.setMattressRunnable(this::mattressSelected);
    	mainWindow.setExportRunnable(this::exportSelected);
    	mainWindow.setMovieRunnable(this::movieSelected);
    	mainWindow.setExtraTmdbImportRunnable(this::additionalTmdbImportSelected);
    	mainWindow.setPlayItemConsumer(this::onPlayMenuItemSelected);	
    	showEmptyBackground();
    	
    	ankiDeckService = new AnkiDeckService();
    	regionDeckService = new RegionDeckService();
    	
    	// Gespeicherte SortOrder laden
        String savedSort = Config.get("pref_sortOrder", "BY_WRONG_COUNT_DESC");
        try {
            currentSortOrder = CardSortOrder.valueOf(savedSort);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Ungültige SortOrder in config.txt: " + savedSort, e);
        }
        mainWindow.setCurrentSortOrder(currentSortOrder);
    	
    	setLearnMenuItemLabels();
    	setPlayMenuItemLabels();
    }
    
	/**
	 * Wird VOR initializeMainWindow aufgerufen (Splash noch sichtbar). Holt Daten im UI-Thread, blockiert aber die App - Splash bleibt sichtbar.
	 */
	public void runPreTasks() {
		fitbitDataFetcher = new DataFetcher(); // Muss Instanzvariable sein, weil wir Daten für den PostTask übergeben. Das macht tmdb sauberer, wie ich finde...
		if (Config.get("offline", "false").equals("false")) {
			fitbitDataFetcher.fetch();
			new Importer().run();
		}
	}

	/**
	 * Wird NACH splashStage.close() aufgerufen (Splash weg, MainWindow sichtbar).
	 * Registriert das MainWindow im SkinService, zeigt Dialoge und speichert Daten.
	 */
	public void runPostTasks() {
	    // Owner-Stage registrieren VOR allen Dialogen
	    SkinService.setOwnerWindow(mainWindow.getStage());
		// Fitbit-Fehler behandeln
		if (fitbitDataFetcher.hasError()) {
			SkinService.get().createAlert(mainWindow.getStage(), "Fitbit-Fehler",
					"Fehler beim Laden der Fitbit-Daten:\n" + fitbitDataFetcher.getError().getMessage(), false, false).showAndWait();
		} else {
			// Fitbit-Dialoge zeigen
			if (fitbitDataFetcher.hasData()) {
				DataReviewService fitbitService = new DataReviewService(fitbitDataFetcher);
				fitbitService.showDialogsAndSave(null);
			}
		}
		
	    StartupService alcoholService = new StartupService();
	    alcoholService.checkAndPrompt();
	    
	    new DiaryDialog().showNew(mainWindow.getStage());
	    
	    new WeekdayDialog().showForDaily();
	    
	    new TurnDialog().showIfDue();
	    
	    new Cleanup().run();
	    
	    try {
	    	new SignalIncrementalImport().run();
	    } catch (Exception e) {
	    	Log.error(this.getClass(), "", e);
			Alert alert = SkinService.get().createAlert(mainWindow.getStage(), "Signal", "Beim Signalimport ist was schiefgelaufen.\nEs wurde nichts in die DB geschrieben.\nBitte anschauen.", false, false);
			alert.showAndWait();
		}
	    
	    try {
	    	new WhatsAppIncrementalImport().run();
	    } catch (Exception e) {
	    	Log.error(this.getClass(), "", e);
			Alert alert = SkinService.get().createAlert(mainWindow.getStage(), "WhatsApp", "Beim WhatsApp-Import ist was schiefgelaufen.\nEs wurde nichts in die DB geschrieben.\nBitte anschauen.", false, false);
			alert.showAndWait();
		}
	    
	    ImageScaler.processImages();

		// Hier kommen später weitere Post-Tasks (tmdb etc.)
	}
    
    public void sessionEnded() {
    	//!Erweiterung Endbedingungen integrieren...
    	//!Architektur Gehört das PopUp hierhin? Oder in die Session wie aktuell?
    	setLearnMenuItemLabels();
    	mainWindow.showSaveSession(false);
    	showEmptyBackground();
    	currentScreen = null;
    	// !Erweiterung im Popup auch fragen ob eine neue Session gestartet werden soll
    }
	
	public void onLearnMenuItemSelected(LearnSessionInfo info) {
	    requestSessionSwitch(() -> {
	    	if (info instanceof AnkiLearnSessionInfo anki) {
				List<Card> dueCards = ankiDeckService.getDueCards(anki.getDeckType()); // !Später: Wenn Session schon den Service bekommt, um die Session zu speichern, warum holt sie sich nicht auch die Karten zum Spielen. Beantwortung muss erfolgen, wenn freies Spiel implementiert wird!
				currentScreen = new AnkiDeckSession(dueCards, this::sessionEnded, ankiDeckService, anki.getDeckType(), currentSortOrder, false);
				mainWindow.showSaveSession(true); //!Später nur wenn es eine Session ist, wo saven überhaupt geht, also keine Regionssessions
		    } else if (info instanceof RegionLearnSessionInfo region) {
		    	Set<ShapeMap> regions = regionDeckService.getRegions(region.getSpec());
		        currentScreen = new RegionSession(region.getSpec(), regions, this::sessionEnded, regionDeckService);
		        mainWindow.showSaveSession(false);
		    }
	        mainWindow.showPane(currentScreen.getView());
	        currentScreen.start();
	    });
	}
	
	public void onPlayMenuItemSelected(PlayMenuItem item) {
	    Object payload = item.payload();
	    
	    if (payload instanceof Deck deckType) {
	        Set<String> availableLabels = ankiDeckService.getAvailableLabels(deckType);
	        AnkiPlayConfigDialog dialog = new AnkiPlayConfigDialog(
	            mainWindow.getStage(),
	            SkinService.get(),
	            deckType,
	            availableLabels
	        );
	        
	        Optional<AnkiPlayConfig> configOpt = dialog.showAndWait();
	        if (configOpt.isEmpty()) return;
	        
	        AnkiPlayConfig config = configOpt.get();
	        
	        List<Card> cards = ankiDeckService.getCardsForPlay(
	            deckType,
	            config.minIndex(),
	            config.maxIndex(),
	            config.maxCards(),
	            config.selectedLabels()
	        );
	        
	        if (cards.isEmpty()) {
	            SkinService.get().createAlert(mainWindow.getStage(), null, "Keine Karten gefunden", false, false).showAndWait();
	            return;
	        }
	        
	        requestSessionSwitch(() -> {
	            // Session starten (ohne Sortierung, gemischt)
	            currentScreen = new AnkiDeckSession(cards, this::sessionEnded, ankiDeckService, deckType, CardSortOrder.RANDOM, true);
	            mainWindow.showPane(currentScreen.getView());
	            //mainWindow.showSaveSession(false); // Play-Sessions sind nicht speicherbar. Warum ist das auskommentiert???
	            currentScreen.start();
	        });
	        
	    } else if (payload == DeckCategory.REGION_DECK) {
	        RegionPlayConfigDialog dialog = new RegionPlayConfigDialog(mainWindow.getStage(), SkinService.get());
	        
	        Optional<RegionPlayConfig> configOpt = dialog.showAndWait();
	        if (configOpt.isEmpty()) return;
	        
	        RegionPlayConfig config = configOpt.get();
	        
	        Set<Deck> selectedDecks = config.selectedDecks();
	        Mode mode = config.mode();
	        
	        // Erstes Deck als primäres, Rest als additional
	        Deck primaryDeck = selectedDecks.iterator().next();
	        Set<Deck> additionalDecks = new HashSet<>(selectedDecks);
	        additionalDecks.remove(primaryDeck);
	        
	        // Spec erstellen
	        SessionSpec spec = new SessionSpec(
	            primaryDeck,
	            mode,
	            additionalDecks.isEmpty() ? null : additionalDecks,
	            true  // isPlaySession
	        );
	        
	        // Regionen holen VOR dem Switch
	        Set<ShapeMap> regions = regionDeckService.getRegions(spec);
	        
	        if (regions.isEmpty()) {
	            SkinService.get().createAlert(mainWindow.getStage(), null, "Keine Regionen gefunden", false, false).showAndWait();
	            return;
	        }
	        
	        requestSessionSwitch(() -> {
	            // Session starten
	            currentScreen = new RegionSession(spec, regions, this::sessionEnded, regionDeckService);
	            mainWindow.showPane(currentScreen.getView());
	            //mainWindow.showSaveSession(false); // Play-Sessions sind nicht speicherbar
	            currentScreen.start();
	        });
	    }
	}
	
	// !Später: "Speichern" wird bei Statistik-Screens nicht ausgeblendet (anders als Region).
	// Prüfen ob Bug — save ergibt für Stats keinen Sinn. Sichtbarkeit an Screen-Typ koppeln?
	public void onStatisticsMenuItemSelected(String item) {
	    requestSessionSwitch(() -> {
	        if ("Dashboard".equals(item)) {
	            currentScreen = new DashboardScreen();
	        } else if ("Fitbit".equals(item)) {
	            currentScreen = new FitbitStatisticsScreen();
	        }  else if ("Alkohol".equals(item)) {
	            currentScreen = new AlcStatisticsScreen();
	        }
            mainWindow.showPane(currentScreen.getView());
            currentScreen.start();
	    });
	}
	
	public void saveMenuItemSelected() {
		if (currentScreen != null)
			currentScreen.endGracefully();
	}
	
	public CardSortOrder getCardSortOrder() {
		return currentSortOrder;
	}
	
	public void escPressed() {
		if (currentScreen != null)
			currentScreen.escClicked();
	}
	
	public void pausePressed() {
		if (currentScreen != null)
			currentScreen.reactOnPauseClick();
	}

	public void cardSortOrderSelected(CardSortOrder sort) {
	    currentSortOrder = sort;
	    Config.set("pref_sortOrder", sort.name()); // Persistieren
	    
	    if (currentScreen == null)
	        return;
	    currentScreen.sort(sort); //!Später dann weiß noch nicht, muss geschaut werden ob die Session zu sort überhaupt passt.
	}
	
	// NEU: Die Refresh-Methode
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
    	new DiaryDialog().showNew(mainWindow.getStage());
    }
    
    public void diaryViewSelected() {
    	requestSessionSwitch(() -> {
    		currentScreen = new DiaryViewerScreen();
    		mainWindow.showPane(currentScreen.getView());
    		currentScreen.start();
    	});
    }
    
    public void movieSelected() {
    	requestSessionSwitch(() -> {
    		currentScreen = new MovieViewerScreen();
    		mainWindow.showPane(currentScreen.getView());
    		currentScreen.start();
    	});
    }
    
    public void weekdaySelected() {
    	new WeekdayDialog().showForPractice();
    }
    
    public void mattressSelected() {
    	new TurnDialog().show();
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
        
        // !Sofort: Das ist aber architektonisch scheiße, dass ich das hier noch mal aufrufen muss!
        //mainWindow.setCurrentSortOrder(currentSortOrder);
        
        if (currentScreen != null) {
            currentScreen.refresh();
            mainWindow.showSaveSession(true); 
        } else {
            showEmptyBackground();
        }
    }
	
	private void setPlayMenuItemLabels() {
	    List<PlayMenuNode> items = new ArrayList<>();
	    
	    // Anki-Decks aus Enum holen
	    for (Deck type : Deck.values()) {
	        if (type.getCategory() == DeckCategory.ANKI_DECK) {
	            items.add(new PlayMenuItem(type.getDisplayName(), type));
	        }
	    }
	    
	    // Region-Config-Eintrag hinzufügen
	    items.add(new PlayMenuItem("Regionen", DeckCategory.REGION_DECK));
	    
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
	    if (currentScreen == null) {
	        startNewSessionRoutine.run();
	        return;
	    }

	    switch (currentScreen.getSwitchStrategy()) {
	        case IMMEDIATE:
	            currentScreen.closeSilent(false);
	            startNewSessionRoutine.run();
	            break;

	        case OFFER_SAVE:
	            // Der komplexe Dialog: Speichern / Verwerfen / Abbrechen
	            var decision = showSaveDiscardCancelDialog();
	            if (decision == SessionSwitchAction.SAVE_AND_SWITCH) {
	                currentScreen.closeSilent(true);
	                setLearnMenuItemLabels();
	                startNewSessionRoutine.run();
	            } else if (decision == SessionSwitchAction.DISCARD_AND_SWITCH) {
	                currentScreen.closeSilent(false);
	                startNewSessionRoutine.run();
	            }
	            // bei CANCEL passiert nichts
	            break;

	        case CONFIRM_DISCARD:
	            // Der simple Dialog: "Achtung, Fortschritt geht verloren! OK / Abbrechen"
	            boolean reallyQuit = showConfirmDiscardDialog();
	            if (reallyQuit) {
	                currentScreen.closeSilent(false); // Nicht speichern, nur schließen
	                startNewSessionRoutine.run();
	            }
	            break;
	    }
	}
	
	private SessionSwitchAction showSaveDiscardCancelDialog() {
	    // 1. Buttons definieren
	    ButtonType btnSave = new ButtonType("Speichern", ButtonBar.ButtonData.YES);
	    ButtonType btnDiscard = new ButtonType("Verwerfen", ButtonBar.ButtonData.NO);
	    ButtonType btnCancel = new ButtonType("Abbrechen", ButtonBar.ButtonData.CANCEL_CLOSE);

	    // 2. Alert direkt mit den Buttons erzeugen
	    Alert alert = SkinService.get().createAlert(
	        mainWindow.getStage(), 
	        "Ungespeicherte Änderungen", 
	        "Du hast ungespeicherten Lernfortschritt...\nSpeichern?", 
	        btnSave, btnDiscard, btnCancel // <--- Viel sauberer!
	    );

	    // 3. Ergebnis auswerten
	    Optional<ButtonType> result = alert.showAndWait();

	    if (result.isEmpty()) return SessionSwitchAction.CANCEL;

	    ButtonType chosen = result.get();
	    if (chosen == btnSave)      return SessionSwitchAction.SAVE_AND_SWITCH;
	    if (chosen == btnDiscard)   return SessionSwitchAction.DISCARD_AND_SWITCH;
	    
	    return SessionSwitchAction.CANCEL;
	}
	
	private boolean showConfirmDiscardDialog() {
	    ButtonType btnConfirm = new ButtonType("Trotzdem beenden", ButtonBar.ButtonData.YES);
	    ButtonType btnCancel = new ButtonType("Abbrechen", ButtonBar.ButtonData.CANCEL_CLOSE);

	    Alert alert = SkinService.get().createAlert(
	        mainWindow.getStage(), 
	        "Sitzung abbrechen?", 
	        "Achtung: Der Fortschritt geht verloren.", 
	        btnConfirm, btnCancel
	    );

	    return alert.showAndWait().orElse(btnCancel) == btnConfirm;
	}
	
    private void setLearnMenuItemLabels() {
        mainWindow.setLearnItems(ankiDeckService.getDueGameInfos());
        mainWindow.addLearnItems(regionDeckService.getDueGameInfos());
    }
    
    private void showEmptyBackground() {
        // 1. Eine dumme, leere Pane erzeugen
        Pane emptyView = new Pane();
        
        // 2. Den "Null"-Hintergrund (Standard-Wallpaper) draufkleben
        emptyView.setBackground(new Background(SkinService.get().getEmptyBackgroundImage()));
        
        // 3. In den Anker hängen
        mainWindow.showPane(emptyView);
    }    
}
