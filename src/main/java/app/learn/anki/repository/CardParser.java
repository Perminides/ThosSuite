package app.learn.anki.repository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import app.learn.anki.model.Card;
import app.learn.anki.model.Card.Answer;
import app.learn.anki.model.Card.AnswerOption;
import app.learn.anki.model.Card.Chunk;
import app.learn.anki.model.Card.ClickMapElements;
import app.learn.anki.model.Card.Fast;
import app.learn.anki.model.Card.FixedStep;
import app.learn.anki.model.Card.Image;
import app.learn.anki.model.Card.Input;
import app.learn.anki.model.Card.MC;
import app.learn.anki.model.Card.MCPlus;
import app.learn.anki.model.Card.MarkMapElements;
import app.learn.anki.model.Card.Output;
import app.learn.anki.model.Card.Pause;
import app.learn.anki.model.Card.Role;
import app.learn.anki.model.Card.ShuffleBlock;
import app.learn.anki.model.Card.SketchImage;
import app.learn.anki.model.Card.SketchImageAdd;
import app.learn.anki.model.Card.SketchImageFill;
import app.learn.anki.model.Card.SketchImageMark;
import app.learn.anki.model.Card.Step;
import app.shared.model.SketchColor;

/**
 * Die Grammatik der Deck-CSVs: aus einer zerlegten Zeile wird eine Karte. Zuständig für beide
 * Schreibweisen der Datei — die Marker {@code <ShuffleStart>}, {@code <ShuffleBreak>},
 * {@code <ShuffleEnd>}, {@code <OnFail>}, die das Gerüst aufspannen, und die Step-Sprache
 * dahinter ({@code Output:}, {@code MC+:}, {@code Fast:} …).
 *
 * <p>Was hier fliegt, sind <b>Lesbarkeits</b>-Fehler: ein unbekannter Step, ein Marker an
 * unmöglicher Stelle, eine Zahl, die keine ist. Ob die fertige Karte <i>Sinn</i> ergibt — ob sie
 * überhaupt etwas fragt, ob der Abspann stumm bleibt —, prüft {@link Card} an seiner Struktur.</p>
 *
 * <p>Maßgeblich für {@code Deck-Syntax.md}: ändert sich {@link #parseStep}, ändert sich das
 * Dokument.</p>
 */
public class CardParser {

	/**
	 * Eine an {@code ;} zerlegte Zeile: id, remark, Labels, danach die Steps.
	 */
	public static Card parse(List<String> csvTokens) {
		int id = Integer.parseInt(csvTokens.get(0));
		String remark = csvTokens.get(1);
		Set<String> labels = splitAndTrim(csvTokens.get(2));

		List<Chunk> out = new ArrayList<>(); // Ergebnisliste: feste Schritte und Bloecke
		List<Step> onFail = new ArrayList<>(); // Alles ab <OnFail>
		List<List<Step>> segments = null; // Segmente zwischen ShuffleStart und ShuffleEnd
		List<Step> cur = null; // Aktuelles Segment

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
					out.add(new ShuffleBlock(List.copyOf(segments)));
					segments = null;
					cur = null;
				}

				if (!s.isEmpty()) {
					Step step = parseStep(s);
					if (inOnFail)
						onFail.add(step);
					else if (segments == null)
						out.add(new FixedStep(step));
					else
						cur.add(step);
				}
			} catch (Exception e) {
				throw new RuntimeException("Problem beim parsen des Hints " + id + " in Step" + raw + " in " + csvTokens, e);
			}
		}
		// Ein <ShuffleStart> ohne <ShuffleEnd> verlöre sonst alles, was seitdem gesammelt wurde:
		// die Schritte liegen in segments/cur und werden nie an out angehängt.
		if (segments != null)
			throw new RuntimeException("<ShuffleStart> ohne <ShuffleEnd>\n" + String.join("\n", csvTokens));

		return new Card(id, remark, labels, out, onFail);
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

        if (slots > Card.MAX_FAST_SLOTS)
            throw new RuntimeException(slots + " Antwortfelder, erlaubt sind " + Card.MAX_FAST_SLOTS);
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
	/** Getrennt wird an {@code >}, nicht am Bindestrich: Der steckt in Namen wie Theodor-Heuss-Platz. */
	private static Step parseClickOrMark(String body, boolean isClick) {
		String[] g = body.split(">");
		Set<String> left = splitAndTrim(g[0]);
		Set<String> right = g.length > 1 ? splitAndTrim(g[1]) : new HashSet<>();
		return isClick ? new ClickMapElements(left, right) : new MarkMapElements(left, right);
	}
}
