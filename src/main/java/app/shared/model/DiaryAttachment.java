package app.shared.model;

/**
 * Ein Attachment eines Tagebucheintrags. Es gibt nur das Originalbild — die Vorschau entsteht
 * beim Laden, indem JavaFX direkt auf die Zielhöhe dekodiert. shared kennt die Ordnerstruktur
 * nicht; das Feature liefert den fertigen Pfad.
 *
 * @param imagePath absoluter Pfad zum Bild
 */
public record DiaryAttachment(String imagePath) {}
