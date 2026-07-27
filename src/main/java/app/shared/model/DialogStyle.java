package app.shared.model;

import javafx.scene.paint.Color;
import javafx.scene.text.Font;

/**
 * Die Skin-Werte, die der Dialog- und Alert-Pfad braucht.
 *
 * <p>Das erste Stück der öffentlichen Fläche von {@code SkinProperties} — bewusst als Record
 * geschnitten statt als Feld-Getter: nach außen geht nur, was jemand tatsächlich braucht, in der
 * Form, in der er es braucht. Der Record wächst, wenn weitere Oberflächen aus dem Skin
 * herauswandern.</p>
 *
 * @param font      für das Padding des Dialog-Titels. Fällt weg, sobald das Padding ins generierte
 *                  CSS gewandert ist (siehe TODO in {@code SuiteHeaderBar}).
 * @param textColor zum Einfärben eines Alert-Bildes.
 */
public record DialogStyle(Font font, Color textColor) {}
