package app.ui.components;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import app.data.DeckCategory;
import app.data.DeckType;
import app.data.MapMetadata;
import app.data.RegionMode;
import app.misc.CssInspector;
import app.ui.skin.Skin;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

// Sofort: Ist halt überhaupt noch nicht gestylet nach meinem Skin!
public class RegionPlayConfigDialog {
    
    private final Dialog<RegionPlayConfig> dialog;
    private final Map<DeckType, CheckBox> deckCheckBoxes = new EnumMap<>(DeckType.class);
    private final ComboBox<RegionMode> modeComboBox = new ComboBox<>();
    private MapMetadata selectedMap = null;

    @SuppressWarnings("unchecked")
    public RegionPlayConfigDialog(Window parent, Skin skin) {
        this.dialog = (Dialog<RegionPlayConfig>) skin.createDialog(parent, "Filter");
        
        dialog.setTitle("Regionen spielen");
        
        VBox mainContent = new VBox(15);
        mainContent.getStyleClass().add("my-dialog-vbox");
        mainContent.setPadding(new Insets(20));
        mainContent.setAlignment(Pos.TOP_CENTER);
        
        // Mode-Auswahl (mittig oben)
        modeComboBox.getItems().addAll(RegionMode.values());
        modeComboBox.setValue(RegionMode.CLICK_CITY_BLANK);
        modeComboBox.setOnAction(_ -> updateCheckBoxStates());        
        mainContent.getChildren().add(modeComboBox);
        
        // Decks gruppieren
        Map<MapMetadata, List<DeckType>> mapGroups = new HashMap<>();
        for (DeckType type : DeckType.values()) {
            if (type.getCategory() != DeckCategory.REGION_DECK) continue;
            mapGroups.computeIfAbsent(type.getMapMetadata(), _ -> new ArrayList<>()).add(type);
        }
        
        // Gruppen erstellen
        List<List<DeckType>> groups = new ArrayList<>();
        List<DeckType> exclusiveGroup = new ArrayList<>();
        
        for (Map.Entry<MapMetadata, List<DeckType>> entry : mapGroups.entrySet()) {
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
        groups.sort(Comparator.comparingInt(List<DeckType>::size).reversed());
        
        // Horizontal Layout
        HBox groupsContainer = new HBox(20);
        groupsContainer.setAlignment(Pos.TOP_CENTER);
        
        for (List<DeckType> group : groups) {
            VBox groupBox = new VBox(5);
            groupBox.setAlignment(Pos.TOP_LEFT);
            
            group.sort(Comparator.comparing(DeckType::getDisplayName));
            for (DeckType type : group) {
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
                Set<DeckType> selectedDecks = new HashSet<>();
                for (Map.Entry<DeckType, CheckBox> entry : deckCheckBoxes.entrySet()) {
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
        for (Map.Entry<DeckType, CheckBox> entry : deckCheckBoxes.entrySet()) {
            if (entry.getValue().isSelected()) {
                selectedMap = entry.getKey().getMapMetadata();
                break;
            }
        }
        
        updateCheckBoxStates();
    }
    
    private void updateCheckBoxStates() {
        RegionMode selectedMode = modeComboBox.getValue();
        
        for (Map.Entry<DeckType, CheckBox> entry : deckCheckBoxes.entrySet()) {
            DeckType type = entry.getKey();
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
    
    public record RegionPlayConfig(Set<DeckType> selectedDecks, RegionMode mode) {}
}