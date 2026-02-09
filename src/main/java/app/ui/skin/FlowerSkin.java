package app.ui.skin;

import app.config.Config;

public class FlowerSkin extends BaseColorSkin {
	@Override
	public String getDisplayName() {
		return "Flower";
	}
	
	public FlowerSkin() {
		super();
		loadAllConfigs(Config.get("configFolder") + "skin_flower.properties");
	}
}
