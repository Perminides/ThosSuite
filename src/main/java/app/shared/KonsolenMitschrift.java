package app.shared;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * !tmp: Fängt {@code System.out} und {@code System.err} ab und bringt sie ins Log.
 *
 * <p><b>Wozu.</b> Die Diagnoseschalter von JavaFX — {@code prism.verbose}, {@code prism.showdirty},
 * der Pulse-Logger — schreiben ausschließlich auf die Konsole. Im gepackten Programm gibt es keine,
 * also war nicht zu erfahren, welche Grafik-Pipeline überhaupt läuft. Auf diesem Weg ist D3D
 * bestätigt worden; {@code prism.verbose} steht seitdem nicht mehr im Build. Der Abgriff bleibt,
 * weil er auch alles fängt, was JavaFX unaufgefordert auf die Konsole schreibt — die Hinweise zu
 * den Preview-Features etwa, oder was nach einem Versionssprung dazukommt.</p>
 *
 * <p><b>Warum gepuffert.</b> Prism meldet sich, sobald die erste Bühne erscheint — und das ist der
 * Splash, lange bevor {@link Log#initLog} den Dateihandler gesetzt hat. Würde die Umleitung erst
 * dort greifen, wäre die interessanteste Zeile schon verloren. Deshalb sammelt {@link #starten()}
 * ab dem allerersten Moment in einen Puffer, den {@link #ausschuetten()} nach der Log-Initialisierung
 * in die Datei kippt. Danach geht jede weitere Zeile direkt ins Log.</p>
 *
 * <p>Die Konsole selbst bleibt bedient: In Eclipse steht weiterhin alles da, wo es hingehört.</p>
 */
public final class KonsolenMitschrift {

	private static final List<String> puffer = new ArrayList<>();
	private static boolean logBereit = false;

	/**
	 * Sperre gegen den Kreisverkehr: {@code Log.info} schreibt über den ConsoleHandler auf die
	 * Konsole — und die ist ab {@link #starten()} genau hierher umgeleitet. Ohne diese Sperre löst
	 * jede Log-Zeile die nächste aus, bis der Stack reißt.
	 */
	private static final ThreadLocal<Boolean> imMelden = ThreadLocal.withInitial(() -> false);

	/** Die echte Konsole, bevor wir uns davorgehängt haben. */
	private static PrintStream originalOut = System.out;

	private KonsolenMitschrift() {}

	/**
	 * Vor {@code launch()} aufzurufen — je früher, desto mehr wird mitgeschrieben.
	 */
	public static void starten() {
		originalOut = System.out;
		System.setOut(umleiten(System.out, "stdout"));
		System.setErr(umleiten(System.err, "stderr"));
	}

	/**
	 * Die unveränderte Konsole — für den ConsoleHandler in {@link Log}.
	 *
	 * <p><b>Warum das nötig ist:</b> Der Handler schreibt jede Log-Zeile auf {@code System.out}. Ist
	 * das unser Abgriff, wird jede Zeile ein zweites Mal geloggt — das Log verdoppelt sich. Bekommt
	 * er den Originalstrom, sieht der Abgriff nur noch, was <i>nicht</i> vom Logger kommt: genau die
	 * Ausgaben von Prism und Co., um die es hier geht.</p>
	 */
	public static PrintStream originalOut() {
		return originalOut;
	}

	/**
	 * Nach {@link Log#initLog} aufzurufen: schreibt das bis dahin Gesammelte in die Datei und schaltet
	 * auf Durchreichen um.
	 */
	public static void ausschuetten() {
		List<String> gesammelt;
		synchronized (puffer) {
			// Erst kopieren, dann außerhalb der Sperre schreiben: Das Loggen läuft über die
			// umgeleitete Konsole wieder hier herein und würde sonst die Liste unter der
			// laufenden Iteration verändern.
			gesammelt = new ArrayList<>(puffer);
			puffer.clear();
			logBereit = true;
		}

		imMelden.set(true);
		try {
			for (String zeile : gesammelt)
				Log.info(KonsolenMitschrift.class, "Konsole (nachgereicht) " + zeile);
		} finally {
			imMelden.set(false);
		}
	}

	/**
	 * Ein Stream, der jede vollständige Zeile abgreift und zusätzlich ans Original weitergibt.
	 *
	 * <p>Gesammelt wird zeilenweise, weil {@code print} auch einzelne Zeichen liefern kann — eine
	 * Log-Zeile je Zeichen wäre unlesbar.</p>
	 */
	private static PrintStream umleiten(PrintStream original, String quelle) {
		ByteArrayOutputStream zeile = new ByteArrayOutputStream();

		OutputStream abgriff = new OutputStream() {
			@Override
			public void write(int b) {
				original.write(b);
				if (b == '\n') {
					melden(quelle, zeile.toString(StandardCharsets.UTF_8).strip());
					zeile.reset();
				} else if (b != '\r') {
					zeile.write(b);
				}
			}

			@Override
			public void flush() {
				original.flush();
			}
		};

		return new PrintStream(abgriff, true, StandardCharsets.UTF_8);
	}

	private static void melden(String quelle, String text) {
		// imMelden: Diese Zeile stammt aus unserem eigenen Log-Aufruf. Sie nochmal zu loggen wäre
		// der Anfang einer Endlosschleife.
		if (text.isEmpty() || imMelden.get())
			return;

		boolean direkt;
		synchronized (puffer) {
			direkt = logBereit;
			if (!direkt)
				puffer.add("[" + quelle + "] " + text);
		}

		if (!direkt)
			return;

		imMelden.set(true);
		try {
			Log.info(KonsolenMitschrift.class, "Konsole [" + quelle + "] " + text);
		} finally {
			imMelden.set(false);
		}
	}
}
