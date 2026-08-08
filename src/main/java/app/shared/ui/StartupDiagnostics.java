package app.shared.ui;

import app.shared.Log;
import app.shared.model.ButtonEnum;
import javafx.animation.AnimationTimer;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.robot.Robot;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;

/**
 * !tmp: Startdiagnose für das Phänomen „Fenster ist da und sieht richtig aus, reagiert aber nicht".
 *
 * <p>Beobachtet: Nach dem Start ist das Hauptfenster vollständig und korrekt gezeichnet, ein Klick
 * auf ein Menü öffnet es aber nicht. Sobald die Maus das Fenster einmal verlassen hat und wieder
 * hineinkommt, geht alles. Tritt sporadisch auf — mal dreimal hintereinander, mal zehnmal nicht.</p>
 *
 * <h2>Was bereits feststeht</h2>
 * Die beiden naheliegenden Erklärungen sind widerlegt. Die Eingabe kommt an — im Protokoll steht der
 * Klick, und die Session startete —, und die Darstellung hängt nicht komplett: Eine mitzählende Zahl
 * lief sichtbar weiter, während der Rest der Fläche einen veralteten Stand zeigte. Übrig bleibt ein
 * <b>partielles</b> Darstellungsproblem. Der ganze Stand steht in
 * {@code docs/Startproblem-Diagnose.md}.
 *
 * <h2>Was hier misst</h2>
 * <ul>
 *   <li><b>Das Eingabeprotokoll</b> hängt als Filter an der Scene und sieht jeden Klick, jede Taste
 *       und jeden Maus-Ein-/Austritt, bevor irgendein Knoten sie behandelt. Der ENTERED-Eintrag
 *       markiert außerdem den Moment der Heilung. <b>Achtung:</b> Klicks in Menü-Popups sieht dieser
 *       Filter <i>nicht</i> — die sind eigene Fenster mit eigener Scene. Ein leeres Protokoll
 *       beweist also nicht, dass nicht geklickt wurde.</li>
 *   <li><b>Die Cursor-Position</b> im Moment des Deckkraftwechsels, gemessen über den {@link Robot}.
 *       Prüft über mehrere Starts, ob das Phänomen damit zusammenfällt, dass der Zeiger beim
 *       Umschalten schon über dem Fenster stand.</li>
 *   <li><b>Rechner, Deckkraft, Pulse, Fensterzahl, Szenenmaße.</b> Der Rechnername trennt beim
 *       Auswerten zwei Populationen; die Pulse-Zahl kann nur einen einzigen Zustand aufdecken —
 *       komplett stehender FX-Thread.</li>
 *   <li><b>Die Konsolenausgabe</b> landet über {@code KonsolenMitschrift} im Log. Erst dadurch ist
 *       zu erfahren, welche Grafik-Pipeline Prism gewählt hat ({@code -Dprism.verbose=true}).</li>
 * </ul>
 *
 * <p>Entfernt, weil ihre Frage beantwortet ist: die sichtbare Sekundenzahl (sie hat den Befund
 * geliefert) und ein Probe-Menü, das sich selbst öffnete — letzteres griff ins Geschehen ein, weil
 * ein Klick des Nutzers es schloss.</p>
 *
 * <p><b>Die Nachfrage am Ende gibt dem Block sein Etikett.</b> Ob der Start gut war, kann die
 * Anwendung nicht selbst entscheiden: In beiden Fällen sehen ihre eigenen Messwerte normal aus.
 * Deshalb fragen wir den Einzigen, der es sieht. Der Dialog ist absichtlich lästig und verschwindet
 * mit dieser Klasse.</p>
 *
 * <p>Diese Klasse wohnt in {@code shared.ui} und nicht bei {@code Log} und {@code DB} in der
 * {@code shared}-Wurzel, weil sie {@link Alerts} braucht — und die Wurzel darf nicht nach
 * {@code shared.ui} greifen (Wächter 7).</p>
 */
public final class StartupDiagnostics {

	private static final long INTERVALL_NS = 1_000_000_000L;

	/** Nachlauf, nachdem die Deckkraft zurückgedreht wurde. Reicht bis über die Nachfrage hinaus. */
	private static final long NACHLAUF_NS = 35_000_000_000L;

	/** Notbremse, falls die Deckkraft nie zurückkommt: dann ist genau das der Befund. */
	private static final long HARTES_ENDE_NS = 180_000_000_000L;

	/** Sekunden nach dem Deckkraftwechsel, gerechnet ab dem sichtbaren Fenster. */
	private static final int NACHFRAGE_SEKUNDEN = 30;

	private StartupDiagnostics() {}

