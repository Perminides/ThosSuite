package app.shared.model;

import java.nio.file.Path;

/**
 * Die vier Bilder einer Bild-Karte: Hintergrund und Mini-Map-Overlay, je einmal für die aktive und
 * einmal für die abgeschaltete Karte.
 *
 * <p>Sie gehören zusammen und werden immer zusammen gebraucht — deshalb ein Bündel statt vier
 * Einzelwerte.</p>
 */
public record MapImages(Path background, Path overlay, Path inactiveBackground, Path inactiveOverlay) {}
