package app.learn.anki.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import app.learn.model.LearnStat;
import app.shared.model.SketchColor;
import app.shared.AppClock;

/**
 * Aktueller Ansatz: Jede Karte gibt es genau 1x im Speicher!
 */
public class Card {
    public sealed interface Step permits Image, ClickMapElements, Output, Input, ChoiceStep, MarkMapElements, Pause, Fast,
            SketchImage, SketchImageAdd, SketchImageMark, SketchImageFill {}

    /** MC und MC+ teilen sich Optionen und Reihenfolge; der Typ trägt das Verhalten. */
    public sealed interface ChoiceStep extends Step permits MC, MCPlus {
        Set<AnswerOption> options();
        List<String> orderHint();
    }

    /** So viele Antwortfelder passen höchstens auf den Schirm — gilt für jeden Skin gleich. */
    public static final int MAX_FAST_SLOTS = 10;

    // Die unterschiedlichen Steps
    public record Image(String file) implements Step {}
    public record ClickMapElements(Set<String> mandatory, Set<String> optional) implements Step {}
    public record Output(String text) implements Step {}
    public record Input(List<String> parts) implements Step {}
    public record MC(Set<AnswerOption> options, List<String> orderHint) implements ChoiceStep {}
    public record MCPlus(Set<AnswerOption> options, List<String> orderHint) implements ChoiceStep {}
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
    public record SketchImageMark(List<Integer> areas) implements Step {}
    public record SketchImageFill(List<Integer> areas, SketchColor color) implements Step {}
    
    public record AnswerOption(String text, Role role) {}
    public enum Role {
        CORRECT, WRONG_ALWAYS_SHOWN, TOLERATED, TOLERATED_ALWAYS_SHOWN, DISTRACTOR_OPTIONAL;

        /** Falsch, aber ohne Abbruch — gleich ob gelost oder immer sichtbar. */
        public boolean tolerated() {
            return this == TOLERATED || this == TOLERATED_ALWAYS_SHOWN;
        }

        /** Steht in jedem Fall in der Auswahl und wird nie weggelost. */
        public boolean alwaysShown() {
            return this == CORRECT || this == WRONG_ALWAYS_SHOWN || this == TOLERATED_ALWAYS_SHOWN;
        }
    }
    public record Answer(String hint, List<String> variants) {} // Eine gesuchte Antwort: ihre Schreibvarianten und ein Hinweis, der bis zum Treffer im Feld steht.  

    /**
     * Ein Stück Karte: ein fester Schritt oder ein Block, dessen Segmente gegeneinander gewürfelt
     * werden. Die Karte hält ihre Stücke und nicht die fertige Folge — gewürfelt wird beim Abrufen,
     * einmal je Durchgang.
     */
    private sealed interface Chunk permits Fixed, Block {}
    private record Fixed(Step step) implements Chunk {}
    private record Block(List<List<Step>> segments) implements Chunk {}

    private final List<Chunk> chunks;
	private final List<Step> onFailSteps;
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
		
		List<Chunk> out = new ArrayList<>(); // Ergebnisliste: feste Schritte und Bloecke
		List<Step> onFail = new ArrayList<>(); // Alles ab <OnFail>
		List<List<Step>> segments = null; // Segmente zwischen ShuffleStart und ShuffleEnd
		List<Step> cur = null; // Aktuelles Segment

		boolean cardExpectsInput = false;
		boolean inOnFail = false;
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

				if (s.startsWith("<OnFail>")) {
					if (segments != null)
						throw new RuntimeException("<OnFail> mitten in einem Shuffle-Block");
					if (inOnFail)
						throw new RuntimeException("Zweimal <OnFail> in derselben Karte");
					inOnFail = true;
					s = s.substring("<OnFail>".length());
				}

				// Wir starten eine neue segments und cur
				if (start) {
					if (inOnFail)
						throw new RuntimeException("Shuffle im <OnFail>-Block");
					segments = new ArrayList<>();
					cur = new ArrayList<>();
				}

				// Wir starten ein neues cur in segments
				if (brk) {
					segments.add(cur);
					cur = new ArrayList<>();
				}

				// Wir fügen cur den segments hinzu und legen den Block ab. Dann cur und segments zurück auf null
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

