package app.quiz;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import app.quiz.model.Question;
import app.quiz.model.Suggestion;
import app.shared.Config;

/**
 * Holt einen Kartenvorschlag von der Claude-API.
 *
 * <p>Der Prompt ist der <b>komplette Anfrage-Body</b> aus der Quiz-DB — Modell, {@code max_tokens},
 * System-Prompt und Nachricht wählt er selbst, nicht der Code. Ersetzt werden nur die Platzhalter
 * {@code <<FRAGE>>}, {@code <<ANTWORT>>} und {@code <<ZUSATZ>>} (der optionale Hinweis, sonst leer),
 * und zwar in den Textwerten des geparsten JSON statt im Rohtext: Fragen enthalten Anführungszeichen
 * und Umbrüche, das Escaping übernimmt Jackson.</p>
 *
 * <p>Raw HTTP statt des offiziellen SDK, weil der Body unverändert durchgereicht wird — das SDK
 * will ihn aus Builder-Aufrufen zusammengesetzt haben.</p>
 *
 * <p>{@link #suggest} läuft im {@code WaitDialog}, also außerhalb des FX-Threads. Deshalb liest
 * der Konstruktor den Schlüssel: {@code Config.get} fällt für unbekannte Schlüssel auf die
 * Suite-DB zurück, und die darf der Arbeits-Thread nicht anfassen.</p>
 */
public class ClaudeClient {

	private static final String ENDPOINT = "https://api.anthropic.com/v1/messages";
	private static final String API_VERSION = "2023-06-01";
	private static final Duration TIMEOUT = Duration.ofMinutes(3);

	private static final ObjectMapper MAPPER = new ObjectMapper();
	private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).build();

	private final String apiKey;

	public ClaudeClient() {
		this.apiKey = Config.get("anthropicApiKey");
	}

	/** @param instruction der Hinweis an Claude, oder {@code null} */
	public Suggestion suggest(String promptJson, Question question, String instruction) {
		if (instruction != null && !promptJson.contains("<<ZUSATZ>>"))
			throw new ClaudeException("Der Prompt hat keinen Platzhalter <<ZUSATZ>>, der Hinweis ginge verloren.");
		JsonNode body = parse(promptJson, "Der Prompt in der Quiz-DB ist kein gültiges JSON");
		fillPlaceholders(body, question, instruction == null ? "" : instruction);

		HttpRequest.Builder request = HttpRequest.newBuilder()
				.uri(URI.create(ENDPOINT))
				.timeout(TIMEOUT)
				.header("x-api-key", apiKey)
				.header("anthropic-version", API_VERSION)
				.header("content-type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(body.toString()));
		// Rückfallmodell bei Ablehnung: der Header gehört zur Form, die der Prompt verwendet
		JsonNode fallbacks = body.path("fallbacks");
		if (fallbacks.isTextual())
			request.header("anthropic-beta", "server-side-fallback-2026-07-01");
		else if (fallbacks.isArray())
			request.header("anthropic-beta", "server-side-fallback-2026-06-01");

		JsonNode response = parse(send(request.build()), "Die Antwort der Claude-API ist kein gültiges JSON");
		return read(response);
	}

	private static void fillPlaceholders(JsonNode node, Question question, String instruction) {
		if (node instanceof ObjectNode object) {
			for (Map.Entry<String, JsonNode> field : object.properties()) {
				if (field.getValue().isTextual())
					field.setValue(object.textNode(replace(field.getValue().asText(), question, instruction)));
				else
					fillPlaceholders(field.getValue(), question, instruction);
			}
		} else if (node instanceof ArrayNode array) {
			for (int i = 0; i < array.size(); i++) {
				if (array.get(i).isTextual())
					array.set(i, array.textNode(replace(array.get(i).asText(), question, instruction)));
				else
					fillPlaceholders(array.get(i), question, instruction);
			}
		}
	}

	private static String replace(String text, Question question, String instruction) {
		return text.replace("<<FRAGE>>", question.question())
				.replace("<<ANTWORT>>", question.answer())
				.replace("<<ZUSATZ>>", instruction);
	}

	private static String send(HttpRequest request) {
		try {
			HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
			if (response.statusCode() != 200)
				throw new ClaudeException("Die Claude-API meldet HTTP " + response.statusCode() + ":\n" + response.body());
			return response.body();
		} catch (IOException e) {
			throw new ClaudeException("Die Claude-API ist nicht erreichbar: " + e.getMessage(), e);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new ClaudeException("Der Aufruf der Claude-API wurde unterbrochen.", e);
		}
	}

	private static Suggestion read(JsonNode response) {
		String stopReason = response.path("stop_reason").asText();
		if (stopReason.equals("refusal"))
			throw new ClaudeException("Claude hat die Anfrage abgelehnt: "
					+ response.path("stop_details").path("explanation").asText("ohne Begründung"));
		if (stopReason.equals("max_tokens"))
			throw new ClaudeException("Die Antwort wurde abgeschnitten, max_tokens reicht nicht. "
					+ "Im Prompt erhöhen.");

		StringBuilder text = new StringBuilder();
		for (JsonNode block : response.path("content")) {
			if (!block.path("type").asText().equals("text"))
				continue; // Denkblöcke kommen leer zurück
			if (!text.isEmpty())
				text.append("\n\n");
			text.append(block.path("text").asText());
		}
		if (text.isEmpty())
			throw new ClaudeException("Die Antwort der Claude-API enthält keinen Text:\n" + response);
		return new Suggestion(text.toString(), response.path("model").asText());
	}

	private static JsonNode parse(String json, String errorMessage) {
		try {
			return MAPPER.readTree(json);
		} catch (IOException e) {
			throw new ClaudeException(errorMessage + ": " + e.getMessage(), e);
		}
	}
}
