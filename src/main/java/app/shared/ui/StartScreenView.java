package app.shared.ui;

import app.shared.model.ScreenView;
import app.shared.skin.SkinService;
import app.shared.ui.components.SuiteBackground;
import javafx.scene.layout.Pane;

/**
 * Der Startbildschirm: nichts als ein Hintergrundbild.
 *
 * <p>Er ist der einzige Bildschirm, der das geschmückte Wallpaper des Skins zeigt — überall sonst
 * liegt Inhalt darüber, hier nicht.</p>
 */
public class StartScreenView implements ScreenView {

	private final Pane pane = new Pane();

	public StartScreenView() {
		rebuild();
	}

	/** Neu aufbauen — nötig nach einem Skinwechsel. */
	public void rebuild() {
		pane.setBackground(SuiteBackground.of(SkinService.get().startScreenWallpaperPath()));
	}

	@Override
	public Pane getPane() {
		return pane;
	}
}
