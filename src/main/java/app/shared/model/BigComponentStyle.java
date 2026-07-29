package app.shared.model;

/**
 * Eckradius und Rahmenbreite großer Flächen — Bildkarte, Bildrahmen und Verwandte.
 *
 * <p>Wer selbst etwas Abgerundetes zeichnet, muss dieselben Maße treffen wie das CSS, sonst passen
 * Rahmen und Inhalt nicht aufeinander. Beide Werte werden deshalb immer zusammen gebraucht.</p>
 *
 * <p>Achtung, zwei Einheiten: hier steht wie im CSS der <b>Radius</b>. JavaFX-Formen
 * ({@code Rectangle.setArcWidth}) wollen den <b>Durchmesser</b> — die Verdopplung passiert dort, wo
 * die Form gebaut wird.</p>
 */
public record BigComponentStyle(int cornerRadius, int borderWidth) {}
