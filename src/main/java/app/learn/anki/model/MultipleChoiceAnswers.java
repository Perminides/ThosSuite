package app.learn.anki.model;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import app.learn.anki.model.Card.AnswerOption;
import app.learn.anki.model.Card.Role;

/** Die für einen Step gezeigten Optionen — eingefrorene Reihenfolge, Rollen dieses Steps. */
public class MultipleChoiceAnswers {

    private final List<AnswerOption> options;

    public MultipleChoiceAnswers(List<AnswerOption> shownOptions) {
        this.options = List.copyOf(shownOptions);
    }

    public List<AnswerOption> getAnswerOptions() {
        return options;
    }

    public Role roleAt(int index) {
        return options.get(index).role();
    }

    public Set<Integer> getCorrectIndexes() {
        Set<Integer> indexes = new HashSet<>();
        for (int i = 0; i < options.size(); i++)
            if (options.get(i).role() == Role.CORRECT)
                indexes.add(i);
        return indexes;
    }

    /** Richtig ist die Auswahl, wenn sie — ohne die nachsichtigen — genau die richtigen trifft. */
    public boolean isFinallyCorrect(Set<Integer> clickedIds) {
        Set<Integer> effective = new HashSet<>(clickedIds);
        for (int i = 0; i < options.size(); i++)
            if (options.get(i).role() == Role.TOLERATED)
                effective.remove(i);
        return getCorrectIndexes().equals(effective);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("[");
        for (AnswerOption option : options) {
            if (sb.length() > 1)
                sb.append(", ");
            sb.append(option.text());
        }
        return sb.append("]").toString();
    }
}
