package app.controller;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.Set;

import app.data.AppClock;
import app.data.LearnStat;
import app.data.MapShape;
import app.data.RegionClickSessionProgress;
import app.data.RegionDeckService;
import app.data.RegionEliminationSessionProgress;
import app.data.RegionMode;
import app.data.RegionSessionProgress;
import app.data.RegionSessionSpec;
import app.data.RegionWriteSessionProgress;
import app.data.SessionSwitchStrategy;
import app.presenter.RegionSessionPresenter;
import app.ui.UIUtils;
import app.ui.skin.SkinService;
import app.util.Log;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.Pane;

// !Später: Das RegionSessionBild croppen und dann den korrekten Rand setzen, wenn isComplete==true. Wobei isComplete meint, dasss das ganze Rechteck ausgefüllt ist.
// !Sofort: Es wäre schon nice bei Finde uf der Karte (schwer) zu wissen, wie viel noch kommen. Also doch einen Fortschritt bitte.
public class RegionSession implements Session {
	
	private final RegionSessionPresenter presenter;
	private final RegionDeckService service;
    private final Controller controller;
    private final RegionSessionSpec spec;
    private final RegionSessionProgress progress;
    private Boolean active;

	public RegionSession(RegionSessionSpec spec, Set<MapShape> regions, Controller controller, RegionDeckService regionService) {
    	this.active = true;
		this.spec = spec;
    	this.service = regionService;
    	switch (spec.getMode().getSubCategory()) {
    		case RegionMode.SubCategory.CLICK: {
    			this.progress = new RegionClickSessionProgress(regions, spec, this);
    			break;
    		}
    		case RegionMode.SubCategory.ELIMINATION: {
    			this.progress = new RegionEliminationSessionProgress(regions, spec, this);
    			break;
    		}
    		case RegionMode.SubCategory.WRITE: {
    			this.progress = new RegionWriteSessionProgress(regions, spec, this);
    			break;
    		}
    		default: throw new RuntimeException("Das ist leider noch nicht implementiert :-)");
    	}
    	this.presenter = new RegionSessionPresenter(progress, spec);
        this.controller = controller;
	}
	
    public void start() {
    	if (!active)
    		throw new RuntimeException("Alter! Die Session ist tot, was willst Du mit dem Leichnam?");
    	Log.info(this, "Starte RegionsSession " + spec.getDeckType().getDisplayName() + " (play = " + spec.isPlaySession() + ")");
        progress.start();
    }

	@Override
	public void escClicked() {
		if (!active)
    		throw new RuntimeException("Alter! Die Session ist tot, was willst Du mit dem Leichnam?");
		progress.cancel();
	}

	@Override
	public SessionSwitchStrategy getSwitchStrategy() {
		if (!active)
    		throw new RuntimeException("Alter! Die Session ist tot, was willst Du mit dem Leichnam?");
		if (!progress.hasProgressed() || spec.isPlaySession())
			return SessionSwitchStrategy.IMMEDIATE;
		else
			return SessionSwitchStrategy.CONFIRM_DISCARD;	
	}
	
	@Override
	public void endPause() {
		if (!active)
    		throw new RuntimeException("Alter! Die Session ist tot, was willst Du mit dem Leichnam?");
		progress.endPause();
	}
	
