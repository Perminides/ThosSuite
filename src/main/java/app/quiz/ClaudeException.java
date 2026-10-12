package app.quiz;

/**
 * Die Claude-API hat keinen brauchbaren Vorschlag geliefert: nicht erreichbar, Fehlerstatus,
 * abgelehnt oder abgeschnitten. Wird als Meldung angezeigt; die Frage bleibt offen.
 */
@SuppressWarnings("serial")
public class ClaudeException extends RuntimeException {

	public ClaudeException(String message) {
		super(message);
	}

	public ClaudeException(String message, Throwable cause) {
		super(message, cause);
	}
}
