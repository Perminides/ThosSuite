package scripts.flaggen;

import static scripts.flaggen.Zeile.bracket;
import static scripts.flaggen.Zeile.untolerated;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Die Antwortzeilen der MC-Schritte. Jede prüft, dass die richtige und die tolerierten Antworten
 * buchstabengleich unter den Optionen stehen — sonst stünde eine Antwort zweimal in der Frage.
 */
final class Antworten {

	private Antworten() {}

	/**
	 * Die richtige Antwort mit {@code +}, alle anderen nackt.
	 *
	 * <p>Sie muss <b>buchstabengleich</b> unter den Optionen stehen — sonst bliebe sie in der
	 * Ablenkerliste stehen und stünde zweimal in der Frage, einmal als richtig und einmal als
	 * falsch. Das faellt beim Lesen der Zeile nicht auf, deshalb faellt es hier auf.</p>
	 */
	static String answer(String correct, String... options) {
		List<String> wrong = new ArrayList<>(List.of(options));
		if (!wrong.remove(correct))
			throw new IllegalArgumentException("Die richtige Antwort '" + correct
					+ "' steht nicht unter ihren Optionen: " + String.join("|", wrong));
		return "MC:+" + correct + "|" + String.join("|", wrong);
	}

	/**
	 * Wie {@link #answer}, aber mit tolerierten Zweitantworten — ein Klick darauf gilt als falsch,
	 * bricht die Karte aber nicht ab.
	 *
	 * <p>Dafür muss die <b>Präfixform</b> her, die Altform kennt kein {@code ~}. Kein {@code =}
	 * dabei: Die Reihenfolge soll weiter gemischt werden, anders als bei {@link #fixedOrder}. Ohne
	 * Klammer bleibt es bei der Altform — es soll sich nur ändern, wo wirklich eine Toleranz steht.</p>
	 */
	static String answer(String correct, List<String> tolerated, String... options) {
		if (tolerated.isEmpty())
			return answer(correct, options);
		List<String> alle = List.of(options);
		if (!alle.contains(correct))
			throw new IllegalArgumentException("Die richtige Antwort '" + correct
					+ "' steht nicht unter ihren Optionen: " + String.join("|", alle));
		for (String value : tolerated)
			if (!alle.contains(value))
				throw new IllegalArgumentException("Die tolerierte Antwort '" + value
						+ "' steht nicht unter ihren Optionen: " + String.join("|", alle));

		List<String> parts = new ArrayList<>();
		parts.add("+" + correct);
		for (String value : tolerated)
			if (!value.equals(correct))
				parts.add("~" + value);
		for (String option : options)
			if (!option.equals(correct) && !tolerated.contains(option))
				parts.add(option);
		return "MC:" + String.join("|", parts);
	}

	static String fixedOrder(String correct, String... options) {
		return fixedOrder(correct, List.of(), options);
	}

	/**
	 * Wie {@link #answer}, aber in fester Reihenfolge: {@code =} hält die Optionen, wie sie hier
	 * stehen, die richtige trägt ihr {@code +}. Für Sätze mit natürlicher Ordnung — Richtungen, Zahlen.
	 *
	 * <p>Tolerierte Antworten tragen ein {@code ~}: Ein Klick darauf gilt als falsch, bricht die Karte
	 * aber nicht ab. Sie müssen genauso buchstabengleich unter den Optionen stehen wie die richtige.</p>
	 */
	static String fixedOrder(String correct, List<String> tolerated, String... options) {
		List<String> parts = new ArrayList<>();
		boolean found = false;
		for (String option : options)
			if (option.equals(correct)) {
				parts.add("+" + option);
				found = true;
			} else if (tolerated.contains(option)) {
				parts.add("~" + option);
			} else {
				parts.add(option);
			}
		if (!found)
			throw new IllegalArgumentException("Die richtige Antwort '" + correct
					+ "' steht nicht unter ihren Optionen: " + String.join("|", options));
		for (String value : tolerated)
			if (!List.of(options).contains(value))
				throw new IllegalArgumentException("Die tolerierte Antwort '" + value
						+ "' steht nicht unter ihren Optionen: " + String.join("|", options));
		return "MC:=" + String.join("|", parts);
	}

	/**
	 * Eine kodierte Spalte: Der Wert ist der Index seiner Antwort. Die Ziffer darf niemals in die
	 * Frage geraten — sie steht im Blatt, weil dort alle Attribute Zahlen sind. Eine Klammer dahinter
	 * trägt tolerierte Codes, {@code 3 (5)} lässt die 5 durchgehen. Mehrere Codes dürfen denselben
	 * Antworttext haben, er steht dann nur einmal in der Frage.
	 */
	static String coded(String value, String... options) {
		return coded(value, List.of(), options);
	}

	/**
	 * Wie {@link #coded(String, String...)}, aber ohne die Antworten in {@code hidden}: Die Codes
	 * bleiben Indizes in {@code options}, gezeigt wird nur der Rest. Für Antworten, die eine frühere
	 * Antwort ausschließt — bei zwei Streifen gibt es keinen mittleren.
	 */
	static String coded(String value, List<String> hidden, String... options) {
		String correct = word(untolerated(value), options);
		List<String> tolerated = new ArrayList<>();
		for (String code : bracket(value)) {
			String text = word(code, options);
			if (!text.equals(correct) && !tolerated.contains(text))
				tolerated.add(text);
		}
		if (hidden.contains(correct) || tolerated.stream().anyMatch(hidden::contains))
			throw new RuntimeException("'" + value + "' zeigt auf eine Antwort, die hier ausgeschlossen ist: " + hidden);
		Set<String> shown = new LinkedHashSet<>(List.of(options));
		shown.removeAll(hidden);
		return answer(correct, tolerated, shown.toArray(String[]::new));
	}

	/** Ein Wert der Spalte, übersetzt in seinen Antworttext. */
	static String word(String value, String... words) {
		return words[Integer.parseInt(value)];
	}
}
