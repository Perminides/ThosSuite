package app.ui.skin;

import app.shared.Config;

public class RedGradientSkin extends BaseColorSkin{
		
	@Override
	public String getDisplayName() {
		return "Drive";
	}
	
	//----------------------
	
	public RedGradientSkin() {
		super();
		loadAllConfigs(Config.get("configFolder") + "skin_drive.properties");
	}
}