	public void end(boolean correct, String wrongId, String text, boolean allowResume) {
		if (!active)
    		throw new RuntimeException("Alter! Die Session ist tot, was willst Du mit dem Leichnam?");
		
		Pane currentPane = getView();
		if (!correct) { // Wenn nicht korrekt, dann zeige den Text an. Ok und Abbruch. Und eventuell, wenn erlaubt, auch Fortfahren...
			Log.info(this, "Alert wird erstellt. correct=" + correct);

			Alert alert = SkinService.get().createAlert(currentPane.getScene().getWindow(), "Nicht korrekt", text, true, allowResume);
			UIUtils.inactivateEscPress(alert);
			Optional<ButtonType> result = alert.showAndWait();

			if (!result.isPresent() || result.get().getButtonData() == ButtonBar.ButtonData.CANCEL_CLOSE) { // Abbruch. Wir speichern nicht, wir beenden sofort.
				active = false;
				controller.sessionEnded();
				return;
			} else if (result.get().getButtonData() == ButtonBar.ButtonData.YES) { // Ok. Wir beenden gleich nach dem Speichern.
				// Nichts weiter zu tun...
			} else if (result.get().getButtonData() == ButtonBar.ButtonData.OTHER) { // Fortsetzen. Wir setzen fort.
				progress.resume();
				return;
			}
		} else if (text != null && !text.isEmpty()) { // Ah. Ich soll was anzeigen, obwohl es korrekt war. Vermutlich ein FreePlay. Na egal, zeigen wir halt an...
			Log.info(this, "Alert wird erstellt. correct=" + correct);
			Alert alert = SkinService.get().createAlert(currentPane.getScene().getWindow(), "Korrekt", text, false, allowResume);
	    	alert.showAndWait();
		}
		
		if (!spec.isPlaySession()) {
			LearnStat stats = service.getLearnStat(spec);
			
    		if (!stats.isDueToday())
    			throw new RuntimeException("Sicherheitsnetz eingebaut. Diese Region war gar nicht dran. Und ich soll den Fortschritt überschreiben? Mache ich ungern!");
			
			stats.setLevel(progress.calculateNewLevel(stats.getLastPlayed(), correct, false));
			stats.setLastPlayed(AppClock.TODAY);
			if (!correct)
				stats.incrementWrongCount();
			service.savePlayedCards(spec, stats, correct, wrongId);
			Alert alert = SkinService.get().createAlert(currentPane.getScene().getWindow(), "Ausblick", getUntilString(stats.getDueDate()), false, false);
	    	alert.showAndWait();
		}
		Log.info(this, "RegionsSession " + spec.getDeckType().getDisplayName() + " (play = " + spec.isPlaySession() + ") beendet.");
		active = false;
		controller.sessionEnded();
	}

	@Override
	public void refresh() {
		if (!active)
    		throw new RuntimeException("Alter! Die Session ist tot, was willst Du mit dem Leichnam?");
		presenter.refresh();
	}
	
	public Pane getView() {
		if (!active)
    		throw new RuntimeException("Alter! Die Session ist tot, was willst Du mit dem Leichnam?");
		return presenter.getView();
	}
	
	public void closeSilent(boolean save) {
		if (!active)
    		throw new RuntimeException("Alter! Die Session ist tot, was willst Du mit dem Leichnam?");
    	active = false;
		if (save)
			throw new RuntimeException("Damit habe ich nun so gar nicht gerechnet. Wieso sollte ich eine unfertige Regionssession speichern?");
	}
	
	private String getUntilString(LocalDate date) {
		long dayDiff = AppClock.TODAY.until(date, ChronoUnit.DAYS);
		long weekDiff = AppClock.TODAY.until(date, ChronoUnit.WEEKS);
		long monthDiff = AppClock.TODAY.until(date, ChronoUnit.MONTHS);
		long yearDiff = AppClock.TODAY.until(date, ChronoUnit.YEARS);
		if (dayDiff == 1l)
			return "Wir sehen uns morgen wieder.";
		if (dayDiff == 2l)
			return "Wir sehen uns übermorgen wieder.";
		if (dayDiff < 7l)
			return "Wir sehen uns in " + dayDiff + " Tagen wieder.";
		if (weekDiff < 10)
			return "Wir sehen uns in " + weekDiff + (weekDiff == 1 ? " Woche" : " Wochen") + " wieder.";
		if (monthDiff < 12)
			return "Wir sehen uns in " + monthDiff + " Monaten wieder.";
		if (monthDiff < 18)
			return "Wir sehen uns in einem Jahr wieder.";
		if (monthDiff < 36)
			return "Wir sehen uns in zwei Jahren wieder.";
		return "Wir sehen uns in " + yearDiff + " Jahren wieder";
	}
}
