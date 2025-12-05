package app.ui.components;

import java.util.Set;

/**
 * Represents the visual state of a ShapeMap component.
 * Used for preserving and restoring map visualization during operations like skin changes.
 */
public record ShapeMapState(
    Set<String> correctShapes,
    Set<String> incorrectShapes,
    Set<String> markedShapes,
    Set<String> activeShapes,
    boolean interactive
) {}