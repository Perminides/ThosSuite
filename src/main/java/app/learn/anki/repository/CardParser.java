package app.learn.anki.repository;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import app.learn.anki.model.Card;
import app.learn.anki.model.Card.Answer;
import app.learn.anki.model.Card.AnswerOption;
import app.learn.anki.model.Card.Chunk;
import app.learn.anki.model.Card.ClickMapElements;
import app.learn.anki.model.Card.Collect;
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

	/** Ohne Wortlisten — ein Step mit Liste bricht dann ab. Für Aufrufer, die keine Listen laden. */
	public static Card parse(List<String> csvTokens) {
		return parse(csvTokens, Map.of());
	}

	/**
	 * Eine an {@code ;} zerlegte Zeile: id, Bemerkung (wird nicht gelesen), Labels, danach die Steps.
	 * Die Wortlisten (Name → Einträge mit ihren Schreibweisen) brauchen Collect und Input mit Liste.
	 */
	public static Card parse(List<String> csvTokens, Map<String, List<List<String>>> wordLists) {
		int id = Integer.parseInt(csvTokens.get(0));
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
					Step step = parseStep(s, wordLists);
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

		return new Card(id, labels, out, onFail);
	}

    // --- Parsing eines einzelnen Step-Strings (ohne Marker) ---
    private static Step parseStep(String s, Map<String, List<List<String>>> wordLists) {
        String[] p = s.split(":", 2);
        String kind = p[0];
        String body = p.length > 1 ? p[1] : "";

        return switch (kind) {
            case "Image" -> new Image(body);
            case "Output"  -> new Output(body);
            case "Input"  -> parseInput(body, wordLists);
            case "Collect" -> parseCollect(body, wordLists);
            case "MC"    -> parseMc(body, false);
            case "MC+"   -> parseMc(body, true);
            case "Click" -> parseClick(body);
            case "Mark"  -> parseMark(body);
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
        for (String raw : splitUnescaped(block, '|')) {
            String s = raw.trim();
            String hint = null;
            if (s.startsWith("<")) {
                int zu = s.indexOf('>');
                if (zu < 0)
                    throw new RuntimeException("Hinweis ohne schließendes >: " + raw);
                hint = unescape(s.substring(1, zu).trim());
                s = s.substring(zu + 1);
            }
            List<String> variants = parseVariants(s);
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
     * <p>Ein {@code \} macht das nächste Zeichen literal (siehe {@link #unescape}). Am Textanfang
     * nötig ist das nur ohne Rolle ({@code \-40°} = Füller „-40°") und im Fall oben.</p>
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
        for (String part : splitUnescaped(rest, '|')) {
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

    /** Ein {@code \} macht das nächste Zeichen literal und wird beim Lesen weggeworfen; {@code \\} ist ein Backslash. */
    private static String unescape(String text) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\\' && i + 1 < text.length())
                c = text.charAt(++i);
            out.append(c);
        }
        return out.toString();
    }

    /** Trennt an {@code sep}, nicht aber an einem mit {@code \} geschützten; der Backslash bleibt für {@link #unescape} stehen. */
    private static List<String> splitUnescaped(String text, char sep) {
        List<String> parts = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\\' && i + 1 < text.length()) {
                cur.append(c).append(text.charAt(++i));
            } else if (c == sep) {
                parts.add(cur.toString());
                cur.setLength(0);
            } else {
                cur.append(c);
            }
        }
        parts.add(cur.toString());
        return parts;
    }

    /** Schreibweisen derselben Antwort, mit {@code ,} getrennt; leere fallen weg. Auch für die Wortlisten. */
    static List<String> parseVariants(String text) {
        List<String> variants = new ArrayList<>();
        for (String variant : splitUnescaped(text, ','))
            if (!variant.trim().isEmpty())
                variants.add(unescape(variant.trim()));
        return List.copyOf(variants);
    }

    // --- Input ---

    /**
     * {@code [<liste>:]<antwort>|<antwort>|-<stoppwort>|~<toleriert>…}, jede mit Schreibweisen {@code a,b,c}.
     *
     * <p>Nackt heißt richtig, {@code -} Stoppwort, {@code ~} toleriert — kein Fehler, kein Treffer.
     * {@code + ?} am Anfang sind reserviert. Ein {@code :} ohne {@code \} trennt die Liste ab.</p>
     *
     * <p>Mit Liste ist jede Schreibweise der Liste ein Stoppwort, außer denen, die eine Antwort oder ein
     * toleriertes Wort selbst trägt — eine Nebenschreibweise, die zwei Einträge teilen, wird so nie zum
     * Stoppwort ihrer eigenen Karte. Ein Listen-Stoppwort, das der Anfang einer richtigen Schreibweise
     * ist, gilt nicht — sonst stürbe „Porto-Novo" schon bei „Porto". Von Hand gesetzte Stoppwörter
     * bleiben, wie sie dastehen.</p>
     *
     * <p>Antworten und Toleriertes müssen dann in der Liste stehen; ihre Schreibweisen kommen von dort
     * (siehe {@link #ausListe}).</p>
     */
    private static Step parseInput(String body, Map<String, List<List<String>>> wordLists) {
        List<String> head = splitUnescaped(body, ':');
        List<List<String>> list = null;
        String name = null;
        if (head.size() > 1) {
            name = unescape(head.get(0).trim());
            list = wordLists.get(name);
            if (list == null)
                throw new RuntimeException("Input: unbekannte Liste '" + name + "', bekannt sind " + wordLists.keySet()
                        + " — ein Doppelpunkt im Text braucht \\: " + body);
            body = body.substring(head.get(0).length() + 1);
        }

        List<List<String>> answers = new ArrayList<>();
        List<String> stopWords = new ArrayList<>();
        Set<String> known = new HashSet<>(); // Antworten und Toleriertes, klein geschrieben
        for (String raw : splitUnescaped(body, '|')) {
            String part = raw.trim();
            if (part.isEmpty())
                continue;
            char first = part.charAt(0);
            if ("+?".indexOf(first) >= 0)
                throw new RuntimeException("Input: '" + first + "' am Anfang ist reserviert, als Text mit \\ schützen: " + body);
            boolean stop = first == '-', tolerated = first == '~';
            List<String> variants = parseVariants(stop || tolerated ? part.substring(1) : part);
            if (variants.isEmpty())
                throw new RuntimeException("Input: Antwort ohne Text: " + body);
            if (list != null && !stop)
                variants = ausListe(variants, list, name, "Input", body);
            if (stop)
                stopWords.addAll(variants);
            else {
                if (!tolerated)
                    answers.add(variants);
                for (String v : variants)
                    known.add(v.toLowerCase());
            }
        }
        if (answers.isEmpty())
            throw new RuntimeException("Input ohne richtige Antwort: " + body);

        if (list != null)
            for (List<String> entry : list)
                for (String v : entry)
                    if (!known.contains(v.toLowerCase()) && !startsAnAnswer(v, answers))
                        stopWords.add(v);
        return new Input(List.copyOf(answers), List.copyOf(stopWords));
    }

    /** Ob {@code word} der Anfang (oder das Ganze) einer richtigen Schreibweise ist. */
    private static boolean startsAnAnswer(String word, List<List<String>> answers) {
        String w = word.toLowerCase();
        for (List<String> variants : answers)
            for (String v : variants)
                if (v.toLowerCase().startsWith(w))
                    return true;
        return false;
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

	// --- Collect

	/**
	 * {@code <liste>:<antwort>|<antwort>|~<toleriert>…}. Nackt heißt nötig, {@code ~} toleriert;
	 * {@code + - ?} sind reserviert. Jede Antwort muss in der Wortliste stehen, ihre Schreibweisen
	 * kommen von dort (siehe {@link #ausListe}); vorgeschlagen wird nur, was in der Liste steht.
	 *
	 * <p>Die Liste ist Pflicht — ohne sie gäbe es nichts zu wählen. Getrennt wird nur am ersten
	 * {@code :}, Antworten dürfen selbst einen tragen.</p>
	 */
	private static Step parseCollect(String body, Map<String, List<List<String>>> wordLists) {
		String[] p = body.split(":", 2);
		if (p.length < 2)
			throw new RuntimeException("Collect braucht eine Liste — Collect:<liste>:<antworten>: " + body);
		List<List<String>> list = wordLists.get(p[0].trim());
		if (list == null)
			throw new RuntimeException("Collect: unbekannte Liste '" + p[0].trim() + "', bekannt sind " + wordLists.keySet());
		List<String> suggestions = new ArrayList<>();
		for (List<String> entry : list)
			suggestions.add(entry.get(0)); // vorgeschlagen wird die erste Schreibweise
		body = p[1];

		List<List<String>> answers = new ArrayList<>();
		List<String> tolerated = new ArrayList<>();
		for (String raw : splitUnescaped(body, '|')) {
			String part = raw.trim();
			if (part.isEmpty())
				continue;
			boolean tilde = part.startsWith("~");
			if (!tilde)
				checkNoRole(part, "Collect", body);
			List<String> variants = parseVariants(tilde ? part.substring(1) : part);
			if (variants.isEmpty())
				throw new RuntimeException("Collect: Antwort ohne Text: " + body);
			variants = ausListe(variants, list, p[0].trim(), "Collect", body);
			if (tilde)
				tolerated.addAll(variants);
			else
				answers.add(variants);
		}
		if (answers.isEmpty())
			throw new RuntimeException("Collect ohne Antwort: " + body);
		return new Collect(List.copyOf(suggestions), List.copyOf(answers), List.copyOf(tolerated));
	}

	/**
	 * Der Listeneintrag einer Antwort. Gefunden wird er über seine <b>Hauptform</b>, die erste
	 * Schreibweise — sie ist der Schlüssel, Nebenschreibweisen dürfen mehrere Einträge teilen
	 * ({@code Tripolis} bei Libyen und beim Libanon). Seine Schreibweisen ersetzen die der Karte, die
	 * Hauptform steht beim Aufdecken.
	 *
	 * <p>Wer eine Liste nennt, schreibt nur Werte aus ihr: Ist die erste Schreibweise der Karte keine
	 * Hauptform der Liste, oder trägt der Eintrag eine weitere Schreibweise der Karte nicht, bricht das
	 * Laden ab — sonst entstünde still eine Antwort, die niemand tippen kann.</p>
	 */
	private static List<String> ausListe(List<String> variants, List<List<String>> list, String name,
			String step, String body) {
		List<String> entry = null;
		for (List<String> candidate : list)
			if (candidate.get(0).equalsIgnoreCase(variants.get(0)))
				entry = candidate;
		if (entry == null)
			throw new RuntimeException(step + ": '" + variants.get(0) + "' ist keine Hauptform der Liste '"
					+ name + "': " + body);
		for (String variant : variants) {
			boolean drin = false;
			for (String spelling : entry)
				if (spelling.equalsIgnoreCase(variant))
					drin = true;
			if (!drin)
				throw new RuntimeException(step + ": '" + variant + "' gehört nicht zum Eintrag '"
						+ String.join(",", entry) + "' der Liste '" + name + "': " + body);
		}
		return entry;
	}

	/** Ein Rollenzeichen, das der Step nicht kennt, ist reserviert — als Text mit {@code \} schützen. */
	private static void checkNoRole(String part, String step, String body) {
		if ("+-~?".indexOf(part.charAt(0)) >= 0)
			throw new RuntimeException(step + ": '" + part.charAt(0) + "' am Anfang ist reserviert, als Text mit \\ schützen: " + body);
	}

	// --- Click/Mark

	/** {@code <pflicht>|<pflicht>|~<toleriert>…} — eine tolerierte Form darf geklickt werden, muss aber nicht. */
	private static Step parseClick(String body) {
		Set<String> mandatory = new HashSet<>();
		Set<String> tolerated = new HashSet<>();
		for (String part : shapeParts(body)) {
			if (part.startsWith("~")) {
				tolerated.add(unescape(part.substring(1).trim()));
			} else {
				checkNoRole(part, "Click", body);
				mandatory.add(unescape(part));
			}
		}
		return new ClickMapElements(mandatory, tolerated);
	}

	private static Step parseMark(String body) {
		Set<String> shapes = new HashSet<>();
		for (String part : shapeParts(body)) {
			checkNoRole(part, "Mark", body);
			shapes.add(unescape(part));
		}
		return new MarkMapElements(shapes);
	}

	/**
	 * Formen, mit {@code |} getrennt, noch mit Rolle und Escape. Ein Kreis ist eine Form:
	 * {@code small:120:340}. Komma und {@code >} sind alte Schreibweisen und brechen ab, statt still
	 * eine Form namens „a,b" zu suchen.
	 */
	private static List<String> shapeParts(String text) {
		if (splitUnescaped(text, ',').size() > 1)
			throw new RuntimeException("Formen trennt '|', nicht ',': " + text);
		if (splitUnescaped(text, '>').size() > 1)
			throw new RuntimeException("'>' gibt es nicht mehr — tolerierte Formen tragen '~': " + text);
		List<String> parts = new ArrayList<>();
		for (String part : splitUnescaped(text, '|'))
			if (!part.trim().isEmpty())
				parts.add(part.trim());
		return parts;
	}
}
