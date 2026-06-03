package app.shared.skin;

import app.shared.Config;

public class FlatWebSkin extends Skin {

	@Override
	public String getDisplayName() {
		return "FlatWeb";
	}
	
	public FlatWebSkin() {
		super();
		loadAllConfigs(Config.get("configFolder") + "skin_flatweb.properties");
	}
	
}
