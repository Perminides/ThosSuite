package app.scripts;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Stream;

/**
 * Erzeugt einen Package-Abhängigkeitsgraphen der ThosSuite als DOT-Datei (Graphviz).
 *
 * Liest alle .java-Dateien unter SOURCE_ROOT, ermittelt pro Datei das eigene Package
 * und die importierten app.*-Packages, und schreibt die aggregierten Package→Package-Kanten
 * als DOT. Externe Imports (javafx.*, java.*, Jackson, ...) werden ignoriert.
 *
 * Granularität: FEIN. Jedes Package ist ein eigener Knoten (app.data.persistence.anki
 * ist NICHT dasselbe wie app.data.persistence).
 *
 * Selbst-Kanten (Datei importiert etwas aus dem eigenen Package) werden weggelassen.
 *
 * FailFast: keine gefangenen Exceptions, die etwas verschlucken. IO-Fehler fliegen hoch.
 *
 * Lokal ausführen, danach die erzeugte .dot mit Graphviz rendern:
 *   dot -Tsvg thossuite_packages.dot -o thossuite_packages.svg
 * oder online (z. B. dreampuf.github.io/GraphvizOnline) reinkopieren.
 */
public class PackageDependencyGraph {

	// ===== Konfiguration — bitte ausfüllen =====

	/** Wurzel des Quellbaums, z. B. ".../src/main/java" oder ".../src". */
	private static final String SOURCE_ROOT = "C:/Users/permi/git/ThosSuite/src/main/java/app";

	/** Zieldatei für den DOT-Output. */
	private static final String OUTPUT_DOT = "C:/Users/permi/Desktop/thossuite_packages.dot";

	/** Nur Packages mit diesem Präfix gelten als "intern" (Kanten dorthin werden behalten). */
	private static final String INTERNAL_PREFIX = "app";

	/**
	 * Package-Präfixe, die komplett ausgeschlossen werden — sowohl als Quelle als auch als Ziel.
	 * misc gehört hier rein: es ist Wegwerf-Code (inkl. dieses Tools selbst) und soll den
	 * Architekturgraphen nicht verrauschen.
	 */
	private static final List<String> EXCLUDED_PREFIXES = List.of(
			"app.scripts"
	);

	// ===== Ende Konfiguration =====

	public static void main(String[] args) {
		Path root = Path.of(SOURCE_ROOT);
		if (!Files.isDirectory(root))
			throw new RuntimeException("SOURCE_ROOT ist kein Verzeichnis: " + root.toAbsolutePath());

		// Aggregierte Kanten: Quell-Package -> Menge der Ziel-Packages
		Map<String, Set<String>> edges = new TreeMap<>();
		// Alle gesehenen internen Packages (auch solche ohne ausgehende Kanten) als Knoten
		Set<String> nodes = new TreeSet<>();

		List<Path> javaFiles = collectJavaFiles(root);
		if (javaFiles.isEmpty())
			throw new RuntimeException("Keine .java-Dateien gefunden unter " + root.toAbsolutePath());

		for (Path file : javaFiles) {
			FileInfo info = parseFile(file);
			if (info == null)
				continue; // Datei ohne package-Deklaration (z. B. package-info? hier ignoriert)

			if (isExcluded(info.packageName))
				continue;

			nodes.add(info.packageName);

			for (String imported : info.importedPackages) {
				if (!imported.startsWith(INTERNAL_PREFIX + "."))
					continue; // extern -> raus
				if (isExcluded(imported))
					continue;
				if (imported.equals(info.packageName))
					continue; // Selbst-Kante -> raus

				nodes.add(imported);
				edges.computeIfAbsent(info.packageName, _ -> new TreeSet<>()).add(imported);
			}
		}

		String dot = renderDot(nodes, edges);
		writeOutput(dot);

		int edgeCount = edges.values().stream().mapToInt(Set::size).sum();
		System.out.println("Fertig. " + nodes.size() + " Packages, " + edgeCount + " Kanten.");
		System.out.println("Geschrieben: " + Path.of(OUTPUT_DOT).toAbsolutePath());
	}

	// ----------------------------------------------------------------

