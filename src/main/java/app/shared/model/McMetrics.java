package app.shared.model;

import javafx.scene.text.Font;

/**
 * Die Maße, die eine Multiple-Choice-Auswahl braucht.
 *
 * <p>Der Knopfabstand darf je Deck abweichen — im Flaggendeck stehen die Knöpfe enger. Aufgelöst
 * wird das in der Ansicht, die als Einzige die Deck-Id kennt; hereingereicht wird das Ergebnis.</p>
 *
 * <p>Die Auswahl misst selbst, wie viele Zeilen ein Antworttext braucht, und schaltet danach ihre
 * Darstellungsstufe um. Dafür braucht sie Schrift und die Ränder, die ein Knopf verbraucht.</p>
 *
 * @param font                Die Schrift der Knöpfe.
 * @param horizontalOverhead  Was Rand und Innenabstand einem Knopf an Breite wegnehmen.
 * @param borderWidth         Rahmenbreite eines Knopfs.
 * @param lineSpacingSqueezed Zeilenabstand in der zweizeiligen Stufe (negativ = enger).
 * @param buttonHeight        Höhe eines Knopfs, aus Schrift und Rändern gerechnet.
 * @param verticalGap         Abstand zwischen zwei Knöpfen.
 */
public record McMetrics(Font font, double horizontalOverhead, double borderWidth,
		double lineSpacingSqueezed, double buttonHeight, int verticalGap) {}
