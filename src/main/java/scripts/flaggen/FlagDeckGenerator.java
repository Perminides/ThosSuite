package scripts.flaggen;

import static scripts.flaggen.Laenderkarten.HAUPTSTADT;
import static scripts.flaggen.Laenderkarten.RANG_SPALTE;
import static scripts.flaggen.Laenderkarten.STAEDTE;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

import app.learn.anki.repository.CardParser;

/**
 * Erzeugt aus dem Systematik-Blatt die Zeilen des Flaggen-Decks.
 *
 * <p>Kein Sonderweg in der Suite: Was hier herauskommt, ist eine ganz normale Deck-Zeile, die der
 * bestehende Parser liest. Die Ableitung steckt in den Kartenbauern — {@link Flaggenkarte} für die
 * Flagge, {@link Laenderkarten} für alles, was am Land hängt. Hier steht nur der Ablauf: welche
 * Kartentypen es gibt, welche Zeile welche Karten bekommt, Prüfen, Schreiben.</p>
 *
 * <p>Aufruf: {@code java scripts.flaggen.FlagDeckGenerator [--offline] [--trocken]}. Erzeugt wird
 * jede Zeile des Blatts; die Deck-Datei entsteht dabei <b>vollständig neu</b> und
 * nach Id sortiert — was nicht aus dem Blatt kommt, überlebt den Lauf nicht. Handgeschriebene
 * Zusatzfragen gehören deshalb in die zweite Deck-Datei. Mit {@code --trocken} wird nur geprüft und
 * nichts geschrieben.</p>
 *
 * <p>Zu den Namen im Paket: Englisch heißt, was von außen aufgerufen wird — dieser Generator,
 * {@link FlagSheetCheck} und das {@link FlagSheet} darunter. Alles, was nur hier drinnen vorkommt,
 * heißt deutsch wie die Begriffe im Blatt: {@link Flaggenkarte}, {@link Zeile}, {@link Antworten}.</p>
 *
 * <p>Jede erzeugte Zeile läuft durch zwei Prüfungen: durch den echten {@link CardParser}, und
 * durch einen Flächenlauf ({@link Skizzen#check}), der mitzählt, wie viele Flächen die Skizze zu
 * jedem Zeitpunkt hat.</p>
 */
public class FlagDeckGenerator {

	static final Path DATA = Path.of(System.getProperty("user.home"),
			"Documents", "Gedächtnis Lernen und so", "ThosSuite", "data");

	/**
	 * Ein Kartentyp: sein Zahlenblock, sein Label, ob eine Zeile die Karte bekommt, und wie sie gebaut
	 * wird. Die Id einer Karte ist {@code block + Zeilen-Id} (siehe {@link Zeile#id}).
	 */
	private record Kartentyp(int block, String label, Predicate<Zeile> gilt, BiConsumer<Zeile, Kartenbauer> bau) {}

	/**
	 * Alle Kartentypen. Ein neuer Typ ist eine Zeile hier und eine Methode in seinem Kartenbauer.
	 *
	 * <p>Jeder Typ hat seinen eigenen Block von zehntausend (Regeln.md, „Nummernblöcke"). Die Blöcke
	 * sind getrennt, weil die handgeschriebenen Zusatzfragen ihre Nummern von Hand bekommen — der
	 * Generator kann nicht wissen, welche dort frei ist, und zwei Karten mit derselben Id teilten sich
	 * den Lernfortschritt. Die Landesfakten hängen nur an der Hauptzeile, also an der ersten Flagge.</p>
	 */
	private final List<Kartentyp> typen;

	private final FlagSheet sheet;
	private final Skizzen skizzen = new Skizzen();
	private final Laenderkarten laender;
	/** Flaggenbilder, die es noch nicht gibt. Gemeldet am Ende, nicht abgebrochen. */
	private final Set<String> missing = new LinkedHashSet<>();

	private FlagDeckGenerator(FlagSheet sheet) {
		this.sheet = sheet;
		this.laender = new Laenderkarten(sheet);
		Flaggenkarte flagge = new Flaggenkarte(skizzen);
		typen = List.of(
				new Kartentyp(0, "Flagge skizzieren", z -> true, flagge::bauen),
				new Kartentyp(10000, "Flagge auf Karte finden", z -> true, laender::kartenkarte),
				new Kartentyp(20000, "Nachbarländer", z -> z.hauptzeile() && laender.hatPflichtNachbarn(z),
						laender::nachbarkarte),
				new Kartentyp(30000, "Hauptstadt", z -> z.hat(HAUPTSTADT),
						Laenderkarten.feld(HAUPTSTADT, "<b>%s</b>: Wie heißt die <b>Hauptstadt</b>?", STAEDTE)),
				new Kartentyp(40000, "Größte Städte", z -> z.hat(RANG_SPALTE), laender::rangkarte));
		Set<Integer> bloecke = new HashSet<>();
		for (Kartentyp typ : typen)
			if (typ.block() % 10000 != 0 || typ.block() >= 1000000 || !bloecke.add(typ.block()))
				throw new RuntimeException("Block " + typ.block() + " von '" + typ.label()
						+ "' ist kein freier Zehntausender unter einer Million");
	}

	public static void main(String[] args) throws IOException {
		List<String> options = List.of(args);
		FlagSheet sheet = options.contains("--offline") ? FlagSheet.read() : FlagSheet.fetch();
		FlagDeckGenerator generator = new FlagDeckGenerator(sheet);

		// Sortiert nach Id — in dieser Reihenfolge entsteht die Deck-Datei.
		Map<Integer, String> generated = new TreeMap<>();
		for (List<String> row : sheet.flags()) {
			generator.generate(generated, new Zeile(sheet, row, generator.missing));
		}
		System.out.println(generated.size() + " Karten erzeugt und geprüft");
		generator.missing.forEach(file -> System.out.println("  ! Flaggenbild fehlt: " + file));
		generator.laender.hinweise().forEach(hinweis -> System.out.println("  ~ " + hinweis));

		if (options.contains("--trocken")) {
			generated.values().forEach(System.out::println);
			return;
		}
		DeckDatei.write(generated);
		DeckDatei.writeWordLists(generator.laender.wordLists());
	}

	/**
	 * Die Karten einer Zeile bauen, prüfen und einhängen. Jede Meldung bekommt hier ihr Land — ohne
	 * das steht da nur „für 'Löwe' fehlt der Artikel" und man sucht die Zeile in 214 anderen.
	 */
	private void generate(Map<Integer, String> generated, Zeile z) {
		try {
			for (Kartentyp typ : typen) {
				if (!typ.gilt().test(z))
					continue;
				int id = typ.block() + z.id();
				Kartenbauer k = new Kartenbauer(id, z.remark(), typ.label());
				typ.bau().accept(z, k);
				check(k.steps());
				if (generated.put(id, String.join(";", k.steps())) != null)
					throw new RuntimeException("die Id " + id + " gibt es zweimal");
			}
		} catch (RuntimeException e) {
			throw new RuntimeException(z.land() + " (Id " + z.id() + "): " + e.getMessage(), e);
		}
	}

	/** Der echte Parser und ein Flächenlauf — beide finden anderes. */
	private void check(List<String> steps) {
		try {
			CardParser.parse(steps, laender.wordLists());
		} catch (RuntimeException e) {
			throw new RuntimeException("die erzeugte Zeile ist nicht lesbar", e);
		}
		skizzen.check(steps);
	}
}
