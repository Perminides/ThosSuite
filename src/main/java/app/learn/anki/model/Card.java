package app.learn.anki.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import app.learn.model.LearnStat;
import app.shared.model.SketchColor;
import app.shared.AppClock;

/**
 * Aktueller Ansatz: Jede Karte gibt es genau 1x im Speicher!
 */
public class Card {
    public sealed interface Step permits Image, ClickMapElements, Output, Input, MC, MarkMapElements, Pause, Fast,
            SketchImage, SketchImageAdd, SketchImageMove, SketchImageMark, SketchImageFill {}

    /** So viele Antwortfelder passen höchstens auf den Schirm — gilt für jeden Skin gleich. */
    public static final int MAX_FAST_SLOTS = 10;

    // Die unterschiedlichen Steps
    public record Image(String file) implements Step {}
    public record ClickMapElements(Set<String> mandatory, Set<String> optional) implements Step {}
    public record Output(String text) implements Step {}
    public record Input(List<String> parts) implements Step {}
    public record MC(Set<AnswerOption> options) implements Step {}
    public record MarkMapElements(Set<String> left, Set<String> right) implements Step {} // Momentan ist right immer leer. Vielleicht will ich später aber auch mal die optionalen Shapes berücksichtigen...
    public record Pause() implements Step {}
    public record Fast(int seconds, boolean ordered, int slots, List<Answer> answers) implements Step {}

    /**
     * Die drei Schritte einer Skizze: laden, eine Fläche hervorheben, eine Fläche färben.
     *
     * <p>Sie zeigen nur an und fragen nichts — gefragt wird daneben per MC. Getrennt, damit der
     * Ablauf vollständig in der Zeile steht und keiner der Schritte etwas über seine Nachbarn
     * wissen muss. {@code SketchImage} ist zugleich das Zurücksetzen: eine neu geladene Struktur
     * ist wieder leer.</p>
     */
    public record SketchImage(String structure) implements Step {}
    public record SketchImageAdd(String structure, int cell, double size, double offsetX,
            double offsetY) implements Step {}
    public record SketchImageMove(int area, int cell) implements Step {}
    public record SketchImageMark(int area) implements Step {}
    public record SketchImageFill(int area, SketchColor color) implements Step {}
    
    public record AnswerOption(String text, boolean correct) {}
    public record Answer(String hint, List<String> variants) {} // Eine gesuchte Antwort: ihre Schreibvarianten und ein Hinweis, der bis zum Treffer im Feld steht.  

    private final List<Step> steps;
	private final int id;
	private final String remark;
	private final Set<String> labels = new HashSet<>();
	
	private LearnStat learnStat;

	public Card(List<String> csvTokens, LearnStat learnStat) {
		this(csvTokens);
		this.learnStat = learnStat;
	}
	
	public Card(List<String> csvTokens) {
		id = Integer.parseInt(csvTokens.get(0));
		remark = csvTokens.get(1);
		labels.addAll(splitAndTrim(csvTokens.get(2)));
		
		List<Step> out = new ArrayList<>(); // Ergebnisliste
		List<List<Step>> segments = null; // Segmente zwischen ShuffleStart und ShuffleEnd
		List<Step> cur = null; // Aktuelles Segment

		boolean cardExpectsInput = false;
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

					// Jedes Segment muss selbst nach Input fragen. Gewürfelt wird die Reihenfolge der
					// Segmente — eines ohne Input rutscht deshalb an JEDER Position unbemerkt durch,
					// und je nach Wurf mal hier, mal dort. Die Prüfung auf Kartenebene reicht nicht:
					// die ist schon zufrieden, wenn irgendein anderes Segment etwas fragt.
					for (int i = 0; i < segments.size(); i++)
						if (!expectsInput(segments.get(i)))
							throw new RuntimeException("Shuffle-Segment " + (i + 1) + " von " + segments.size()
									+ " erwartet keinen Input");

					Collections.shuffle(segments);
					for (var seg : segments)
						out.addAll(seg);
					segments = null;
					cur = null;
				}