	private static List<Path> collectJavaFiles(Path root) {
		try (Stream<Path> stream = Files.walk(root)) {
			return stream
					.filter(Files::isRegularFile)
					.filter(p -> p.getFileName().toString().endsWith(".java"))
					.filter(p -> !p.getFileName().toString().equals("package-info.java"))
					.toList();
		} catch (IOException e) {
			throw new UncheckedIOException("Konnte Quellbaum nicht durchlaufen: " + root, e);
		}
	}

	/** Hält das Ergebnis des Parsens einer Datei. */
	private record FileInfo(String packageName, Set<String> importedPackages) {}

	/**
	 * Liest package-Deklaration und alle app.*-Import-Packages aus einer Datei.
	 * Behandelt:
	 *  - mehrzeilige Blockkommentare (damit "import" in Kommentaren nicht zählt)
	 *  - "import static app.Foo.bar" (das Package ist app, ohne Foo und bar)
	 * Gibt null zurück, wenn keine package-Zeile gefunden wurde.
	 */
	private static FileInfo parseFile(Path file) {
		List<String> lines;
		try {
			lines = Files.readAllLines(file);
		} catch (IOException e) {
			throw new UncheckedIOException("Konnte Datei nicht lesen: " + file, e);
		}

		String packageName = null;
		Set<String> imported = new TreeSet<>();
		boolean inBlockComment = false;

		for (String raw : lines) {
			String line = stripComments(raw, inBlockComment);
			// stripComments liefert den Code-Anteil; den Kommentarzustand danach ermitteln wir separat
			inBlockComment = updateBlockCommentState(raw, inBlockComment);

			String trimmed = line.trim();
			if (trimmed.isEmpty())
				continue;

			if (packageName == null && trimmed.startsWith("package ")) {
				packageName = extractPackage(trimmed);
				continue;
			}

			if (trimmed.startsWith("import ")) {
				String pkg = extractImportPackage(trimmed);
				if (pkg != null)
					imported.add(pkg);
			}

			// Sobald die erste Typdeklaration kommt, sind package/imports durch.
			// (Heuristik: class/interface/enum/record am Zeilenanfang ohne führendes "import")
			if (startsTypeDeclaration(trimmed))
				break;
		}

		if (packageName == null)
			return null;
		return new FileInfo(packageName, imported);
	}

	/**
	 * Entfernt Zeilenkommentare und den Inhalt von Blockkommentaren aus EINER Zeile,
	 * gegeben den Kommentarzustand zu Zeilenbeginn. Gibt nur den "Code"-Anteil zurück.
	 * Vereinfachte Behandlung — reicht, weil wir nur package/import-Zeilen auswerten,
	 * die nie Strings mit "//" oder "/*" enthalten.
	 */
	private static String stripComments(String line, boolean inBlockCommentAtStart) {
		StringBuilder out = new StringBuilder();
		boolean inBlock = inBlockCommentAtStart;
		int i = 0;
		while (i < line.length()) {
			if (inBlock) {
				int end = line.indexOf("*/", i);
				if (end < 0) {
					i = line.length(); // Rest ist Kommentar
				} else {
					i = end + 2;
					inBlock = false;
				}
			} else {
				if (i + 1 < line.length() && line.charAt(i) == '/' && line.charAt(i + 1) == '/') {
					break; // Rest der Zeile ist Zeilenkommentar
				} else if (i + 1 < line.length() && line.charAt(i) == '/' && line.charAt(i + 1) == '*') {
					inBlock = true;
					i += 2;
				} else {
					out.append(line.charAt(i));
					i++;
				}
			}
		}
		return out.toString();
	}

	/** Ermittelt den Blockkommentar-Zustand NACH dieser Zeile. */
	private static boolean updateBlockCommentState(String line, boolean inBlockAtStart) {
		boolean inBlock = inBlockAtStart;
		int i = 0;
		while (i < line.length()) {
			if (inBlock) {
				int end = line.indexOf("*/", i);
				if (end < 0)
					return true;
				i = end + 2;
				inBlock = false;
			} else {
				int lineComment = line.indexOf("//", i);
				int blockStart = line.indexOf("/*", i);
				if (blockStart < 0)
					return false;
				if (lineComment >= 0 && lineComment < blockStart)
					return false; // Zeilenkommentar schluckt den Rest, kein offener Block
				int end = line.indexOf("*/", blockStart + 2);
				if (end < 0)
					return true; // Block geht über das Zeilenende hinaus
				i = end + 2;
			}
		}
		return false;
	}

