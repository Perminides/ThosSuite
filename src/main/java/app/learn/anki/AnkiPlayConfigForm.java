package app.learn.anki;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import app.learn.model.Deck;
import app.shared.model.AnkiDialogState;
import app.shared.ui.surfaces.dialogs.AnkiConfigDialog;

public final class AnkiPlayConfigForm {

    private static final int COLUMN_SIZE = 10;

    // Definition welche Karten gespielt werden sollen.
    public record AnkiPlayConfig(int minIndex, int maxIndex, int maxCards, Set<String> selectedLabels) {}

    private AnkiPlayConfigForm() {}

    public static Optional<AnkiPlayConfig> show(Deck deckType, Set<String> availableLabels) {
        String title = deckType.getDisplayName() + " spielen";
        Optional<AnkiDialogState> result =
                new AnkiConfigDialog(title, "0", "50000000", "20", labelColumns(availableLabels)).showAndWait();
        return result.map(AnkiPlayConfigForm::toConfig);
    }

    // Labels sortiert, in Spalten zu je 10
    private static List<List<String>> labelColumns(Set<String> availableLabels) {
        List<String> sorted = new ArrayList<>(availableLabels);
        Collections.sort(sorted);
        List<List<String>> columns = new ArrayList<>();
        List<String> current = new ArrayList<>();
        for (String label : sorted) {
            if (current.size() == COLUMN_SIZE) { columns.add(current); current = new ArrayList<>(); }
            current.add(label);
        }
        if (!current.isEmpty()) columns.add(current);
        return columns;
    }

    private static AnkiPlayConfig toConfig(AnkiDialogState state) {
        // Gültigkeit hat der Dialog schon sichergestellt (OK war nur bei gültigen Feldern aktiv).
        // Leer -> Default: min 0, max/maxCards unbegrenzt (MAX_VALUE) — wie im Original.
        return new AnkiPlayConfig(
                parseOr(state.minText(), 0),
                parseOr(state.maxText(), Integer.MAX_VALUE),
                parseOr(state.maxCardsText(), Integer.MAX_VALUE),
                new HashSet<>(state.selectedLabels()));
    }

    private static int parseOr(String s, int fallback) {
        if (s == null || s.isBlank()) return fallback;
        return Integer.parseInt(s.trim());
    }
}