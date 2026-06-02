package app.ui.skin;

import app.shared.Config;

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