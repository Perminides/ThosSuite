package app.shared.skin;

import app.shared.Config;

public abstract class BaseColorSkin extends Skin {

	public BaseColorSkin() {
		super();
		loadAllConfigs(Config.getPath("configFolder").resolve("skin_basecolor.properties"));
	}
	
}
