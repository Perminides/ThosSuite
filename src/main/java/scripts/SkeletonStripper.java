package scripts;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.EnumConstantDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;

/**
 * Erzeugt ein "Skelett" des ThosSuite-Quellcodes für ein Architektur-Review:
 * pro Klasse die vollständige Deklaration (extends/implements), die Importe,
 * die Felder, und die Methoden-/Konstruktor-SIGNATUREN — Rümpfe werden durch
 * die Zeilenzahl ersetzt (Navigationssignal für lange Methoden).
 *
 * Ziel: das ganze Projekt passt in EINEN lesbaren Durchgang, ohne Methodenrümpfe.
 *
 * Behält:
 *  - package + alle imports (Abhängigkeits-Info)
 *  - vollständige Typdeklaration inkl. extends/implements und verschachtelter Typen
 *  - Felder (Zustandsbesitz)
 *  - Methoden-/Konstruktorsignaturen + Zeilenzahl des entfernten Rumpfes
 *  - verhaltenssteuernde Enum-Konstanten (Werte sichtbar)
 *
 * Strippt:
 *  - Methoden-/Konstruktor-RÜMPFE (ersetzt durch "// <n> Zeilen")
 *  - alle Kommentare (sonst bläht das Skelett auf)
 *
 * Ausgeschlossen: app.misc (Wegwerf-Code, inkl. dieses Tools).
 *
 * Ausgabe: EINE Datei, nach Package gruppiert.
 *
 * FailFast: Parse- und IO-Fehler fliegen hoch.
 */
public class SkeletonStripper {

	// ===== Konfiguration — bitte ausfüllen =====

	private static final String SOURCE_ROOT = "C:/Users/permi/git/ThosSuite/src/main/java/app";

	private static final String OUTPUT_FILE = "C:/Users/permi/Desktop/thossuite_skeleton.txt";

	/** Packages mit diesem Präfix werden komplett übersprungen. */
	private static final List<String> EXCLUDED_PREFIXES = List.of("app.scripts"); // Wird ignoriert wenn INCLUDED nicht leer ist.
	private static final List<String> INCLUDED_PREFIXES = List.of("app.shared");

	// ===== Ende Konfiguration =====

	public static void main(String[] args) {
		StaticJavaParser.getParserConfiguration()
        .setLanguageLevel(com.github.javaparser.ParserConfiguration.LanguageLevel.JAVA_25);
		
		Path root = Path.of(SOURCE_ROOT);
		if (!Files.isDirectory(root))
			throw new RuntimeException("SOURCE_ROOT ist kein Verzeichnis: " + root.toAbsolutePath());

		List<Path> javaFiles = collectJavaFiles(root);
		if (javaFiles.isEmpty())
			throw new RuntimeException("Keine .java-Dateien gefunden unter " + root.toAbsolutePath());

		// Skelette pro Package sammeln, damit die Ausgabe nach Package gruppiert ist
		TreeMap<String, List<String>> byPackage = new TreeMap<>();

		for (Path file : javaFiles) {
			CompilationUnit cu = parse(file);

			String pkg = cu.getPackageDeclaration()
					.map(pd -> pd.getNameAsString())
					.orElse("(default)");

			if (isExcluded(pkg))
				continue;

			StringBuilder sb = new StringBuilder();
			sb.append("// ===== Datei: ").append(file.getFileName()).append(" =====\n");

			// Importe (sortiert, nur zur Lesbarkeit)
			List<String> imports = cu.getImports().stream()
					.map(i -> "import " + (i.isStatic() ? "static " : "") + i.getNameAsString()
							+ (i.isAsterisk() ? ".*" : "") + ";")
					.sorted()
					.toList();
			imports.forEach(line -> sb.append(line).append('\n'));
			if (!imports.isEmpty())
				sb.append('\n');

			// Top-Level-Typen der Datei
			for (TypeDeclaration<?> type : cu.getTypes()) {
				renderType(type, sb, 0);
				sb.append('\n');
			}

			byPackage.computeIfAbsent(pkg, _ -> new ArrayList<>()).add(sb.toString());
		}

		String output = renderOutput(byPackage);
		writeOutput(output);

		int classCount = byPackage.values().stream().mapToInt(List::size).sum();
		System.out.println("Fertig. " + byPackage.size() + " Packages, " + classCount + " Dateien.");
		System.out.println("Geschrieben: " + Path.of(OUTPUT_FILE).toAbsolutePath());
	}

