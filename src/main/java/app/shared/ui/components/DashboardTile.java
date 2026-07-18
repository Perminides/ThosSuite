package app.shared.ui.components;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class DashboardTile extends VBox {
    
    private static final double TILE_WIDTH = 450;
    private static final double TILE_HEIGHT = 400;
    private static final double TOP_HEIGHT = 300;
    private static final double BOTTOM_HEIGHT = 100;
    
    private final Label topLabel;
    private final Label bottomLabel;
    
    public DashboardTile(String topValue, String bottomText) {
        this.topLabel = new Label(topValue);
        this.bottomLabel = new Label(bottomText);
        
        buildUi();
    }
    
    private void buildUi() {
        // Tile selbst
        setPrefSize(TILE_WIDTH, TILE_HEIGHT);
        setMinSize(TILE_WIDTH, TILE_HEIGHT);
        setMaxSize(TILE_WIDTH, TILE_HEIGHT);
        getStyleClass().add("dashboard-tile");
        
        // Oberer Bereich (große Zahl)
        VBox topBox = new VBox(topLabel);
        topBox.setPrefHeight(TOP_HEIGHT);
        topBox.setAlignment(Pos.CENTER);
        topBox.getStyleClass().add("dashboard-tile-top");
        
        topLabel.getStyleClass().add("dashboard-tile-value");
        
        // Unterer Bereich (Beschreibung)
        VBox bottomBox = new VBox(bottomLabel);
        bottomBox.setPrefHeight(BOTTOM_HEIGHT);
        bottomBox.setAlignment(Pos.CENTER);
        bottomBox.getStyleClass().add("dashboard-tile-bottom");
        
        bottomLabel.getStyleClass().add("dashboard-tile-label");
        bottomLabel.setWrapText(true);
        bottomLabel.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        
        getChildren().addAll(topBox, bottomBox);
    }
}