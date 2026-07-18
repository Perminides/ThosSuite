package app.learn.region;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import app.learn.model.Deck;
import app.learn.model.DeckCategory;
import app.learn.model.MapMetadata;
import app.learn.region.model.Mode;
import app.shared.model.RegionDialogState;
import app.shared.model.RegionDialogState.Choice;
import app.shared.model.RegionDialogState.Toggle;
import app.shared.ui.surfaces.dialogs.RegionConfigDialog;

public final class RegionPlayConfigForm {

	/**
	 * Was gespielt werden soll.
	 */
    public record RegionPlayConfig(Set<Deck> selectedDecks, Mode mode) {}

    // Konstruktor verstecken
    private RegionPlayConfigForm() {}

    // Start
    public static Optional<RegionPlayConfig> show() {
        RegionDialogState initial = getInitialState(); // Im initialen Zustand ist alles anklickbar, deswegen brauchen wir kein reduce aufzurufen hier.
        Optional<RegionDialogState> result =
                new RegionConfigDialog(initial, RegionPlayConfigForm::reduce).showAndWait();
        return result.map(RegionPlayConfigForm::toConfig);
    }

    /**
     * Mappt einen Einganszustand auf einen Ausgangszustand:
     * 
     * <ul>
     *   <li>Checke die aktuell ausgewählten Regionen</li>
     *   <li>Erlaube alle Modi, die zu den ausgewählten Regionen passen (Eine Region ohne Hauptstadt lässt Hauptstadt-Modi entfernen)</li>
     *   <li>Lasse genau die Regionen auswählbar, die
     *   <ul>
     *     <li>zu den ausgewählten Regionen die identische Karte haben.</li>
     *     <li>zu dem ausgewählten Modus passen</li>
     *   </ul>
     * </ul>
     * 
     * @param in
     * @return
     */
    private static RegionDialogState reduce(RegionDialogState in) {
        // angehakte Decks sammeln (Label ist die id — Namen eindeutig)
        Set<Deck> checked = new HashSet<>();
        for (List<Toggle> column : in.deckColumns())
            for (Toggle t : column)
                if (t.checked()) checked.add(deckByLabel(t.label()));

        boolean anySelected = !checked.isEmpty();
        boolean anyHasCapital = false;
        MapMetadata selectedMap = null;
        for (Deck d : checked) {
            if (d.hasCapital()) anyHasCapital = true;
            if (selectedMap == null) selectedMap = d.getMapMetadata();
        }

        // kompatible Modi + effektiver Modus
        List<Mode> compatible = new ArrayList<>();
        for (Mode m : Mode.values())
            if (!needsCapital(m) || !anySelected || anyHasCapital)
                compatible.add(m);
        Mode current = selectedMode(in);
        Mode effective = compatible.contains(current) ? current : compatible.get(0);

        List<Choice> modes = new ArrayList<>();
        for (Mode m : compatible)
            modes.add(new Choice(m.toString(), m == effective));

        // Deck-Spalten mit disabled neu aufbauen — Struktur bleibt gleich
        List<List<Toggle>> deckColumns = new ArrayList<>();
        for (List<Toggle> column : in.deckColumns()) {
            List<Toggle> newColumn = new ArrayList<>();
            for (Toggle t : column) {
                Deck d = deckByLabel(t.label());
                boolean disabled = !t.checked() && (
                        (selectedMap != null && !d.getMapMetadata().equals(selectedMap))
                     || (needsCapital(effective) && !d.hasCapital()));
                newColumn.add(new Toggle(t.label(), t.checked(), disabled));
            }
            deckColumns.add(newColumn);
        }
        return new RegionDialogState(modes, deckColumns);
    }

    /**
     * Alle Regionen auswählbar, keine Region ausgewählt. Gewählter Modus = Elimintaion Region.
     * 
     * @return
     */
    private static RegionDialogState getInitialState() {
        List<Choice> modes = new ArrayList<>();
        for (Mode m : Mode.values())
            modes.add(new Choice(m.toString(), m == Mode.ELIMINATION_REGION));
        return new RegionDialogState(modes, buildDeckColumns());
    }

    /**
     * Erstellt einmal initial beim Erstellen des Dialogs die Regions-Spalten mit den Checkboxes 
     * 
     * @return
     */
    private static List<List<Toggle>> buildDeckColumns() {
        Map<MapMetadata, List<Deck>> byMap = new LinkedHashMap<>();
        for (Deck d : Deck.values()) {
            if (d.getCategory() != DeckCategory.REGION_DECK) continue;
            byMap.computeIfAbsent(d.getMapMetadata(), _ -> new ArrayList<>()).add(d);
        }
        List<List<Deck>> groups = new ArrayList<>();
        List<Deck> singles = new ArrayList<>();
        for (List<Deck> group : byMap.values()) {
            if (group.size() == 1) singles.add(group.get(0));
            else groups.add(group);
        }
        if (!singles.isEmpty()) groups.add(singles);
        groups.sort(Comparator.comparingInt((List<Deck> g) -> g.size()).reversed());

        List<List<Toggle>> columns = new ArrayList<>();
        for (List<Deck> group : groups) {
            group.sort(Comparator.comparing(Deck::getDisplayName));
            List<Toggle> column = new ArrayList<>();
            for (Deck d : group)
                column.add(new Toggle(d.getDisplayName(), false, false));
            columns.add(column);
        }
        return columns;
    }

    /**
     * Ermittelt aus dem aktuellen Zustand des Dialogs, was jetzt eigentlich gespielt werden soll.
     * 
     * @param state
     * @return
     */
    private static RegionPlayConfig toConfig(RegionDialogState state) {
        Set<Deck> selected = new HashSet<>();
        for (List<Toggle> column : state.deckColumns())
            for (Toggle t : column)
                if (t.checked()) selected.add(deckByLabel(t.label()));
        return new RegionPlayConfig(selected, selectedMode(state));
    }

    // ----- HELPER -----
    
    private static Deck deckByLabel(String label) {
        for (Deck d : Deck.values())
            if (d.getDisplayName().equals(label)) return d;
        throw new IllegalStateException("Kein Deck zum Label: " + label);
    }

    private static Mode selectedMode(RegionDialogState in) {
        for (Choice c : in.modes())
            if (c.selected()) return modeByLabel(c.label());
        return Mode.ELIMINATION_REGION;
    }

    private static Mode modeByLabel(String label) {
        for (Mode m : Mode.values())
            if (m.toString().equals(label)) return m;
        throw new IllegalStateException("Kein Mode zum Label: " + label);
    }

    private static boolean needsCapital(Mode m) {
        return m.getCapitalOrRegion() == Mode.CapitalOrRegion.CAPITAL
            || m.getCapitalOrRegion() == Mode.CapitalOrRegion.BOTH;
    }
}