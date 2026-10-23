package scripts.flaggen;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Die Strukturdateien der Skizzen: wie viele Flächen eine Datei hat und welche festen Farben sie
 * trägt. Dazu der Flächenlauf über eine fertige Zeile.
 */
final class Skizzen {

	private static final Path SKETCHES = FlagDeckGenerator.DATA.resolve("sketches");

	/** Die Farben, die eine Skizze kennt — zugleich die Optionen jeder Farbfrage. */
	static final List<String> COLORS =
			List.of("Rot", "Blau", "Hellblau", "Grün", "Gelb", "Orange", "Weiß", "Schwarz");

	private final Map<String, Integer> areas = new LinkedHashMap<>();
	private final Map<String, List<String>> sketchColors = new LinkedHashMap<>();

	/**
	 * Der Flächenlauf: zählt mit, wie viele Flächen die Skizze zu jedem Zeitpunkt hat. Ein
	 * {@code Mark} oder {@code Fill} auf eine Fläche, die es noch nicht gibt, fiele sonst erst mitten
	 * in einer Lern-Session auf.
	 */
	void check(List<String> steps) {
		int available = 0;
		Set<Integer> filled = new HashSet<>();
		for (String raw : steps) {
			String step = withoutMarkers(raw);
			if (step.startsWith("SketchImage:"))
				available = areasOf("backgrounds", step.substring("SketchImage:".length()));
			else if (step.startsWith("SketchImageAdd:")) {
				String[] teile = step.substring("SketchImageAdd:".length()).split(",");
				// Der Renderer wirft bei allem ausserhalb von -1..8 — das gehoert hierhin und nicht
				// mitten in eine Session. -1 ist der Leinwand-Modus von Goesch und Dreieck.
				int cell = Integer.parseInt(teile[1].trim());
				if (cell < -1 || cell > 8)
					throw new RuntimeException(step + ": Rasterfeld " + cell + " liegt ausserhalb von -1..8");
				available += areasOf("elements", teile[0]);
			}
			else if (step.startsWith("SketchImageMark:") || step.startsWith("SketchImageFill:")) {
				for (String value : step.substring(step.indexOf(':') + 1).split(",")[0].split("\\|")) {
					int area = Integer.parseInt(value.trim());
					if (area >= available)
						throw new RuntimeException(step + ", aber es gibt erst " + available + " Flächen");
					if (step.startsWith("SketchImageFill:") && !filled.add(area))
						throw new RuntimeException("Fläche " + area + " wird doppelt gefüllt");
				}
			}
		}
	}

	/**
	 * Der Schritt ohne seine Shuffle-Marker, in derselben Reihenfolge abgeschnitten wie im Parser.
	 * Nicht bis zum letzten {@code >} springen: ein Fragetext darf {@code <br />} enthalten.
	 */
	private static String withoutMarkers(String raw) {
		String step = raw;
		for (String marker : List.of("<ShuffleStart>", "<ShuffleBreak>", "<ShuffleEnd>"))
			if (step.startsWith(marker))
				step = step.substring(marker.length());
		return step;
	}

	int areasOf(String subfolder, String sketch) {
		return areas.computeIfAbsent(subfolder + "/" + sketch, key -> read(subfolder, sketch).size());
	}

	/**
	 * Die festen Farben einer Strukturdatei, in Flächenreihenfolge — leer, wenn sie keine trägt.
	 *
	 * <p>Ein {@code farbe} in den {@code properties} heißt: Diese Figur sieht immer gleich aus, ihre
	 * Farbe ist keine Frage. Der Union Jack ist der Fall — er wird gefärbt, sobald er auftaucht, und
	 * kommt in keiner Farbfrage vor. Die Suite liest die Eigenschaft nicht, sie ist eine Abmachung
	 * zwischen der Datei und diesem Generator.</p>
	 */
	List<String> colorsOf(String subfolder, String sketch) {
		return sketchColors.computeIfAbsent(subfolder + "/" + sketch,
				key -> readColors(subfolder, sketch));
	}

	private static List<String> readColors(String subfolder, String sketch) {
		JsonNode features = tree(subfolder, sketch).get("features");
		List<String> result = new ArrayList<>();
		for (JsonNode feature : features) {
			JsonNode color = feature.path("properties").path("farbe");
			if (color.isMissingNode())
				continue;
			if (!COLORS.contains(color.asText()))
				throw new RuntimeException(sketch + ": '" + color.asText() + "' ist keine Skizzenfarbe");
			result.add(color.asText());
		}
		// Halb gefärbt gibt es nicht: Sonst hinge an einer vergessenen Zeile in der Datei, ob eine
		// Fläche gefragt wird oder nicht, und das sähe man erst mitten in einer Session.
		if (!result.isEmpty() && result.size() != features.size())
			throw new RuntimeException(sketch + ": " + result.size() + " Farben, aber "
					+ features.size() + " Flächen — entweder alle oder keine");
		return result;
	}

	/** Die Flächen einer Strukturdatei — gezählt wird nur, wie viele es sind. */
	private static List<double[]> read(String subfolder, String sketch) {
		List<double[]> result = new ArrayList<>();
		for (JsonNode feature : tree(subfolder, sketch).get("features")) {
			JsonNode geometry = feature.get("geometry");
			if (geometry.get("type").asText().equals("Point")) {
				double x = geometry.get("coordinates").get(0).asDouble();
				double radius = feature.get("properties").get("radius").asDouble();
				result.add(new double[] {x - radius, x + radius});
				continue;
			}
			double min = Double.MAX_VALUE;
			double max = -Double.MAX_VALUE;
			for (JsonNode polygon : geometry.get("coordinates"))
				for (JsonNode ring : polygon)
					for (JsonNode point : ring) {
						min = Math.min(min, point.get(0).asDouble());
						max = Math.max(max, point.get(0).asDouble());
					}
			result.add(new double[] {min, max});
		}
		return result;
	}

	private static JsonNode tree(String subfolder, String sketch) {
		Path file = SKETCHES.resolve(subfolder).resolve(sketch + ".geojson");
		try {
			return new ObjectMapper().readTree(file.toFile());
		} catch (IOException e) {
			throw new RuntimeException("Strukturdatei fehlt: " + file, e);
		}
	}
}
