package app.learn.region;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import app.learn.model.LearnStat;
import app.learn.model.MapShape;
import app.learn.region.model.Mode;
import app.learn.region.model.SessionSpec;
import app.shared.AppClock;
import app.shared.Log;
import app.shared.model.AlertOptions;
import app.shared.model.ButtonEnum;
import app.shared.model.Screen;
import app.shared.model.ScreenView;
import app.shared.model.SessionSwitchStrategy;
import app.shared.ui.Alerts;

// !Idee: Das RegionSessionBild croppen und dann den korrekten Rand setzen, wenn isComplete==true. Wobei isComplete meint, dasss das ganze Rechteck ausgefüllt ist.
// !Idee: Es wäre schon nice bei Finde uf der Karte (schwer) zu wissen, wie viel noch kommen. Also doch einen Fortschritt bitte.
/**
 * Schale der Regions-Lernsession und Ansprechpartner für den Controller (Screen). Hält den
 * SessionProgress (je nach Modus Click/Elimination/Write) und den Presenter; wertet über end(...)
 * das Sessionergebnis aus (Alert, LearnStat, Speichern).
 *
 * !Architektur: Auswertungs-Asymmetrie zu Anki — hier STEUERT der Progress die Auswertung, indem er
 * end(correct, wrongId, incorrectText, allowResume) ruft; bei Anki wertet die Session selbst aus und
 * der Progress liefert nur Daten. Ist die Verantwortungsverteilung gewollt oder soll sie angeglichen
 * werden?
 */
public class RegionSession implements Screen {
	
	private final SessionPresenter presenter;
	private final RegionDeckService service;
	Runnable onSessionEnded;
    private final SessionSpec spec;
    private final SessionProgress progress;

	public RegionSession(SessionSpec spec, Set<MapShape> regions, Runnable onSessionEnded, RegionDeckService regionService) {
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
    	Log.info(this, "Starte RegionsSession " + spec.getDeckType().getDisplayName() + " (play = " + spec.isPlaySession() + ")");
        progress.start();
    }

	@Override
	public void escClicked() {
		progress.cancel();
	}

	@Override
	public SessionSwitchStrategy getSwitchStrategy() {
		if (!progress.hasProgressed() || spec.isPlaySession())
			return SessionSwitchStrategy.IMMEDIATE;
		else
			return SessionSwitchStrategy.CONFIRM_DISCARD;	
	}
	
	@Override
	public void reactOnPauseClick() {
		progress.endPause();
	}
	
	/**
	 * Das Ende einer Regions-Session — wobei „Ende" eine Absicht ist, keine Zusage: bei einem
	 * Fehler darf der Nutzer fortsetzen, dann läuft die Session weiter.
	 *
	 * <p><b>Welcher Dialog wann kommt</b>, ist über zwei Achsen verteilt und deshalb hier
	 * einmal aufgeschrieben:</p>
	 * <table border="1">
	 *   <caption>Dialoge je Modus und Ergebnis</caption>
	 *   <tr><th>&nbsp;</th><th>nach dem Ergebnis</th><th>nach dem Speichern</th></tr>
	 *   <tr><td>Lernen, richtig</td>      <td>—</td>                 <td>„Ausblick"</td></tr>
	 *   <tr><td>Lernen, falsch</td>       <td>„Nicht korrekt"</td>   <td>„Ausblick" (wenn OK gewählt)</td></tr>
	 *   <tr><td>Freies Spiel, richtig</td><td>„Super gemacht!"</td>  <td>—</td></tr>
	 *   <tr><td>Freies Spiel, falsch</td> <td>Fehlerliste</td>       <td>—</td></tr>
	 * </table>
	 * <p>Es kommt also immer mindestens ein Dialog. Im Lernmodus wird bei Erfolg bewusst nicht
	 * gelobt: gleich danach steht ohnehin der Ausblick, und zwei Dialoge hintereinander nerven.
	 * Im freien Spiel wird nichts gespeichert, dort muss das Lob den Abschluss machen.</p>
	 *
	 * @param correct       war die Session erfolgreich
	 * @param wrongId       die falsch beantwortete Form; nur fürs Speichern
	 * @param incorrectText was im Fehlerdialog steht; bei {@code correct} unbenutzt
	 * @param allowResume   darf der Nutzer im Fehlerdialog fortsetzen
	 */
	public void end(boolean correct, String wrongId, String incorrectText, boolean allowResume) {
		if (correct && incorrectText != null && !incorrectText.isBlank())
			throw new RuntimeException("War doch alles richtig — was soll ich mit einem Fehlertext? "
					+ "Um die Anzeige bei Erfolg kümmere ich mich selbst: " + incorrectText);
		if (!correct && (incorrectText == null || incorrectText.isBlank()))
			throw new RuntimeException("Hase, Du solltest schon eine Rückmeldung geben, was falsch war, oder?");

		if (correct) {
			// Gelobt wird nur im freien Spiel — im Lernmodus folgt gleich der Ausblick.
			if (spec.isPlaySession())
				Alerts.show("Korrekt", "Super gemacht!", ButtonEnum.OK);
			saveAndEndSession(correct, wrongId);
		} else {
			switch (showMistakeAlert(incorrectText, allowResume)) {
				case OK     -> saveAndEndSession(correct, wrongId);   // auswerten, dann Schluss
				case CANCEL -> endSession();                          // Abbruch: nichts speichern
				case RESUME -> progress.resume();                     // weiterspielen: kein Ende
				default     -> throw new RuntimeException("Diesen Knopf kenne ich hier nicht. "
						+ "Neu dazugebaut und den switch vergessen?");
			}
		}
	}

