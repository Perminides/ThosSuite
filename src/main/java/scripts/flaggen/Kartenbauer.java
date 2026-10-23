package scripts.flaggen;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Die Schritte einer Karte, von Id, Bemerkung und Label an. Eine Instanz je Karte: Der wartende
 * Shuffle-Marker gehört zu genau dieser Karte und kann nicht in die nächste hinüberwandern.
 */
final class Kartenbauer {

	private final List<String> steps = new ArrayList<>();
	/** {@code <ShuffleEnd>} gehört an den Schritt NACH dem Block — er selbst liegt außerhalb. */
	private String pending = "";

	Kartenbauer(int id, String remark, String label) {
		steps.add(String.valueOf(id));
		steps.add(remark);
		steps.add(label);
	}

	/**
	 * Ein Schritt, mit dem wartenden Shuffle-Marker davor.
	 *
	 * <p>Das Semikolon trennt die Spalten der Deck-Datei. Stünde eines in einem Fragetext, verschöben
	 * sich ab da alle Schritte der Karte — und zwar still, denn die Zeile bliebe lesbar.</p>
	 */
	void add(String step) {
		if (step.indexOf(';') >= 0)
			throw new RuntimeException("Ein Semikolon zerlegt die Deck-Zeile: " + step);
		steps.add(pending + step);
		pending = "";
	}

	void ask(String question, String mc) {
		add("Output:" + question);
		add(mc);
	}

	/**
	 * Ein Shuffle-Block: je Eintrag ein Segment, gemischt wird die Reihenfolge der Segmente. Bei
	 * einem einzigen Eintrag gibt es nichts zu mischen — dann bleiben die Marker ganz weg.
	 */
	<T> void shuffled(List<T> items, Consumer<T> segment) {
		if (items.size() < 2) {
			items.forEach(segment);
			return;
		}
		// Ein wartendes <ShuffleEnd> kann nicht auf demselben Schritt stehen wie der neue Start: Der
		// Parser prüft auf Start, bevor er das End abschneidet. Also erst allein hinausschreiben.
		if (!pending.isEmpty())
			add("");
		for (int i = 0; i < items.size(); i++) {
			pending = i == 0 ? "<ShuffleStart>" : "<ShuffleBreak>";
			segment.accept(items.get(i));
		}
		pending = "<ShuffleEnd>";
	}

	/**
	 * Die Zeile bis hierher, veränderbar — für Nachläufe über die fertige Karte wie den Hinweis.
	 * Ein noch wartender Marker hieße: Die Karte endet mitten in einem Shuffle-Block.
	 */
	List<String> steps() {
		if (!pending.isEmpty())
			throw new RuntimeException("Die Karte endet mit einem offenen " + pending);
		return steps;
	}
}
