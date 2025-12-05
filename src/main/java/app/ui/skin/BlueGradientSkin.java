package app.ui.skin;

import app.config.Config;

public class BlueGradientSkin extends BaseColorSkin{
		
	@Override
	public String getDisplayName() {
		return "Moonlight";
	}
	
	//----------------------
	
	public BlueGradientSkin() {
		super();
		loadAllConfigs(Config.get("configFolder") + "skin_moonlight.properties");
	}
}