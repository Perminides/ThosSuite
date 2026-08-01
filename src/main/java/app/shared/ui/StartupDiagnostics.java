package app.shared.ui;

import app.shared.Log;
import app.shared.model.ButtonEnum;
import javafx.animation.AnimationTimer;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;

/**
 * !tmp: Startdiagnose für das Phänomen „Fenster nimmt Klicks an, zeigt aber nichts".
 *
 * <p>Beobachtet: Nach dem Start bleibt das Bild stehen, Klicks kommen aber an (ein weggeklickter
 * Tagebuch-Dialog war weg, sobald die Maus das Fenster einmal verlassen und wieder betreten hatte).
 * Tritt sporadisch auf — mal dreimal hintereinander, mal zehnmal nicht.</p>
 *
 * <p><b>Wozu das hier reicht.</b> Dass Klicks ankommen, schließt einen hängenden FX-Thread aus;
 * es ist ein Präsentationsproblem. Dafür bleiben zwei Erklärungen, und diese Diagnose trennt sie:</p>
 * <ol>
 *   <li><b>Die Deckkraft steht noch auf 0.</b> Der Start zeigt das Fenster mit {@code opacity=0}
 *       gegen den White-Flash und holt das erst hinter einer {@code PauseTransition} und zwei
 *       geschachtelten {@code runLater} zurück. Ein Fenster mit Deckkraft 0 ist unter Windows
 *       weiterhin klickbar — das passt auf die Beobachtung. Dann wäre es unser Fehler.</li>
 *   <li><b>Windows setzt das Fenster nicht neu zusammen.</b> Deckkraft ist 1, das Bild kommt
 *       trotzdem erst beim nächsten Anlass — und „Maus raus und wieder rein" ist genau so einer.
 *       Dann liegt es nicht an uns.</li>
 * </ol>
 *
 * <p>Die Unterscheidung hängt an <b>einer</b> Zahl: der Deckkraft im Moment des Stillstands. Die
 * Pulse-Zahl steht daneben, weil eine Zeile ohne Pulse-Fortschritt beide Erklärungen widerlegen
 * würde — dann stünde doch der FX-Thread.</p>
 *
 * <p><b>Die Fensterzahl ist für den Dialog-Fall da.</b> Als das Phänomen zuletzt auftrat, hing ein
 * Startup-Dialog: weggeklickt, aber weiter zu sehen. Für diesen Fall sagt die Deckkraft der
 * Hauptbühne nichts — die steht dann ohnehin auf 0. Sinkt die Fensterzahl in dem Moment, in dem der
 * Dialog auf dem Bildschirm stehen bleibt, ist bewiesen, dass er längst zu ist und nur das Bild
 * nicht nachkommt.</p>
 *
 * <p><b>Und die Nachfrage am Ende gibt dem Block sein Etikett.</b> Ohne sie stehen im Log zwanzig
 * Diagnose-Blöcke, von denen drei interessant sind, und man sieht ihnen nicht an, welche. Die App
 * kann das nicht selbst entscheiden: dass Windows das Bild nicht zeigt, ist von innen unsichtbar —
 * Pulse und Deckkraft sehen dann völlig normal aus. Deshalb fragen wir den Einzigen, der es sieht.
 * Der Dialog ist absichtlich lästig und verschwindet mit dieser Klasse.</p>
 *
 * <p>Warum die Nachfrage erst nach {@value #NACHFRAGE_SEKUNDEN} Sekunden kommt und trotzdem
 * verlässlich sichtbar ist: das Symptom löst sich empirisch nach wenigen Sekunden von selbst auf,
 * sobald die Maus das Fenster einmal verlässt. Bliebe der Dialog wider Erwarten unsichtbar, wäre
 * genau das der interessanteste Befund.</p>
 *
 * <p>Diese Klasse wohnt in {@code shared.ui} und nicht bei {@code Log} und {@code DB} in der
 * {@code shared}-Wurzel, weil sie {@link Alerts} braucht — und die Wurzel darf nicht nach
 * {@code shared.ui} greifen (Wächter 7).</p>
 */
public final class StartupDiagnostics {

	private static final long INTERVALL_NS = 1_000_000_000L;

	/** Nachlauf, nachdem die Deckkraft zurückgedreht wurde. Reicht bis über die Nachfrage hinaus. */
	private static final long NACHLAUF_NS = 25_000_000_000L;

	/** Notbremse, falls die Deckkraft nie zurückkommt: dann ist genau das der Befund. */
	private static final long HARTES_ENDE_NS = 180_000_000_000L;

	private static final int NACHFRAGE_SEKUNDEN = 20;

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

		new AnimationTimer() {
			private long start;
			private long letzteAusgabe;
			private long deckkraftDa;
			private int pulses;

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

				// Der Moment, in dem das Fenster sichtbar wird — ab hier zählt die Nachfrage.
				if (deckkraftDa == 0 && stage.getOpacity() == 1) {
					deckkraftDa = now;
					nachfrageStarten();
				}

				Log.info(StartupDiagnostics.class, "Startdiagnose"
						+ " t=" + (now - start) / INTERVALL_NS + "s"
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
					Log.info(StartupDiagnostics.class, "Startdiagnose Mitschrift beendet");
					stop();
				}
			}
		}.start();
	}

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
				"War der Start eben normal — Fenster sofort da und alles gleich bedienbar?\n\n"
				+ "„Nein\" heißt: das Bild hing, bis Du die Maus einmal aus dem Fenster heraus und "
				+ "wieder hinein bewegt hast. Klicks kamen dabei durchaus an, man sah nur nichts.\n\n"
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
				+ " prism.order=" + System.getProperty("prism.order")
				+ " prism.allowhidpi=" + System.getProperty("prism.allowhidpi")
				+ " prism.lcdtext=" + System.getProperty("prism.lcdtext")
				+ " glass.win.uiScale=" + System.getProperty("glass.win.uiScale")
				+ " sun.java2d.uiScale=" + System.getProperty("sun.java2d.uiScale")
				+ " outputScale=" + primaer.getOutputScaleX() + "x" + primaer.getOutputScaleY()
				+ " bounds=" + primaer.getBounds());
	}

	/** Die Scene fehlt nur, wenn etwas sehr schiefgegangen ist — dann steht das hier statt eines NPE. */
	private static String sceneMasse(Stage stage) {
		if (stage.getScene() == null)
			return "keine";
		return (int) stage.getScene().getWidth() + "x" + (int) stage.getScene().getHeight();
	}
}