	// ----------------------------------------------------------------

	private static List<Path> collectJavaFiles(Path root) {
		try (Stream<Path> stream = Files.walk(root)) {
			return stream
					.filter(Files::isRegularFile)
					.filter(p -> p.getFileName().toString().endsWith(".java"))
					.filter(p -> !p.getFileName().toString().equals("package-info.java"))
					.sorted()
					.toList();
		} catch (IOException e) {
			throw new UncheckedIOException("Konnte Quellbaum nicht durchlaufen: " + root, e);
		}
	}

	private static CompilationUnit parse(Path file) {
		try {
			return StaticJavaParser.parse(file);
		} catch (IOException e) {
			throw new UncheckedIOException("Konnte Datei nicht parsen/lesen: " + file, e);
		}
	}

	/**
	 * Rendert einen Typ (Klasse/Interface/Enum/Record) inkl. Kopf, Feldern,
	 * Signaturen und verschachtelter Typen. Rekursiv für innere Typen.
	 */
	private static void renderType(TypeDeclaration<?> type, StringBuilder sb, int depth) {
		String indent = "    ".repeat(depth);

		// --- Kopf: die Deklarationszeile bis zur öffnenden Klammer ---
		sb.append(indent).append(headerLine(type)).append(" {\n");

		String innerIndent = "    ".repeat(depth + 1);

		// --- Enum-Konstanten (verhaltenssteuerndes Vokabular, mit Werten) ---
		if (type instanceof EnumDeclaration en) {
			List<String> consts = en.getEntries().stream()
					.map(SkeletonStripper::enumConstant)
					.toList();
			if (!consts.isEmpty())
				sb.append(innerIndent).append(String.join(", ", consts)).append(";\n");
		}

		// --- Felder (Zustandsbesitz) ---
		for (FieldDeclaration field : type.getFields()) {
			sb.append(innerIndent).append(fieldLine(field)).append('\n');
		}

		// --- Konstruktoren ---
		for (ConstructorDeclaration ctor : type.getConstructors()) {
			int bodyLines = countBodyLines(ctor);
			sb.append(innerIndent).append(constructorSignature(ctor))
			  .append(" { ").append(bodyComment(bodyLines)).append(" }\n");
		}

		// --- Methoden ---
		for (MethodDeclaration method : type.getMethods()) {
			String sig = methodSignature(method);
			if (method.getBody().isEmpty()) {
				// abstrakte / Interface-Methode ohne Rumpf
				sb.append(innerIndent).append(sig).append(";\n");
			} else {
				int bodyLines = countBodyLines(method);
				sb.append(innerIndent).append(sig)
				  .append(" { ").append(bodyComment(bodyLines)).append(" }\n");
			}
		}

		// --- Verschachtelte Typen rekursiv ---
		for (Node child : type.getChildNodes()) {
			if (child instanceof TypeDeclaration<?> nested) {
				sb.append('\n');
				renderType(nested, sb, depth + 1);
			}
		}

		sb.append(indent).append("}\n");
	}

	/**
	 * Baut die Kopfzeile eines Typs: Modifier, Art (class/interface/enum/record),
	 * Name, Typparameter, extends/implements. Für Records die Parameterliste.
	 *
	 * Wir nehmen die erste Zeile der toString()-Repräsentation des Typs bis zur
	 * ersten '{' — das enthält Modifier, Namen, Typparameter, extends/implements
	 * und (bei Records) die Komponenten, ohne den Rumpf.
	 */
	private static String headerLine(TypeDeclaration<?> type) {
		// removeBody-Trick: wir klonen flach und schneiden am '{'.
		// Robust und sprachunabhängig gegenüber class/interface/enum/record.
		String full = type.toString();
		int brace = full.indexOf('{');
		String head = brace >= 0 ? full.substring(0, brace) : full;
		// Kommentare/Annotationen, die JavaParser mit ausgibt, in einer Zeile glätten
		return head.replaceAll("\\s+", " ").trim();
	}

	private static String enumConstant(EnumConstantDeclaration entry) {
		// Name plus ggf. Argumente, z. B. QUESTION("Question")
		String name = entry.getNameAsString();
		if (entry.getArguments().isEmpty())
			return name;
		String args = entry.getArguments().stream()
				.map(Node::toString)
				.collect(Collectors.joining(", "));
		return name + "(" + args + ")";
	}

