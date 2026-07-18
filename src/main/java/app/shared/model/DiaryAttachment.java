package app.shared.model;

/**
 * Ein Attachment eines Tagebucheintrags, mit absoluten Pfaden zu Originalbild und
 * Thumbnail. shared kennt die Ordnerstruktur nicht — das Feature liefert beide Pfade fertig.
 *
 * @param imagePath     absoluter Pfad zum Originalbild
 * @param thumbnailPath absoluter Pfad zum Thumbnail
 */
public record DiaryAttachment(String imagePath, String thumbnailPath) {}