	/**
	 * Hängt sich an den Pulse und schreibt im Sekundentakt eine Zeile. Aufzurufen, sobald die Stage
	 * sichtbar ist — also <i>vor</i> dem Zurückdrehen der Deckkraft, damit der Übergang im Log steht
	 * und nicht nur sein Ergebnis.
	 *
	 * <p>Läuft bewusst über die Startup-Dialoge hinweg: die PostTasks können mehrere Minuten
	 * dauern, wenn jemand langsam durchklickt, und genau dort trat das Phänomen zuletzt auf. Ein
	 * modales {@code showAndWait()} hält den Pulse nicht an — es startet einen verschachtelten
	 * Event-Loop, der weiter pumpt. Deshalb schreibt diese Diagnose auch dann weiter.</p>
	 */
	public static void watch(Stage stage) {
		logUmgebung();
		Runnable protokollAbhaengen = eingabeProtokollAnhaengen(stage.getScene());

		new AnimationTimer() {
			private long start;
			private long letzteAusgabe;
			private long deckkraftDa;
			private int pulses;
			private int sekunde;

			@Override
			public void handle(long now) {
				pulses++;

				if (start == 0) {
					start = now;
					letzteAusgabe = now;
					return;
				}
				if (now - letzteAusgabe < INTERVALL_NS)
					return;

				sekunde = (int) ((now - start) / INTERVALL_NS);

				// Der Moment, in dem das Fenster sichtbar wird — ab hier zählt die Nachfrage.
				if (deckkraftDa == 0 && stage.getOpacity() == 1) {
					deckkraftDa = now;
					cursorLageLoggen(stage);
					nachfrageStarten();
				}

				Log.info(StartupDiagnostics.class, "Startdiagnose"
						+ " t=" + sekunde + "s"
						+ " pulses=" + pulses
						+ " opacity=" + stage.getOpacity()
						+ " focused=" + stage.isFocused()
						+ " showing=" + stage.isShowing()
						+ " fenster=" + Window.getWindows().size()
						+ " scene=" + sceneMasse(stage));

				pulses = 0;
				letzteAusgabe = now;

				boolean nachlaufVorbei = deckkraftDa != 0 && now - deckkraftDa >= NACHLAUF_NS;
				if (nachlaufVorbei || now - start >= HARTES_ENDE_NS) {
					protokollAbhaengen.run();
					Log.info(StartupDiagnostics.class, "Startdiagnose Mitschrift beendet");
					stop();
				}
			}
		}.start();
	}

	// ========================================================================
	// Die Messungen
	// ========================================================================

	/**
	 * Klicks, Tasten und Maus-Ein-/Austritte, mitgeschrieben als <b>Filter</b> auf der Scene: So
	 * steht die Zeile im Log, bevor irgendein Knoten das Ereignis behandeln oder verbrauchen kann.
	 * Bewegungen werden auf eine Zeile je Sekunde gedrosselt, sonst ersäuft das Log.
	 *
	 * @return die Routine, die alle Filter wieder abhängt — aufzurufen, wenn die Mitschrift endet.
	 *         Sonst protokolliert die Diagnose bis zum Programmende jede Mausbewegung mit.
	 */
	private static Runnable eingabeProtokollAnhaengen(Scene scene) {
		if (scene == null) {
			Log.warn(StartupDiagnostics.class, "Startdiagnose Eingabeprotokoll nicht moeglich: keine Scene");
			return () -> {};
		}

		EventHandler<MouseEvent> klick = e -> Log.info(StartupDiagnostics.class,
				"Startdiagnose EINGABE klick x=" + (int) e.getSceneX() + " y=" + (int) e.getSceneY()
						+ " ziel=" + zielName(e.getTarget()));

		EventHandler<KeyEvent> taste = e -> Log.info(StartupDiagnostics.class,
				"Startdiagnose EINGABE taste " + e.getCode());

		EventHandler<MouseEvent> betritt = _ -> Log.info(StartupDiagnostics.class,
				"Startdiagnose EINGABE maus betritt fenster");

		EventHandler<MouseEvent> verlaesst = _ -> Log.info(StartupDiagnostics.class,
				"Startdiagnose EINGABE maus verlaesst fenster");

		long[] letzteBewegung = {0};
		EventHandler<MouseEvent> bewegung = e -> {
			long jetzt = System.nanoTime();
			if (jetzt - letzteBewegung[0] < INTERVALL_NS)
				return;
			letzteBewegung[0] = jetzt;
			Log.info(StartupDiagnostics.class, "Startdiagnose EINGABE bewegung x="
					+ (int) e.getSceneX() + " y=" + (int) e.getSceneY());
		};

		scene.addEventFilter(MouseEvent.MOUSE_PRESSED, klick);
		scene.addEventFilter(KeyEvent.KEY_PRESSED, taste);
		scene.addEventFilter(MouseEvent.MOUSE_ENTERED, betritt);
		scene.addEventFilter(MouseEvent.MOUSE_EXITED, verlaesst);
		scene.addEventFilter(MouseEvent.MOUSE_MOVED, bewegung);

		return () -> {
			scene.removeEventFilter(MouseEvent.MOUSE_PRESSED, klick);
			scene.removeEventFilter(KeyEvent.KEY_PRESSED, taste);
			scene.removeEventFilter(MouseEvent.MOUSE_ENTERED, betritt);
			scene.removeEventFilter(MouseEvent.MOUSE_EXITED, verlaesst);
			scene.removeEventFilter(MouseEvent.MOUSE_MOVED, bewegung);
		};
	}

