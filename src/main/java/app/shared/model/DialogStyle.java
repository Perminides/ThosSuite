package app.shared.model;

import javafx.scene.paint.Color;

/**
 * Die Skin-Werte, die der Dialog- und Alert-Pfad braucht.
 *
 * <p>Zweckgeschnitten statt Feld-Getter: nach außen geht nur, was jemand tatsächlich braucht, in der
 * Form, in der er es braucht. Der Record wächst, wenn eine weitere Oberfläche einen Wert braucht.</p>
 *
 * @param textColor zum Einfärben eines Alert-Bildes.
 */
public record DialogStyle(Color textColor) {}
