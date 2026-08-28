package app.learn.anki.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import app.learn.anki.model.Card.AnswerOption;

public class MultipleChoiceAnswers {

    private final List<AnswerOption> options = new ArrayList<>();
    
 // Konstruktor A: Zufällige Auswahl (für neue Fragen)
    public MultipleChoiceAnswers(Set<AnswerOption> pool, int maxOptions) {
    	// 1. Korrekte und falsche Antworten trennen — die korrekten MÜSSEN rein,
        //    die falschen sind der Pool für Distraktoren.
        List<AnswerOption> correctOnes = new ArrayList<>();
        List<AnswerOption> wrongOnes = new ArrayList<>();
        for (AnswerOption option : pool) {
            if (option.correct())
                correctOnes.add(option);
            else
                wrongOnes.add(option);
        }
        Collections.shuffle(wrongOnes);

        // 3. Liste zusammenbauen
        List<AnswerOption> selection = new ArrayList<>(correctOnes);

        // 4. Auffüllen bis maxOptions (falls noch Platz ist)
        int slotsFree = maxOptions - selection.size();
        if (slotsFree > 0) {
            // Nimm so viele falsche, wie reinpassen (oder so viele wie da sind)
            selection.addAll(wrongOnes.subList(0, Math.min(wrongOnes.size(), slotsFree)));
        }
        
        // 5. Die finale Auswahl mischen, damit die richtigen Antworten nicht immer oben stehen
        Collections.shuffle(selection);
        
        options.addAll(selection);
    }
    
 // Konstruktor B: Vorgegebene Reihenfolge von dem Step davor
    public MultipleChoiceAnswers(Set<AnswerOption> pool, List<String> targetOrder) {
        Map<String, Boolean> correctness = new HashMap<>();
        for (AnswerOption option : pool)
            if (correctness.put(option.text(), option.correct()) != null)
                throw new IllegalStateException("Zwei Antworten mit demselben Text: " + option.text());

            
        for (String text : targetOrder) {
            // Wir bauen die Optionen exakt so auf, wie das UI sie erwartet
            // Aber wir holen uns die "Korrektheit" frisch aus dem Pool (falls sich die Wahrheit geändert hat!)
            boolean isCorrect = correctness.getOrDefault(text, false);
            options.add(new AnswerOption(text, isCorrect));
        }
    }

    public List<AnswerOption> getAnswerOptions() {
        return options;
    }

    public boolean isCorrectSoFar(Set<Integer> clickedIds) {
        for (Integer clickedId : clickedIds) {
        	if (!options.get(clickedId).correct())
        		return false;
        }
        return true;
    }
    
    public boolean isFinallyCorrect(Set<Integer> clickedIds) {
    	Set<Integer> correctIndexes = new HashSet<>();
    	for (int i = 0; i < options.size(); i++)
    		if (options.get(i).correct())
    			correctIndexes.add(i);
    	return correctIndexes.equals(clickedIds);
    }
    
    public void reorderToMatch(List<String> targetTextOrder) {
        Map<String, AnswerOption> textToOption = new HashMap<>();
        for (AnswerOption opt : options) {
            textToOption.put(opt.text(), opt);
        }
        
        options.clear();
        for (String text : targetTextOrder) {
            options.add(textToOption.get(text));
        }
    }

    /** Mehr als eine richtige Antwort heißt: erst alle markieren, dann gesammelt absenden. */
    public boolean isCollectMode() {
        return getCorrectIndexes().size() > 1;
    }

    public Set<Integer> getCorrectIndexes() {
        Set<Integer> indexes = new HashSet<>();
        for (int i = 0; i < options.size(); i++) {
            if (options.get(i).correct()) {
                indexes.add(i);
            }
        }
        return indexes;
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