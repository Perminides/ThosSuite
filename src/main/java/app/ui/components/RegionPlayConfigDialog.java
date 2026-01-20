package app.ui.components;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import app.data.DeckCategory;
import app.data.Deck;
import app.data.MapMetadata;
import app.data.RegionMode;
import app.ui.skin.Skin;
import javafx.geometry.Pos;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

public class RegionPlayConfigDialog {
    
    private final Dialog<RegionPlayConfig> dialog;
    private final Map<Deck, CheckBox> deckCheckBoxes = new EnumMap<>(Deck.class);
    private final ComboBox<RegionMode> modeComboBox = new ComboBox<>();
    private MapMetadata selectedMap = null;

    @SuppressWarnings("unchecked")
    public RegionPlayConfigDialog(Window parent, Skin skin) {
        this.dialog = (Dialog<RegionPlayConfig>) skin.createDialog(parent, "Filter");
        
        dialog.setTitle("Regionen spielen");
        
        VBox mainContent = skin.createDialogContent();
        
        // Mode-Auswahl (mittig oben)
        modeComboBox.getItems().addAll(RegionMode.values());
        modeComboBox.setValue(RegionMode.CLICK_CITY_BLANK);
        modeComboBox.setOnAction(_ -> updateCheckBoxStates());        
        mainContent.getChildren().add(modeComboBox);
        
        // Decks gruppieren
        Map<MapMetadata, List<Deck>> mapGroups = new HashMap<>();
        for (Deck type : Deck.values()) {
            if (type.getCategory() != DeckCategory.REGION_DECK) continue;
            mapGroups.computeIfAbsent(type.getMapMetadata(), _ -> new ArrayList<>()).add(type);
        }
        
        // Gruppen erstellen
        List<List<Deck>> groups = new ArrayList<>();
        List<Deck> exclusiveGroup = new ArrayList<>();
        
        for (Map.Entry<MapMetadata, List<Deck>> entry : mapGroups.entrySet()) {
            if (entry.getValue().size() == 1) {
                exclusiveGroup.add(entry.getValue().get(0));
            } else {
                groups.add(entry.getValue());
            }
        }
        
        if (!exclusiveGroup.isEmpty()) {
            groups.add(exclusiveGroup);
        }
        
        // Nach Größe sortieren (größte zuerst)
        groups.sort(Comparator.comparingInt(List<Deck>::size).reversed());
        
        // Horizontal Layout
        HBox groupsContainer = new HBox(20);
        groupsContainer.setAlignment(Pos.TOP_CENTER);
        
        for (List<Deck> group : groups) {
            VBox groupBox = new VBox(5);
            groupBox.setAlignment(Pos.TOP_LEFT);
            
            group.sort(Comparator.comparing(Deck::getDisplayName));
            for (Deck type : group) {
                CheckBox checkBox = new CheckBox(type.getDisplayName());
                checkBox.setOnAction(_ -> onDeckCheckBoxChanged());
                deckCheckBoxes.put(type, checkBox);
                groupBox.getChildren().add(checkBox);
            }
            
            groupsContainer.getChildren().add(groupBox);
        }
        
        mainContent.getChildren().add(groupsContainer);
        
        dialog.getDialogPane().setContent(mainContent);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        
        // Result Converter
        dialog.setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                Set<Deck> selectedDecks = new HashSet<>();
                for (Map.Entry<Deck, CheckBox> entry : deckCheckBoxes.entrySet()) {
                    if (entry.getValue().isSelected()) {
                        selectedDecks.add(entry.getKey());
                    }
                }
                
                if (selectedDecks.isEmpty()) return null;
                
                return new RegionPlayConfig(selectedDecks, modeComboBox.getValue());
            }
            return null;
        });
        
        // OK-Button nur aktivieren wenn mindestens ein Deck gewählt
        dialog.getDialogPane().lookupButton(ButtonType.OK).disableProperty().bind(
            javafx.beans.binding.Bindings.createBooleanBinding(
                () -> deckCheckBoxes.values().stream().noneMatch(CheckBox::isSelected),
                deckCheckBoxes.values().stream().map(CheckBox::selectedProperty).toArray(javafx.beans.property.BooleanProperty[]::new)
            )
        );
    }
    
    private void onDeckCheckBoxChanged() {
        selectedMap = null;
        for (Map.Entry<Deck, CheckBox> entry : deckCheckBoxes.entrySet()) {
            if (entry.getValue().isSelected()) {
                selectedMap = entry.getKey().getMapMetadata();
                break;
            }
        }
        
        updateCheckBoxStates();
    }
    
    private void updateCheckBoxStates() {
        RegionMode selectedMode = modeComboBox.getValue();
        
        for (Map.Entry<Deck, CheckBox> entry : deckCheckBoxes.entrySet()) {
            Deck type = entry.getKey();
            CheckBox checkBox = entry.getValue();
            
            if (checkBox.isSelected()) continue;
            
            boolean disable = false;
            
            if (selectedMap != null && !type.getMapMetadata().equals(selectedMap)) {
                disable = true;
            }
            
            if ((selectedMode.getCapitalOrRegion() == RegionMode.CapitalOrRegion.CAPITAL ||
            		selectedMode.getCapitalOrRegion() == RegionMode.CapitalOrRegion.BOTH) && !type.hasCapital()) {
                disable = true;
            }
            
            checkBox.setDisable(disable);
        }
    }
    
    public java.util.Optional<RegionPlayConfig> showAndWait() {
        return dialog.showAndWait();
    }
    
    public record RegionPlayConfig(Set<Deck> selectedDecks, RegionMode mode) {}
}