	private static String fieldLine(FieldDeclaration field) {
		// Komplette Felddeklaration in einer Zeile, Initializer behalten
		// (Initializer sind oft architektonisch aussagekräftig: new HashMap<>(), Singletons …)
		return field.toString().replaceAll("\\s+", " ").trim();
	}

	private static String constructorSignature(ConstructorDeclaration ctor) {
		StringBuilder sb = new StringBuilder();
		sb.append(modifiers(ctor.getModifiers()));
		sb.append(ctor.getNameAsString());
		sb.append('(');
		sb.append(ctor.getParameters().stream()
				.map(p -> p.getType() + " " + p.getNameAsString())
				.collect(Collectors.joining(", ")));
		sb.append(')');
		if (!ctor.getThrownExceptions().isEmpty()) {
			sb.append(" throws ").append(ctor.getThrownExceptions().stream()
					.map(Object::toString).collect(Collectors.joining(", ")));
		}
		return sb.toString();
	}

	private static String methodSignature(MethodDeclaration method) {
		StringBuilder sb = new StringBuilder();
		sb.append(modifiers(method.getModifiers()));
		if (!method.getTypeParameters().isEmpty()) {
			sb.append('<').append(method.getTypeParameters().stream()
					.map(Object::toString).collect(Collectors.joining(", "))).append("> ");
		}
		sb.append(method.getType()).append(' ');
		sb.append(method.getNameAsString());
		sb.append('(');
		sb.append(method.getParameters().stream()
				.map(p -> p.getType() + " " + p.getNameAsString())
				.collect(Collectors.joining(", ")));
		sb.append(')');
		if (!method.getThrownExceptions().isEmpty()) {
			sb.append(" throws ").append(method.getThrownExceptions().stream()
					.map(Object::toString).collect(Collectors.joining(", ")));
		}
		return sb.toString();
	}

	private static String modifiers(com.github.javaparser.ast.NodeList<com.github.javaparser.ast.Modifier> mods) {
		if (mods.isEmpty())
			return "";
		return mods.stream().map(m -> m.getKeyword().asString())
				.collect(Collectors.joining(" ")) + " ";
	}

	/** Zeilenzahl des Methoden-/Konstruktorrumpfes (für Smell-Navigation). */
	private static int countBodyLines(Node node) {
		Optional<com.github.javaparser.ast.stmt.BlockStmt> body = Optional.empty();
		if (node instanceof MethodDeclaration m)
			body = m.getBody();
		else if (node instanceof ConstructorDeclaration c)
			body = Optional.of(c.getBody());

		if (body.isEmpty())
			return 0;
		return body.get().getRange()
				.map(r -> r.end.line - r.begin.line + 1)
				.orElse(0);
	}

	private static String bodyComment(int bodyLines) {
		return "/* " + bodyLines + " Zeilen */";
	}

	private static boolean isExcluded(String packageName) {
		if (!INCLUDED_PREFIXES.isEmpty()) {
			for (String prefix : INCLUDED_PREFIXES) {
				if (packageName.equals(prefix) || packageName.startsWith(prefix + "."))
					return false;
			}
			return true;
		} else {
			for (String prefix : EXCLUDED_PREFIXES) {
				if (packageName.equals(prefix) || packageName.startsWith(prefix + "."))
					return true;
			}
		}
		return false;
	}

	private static String renderOutput(TreeMap<String, List<String>> byPackage) {
		StringBuilder sb = new StringBuilder();
		sb.append("// ThosSuite — Klassen-Skelett (Rümpfe gestrippt)\n");
		sb.append("// Generiert von app.misc.SkeletonStripper\n\n");
		for (var entry : byPackage.entrySet()) {
			sb.append("\n// ##################################################\n");
			sb.append("// PACKAGE ").append(entry.getKey()).append('\n');
			sb.append("// ##################################################\n\n");
			for (String classSkeleton : entry.getValue()) {
				sb.append(classSkeleton).append('\n');
			}
		}
		return sb.toString();
	}

	private static void writeOutput(String output) {
		try {
			Files.writeString(Path.of(OUTPUT_FILE), output);
		} catch (IOException e) {
			throw new UncheckedIOException("Konnte Skelett-Datei nicht schreiben: " + OUTPUT_FILE, e);
		}
	}
}