package app.shared.ui.dialog;

import java.util.List;

/** Region-Dialog-Zustand: reine Strings/booleans, kein javafx, kein Deck/Mode. */
public record RegionDialogState(List<Choice> modes, List<List<Toggle>> deckColumns) {

    /** Dropdown-Eintrag. Genau einer hat selected == true. */
    public record Choice(String label, boolean selected) {}

    /** Checkbox. disabled = ausgegraut (fremde Karte / Modus braucht Hauptstadt). */
    public record Toggle(String label, boolean checked, boolean disabled) {}
}