	/**
	 * Zeigt, was schiefging, und holt die Entscheidung des Nutzers ein.
	 *
	 * <p><b>Abbrechen gibt es nur im Lernmodus.</b> Der Knopf bedeutet „beenden, ohne den
	 * Fortschritt fortzuschreiben" — im freien Spiel wird ohnehin nichts geschrieben, dort täte er
	 * exakt dasselbe wie OK. Zwei Knöpfe mit einem Verhalten sind eine Attrappe.</p>
	 *
	 * <p>Der {@code CANCEL}-Zweig beim Aufrufer bleibt trotzdem nötig: Wer das Fenster über das X
	 * schließt, bekommt von {@link Alerts} ebenfalls {@code CANCEL} zurück.</p>
	 */
	private ButtonEnum showMistakeAlert(String incorrectText, boolean allowResume) {
		Log.info(this, "Alert wird erstellt. correct=false");

		List<ButtonEnum> knoepfe = new ArrayList<>();
		knoepfe.add(ButtonEnum.OK);
		if (!spec.isPlaySession())
			knoepfe.add(ButtonEnum.CANCEL);
		if (allowResume)
			knoepfe.add(ButtonEnum.RESUME);

		return Alerts.show("Nicht korrekt", incorrectText, new AlertOptions().noEsc(),
				knoepfe.toArray(new ButtonEnum[0]));
	}

	/** Im Lernmodus fortschreiben und zeigen, wann das Deck wieder dran ist. Danach ist Schluss. */
	private void saveAndEndSession(boolean correct, String wrongId) {
		if (!spec.isPlaySession()) {
			LearnStat stats = fortschrittSpeichern(correct, wrongId);
			Alerts.show("Ausblick", getUntilString(stats.getDueDate()), ButtonEnum.OK);
		}
		endSession();
	}

	/** Bewertet das Ergebnis und schreibt es fort. Liefert den neuen Stand für den Ausblick. */
	private LearnStat fortschrittSpeichern(boolean correct, String wrongId) {
		LearnStat stats = service.getLearnStat(spec);

		if (!stats.isDueToday())
			throw new RuntimeException("Sicherheitsnetz eingebaut. Diese Region war gar nicht dran. Und ich soll den Fortschritt überschreiben? Mache ich ungern!");

		stats.setLevel(progress.calculateNewLevel(stats.getLastPlayed(), correct, false));
		stats.setLastPlayed(AppClock.TODAY);
		if (!correct)
			stats.incrementWrongCount();
		service.savePlayedCards(spec, stats, correct, wrongId);
		return stats;
	}

	/** Beim Controller abmelden — der ersetzt die Session, danach ist sie unerreichbar. */
	private void endSession() {
		Log.info(this, "RegionsSession " + spec.getDeckType().getDisplayName() + " (play = " + spec.isPlaySession() + ") beendet.");
		onSessionEnded.run();
	}

	@Override
	public void refresh() {
		presenter.refresh();
	}
	
	public ScreenView getView() {
		return presenter.getView();
	}
	
	public void closeSilent(boolean save) {
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
