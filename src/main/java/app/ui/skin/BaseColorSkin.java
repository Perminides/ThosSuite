package app.ui.skin;

import app.config.Config;

public abstract class BaseColorSkin extends Skin {

	public BaseColorSkin() {
		super();
		loadAllConfigs(Config.get("configFolder") + "skin_basecolor.properties");
	}
	
}
