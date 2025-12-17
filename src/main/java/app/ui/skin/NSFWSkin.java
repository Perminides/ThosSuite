package app.ui.skin;

import app.config.Config;

public class NSFWSkin extends BaseColorSkin{
		
	@Override
	public String getDisplayName() {
		return "NSFW";
	}
	
	//----------------------
	
	public NSFWSkin() {
		super();
		loadAllConfigs(Config.get("configFolder") + "skin_nsfw.properties");
	}
}