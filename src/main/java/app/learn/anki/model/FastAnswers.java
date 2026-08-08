package app.learn.anki.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import app.learn.anki.model.Card.Answer;
import app.learn.anki.model.Card.Fast;

/**
 * Der Stand eines laufenden {@link Fast}-Steps: welche Antwort schon saß, in welchem Feld sie steht
 * und was beim Zeitablauf noch aufzudecken ist. Pendant zu {@link MultipleChoiceAnswers} — der
 * CardProgress hält beide für die Dauer eines Steps.
 *
 * <p><b>Gebunden oder nicht.</b> Ein Feld gehört genau dann einer bestimmten Antwort, wenn es etwas
 * zu binden gibt: ein Hinweistext oder eine erzwungene Reihenfolge. Dann steht jede Antwort in ihrem
 * eigenen Feld. Ohne Bindung trägt die Position keine Information — die Felder füllen sich von oben
 * in der Reihenfolge, in der die Antworten kommen, und der Vorrat darf größer sein als die Zahl der
 * Felder.</p>
 */
public class FastAnswers {

	/** Ein Treffer: in welches Feld er gehört und was dort stehen soll. */
	public record Hit(int slot, String text) {}

	private final Fast step;
	private final boolean bound;
	private final Map<String, List<Integer>> answerIndexesByVariant = new HashMap<>();

	private final int[] slotToAnswer; // -1 = Feld noch leer
	private final boolean[] answered;
	private int filledSlots;
	private int nextOrdered;

	public FastAnswers(Fast step) {
		this.step = step;
		this.bound = step.ordered() || hasHints(step);
		this.slotToAnswer = new int[step.slots()];
		this.answered = new boolean[step.answers().size()];

		for (int slot = 0; slot < slotToAnswer.length; slot++)
			slotToAnswer[slot] = -1;

		for (int index = 0; index < step.answers().size(); index++)
			for (String variant : step.answers().get(index).variants())
				answerIndexesByVariant.computeIfAbsent(variant.toLowerCase(), _ -> new ArrayList<>()).add(index);
	}

	/**
	 * Prüft die Eingabe gegen die noch offenen Antworten.
	 *
	 * @return das getroffene Feld samt anzuzeigendem Text, oder {@code null} wenn die Eingabe (noch)
	 *         auf keine offene Antwort passt — auch bei einer bereits gegebenen Antwort.
	 */
	public Hit accept(String typed) {
		List<Integer> kandidaten = answerIndexesByVariant.get(typed.trim().toLowerCase());
		if (kandidaten == null)
			return null;

		int answerIndex = pickAnswer(kandidaten);
		if (answerIndex < 0)
			return null;

		int slot = bound ? answerIndex : nextFreeSlot();
		answered[answerIndex] = true;
		slotToAnswer[slot] = answerIndex;
		filledSlots++;
		if (step.ordered())
			nextOrdered++;
		return new Hit(slot, textOf(answerIndex));
	}

	public boolean isComplete() {
		return filledSlots >= slotToAnswer.length;
	}

	/** Die Texte, die zu Beginn in den Feldern stehen — ein Hinweis oder nichts. */
	public List<String> slotHints() {
		List<String> hints = new ArrayList<>();
		for (int slot = 0; slot < slotToAnswer.length; slot++) {
			String hint = bound ? step.answers().get(slot).hint() : null;
			hints.add(hint == null ? "" : hint);
		}
		return hints;
	}

	/**
	 * Was beim Zeitablauf in den noch leeren Feldern erscheint. Ohne Bindung kommen die Antworten aus
	 * dem Vorrat in CSV-Reihenfolge nach — bei einem großen Vorrat sieht man also Beispiele, nicht alles.
	 */
	public Map<Integer, String> revealRest() {
		Map<Integer, String> rest = new LinkedHashMap<>();
		int ungenannt = 0;
		for (int slot = 0; slot < slotToAnswer.length; slot++) {
			if (slotToAnswer[slot] >= 0)
				continue;
			if (bound) {
				rest.put(slot, textOf(slot));
			} else {
				while (ungenannt < answered.length && answered[ungenannt])
					ungenannt++;
				if (ungenannt < answered.length) {
					rest.put(slot, textOf(ungenannt));
					ungenannt++;
				}
			}
		}
		return rest;
	}

	/**
	 * Das Feld, das als nächstes dran ist.
	 *
	 * @return der Feldindex, oder {@code null} wenn die Reihenfolge egal ist oder der Step durch ist.
	 *         Ohne Reihenfolge ist es ohnehin sichtbar das oberste ungefüllte.
	 */
	public Integer expectedSlot() {
		if (!step.ordered() || isComplete())
			return null;
		return nextOrdered;
	}

	private int pickAnswer(List<Integer> kandidaten) {
		for (int index : kandidaten) {
			if (answered[index])
				continue;
			if (step.ordered() && index != nextOrdered)
				continue;
			return index;
		}
		return -1;
	}

	private int nextFreeSlot() {
		for (int slot = 0; slot < slotToAnswer.length; slot++)
			if (slotToAnswer[slot] < 0)
				return slot;
		throw new RuntimeException("Kein freies Antwortfeld mehr — isComplete() hätte vorher greifen müssen");
	}

	/** Aufgedeckt wird immer die erste Schreibvariante. */
	private String textOf(int answerIndex) {
		return step.answers().get(answerIndex).variants().get(0);
	}


	private static boolean hasHints(Fast step) {
		for (Answer answer : step.answers())
			if (answer.hint() != null)
				return true;
		return false;
	}
}
