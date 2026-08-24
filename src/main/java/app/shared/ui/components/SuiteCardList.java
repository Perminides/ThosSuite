package app.shared.ui.components;

import java.util.List;

import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * Eine senkrechte Liste von Karten in einem Rollbereich — die Trefferanzeige von Tagebuch und Film.
 *
 * <p><b>Die Liste zieht ihre Karten auf ihre eigene Breite</b> — sie setzt das selbst und verlangt
 * es den Karten nicht ab. Wie breit die Liste selbst wird, entscheidet der Aufrufer —
 * das Tagebuch deckelt sie auf eine lesbare Zeilenlänge, der Film lässt sie den Platz nehmen, der
 * neben der Suchspalte übrig bleibt.</p>
 *
 * <p>Die Scrollbar bleibt <b>dauerhaft sichtbar</b>. Sonst wechselt die Kartenbreite um ihre Breite,
 * sobald ein Treffer mehr dazukommt, und alles darüber verrutscht mit.</p>
 *
 * <p>Das Aussehen kommt über {@code .suite-card-list} aus dem Skin — auch der Platz, den ein
 * Kartenschatten braucht. Der ist hier kein Beiwerk: Der Viewport einer ScrollPane clippt hart, und
 * ohne diesen Innenabstand wird der Schatten an den Seiten schlicht abgeschnitten.</p>
 */
public class SuiteCardList extends ScrollPane {

	private final VBox cards = new VBox();

	public SuiteCardList() {
		super();
		cards.getStyleClass().add("cards");
		setContent(cards);

		setFitToWidth(true); // Die Karten bekommen die Breite des Viewports, statt ihre Wunschbreite zu behalten
		setFitToHeight(false);
		setHbarPolicy(ScrollBarPolicy.NEVER);
		setVbarPolicy(ScrollBarPolicy.ALWAYS);
		getStyleClass().add("suite-card-list");
	}

	/** Ersetzt den Inhalt. Der Schnittmengen-Typ verlangt beides: eine Fläche, die die Liste zeigen und breitziehen kann, und eine {@link Card}. */
	public <T extends Region & Card> void setCards(List<T> content) {
		for (Region map : content)
			map.setMaxWidth(Double.MAX_VALUE); // Die Liste sorgt selbst dafür, statt sich darauf zu verlassen, dass jede Karte daran denkt

		cards.getChildren().setAll(content);
	}
}
