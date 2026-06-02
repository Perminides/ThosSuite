package app.ui.skin;

import app.shared.Config;

public abstract class BaseColorSkin extends Skin {

	public BaseColorSkin() {
		super();
		loadAllConfigs(Config.get("configFolder") + "skin_basecolor.properties");
	}
	
}