	/** "package app.ui.skin;" -> "app.ui.skin" */
	private static String extractPackage(String trimmed) {
		String rest = trimmed.substring("package ".length()).trim();
		int semi = rest.indexOf(';');
		if (semi < 0)
			throw new RuntimeException("package-Zeile ohne Semikolon: " + trimmed);
		return rest.substring(0, semi).trim();
	}

	/**
	 * "import app.data.MapShape;"        -> "app.data"
	 * "import static app.util.Log.info;" -> "app.util"
	 * Gibt null zurück für nicht-app-Imports (werden eh später gefiltert, aber so sparen wir Arbeit).
	 */
	private static String extractImportPackage(String trimmed) {
		String rest = trimmed.substring("import ".length()).trim();
		if (rest.startsWith("static "))
			rest = rest.substring("static ".length()).trim();

		int semi = rest.indexOf(';');
		if (semi < 0)
			throw new RuntimeException("import-Zeile ohne Semikolon: " + trimmed);
		String fqn = rest.substring(0, semi).trim();

		if (fqn.endsWith(".*")) {
			// Wildcard-Import: "app.data.*" -> Package ist "app.data"
			return fqn.substring(0, fqn.length() - 2);
		}

		// Statisches Member oder Typ: letztes Segment ist Member/Typname, davor das Package.
		// Für statische Importe ist das letzte Segment die Methode/Konstante, das vorletzte der Typ.
		// Wir wollen das PACKAGE. Heuristik: alles bis zum letzten Segment, das mit Großbuchstabe beginnt,
		// gilt als Typ; alles davor ist Package.
		String[] parts = fqn.split("\\.");
		int typeIndex = -1;
		for (int k = 0; k < parts.length; k++) {
			if (!parts[k].isEmpty() && Character.isUpperCase(parts[k].charAt(0))) {
				typeIndex = k;
				break;
			}
		}
		if (typeIndex <= 0)
			return null; // kein erkennbarer Typ oder gleich am Anfang groß -> ignorieren
		StringBuilder pkg = new StringBuilder();
		for (int k = 0; k < typeIndex; k++) {
			if (k > 0)
				pkg.append('.');
			pkg.append(parts[k]);
		}
		return pkg.toString();
	}

	private static boolean startsTypeDeclaration(String trimmed) {
		// grobe Heuristik, reicht: erste echte Typdeklaration beendet den Header
		return trimmed.matches("^(public |final |abstract |sealed |non-sealed |strictfp |)*(class|interface|enum|record|@interface)\\b.*");
	}

	private static boolean isExcluded(String packageName) {
		for (String prefix : EXCLUDED_PREFIXES) {
			if (packageName.equals(prefix) || packageName.startsWith(prefix + "."))
				return true;
		}
		return false;
	}

	// ----------------------------------------------------------------

	private static String renderDot(Set<String> nodes, Map<String, Set<String>> edges) {
		// SCCs berechnen: Knoten in einer SCC mit Größe > 1 liegen auf einem Zyklus.
		Map<String, Integer> sccId = computeSccIds(nodes, edges);
		Map<Integer, Long> sccSize = sccId.values().stream()
				.collect(java.util.stream.Collectors.groupingBy(id -> id, java.util.stream.Collectors.counting()));
		Set<String> cyclicNodes = new TreeSet<>();
		for (var e : sccId.entrySet()) {
			if (sccSize.get(e.getValue()) > 1)
				cyclicNodes.add(e.getKey());
		}

		StringBuilder sb = new StringBuilder();
		sb.append("digraph ThosSuitePackages {\n");
		sb.append("  rankdir=TB;\n");
		sb.append("  node [shape=box, fontname=\"Helvetica\"];\n");
		sb.append("  edge [color=\"#888888\"];\n\n");

		sb.append("  // Rote Knoten/Kanten liegen auf einem Zyklus (gleiche SCC).\n");
		sb.append("  // Gestrichelt-rot: Kante schließt einen Zyklus (Ziel-SCC == Quell-SCC).\n\n");

		// Knoten deklarieren (Label = Package ohne führendes "app.", kürzer lesbar)
		for (String node : nodes) {
			boolean cyclic = cyclicNodes.contains(node);
			sb.append("  ").append(quote(node))
			  .append(" [label=").append(quote(shortLabel(node)));
			if (cyclic)
				sb.append(", color=\"#cc0000\", penwidth=2, fontcolor=\"#cc0000\"");
			sb.append("];\n");
		}
		sb.append('\n');

		// Kanten — rot, wenn Quelle und Ziel in derselben SCC liegen (also Teil eines Zyklus)
		for (var entry : edges.entrySet()) {
			String from = entry.getKey();
			for (String to : entry.getValue()) {
				boolean onCycle = sccId.containsKey(from) && sccId.containsKey(to)
						&& sccId.get(from).equals(sccId.get(to))
						&& sccSize.get(sccId.get(from)) > 1;
				sb.append("  ").append(quote(from)).append(" -> ").append(quote(to));
				if (onCycle)
					sb.append(" [color=\"#cc0000\", penwidth=2]");
				sb.append(";\n");
			}
		}

		sb.append("}\n");
		return sb.toString();
	}

