package app.learn.region;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Set;

import app.learn.model.LearnStat;
import app.learn.model.MapShape;
import app.learn.region.model.Mode;
import app.learn.region.model.SessionSpec;
import app.shared.AppClock;
import app.shared.Log;
import app.shared.model.AlertOptions;
import app.shared.model.ButtonEnum;
import app.shared.model.SessionSwitchStrategy;
import app.shared.skin.SkinService;
import app.shared.ui.contracts.Screen;
import app.shared.ui.contracts.ScreenView;

// !Später: Das RegionSessionBild croppen und dann den korrekten Rand setzen, wenn isComplete==true. Wobei isComplete meint, dasss das ganze Rechteck ausgefüllt ist.
// !Sofort: Es wäre schon nice bei Finde uf der Karte (schwer) zu wissen, wie viel noch kommen. Also doch einen Fortschritt bitte.
/**
 * Schale der Regions-Lernsession und Ansprechpartner für den Controller (Screen). Hält den
 * SessionProgress (je nach Modus Click/Elimination/Write) und den Presenter; wertet über end(...)
 * das Sessionergebnis aus (Alert, LearnStat, Speichern).
 *
 * !Architektur (learn-Innensanierung, eigene Session — nicht nebenbei anfassen):
 *  1) active-Flag + die !active-Wächter in fast jeder Methode: Die Exception ist nie geflogen.
 *     Zu prüfen, ob ein Aufruf auf einer toten Session strukturell überhaupt möglich ist. Wenn
 *     nein, fallen Flag UND alle Wächter weg (gilt analog für die AnkiSession-Seite).
 *  2) Auswertungs-Asymmetrie zu Anki: Hier STEUERT der Progress die Auswertung, indem er
 *     end(correct, wrongId, text, allowResume) ruft; bei Anki wertet die Session selbst aus und
 *     der Progress liefert nur Daten. Zusammen mit dem schon markierten FreePlay-Umbau von end(...)
 *     zu klären — ist die Verantwortungsverteilung gewollt oder soll sie angeglichen werden?
 */
public class RegionSession implements Screen {
	
	private final SessionPresenter presenter;
	private final RegionDeckService service;
	Runnable onSessionEnded;
    private final SessionSpec spec;
    private final SessionProgress progress;
    private Boolean active;

	public RegionSession(SessionSpec spec, Set<MapShape> regions, Runnable onSessionEnded, RegionDeckService regionService) {
    	this.active = true;
		this.spec = spec;
    	this.service = regionService;
    	switch (spec.getMode().getSubCategory()) {
    		case Mode.SubCategory.CLICK: {
    			this.progress = new ClickSessionProgress(regions, spec, this);
    			break;
    		}
    		case Mode.SubCategory.ELIMINATION: {
    			this.progress = new EliminationSessionProgress(regions, spec, this);
    			break;
    		}
    		case Mode.SubCategory.WRITE: {
    			this.progress = new WriteSessionProgress(regions, spec, this);
    			break;
    		}
    		default: throw new RuntimeException("Das ist leider noch nicht implementiert :-)");
    	}
    	this.presenter = new SessionPresenter(progress, spec);
        this.onSessionEnded = onSessionEnded;
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
	public void reactOnPauseClick() {
		if (!active)
    		throw new RuntimeException("Alter! Die Session ist tot, was willst Du mit dem Leichnam?");
		progress.endPause();
	}
	
	public void end(boolean correct, String wrongId, String text, boolean allowResume) {
		if (!active)
    		throw new RuntimeException("Alter! Die Session ist tot, was willst Du mit dem Leichnam?");
		
		if (!correct) { // Wenn nicht korrekt, dann zeige den Text an. Ok und Abbruch. Und eventuell, wenn erlaubt, auch Fortfahren...
			Log.info(this, "Alert wird erstellt. correct=" + correct);
			// !Sofort: Refactor: Skin holt sich das MainWindows selbst herrje!
			ButtonEnum result = allowResume
				    ? SkinService.get().showAlert("Nicht korrekt", text, new AlertOptions().noEsc(),
				          ButtonEnum.OK, ButtonEnum.CANCEL, ButtonEnum.RESUME)
				    : SkinService.get().showAlert("Nicht korrekt", text, new AlertOptions().noEsc(),
				          ButtonEnum.OK, ButtonEnum.CANCEL);

			if (result == ButtonEnum.CANCEL) { // Abbruch. Wir speichern nicht, wir beenden sofort.
				active = false;
				onSessionEnded.run();
				return;
			} else if (result == ButtonEnum.OK) { // Ok. Wir beenden gleich nach dem Speichern.
				// Nichts weiter zu tun...
			} else if (result == ButtonEnum.RESUME) { // Fortsetzen. Wir setzen fort.
				progress.resume();
				return;
			}
		} else if (text != null && !text.isEmpty()) { // Ah. Ich soll was anzeigen, obwohl es korrekt war. Vermutlich ein FreePlay. Na egal, zeigen wir halt an...
			Log.info(this, "Alert wird erstellt. correct=" + correct);
			SkinService.get().showAlert("Korrekt", text, ButtonEnum.OK);
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
			SkinService.get().showAlert("Ausblick", getUntilString(stats.getDueDate()), ButtonEnum.OK);
		}
		Log.info(this, "RegionsSession " + spec.getDeckType().getDisplayName() + " (play = " + spec.isPlaySession() + ") beendet.");
		active = false;
		onSessionEnded.run();
	}

	@Override
	public void refresh() {
		if (!active)
    		throw new RuntimeException("Alter! Die Session ist tot, was willst Du mit dem Leichnam?");
		presenter.refresh();
	}
	
	public ScreenView getView() {
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
