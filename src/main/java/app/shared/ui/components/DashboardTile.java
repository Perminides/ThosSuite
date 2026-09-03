package app.shared.ui.components;

import app.shared.model.DashboardTileStyle;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/**
 * Eine Dashboard-Kachel: große Zahl oben, Beschriftung unten.
 *
 * <p>Passiver Baustein — die Maße kommen als {@link DashboardTileStyle} herein, die Kachel holt
 * sich nichts. Starr: pref, min und max sind identisch.</p>
 */
public class DashboardTile extends VBox {

    private final Label topLabel;
    private final Label bottomLabel;

    public DashboardTile(String topValue, String bottomText, DashboardTileStyle style) {
        this.topLabel = new Label(topValue);
        this.bottomLabel = new Label(bottomText);

        buildUi(style);
    }

    private void buildUi(DashboardTileStyle style) {
        // Tile selbst
        setPrefSize(style.width(), style.height());
        setMinSize(style.width(), style.height());
        setMaxSize(style.width(), style.height());
        getStyleClass().add("dashboard-tile");
        
        // Oberer Bereich (große Zahl)
        VBox topBox = new VBox(topLabel);
        topBox.setAlignment(Pos.CENTER);
        topBox.getStyleClass().add("dashboard-tile-top");
        
        topLabel.getStyleClass().add("dashboard-tile-value");
        
        // Unterer Bereich (Beschreibung)
        VBox bottomBox = new VBox(bottomLabel);
        bottomBox.setAlignment(Pos.CENTER);
        bottomBox.getStyleClass().add("dashboard-tile-bottom");
        
        bottomLabel.getStyleClass().add("dashboard-tile-label");
        bottomLabel.setWrapText(true);
        bottomLabel.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        
        getChildren().addAll(topBox, bottomBox);
    }
}