				if (!s.isEmpty()) {
					Step step = parseStep(s);
					if (expectsInput(step))
						cardExpectsInput = true;
					if (segments == null)
						out.add(step);
					else
						cur.add(step);
				}
			} catch (Exception e) {
				throw new RuntimeException("Problem beim parsen des Hints " + id + " in Step" + raw + " in " + csvTokens, e);
			}
		}
		// Ein <ShuffleStart> ohne <ShuffleEnd> verlöre sonst alles, was seitdem gesammelt wurde:
		// die Schritte liegen in segments/cur und werden nie an out angehängt. Die Karte käme dabei
		// sogar durch die Input-Prüfung, weil cardExpectsInput von genau diesen verlorenen Schritten
		// gesetzt worden sein kann — sie wäre also verstümmelt und trotzdem still.
		if (segments != null)
			throw new RuntimeException("<ShuffleStart> ohne <ShuffleEnd>\n" + String.join("\n", csvTokens));

		if (cardExpectsInput)
			steps = List.copyOf(out);
		else
			throw new RuntimeException("Karte erwartet keinen Input\n" + String.join("\n", csvTokens));
	}

	/** Die Schritte, bei denen die Karte etwas von mir will. Alles andere zeigt nur an. */
	private static boolean expectsInput(Step step) {
		return step instanceof MC || step instanceof Input || step instanceof ClickMapElements
				|| step instanceof Fast;
	}

	/** Ob in dieser Folge überhaupt irgendwo nach Input gefragt wird. */
	private static boolean expectsInput(List<Step> steps) {
		for (Step step : steps)
			if (expectsInput(step))
				return true;
		return false;
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
            case "Fast"  -> parseFast(body);
            case "SketchImage"     -> new SketchImage(body.trim());
            case "SketchImageAdd"  -> parseSketchAdd(body);
            case "SketchImageMove" -> parseSketchMove(body);
            case "SketchImageMark" -> new SketchImageMark(Integer.parseInt(body.trim()));
            case "SketchImageFill" -> parseSketchFill(body);
            default      -> throw new RuntimeException("Unbekannter Step: " + kind);
        };
    }

    /**
     * {@code <struktur>,<rasterfeld 0..8>[,<groesse>[,<dx>,<dy>]]} — haengt an, ohne die bisherigen
     * Fuellungen zu verlieren.
     *
     * <p>Groesse 1,0 heisst „fuellt ein Rasterfeld"; groesser ist erlaubt. Der Versatz zieht
     * Geschwister im selben Feld auseinander und rechnet in den Koordinaten der Elementdatei.
     * Beides ist im Generator ausgerechnet — die Karte fuehrt nur aus.</p>
     */
    private static Step parseSketchAdd(String body) {
        String[] parts = body.split(",");
        if (parts.length != 2 && parts.length != 3 && parts.length != 5)
            throw new RuntimeException("SketchImageAdd braucht Struktur und Rasterfeld, dazu"
                    + " wahlweise die Groesse oder Groesse und Versatz: " + body);
        double size = parts.length > 2 ? Double.parseDouble(parts[2].trim()) : 1;
        double dx = parts.length > 4 ? Double.parseDouble(parts[3].trim()) : 0;
        double dy = parts.length > 4 ? Double.parseDouble(parts[4].trim()) : 0;
        return new SketchImageAdd(parts[0].trim(), Integer.parseInt(parts[1].trim()), size, dx, dy);
    }

    /** {@code <fläche>,<rasterfeld 0..8>} — setzt eine vorhandene Flaeche um, ohne sie neu zu bauen. */
    private static Step parseSketchMove(String body) {
        String[] parts = body.split(",", 2);
        if (parts.length < 2)
            throw new RuntimeException("SketchImageMove braucht Fläche und Rasterfeld: " + body);
        return new SketchImageMove(Integer.parseInt(parts[0].trim()), Integer.parseInt(parts[1].trim()));
    }

    /** {@code <fläche>,<farbname>} — ein unbekannter Farbname fliegt beim Einlesen des Decks. */
    private static Step parseSketchFill(String body) {
        String[] parts = body.split(",", 2);
        if (parts.length < 2)
            throw new RuntimeException("SketchImageFill braucht Fläche und Farbe: " + body);
        return new SketchImageFill(Integer.parseInt(parts[0].trim()), SketchColor.fromLabel(parts[1].trim()));
    }

    // --- Fast ---

    /**
     * {@code <sekunden>:<ordered|any|anyN>:<antwort>|<antwort>|…}
     *
     * <p>Vor den Antworten stehen immer genau zwei Pflichtfelder. Nur deshalb darf der Antwortblock
     * selbst Doppelpunkte tragen ("Blade Runner: 2049") — das Limit trennt sauber ab.</p>
     */
    private static Step parseFast(String body) {
        String[] parts = body.split(":", 3);
        if (parts.length < 3)
            throw new RuntimeException("Fast braucht Sekunden, Modus und Antworten: " + body);

        int seconds = Integer.parseInt(parts[0].trim());
        if (seconds < 1)
            throw new RuntimeException("Fast braucht eine Zeit größer null: " + body);

        String mode = parts[1].trim();
        List<Answer> answers = parseFastAnswers(parts[2]);
        boolean ordered = mode.equals("ordered");
        int slots = answers.size();

        if (!ordered && !mode.equals("any")) {
            if (!mode.startsWith("any"))
                throw new RuntimeException("Unbekannter Fast-Modus: " + mode);
            slots = Integer.parseInt(mode.substring(3));
            if (slots < 1 || slots > answers.size())
                throw new RuntimeException("anyN braucht 1 bis " + answers.size() + " Felder, nicht " + slots);
            if (hasHints(answers))
                throw new RuntimeException("anyN und Hinweise gehen nicht zusammen: ohne Bindung gibt es kein Feld für den Hinweis");
        }

        if (slots > MAX_FAST_SLOTS)
            throw new RuntimeException(slots + " Antwortfelder, erlaubt sind " + MAX_FAST_SLOTS);
        if (!ordered)
            checkNoDuplicates(answers);

        return new Fast(seconds, ordered, slots, answers);
    }

    private static List<Answer> parseFastAnswers(String block) {
        List<Answer> answers = new ArrayList<>();
        for (String raw : block.split("\\|")) {
            String s = raw.trim();
            String hint = null;
            if (s.startsWith("<")) {
                int zu = s.indexOf('>');
                if (zu < 0)
                    throw new RuntimeException("Hinweis ohne schließendes >: " + raw);
                hint = s.substring(1, zu).trim();
                s = s.substring(zu + 1);
            }
            List<String> variants = new ArrayList<>();
            for (String variant : s.split(","))
                if (!variant.trim().isEmpty())
                    variants.add(variant.trim());
            if (variants.isEmpty())
                throw new RuntimeException("Antwort ohne Text: " + raw);
            answers.add(new Answer(hint, List.copyOf(variants)));
        }
        if (answers.isEmpty())
            throw new RuntimeException("Fast ohne Antworten: " + block);
        return List.copyOf(answers);
    }

    private static boolean hasHints(List<Answer> answers) {
        for (Answer answer : answers)
            if (answer.hint() != null)
                return true;
        return false;
    }

    /** Ohne Reihenfolge wäre bei einer doppelten Variante nicht entscheidbar, welches Feld gemeint ist. */
    private static void checkNoDuplicates(List<Answer> answers) {
        Set<String> seen = new HashSet<>();
        for (Answer answer : answers)
            for (String variant : answer.variants())
                if (!seen.add(variant.toLowerCase()))
                    throw new RuntimeException("Antwort '" + variant + "' kommt mehrfach vor — nur bei ordered erlaubt");
    }

    /**
     * Zerlegt einen mit {@code |} getrennten Abschnitt und legt jeden nicht-leeren Teil als Antwort
     * der gegebenen Sorte ab.
     */
    private static void collectAnswers(String abschnitt, boolean correct, Set<AnswerOption> options) {
        for (String text : abschnitt.split("\\|")) {
            String trimmed = text.trim(); // Sicherheitshalber Leerzeichen entfernen
            if (!trimmed.isEmpty())
                options.add(new AnswerOption(trimmed, correct));
        }
    }

    // Multiple Choice
    private static Set<AnswerOption> parseMcAnswers(String mcStepString) {
        Set<AnswerOption> options = new HashSet<>();
        String[] split = mcStepString.split("\\*");
        
        // Teil 1: Die korrekten Antworten (vor dem Sternchen)
        collectAnswers(split[0], true, options);

        // Teil 2: Die falschen Antworten (nach dem Sternchen, falls vorhanden)
        if (split.length > 1)
            collectAnswers(split[1], false, options);

        return options;
    }

	private static Set<String> splitAndTrim(String commaSeparated) {
		Set<String> result = new HashSet<>();
		for (String part : commaSeparated.split(","))
			result.add(part.trim());
		return result;
	}

	// --- Click/Mark
	private static Step parseClickOrMark(String body, boolean isClick) {
		String[] g = body.split("-");
		Set<String> left = splitAndTrim(g[0]);
		Set<String> right = g.length > 1 ? splitAndTrim(g[1]) : new HashSet<>();
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
	
	public Set<String> getLabels() {
		return labels;
	}
}