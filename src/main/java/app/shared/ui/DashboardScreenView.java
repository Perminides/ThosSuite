package app.shared.ui;

import java.util.ArrayList;
import java.util.List;

import app.shared.model.DashboardTileData;
import app.shared.model.DashboardTileStyle;
import app.shared.model.ScreenView;
import app.shared.skin.SkinService;
import app.shared.ui.components.SuiteBackground;
import app.shared.ui.components.DashboardTile;
import javafx.geometry.Pos;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Pane;

/**
 * Das Dashboard: eine FlowPane voller Kacheln.
 *
 * <p>Ja, ich habe überlegt die generischer zu machen. Aber am Ende ist es doch zu speziell als dass
 * ich glaube, dass man die jemals wiederverwenden kann.</p>
 *
 * <p>Bekommt vom Controller reine Daten ({@link DashboardTileData}) und baut die Kacheln selbst —
 * der Controller fasst dafür keinen javafx-Typ mehr an. Die Maße holt sich diese Klasse als Record
 * aus dem Skin und reicht sie in die Kacheln hinein; {@link DashboardTile} bleibt passiv.</p>
 */
public class DashboardScreenView implements ScreenView {

	private final FlowPane pane = new FlowPane();

	public DashboardScreenView() {
		// !Sofort: Magic Numbers
		pane.setHgap(20);
		pane.setVgap(20);
		pane.setAlignment(Pos.CENTER);
	}

	public void build(List<DashboardTileData> tiles) {
		pane.setBackground(SuiteBackground.of(SkinService.get().emptyWallpaperPath()));

		DashboardTileStyle style = SkinService.get().dashboardTileStyle();
		List<DashboardTile> built = new ArrayList<>();
		for (DashboardTileData data : tiles)
			built.add(new DashboardTile(data.value(), data.label(), style));
		pane.getChildren().setAll(built);
	}

	@Override
	public Pane getPane() {
		return pane;
	}
}
