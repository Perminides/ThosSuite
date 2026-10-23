package app.tmp;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import app.shared.AppClock;
import app.shared.Config;
import app.shared.Log;

/**
 * <b>Wegwerf-Experiment:</b> beobachtet, wie stabil die Wikidata-Länderdaten über einige Wochen
 * sind. Ergebnis ist allein der Inhalt von {@code data/wikidata.db}; ausgewertet wird von Hand.
 *
 * <p>Einmal am Tag — auch ein gescheiterter Versuch zählt, damit der Splash höchstens einmal täglich
 * bis zur Zeitgrenze wartet — holt eine einzige Abfrage für die feste
 * Codeliste unten Name, Einwohner, Währung, Amtssprachen, Staatsoberhaupt, Regierungschef,
 * Hauptstadt und Flagge. Je Land wird die Antwort in einen sortierten Textblock umgewandelt und mit
 * der letzten gespeicherten Version verglichen; nur Abweichungen werden gespeichert. Bei Abrufen
 * mit Änderungen liegt die rohe Antwort zusätzlich als {@code log/wikidata-<zeitpunkt>.json}.</p>
 *
 * <p>Auswahlregel je Feld: veraltet markierte Einträge nie, bevorzugte, wenn es für das Feld
 * welche gibt, sonst alle normalen. Ein Land ohne jede Zeile in der Antwort bekommt eine Version
 * mit leerem Inhalt.</p>
 *
 * <p><b>Ausbau:</b> das Paket {@code app.tmp} löschen und im {@code Controller} die Stellen mit
 * {@code wikidataError} entfernen. Sonst hängt nichts daran.</p>
 */
public class WikidataProbe {

	private static final String ENDPOINT = "https://query.wikidata.org/sparql";
	// Wie das Limit von Wikidata selbst: ungecacht dauert die Abfrage gemessen 5 bis 37 s
	private static final Duration TIMEOUT = Duration.ofSeconds(60);
	private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(15);
	private static final String ENTITY_PREFIX = "http://www.wikidata.org/entity/";
	private static final String FILE_PREFIX = "http://commons.wikimedia.org/wiki/Special:FilePath/";
	private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
	private static final DateTimeFormatter FILE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH-mm-ss");

	private static final ObjectMapper MAPPER = new ObjectMapper();
	private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build();

	/** Alle Codes, die Wikidata am 23.10.2026 an einem Eintrag führte — historische wie DD und YU eingeschlossen. */
	private static final String CODES = """
			AC AD AE AF AG AI AL AM AN AO AQ AR AS AT AU AW AX AZ BA BB
			BD BE BF BG BH BI BJ BL BM BN BO BQ BR BS BT BV BW BY BZ CA
			CC CD CF CG CH CI CK CL CM CN CO CP CQ CR CU CV CW CX CY CZ
			DD DE DG DJ DK DM DO DZ EC EE EG EH ER ES ET FI FJ FK FM FO
			FR GA GB GD GE GF GG GH GI GL GM GN GP GQ GR GS GT GU GW GY
			HK HM HN HR HT HU ID IE IL IM IN IO IQ IR IS IT JE JM JO JP
			KE KG KH KI KM KN KP KR KW KY KZ LA LB LC LI LK LR LS LT LU
			LV LY MA MC MD ME MF MG MH MK ML MM MN MO MP MQ MR MS MT MU
			MV MW MX MY MZ NA NC NE NF NG NI NL NO NP NR NU NZ OM PA PC
			PE PF PG PH PK PL PM PN PR PS PT PW PY QA RE RO RS RU RW SA
			SB SC SD SE SG SH SI SJ SK SL SM SN SO SR SS ST SV SX SY SZ
			TA TC TD TF TG TH TJ TK TL TM TN TO TR TT TV TW TZ UA UG UM
			US UY UZ VA VC VE VG VI VN VU WF WS XK YE YT YU ZA ZM ZW
			""";

	// Bewusst ohne UNION: mit mehreren Codes wertet Wikidata die Zweige ungebunden aus und läuft in die 60 s
	private static final String QUERY = """
			SELECT ?code ?land ?landLabel ?feld ?wert ?wertLabel ?datum ?teil ?teilLabel WHERE {
			  VALUES ?code { <<CODES>> }
			  ?land wdt:P297 ?code .
			  VALUES (?feld ?p ?ps ?pq) {
			    ("einwohner"       p:P1082 ps:P1082 pq:P585)
			    ("waehrung"        p:P38   ps:P38   pq:P580)
			    ("amtssprache"     p:P37   ps:P37   pq:P580)
			    ("staatsoberhaupt" p:P35   ps:P35   pq:P580)
			    ("regierungschef"  p:P6    ps:P6    pq:P580)
			    ("hauptstadt"      p:P36   ps:P36   pq:P580)
			    ("flagge"          p:P41   ps:P41   pq:P580)
			  }
			  ?land ?p ?st . ?st ?ps ?wert ; wikibase:rank ?rank .
			  FILTER(?rank != wikibase:DeprecatedRank)
			  FILTER(?rank = wikibase:PreferredRank || NOT EXISTS { ?land ?p ?st2 . ?st2 wikibase:rank wikibase:PreferredRank })
			  OPTIONAL { ?st ?pq ?datum }
			  OPTIONAL { ?st pq:P518 ?teil }
			  SERVICE wikibase:label { bd:serviceParam wikibase:language "de,en". }
			} ORDER BY ?code STR(?land) ?feld STR(?wert) ?datum STR(?teil)
			""";