					out.add(new Block(List.copyOf(segments)));
					segments = null;
					cur = null;
				}

				if (!s.isEmpty()) {
					Step step = parseStep(s);
					if (expectsInput(step)) {
						// Die Karte ist an dieser Stelle längst entschieden — eine Frage danach wäre keine.
						if (inOnFail)
							throw new RuntimeException("Der <OnFail>-Block fragt etwas: " + s);
						cardExpectsInput = true;
					}
					if (inOnFail)
						onFail.add(step);
					else if (segments == null)
						out.add(new Fixed(step));
					else
						cur.add(step);
				}
			} catch (Exception e) {
				throw new RuntimeException("Problem beim parsen der Ankikarte " + id + " in Step" + raw + " in " + csvTokens, e);
			}
		}
		// Ein <ShuffleStart> ohne <ShuffleEnd> verlöre sonst alles, was seitdem gesammelt wurde:
		// die Schritte liegen in segments/cur und werden nie an out angehängt. Die Karte käme dabei
		// sogar durch die Input-Prüfung, weil cardExpectsInput von genau diesen verlorenen Schritten
		// gesetzt worden sein kann — sie wäre also verstümmelt und trotzdem still.
		if (segments != null)
			throw new RuntimeException("<ShuffleStart> ohne <ShuffleEnd>\n" + String.join("\n", csvTokens));

		if (cardExpectsInput)
			chunks = List.copyOf(out);
		else
			throw new RuntimeException("Karte erwartet keinen Input\n" + String.join("\n", csvTokens));
		onFailSteps = List.copyOf(onFail);
	}

	/** Die Schritte, bei denen die Karte etwas von mir will. Alles andere zeigt nur an. */
	private static boolean expectsInput(Step step) {
		return step instanceof ChoiceStep || step instanceof Input || step instanceof ClickMapElements
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
            case "MC"    -> parseMc(body, false);
            case "MC+"   -> parseMc(body, true);
            case "Click" -> parseClickOrMark(body, true);
            case "Mark"  -> parseClickOrMark(body, false);
            case "Pause" -> new Pause();
            case "Fast"  -> parseFast(body);
            case "SketchImage"     -> new SketchImage(body.trim());
            case "SketchImageAdd"  -> parseSketchAdd(body);
            case "SketchImageMark" -> new SketchImageMark(parseAreas(body));
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

    /** {@code <fläche>,<farbname>} — ein unbekannter Farbname fliegt beim Einlesen des Decks. */
    private static Step parseSketchFill(String body) {
        String[] parts = body.split(",", 2);
        if (parts.length < 2)
            throw new RuntimeException("SketchImageFill braucht Fläche und Farbe: " + body);
        return new SketchImageFill(parseAreas(parts[0]), SketchColor.fromLabel(parts[1].trim()));
    }

    /**
     * Eine oder mehrere Flächennummern, mit {@code |} getrennt: {@code 3|4}.
     *
     * <p>Mehrere stehen für ein Element, das aus mehreren Flächen besteht — das Emblem etwa. Sie
     * werden gemeinsam hervorgehoben und gemeinsam gefüllt, weil die Farbe dem ganzen Element gilt
     * und nicht einem seiner Teile.</p>
     */
    private static List<Integer> parseAreas(String body) {
        List<Integer> areas = new ArrayList<>();
        for (String part : body.split("\\|"))
            areas.add(Integer.parseInt(part.trim()));
        if (areas.isEmpty())
            throw new RuntimeException("Kein Flächenwert: " + body);
        return List.copyOf(areas);
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
     * Zerlegt einen MC-Body. <b>Eine</b> Schreibweise: Optionen trennt {@code |}, ein führendes
     * {@code + - ~ ?} gibt die Rolle, nackt heißt {@code ?}. Die Rolle frisst nur ihr Zeichen, der
     * Rest ist Text ({@code +-40°} = richtig „-40°", {@code ++5} = richtig „+5").
     *
     * <p>Die eine Doppelrolle: {@code -~} oder {@code ~-} heißt toleriert und immer sichtbar. Sie
     * frisst beide Zeichen ({@code -~-40°} = „-40°"). Beginnt der Text einer <i>einfachen</i> Rolle
     * mit dem anderen Zeichen des Paars, braucht er deshalb ein Escape: {@code ~\-40°}.</p>
     *
     * <p>Ein führendes {@code \} vor dem Text wird weggeworfen und macht das nächste Zeichen
     * literal. Nötig ist das nur ohne Rolle ({@code \-40°} = Füller „-40°") und im Fall oben.</p>
     *
     * <p>Ein führendes {@code =} hält die geschriebene Reihenfolge als {@code orderHint} fest.</p>
     *
     * <p>Die alte Sternform {@code a|b*c|d} gibt es nicht mehr. Sie hat die Bedeutung einer Antwort
     * davon abhängig gemacht, ob eine <i>andere</i> Antwort derselben Zeile mit {@code +} beginnt —
     * ein führendes Minus war mal Text und mal Rolle. Ein Step, der noch so geschrieben ist, hat
     * jetzt keine richtige Antwort und bricht unten ab, statt still etwas anderes zu bedeuten.</p>
     */
    private static Step parseMc(String body, boolean plus) {
        String rest = body.trim();
        boolean ordered = rest.startsWith("=");
        if (ordered)
            rest = rest.substring(1);

        Set<AnswerOption> options = new LinkedHashSet<>();
        Set<String> seen = new HashSet<>();
        for (String part : rest.split("\\|", -1)) {
            String p = part.trim();
            if (hasDoubleRole(p))
                addOption(options, seen, unescape(p.substring(2)), Role.TOLERATED_ALWAYS_SHOWN);
            else if (hasRolePrefix(p))
                addOption(options, seen, unescape(p.substring(1)), roleOf(p));
            else
                addOption(options, seen, unescape(p), Role.DISTRACTOR_OPTIONAL);
        }

        boolean hasCorrect = false;
        for (AnswerOption option : options)
            if (option.role() == Role.CORRECT)
                hasCorrect = true;
        if (!hasCorrect)
            throw new RuntimeException("MC ohne richtige Antwort — fehlt ein '+'? — " + body);

        List<String> orderHint = new ArrayList<>();
        if (ordered)
            for (AnswerOption option : options)
                orderHint.add(option.text());

        return plus ? new MCPlus(options, orderHint) : new MC(options, orderHint);
    }

    /** Legt eine nicht-leere Option ab und bricht bei doppeltem Text ab. */
    private static void addOption(Set<AnswerOption> options, Set<String> seen, String text, Role role) {
        if (text.isEmpty())
            return;
        if (!seen.add(text))
            throw new RuntimeException("MC: Antwort '" + text + "' kommt doppelt vor");
        options.add(new AnswerOption(text, role));
    }

    /** Die eine Doppelrolle: {@code -~} oder {@code ~-} heißt toleriert und immer sichtbar. */
    private static boolean hasDoubleRole(String part) {
        return part.startsWith("-~") || part.startsWith("~-");
    }

    private static boolean hasRolePrefix(String part) {
        return !part.isEmpty() && "+-~?".indexOf(part.charAt(0)) >= 0;
    }

    /** Ein führendes {@code \} macht das nächste Zeichen literal — es wird beim Lesen weggeworfen. */
    private static String unescape(String text) {
        return text.startsWith("\\") ? text.substring(1) : text;
    }

    private static Role roleOf(String part) {
        if (!hasRolePrefix(part))
            return Role.DISTRACTOR_OPTIONAL;
        return switch (part.charAt(0)) {
            case '+' -> Role.CORRECT;
            case '-' -> Role.WRONG_ALWAYS_SHOWN;
            case '~' -> Role.TOLERATED;
            default  -> Role.DISTRACTOR_OPTIONAL; // '?'
        };
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

	/**
	 * Die Schritte eines Durchgangs. Shuffle-Blöcke werden dabei frisch gewürfelt
	 */
	public List<Step> getSteps() {
		List<Step> out = new ArrayList<>();
		for (Chunk chunk : chunks) {
			if (chunk instanceof Fixed fixed) {
				out.add(fixed.step());
				continue;
			}
			List<List<Step>> segments = new ArrayList<>(((Block) chunk).segments());
			Collections.shuffle(segments);
			for (List<Step> segment : segments)
				out.addAll(segment);
		}
		return List.copyOf(out);
	}

	/**
	 * Der Abspann hinter {@code <OnFail>} — läuft nur, wenn die Karte falsch beantwortet wurde, und
	 * ersetzt dann das sofortige Ende. Leer, wenn die Zeile keinen Block trägt.
	 *
	 * <p>Er zeigt nur an: eine Merkhilfe fürs nächste Mal, die Auflösung, das echte Bild. Eine Frage
	 * darf nicht darin stehen — an dieser Stelle ist die Karte längst entschieden.</p>
	 *
	 * <p>Eine eigene Liste und kein Index in {@link #getSteps()}: So kann kein Aufrufer versehentlich
	 * durchlaufen und den Abspann auch nach einem fehlerfreien Durchgang zeigen.</p>
	 */
	public List<Step> getOnFailSteps() {
		return onFailSteps;
	}
	
	public Set<String> getLabels() {
		return labels;
	}
}