package app.shared.ui.surfaces;

import java.util.Arrays;

import app.shared.skin.SkinService;
import app.shared.ui.components.DashboardTile;
import app.shared.ui.contracts.ScreenView;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.layout.Background;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Pane;


/**
 * Ja, ich habe überlegt die generischer zu machen. Aber am Ende ist es doch zu speziell als dass ich glaube,
 * dass man die jemals wiederverwenden kann. Nur zum Ansehen, bekommt DashboardTiles / VBox als Komponenten und ordnet die in
 * einer FlowPane an? Hm, naja ;-)
 */
public class DashboardScreenView implements ScreenView {
	
	private FlowPane pane = new FlowPane();
	
	public DashboardScreenView () {
		pane = new FlowPane();
		pane.setHgap(20);
		pane.setVgap(20);
		pane.setAlignment(Pos.CENTER);
	}
	
	public void build(DashboardTile... tiles) {
		pane.setBackground(new Background(SkinService.get().getEmptyBackgroundImage()));
		Node[] views = Arrays.stream(tiles).toArray(Node[]::new);
		pane.getChildren().setAll(views);
	}

	@Override
	public Pane getPane() {
		return pane;
	}

}