	/**
	 * Stand der Zeiger im Moment des Deckkraftwechsels schon über dem Fenster? Ein Fenster mit
	 * Deckkraft 0 ist für die Maus durchlässig; ob der Übergang nach 1 ohne Mausbewegung etwas
	 * hinterlässt, entscheidet erst die Häufung über mehrere Starts.
	 */
	private static void cursorLageLoggen(Stage stage) {
		Point2D zeiger = new Robot().getMousePosition();
		boolean drueber = zeiger.getX() >= stage.getX()
				&& zeiger.getX() <= stage.getX() + stage.getWidth()
				&& zeiger.getY() >= stage.getY()
				&& zeiger.getY() <= stage.getY() + stage.getHeight();

		Log.info(StartupDiagnostics.class, "Startdiagnose CURSOR beim Deckkraftwechsel"
				+ " x=" + (int) zeiger.getX() + " y=" + (int) zeiger.getY()
				+ " ueberFenster=" + drueber);
	}

	// ========================================================================
	// Nachfrage und Rahmen
	// ========================================================================

	/**
	 * Die Nachfrage läuft über eine {@code PauseTransition} auf dem FX-Thread — kein eigener Thread,
	 * JavaFX-UI gehört dorthin.
	 *
	 * <p>Das {@code runLater} im {@code setOnFinished} ist Pflicht und kein Zierrat:
	 * {@code showAndWait()} darf nicht aus einer laufenden Animation heraus aufgerufen werden, sonst
	 * fliegt eine {@code IllegalStateException}. Dieselbe Falle steht schon im Startablauf von
	 * {@code ThosSuiteApp} beschrieben.</p>
	 */
	private static void nachfrageStarten() {
		PauseTransition wartezeit = new PauseTransition(Duration.seconds(NACHFRAGE_SEKUNDEN));
		wartezeit.setOnFinished(_ -> Platform.runLater(StartupDiagnostics::nachfrageZeigen));
		wartezeit.play();
	}

	private static void nachfrageZeigen() {
		ButtonEnum antwort = Alerts.show("Startdiagnose",
				"War der Start eben normal — alles sofort bedienbar?\n\n"
				+ "„Nein\" heißt: Das Fenster sah richtig aus, hat aber auf Klicks nicht reagiert, "
				+ "bis Du die Maus einmal aus dem Fenster heraus und wieder hinein bewegt hast.\n\n"
				+ "Die Antwort landet im Log und etikettiert die Messwerte darüber. Diese Frage "
				+ "verschwindet wieder, sobald die Ursache gefunden ist.",
				ButtonEnum.YES, ButtonEnum.NO);

		if (antwort == ButtonEnum.NO)
			Log.warn(StartupDiagnostics.class, "Startdiagnose BEFUND: Start war NICHT normal "
					+ "— die Messwerte oben gehören zu einem kaputten Start");
		else if (antwort == ButtonEnum.YES)
			Log.info(StartupDiagnostics.class, "Startdiagnose BEFUND: Start war normal");
		else
			Log.info(StartupDiagnostics.class, "Startdiagnose BEFUND: keine Angabe (weggeklickt)");
	}

	/**
	 * Einmalig die Rahmenbedingungen, die das Zusammensetzen des Fensters beeinflussen. Bislang
	 * stand davon nur {@code prism.allowhidpi} im Log, und das erst noch an anderer Stelle.
	 */
	private static void logUmgebung() {
		Screen primaer = Screen.getPrimary();
		Log.info(StartupDiagnostics.class, "Startdiagnose Umgebung"
				+ " rechner=" + rechnerName()
				+ " prism.order=" + System.getProperty("prism.order")
				+ " prism.allowhidpi=" + System.getProperty("prism.allowhidpi")
				+ " prism.lcdtext=" + System.getProperty("prism.lcdtext")
				+ " glass.win.uiScale=" + System.getProperty("glass.win.uiScale")
				+ " sun.java2d.uiScale=" + System.getProperty("sun.java2d.uiScale")
				+ " outputScale=" + primaer.getOutputScaleX() + "x" + primaer.getOutputScaleY()
				+ " bounds=" + primaer.getBounds());
	}

	/**
	 * Auf welchem Rechner das hier lief. Trennt beim Auswerten zwei Populationen, die sonst in einer
	 * Statistik verschwimmen — das Phänomen tritt auf verschiedenen Geräten verschieden oft auf.
	 */
	private static String rechnerName() {
		String name = System.getenv("COMPUTERNAME");
		return name == null ? "unbekannt" : name;
	}

	/** Die Scene fehlt nur, wenn etwas sehr schiefgegangen ist — dann steht das hier statt eines NPE. */
	private static String sceneMasse(Stage stage) {
		if (stage.getScene() == null)
			return "keine";
		return (int) stage.getScene().getWidth() + "x" + (int) stage.getScene().getHeight();
	}

	/** Ziel eines Ereignisses als lesbarer Name — das Ziel ist oft die Scene selbst, nicht ein Knoten. */
	private static String zielName(Object ziel) {
		if (ziel instanceof Node knoten)
			return knoten.getClass().getSimpleName();
		return ziel == null ? "null" : ziel.getClass().getSimpleName();
	}
}
