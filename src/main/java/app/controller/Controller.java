package app.controller;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import app.data.AnkiCard;
import app.data.AnkiDeckService;
import app.data.AnkiLearnSessionInfo;
import app.data.CardSortOrder;
import app.data.LearnSessionInfo;
import app.data.MapShape;
import app.data.RegionDeckService;
import app.data.RegionLearnSessionInfo;
import app.ui.MainWindow;
import app.ui.skin.Skin;
import app.ui.skin.SkinService;
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
    private Session currentSession; //!Später natürlich nicht mehr nur MapDeckSessions...
    private CardSortOrder currentSortOrder = CardSortOrder.BY_WRONG_COUNT_DESC; //!Architektur Sofort: Momentan muss die Anfangssortorder an 2 Stellen gesetzt werden. Gruselig!
    
    public Controller(MainWindow mainWindow) {
    	this.mainWindow = mainWindow;
    	//!Später: Wenn sich herausstellt, dass eh nur der Controller die ganzen Menü-Events erhält, dann darf das MainWindow auch den Controller kennen und die Methoden direkt aufrufen. Außer Claude erklärt mir, was an dieser zirkulären Beziehung nun so gefährlich sein soll...
    	mainWindow.setEscPressedRunnable(this::escPressed);
    	mainWindow.setPausePressedRunnable(this::pausePressed);
    	mainWindow.setSaveRunnable(this::saveMenuItemSelected);
    	mainWindow.setLearnSessionConsumer(this::onLearnMenuItemSelected);
    	mainWindow.setSortConsumer(this::cardSortOrderSelected);
    	mainWindow.setSkinChangeConsumer(this::newSkinSelected);
    	showEmptyBackground();
    	ankiDeckService = new AnkiDeckService();
    	regionDeckService = new RegionDeckService();
    	setLearnMenuItemLabels();
    	mainWindow.show(); 
    }
    
    public void sessionEnded() {
    	//!Erweiterung Endbedingungen integrieren...
    	//!Architektur Gehört das PopUp hierhin? Oder in die Session wie aktuell?
    	setLearnMenuItemLabels();
    	mainWindow.showSaveSession(false);
    	showEmptyBackground();
    	currentSession = null;
    	// !Erweiterung im Popup auch fragen ob eine neue Session gestartet werden soll
    }

	public void onLearnMenuItemSelected(LearnSessionInfo info) {
		if (info instanceof AnkiLearnSessionInfo anki) {
			List<AnkiCard> dueCards = ankiDeckService.getDueCards(anki.getDeckType()); // !Später: Wenn Session schon den Service bekommt, um die Session zu speichern, warum holt sie sich nicht auch die Karten zum Spielen. Beantwortung muss erfolgen, wenn freies Spiel implementiert wird!
			currentSession = new AnkiDeckSession(dueCards, this, ankiDeckService, anki.getDeckType(), currentSortOrder == null ? Collections::shuffle : currentSortOrder::sort);
			mainWindow.showSaveSession(true); //!Später nur wenn es eine Session ist, wo saven überhaupt geht, also keine Regionssessions
	    } else if (info instanceof RegionLearnSessionInfo region) {
	    	Set<MapShape> regions = regionDeckService.getRegions(region.getSpec().getDeckType());
	        currentSession = new RegionSession(region.getSpec(), regions, this, regionDeckService);
	        mainWindow.showSaveSession(true); //!Später nur wenn es eine Session ist, wo saven überhaupt geht, also keine Regionssessions
	    }
		mainWindow.showPane(currentSession.getView());
		currentSession.start();
	}
	
	public void saveMenuItemSelected() {
		if (currentSession != null)
			currentSession.end();
	}
	
	public void escPressed() {
		if (currentSession != null)
			currentSession.cancel();
	}
	
	public void pausePressed() {
		if (currentSession != null)
			currentSession.endPause();
	}

	public void cardSortOrderSelected(CardSortOrder sort) {
		currentSortOrder = sort;
		if (currentSession == null) //!Später dann weiß noch nicht, muss geschaut werden ob die Session zu sort überhaupt passt.
			return;
		currentSession.sort(sort);
		
	}
	
	public void newSkinSelected(Skin newSkin) {
		if (newSkin == SkinService.get())
			return;
		
		SkinService.set(newSkin);
		mainWindow.buildStyledUi();
		
		if (currentSession != null) {
			currentSession.refresh();
			mainWindow.showSaveSession(true); // !Später Ach, das ist aber etwas unschön hier, oder? Ist die Frage wie wichtig eine cleverere Logik hier ist. Man kann es auch so lassen...
		} else {
			showEmptyBackground();
		}
	}
	
    private void setLearnMenuItemLabels() {
        mainWindow.setLearnItems(ankiDeckService.getDueGameInfos());
        mainWindow.addLearnItems(regionDeckService.getDueGameInfos());
    }
    
    private void showEmptyBackground() {
        // 1. Eine dumme, leere Pane erzeugen
        Pane emptyView = new Pane();
        
        // 2. Den "Null"-Hintergrund (Standard-Wallpaper) draufkleben
        emptyView.setBackground(new Background(SkinService.get().getBackgroundImage(null)));
        
        // 3. In den Anker hängen
        mainWindow.showPane(emptyView);
    }
}