	/**
	 * Holt, vergleicht und speichert — sofern heute noch kein Abruf versucht wurde. Jeder Fehler wird
	 * als fehlgeschlagener Abruf protokolliert und weitergeworfen; der Controller meldet ihn.
	 */
	public void run() {
		try (WikidataRepository repository = new WikidataRepository()) {
			if (repository.hasFetchOn(AppClock.TODAY))
				return;

			LocalDateTime fetchedAt = LocalDateTime.now();
			long start = System.nanoTime();
			try {
				String body = send(buildQuery());
				Map<String, String> contents = toContents(MAPPER.readTree(body));

				Map<String, String> latest = repository.loadLatestContents();
				Map<String, String> changed = new HashMap<>();
				for (String code : codes()) {
					String content = contents.containsKey(code) ? contents.get(code) : "";
					if (!content.equals(latest.get(code)))
						changed.put(code, content);
				}

				if (!changed.isEmpty())
					Files.writeString(Config.getPath("logFolder").resolve("wikidata-" + fetchedAt.format(FILE_FORMAT) + ".json"), body);
				repository.saveSuccessfulFetch(fetchedAt.format(TIMESTAMP_FORMAT), millisSince(start), changed);
				Log.info(this, "Wikidata-Abruf: " + changed.size() + " Länder geändert, " + millisSince(start) + " ms");
			} catch (Exception e) {
				try {
					repository.saveFailedFetch(fetchedAt.format(TIMESTAMP_FORMAT), millisSince(start), e.toString());
				} catch (RuntimeException saveError) {
					saveError.addSuppressed(e);
					throw saveError;
				}
				throw e instanceof RuntimeException r ? r : new RuntimeException(e.getMessage(), e);
			}
		}
	}

	private static String buildQuery() {
		StringBuilder values = new StringBuilder();
		for (String code : codes())
			values.append('"').append(code).append("\" ");
		return QUERY.replace("<<CODES>>", values.toString().trim());
	}

	private static List<String> codes() {
		List<String> codes = new ArrayList<>();
		for (String code : CODES.trim().split("\\s+"))
			codes.add(code);
		return codes;
	}

	private static String send(String query) {
		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(ENDPOINT))
				.timeout(TIMEOUT)
				.header("Accept", "application/sparql-results+json")
				.header("Content-Type", "application/x-www-form-urlencoded")
				.header("User-Agent", "ThosSuite/1.0 (private Lernsuite, Stabilitaetsbeobachtung)")
				.POST(HttpRequest.BodyPublishers.ofString("query=" + URLEncoder.encode(query, StandardCharsets.UTF_8)))
				.build();
		try {
			HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
			if (response.statusCode() != 200)
				throw new RuntimeException("Wikidata meldet HTTP " + response.statusCode());
			return response.body();
		} catch (HttpTimeoutException e) {
			throw new RuntimeException("Wikidata antwortet nicht (Zeitüberschreitung nach " + TIMEOUT.toSeconds() + " s)", e);
		} catch (ConnectException e) {
			throw new RuntimeException("Wikidata ist nicht erreichbar", e);
		} catch (IOException e) {
			throw new RuntimeException("Wikidata-Abruf fehlgeschlagen: " + e.getMessage(), e);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException("Der Wikidata-Abruf wurde unterbrochen", e);
		}
	}

	/** Je Code ein Textblock: die Zeile {@code land} zuerst, darunter die Werte sortiert. */
	private static Map<String, String> toContents(JsonNode response) {
		Map<String, String> landLines = new HashMap<>();
		Map<String, List<String>> valueLines = new HashMap<>();
		for (JsonNode row : response.path("results").path("bindings")) {
			String code = text(row, "code");
			landLines.put(code, line("land", shortId(text(row, "land")), text(row, "landLabel"), "", "", ""));

			String value = text(row, "wert");
			String name = "";
			if (value.startsWith(ENTITY_PREFIX)) {
				value = shortId(value);
				name = text(row, "wertLabel");
			} else if (value.startsWith(FILE_PREFIX)) {
				value = URLDecoder.decode(value.substring(FILE_PREFIX.length()), StandardCharsets.UTF_8);
			}
			String date = text(row, "datum");
			if (date.length() > 10)
				date = date.substring(0, 10);

			valueLines.computeIfAbsent(code, _ -> new ArrayList<>())
					.add(line(text(row, "feld"), value, name, date, shortId(text(row, "teil")), text(row, "teilLabel")));
		}

		Map<String, String> contents = new HashMap<>();
		for (Map.Entry<String, List<String>> entry : valueLines.entrySet()) {
			List<String> lines = entry.getValue();
			Collections.sort(lines);
			lines.add(0, landLines.get(entry.getKey()));
			contents.put(entry.getKey(), String.join("\n", lines));
		}
		return contents;
	}

	private static String line(String... columns) {
		List<String> cleaned = new ArrayList<>();
		for (String column : columns)
			cleaned.add(column.replace("|", "/"));
		return String.join(" | ", cleaned);
	}

	/** Der Wert einer Spalte, oder {@code ""}, wenn Wikidata sie für diese Zeile weglässt. */
	private static String text(JsonNode row, String column) {
		return row.path(column).path("value").asText("");
	}

	private static String shortId(String uri) {
		return uri.startsWith(ENTITY_PREFIX) ? uri.substring(ENTITY_PREFIX.length()) : uri;
	}

	private static long millisSince(long start) {
		return (System.nanoTime() - start) / 1_000_000;
	}
}
