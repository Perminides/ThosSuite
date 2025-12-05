package app.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class MultipleChoiceAnswers {

    public record AnswerOption(String text, boolean correct) {}

    private final List<AnswerOption> options = new ArrayList<>();

    public MultipleChoiceAnswers(String mcStepString) {
        
        String[] split = mcStepString.split("\\*");
        
        Arrays.stream(split[0].split("\\|"))
            .map(x -> new AnswerOption(x, true))
            .forEach(options::add);
        
        if (split.length > 1)
        	Arrays.stream(split[1].split("\\|"))
            	.map(x -> new AnswerOption(x, false))
            	.forEach(options::add);
        
        Collections.shuffle(options);
    }

    public List<AnswerOption> getAnswerOptions() {
        return options;
    }

    public boolean isCorrectSoFar(Set<Integer> clickedIds) {
        for (Integer clickedId : clickedIds) {
        	if (!options.get(clickedId).correct)
        		return false;
        }
        return true;
    }
    
    public boolean isFinallyCorrect(Set<Integer> clickedIds) {
    	Set<Integer> correctIndexes = IntStream.range(0, options.size())
    		    .filter(i -> options.get(i).correct())
    		    .boxed()
    		    .collect(Collectors.toSet());
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
    	return options.stream().map(ao -> ao.text()).toString();
    }
}