	/**
	 * Tarjans Algorithmus für starke Zusammenhangskomponenten (SCC).
	 * Liefert pro Knoten eine SCC-ID. Knoten, deren SCC mehr als ein Mitglied hat,
	 * liegen auf mindestens einem gerichteten Zyklus.
	 *
	 * Iterativ implementiert (kein Rekursionsstack), damit auch tiefe Graphen nicht
	 * den Java-Stack sprengen — FailFast soll bei echten Fehlern knallen, nicht bei Tiefe.
	 */
	private static Map<String, Integer> computeSccIds(Set<String> nodes, Map<String, Set<String>> edges) {
		Map<String, Integer> index = new java.util.HashMap<>();
		Map<String, Integer> lowlink = new java.util.HashMap<>();
		Map<String, Boolean> onStack = new java.util.HashMap<>();
		java.util.Deque<String> stack = new java.util.ArrayDeque<>();
		Map<String, Integer> sccId = new java.util.HashMap<>();
		int[] counter = {0};
		int[] sccCounter = {0};

		for (String start : nodes) {
			if (index.containsKey(start))
				continue;

			// Iterativer DFS mit explizitem Arbeitsstack.
			java.util.Deque<String> work = new java.util.ArrayDeque<>();
			Map<String, java.util.Iterator<String>> iterators = new java.util.HashMap<>();
			work.push(start);

			while (!work.isEmpty()) {
				String v = work.peek();

				if (!index.containsKey(v)) {
					index.put(v, counter[0]);
					lowlink.put(v, counter[0]);
					counter[0]++;
					stack.push(v);
					onStack.put(v, true);
					iterators.put(v, edges.getOrDefault(v, Set.of()).iterator());
				}

				java.util.Iterator<String> it = iterators.get(v);
				boolean descended = false;
				while (it.hasNext()) {
					String w = it.next();
					if (!nodes.contains(w))
						continue; // Ziel nicht im Knotenset (sollte nicht vorkommen)
					if (!index.containsKey(w)) {
						work.push(w);
						descended = true;
						break;
					} else if (Boolean.TRUE.equals(onStack.get(w))) {
						lowlink.put(v, Math.min(lowlink.get(v), index.get(w)));
					}
				}
				if (descended)
					continue;

				// Alle Nachbarn abgearbeitet: lowlink von Kindern hochpropagieren
				for (String w : edges.getOrDefault(v, Set.of())) {
					if (Boolean.TRUE.equals(onStack.get(w)) && index.containsKey(w))
						lowlink.put(v, Math.min(lowlink.get(v), lowlink.get(w)));
				}

				if (lowlink.get(v).equals(index.get(v))) {
					// v ist Wurzel einer SCC — alle bis v vom Stack bilden die SCC
					int id = sccCounter[0]++;
					String w;
					do {
						w = stack.pop();
						onStack.put(w, false);
						sccId.put(w, id);
					} while (!w.equals(v));
				}
				work.pop();
			}
		}
		return sccId;
	}

	private static String shortLabel(String pkg) {
		String prefix = INTERNAL_PREFIX + ".";
		return pkg.startsWith(prefix) ? pkg.substring(prefix.length()) : pkg;
	}

	private static String quote(String s) {
		return '"' + s.replace("\"", "\\\"") + '"';
	}

	private static void writeOutput(String dot) {
		try {
			Files.writeString(Path.of(OUTPUT_DOT), dot);
		} catch (IOException e) {
			throw new UncheckedIOException("Konnte DOT-Datei nicht schreiben: " + OUTPUT_DOT, e);
		}
	}
}