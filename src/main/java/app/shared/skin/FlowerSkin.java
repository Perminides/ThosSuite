package app.shared.skin;

import app.shared.Config;

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
