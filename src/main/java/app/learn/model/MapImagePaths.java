package app.learn.model;

import java.nio.file.Path;

/**
 * Die vier skin-abhängigen Bildpfade einer Bild-Karte. Frameworkfrei (nur {@link Path}), Transport zwischen
 * {@code MapService.imagePathsFor(...)} und der Bild-Karten-Seite. Die inaktiven Pfade dürfen {@code null} sein
 * (Karte ohne inaktive Variante).
 */
public record MapImagePaths(Path background, Path overlay, Path inactiveBackground, Path inactiveOverlay) {
}