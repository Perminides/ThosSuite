package app.shared.skin;

/**
 * Die Bestandteile einer Lern-Session, für die der Skin ein Feld kennt.
 *
 * <p>Nach außen ist das ein Bestandteil, nach innen ein Property-Name — die Endung bleibt in diesem
 * Paket. Wer {@code sessionBounds(…)} ruft, nennt also nie einen Schlüssel aus einer Datei.</p>
 *
 * <p>Die Textfelder fehlen hier absichtlich: die kommen über {@link Skin.TextLabelType}, der außer
 * dem Maß auch den Anzeigenamen und die CSS-Kennung trägt.</p>
 */
public enum LearnComponent {

	MAP("SessionMapPanel"),
	TEXT_INPUT("SessionTextInputPanel"),
	IMAGE("SessionImagePanel"),
	MC("SessionMcPanel"),
	BACK_BUTTON("SessionBackButton");

	private final String suffix;

	LearnComponent(String suffix) {
		this.suffix = suffix;
	}

	String suffix() {
		return suffix;
	}
}
