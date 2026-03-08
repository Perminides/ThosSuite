package app.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javafx.geometry.Point2D;

/**
 * Aktueller Ansatz: Jede Karte gibt es genau 1x im Speicher!
 */
public class AnkiCard {
	private static Integer debug = 0; 
    public sealed interface Step permits Image, ClickZone, ClickMapElements, Output, Input, MC, MarkZone, MarkMapElements, Pause {}
    
    public record AnswerOption(String text, boolean correct) {}
    public record Image(String file) implements Step {}
    public record ClickZone(String file, Point2D point) implements Step {}
    public record ClickMapElements(Set<String> mandatory, Set<String> optional) implements Step {}
    public record Output(String text) implements Step {}
    public record Input(List<String> parts) implements Step {}
    public record MC(Set<AnswerOption> options) implements Step {}
    public record MarkZone(String file, Point2D point) implements Step {}
    public record MarkMapElements(Set<String> left, Set<String> right) implements Step {} // Momentan ist right immer leer. Vielleicht will ich später aber auch mal die optionalen Shapes berücksichtigen...
    public record Pause() implements Step {}

    private final List<Step> steps;
	private final int id;
	private final String remark;
	private final Set<String> labels = new HashSet<>();
	
	private MC lastMCStep = null;
	private LearnStat learnStat;
	private transient AnkiCardProgress progress;

	public AnkiCard(List<String> csvTokens, LearnStat learnStat) {
		this(csvTokens);
		this.learnStat = learnStat;
	}
	
	public AnkiCard(List<String> csvTokens) {
		id = Integer.parseInt(csvTokens.get(0));
		remark = csvTokens.get(1);
		Arrays.stream(csvTokens.get(2).split("\\,")).map(String::trim).forEach(labels::add);
		
		List<Step> out = new ArrayList<>(); // Ergebnisliste
		List<List<Step>> segments = null; // Segmente zwischen ShuffleStart und ShuffleEnd
		List<Step> cur = null; // Aktuelles Segment

		for (String raw : csvTokens.subList(3, csvTokens.size())) {
			try {
				String s = raw.trim();

				boolean start = s.startsWith("<ShuffleStart>");
				if (start)
					s = s.substring("<ShuffleStart>".length());

				boolean brk = s.startsWith("<ShuffleBreak>");
				if (brk)
					s = s.substring("<ShuffleBreak>".length());

				boolean end = s.startsWith("<ShuffleEnd>");
				if (end)
					s = s.substring("<ShuffleEnd>".length());

				// Wir starten eine neue segments und cur
				if (start) {
					segments = new ArrayList<>();
					cur = new ArrayList<>();
				}

				// Wir starten ein neues cur in segments
				if (brk) {
					segments.add(cur);
					cur = new ArrayList<>();
				}

				// Wir fügen cur den segments hinzu, shufflen letztere und fügen sie out hinzu, Dann cur und segment zurück auf null
				if (end) {
					if (cur != null)
						segments.add(cur);
					Collections.shuffle(segments);
					for (var seg : segments)
						out.addAll(seg);
					segments = null;
					cur = null;
				}

				if (!s.isEmpty()) {
					Step step = parseStep(s);
					if (segments == null)
						out.add(step);
					else
						cur.add(step);
				}
			} catch (Exception e) {
				throw new RuntimeException("Problem beim parsen des Hints " + id + " in Step" + raw + " in " + csvTokens, e);
			}
		}
		steps = List.copyOf(out);
	}
	
	   // --- Parsing eines einzelnen Step-Strings (ohne Marker) ---
    private static Step parseStep(String s) {
        String[] p = s.split(":", 2);
        String kind = p[0];
        String body = p.length > 1 ? p[1] : "";

        return switch (kind) {
            case "Image" -> new Image(body);
            case "Output"  -> new Output(body);
            case "Input"  -> new Input(Arrays.asList(body.split("\\|")));
            case "MC"    -> new MC(parseMcAnswers(body));
            case "Click" -> parseClickOrMark(body, true);
            case "Mark"  -> parseClickOrMark(body, false);
            case "Pause" -> new Pause();
            default      -> throw new RuntimeException("Unbekannter Step: " + kind);
        };
    }

    // Multiple Choice
    private static Set<AnswerOption> parseMcAnswers(String mcStepString) {
        Set<AnswerOption> options = new HashSet<>();
        String[] split = mcStepString.split("\\*");
        
        // Teil 1: Die korrekten Antworten (vor dem Sternchen)
        Arrays.stream(split[0].split("\\|"))
            .map(String::trim) // Sicherheitshalber Leerzeichen entfernen
            .filter(s -> !s.isEmpty())
            .map(x -> new AnswerOption(x, true))
            .forEach(options::add);
        
        // Teil 2: Die falschen Antworten (nach dem Sternchen, falls vorhanden)
        if (split.length > 1) {
            Arrays.stream(split[1].split("\\|"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(x -> new AnswerOption(x, false))
                .forEach(options::add);
        }
        return options;
    }

	// --- Click/Mark
	private static Step parseClickOrMark(String body, boolean isClick) {
		String[] g = body.split("-");
		Set<String> left = Arrays.stream(g[0].split(",")).map(String::trim).collect(Collectors.toSet());
		Set<String> right = new HashSet<>();
		if (g.length > 1)
			right = Arrays.stream(g[1].split(",")).map(String::trim).collect(Collectors.toSet());
		return isClick ? new ClickMapElements(left, right) : new MarkMapElements(left, right);
	}
    
    public boolean isDueToday() {
    	return (learnStat != null && learnStat.getDueDate().isBefore(AppClock.TODAY.plusDays(1)));
    }
    
    public boolean isNew() {
    	return (learnStat == null);
    }
    
    public int getId() {
    	return id;
    }
    
    public LearnStat getLearnStat() {
    	return learnStat;
    }

	public void setLearnStat(LearnStat learnStat) {
		this.learnStat = learnStat;
	}

	public List<Step> getSteps() {
		return steps;
	}    
	
	public AnkiCardProgress getProgress() {
		return progress;
	}
	
	public void setProgress (AnkiCardProgress progress) {
		this.progress = progress;
	}
	
	public Set<String> getLabels() {
		return labels;
	}
}