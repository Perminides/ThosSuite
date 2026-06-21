package app.shared.skin;

import app.shared.Config;

public class SpicySkin extends BaseColorSkin{
		
	@Override
	public String getDisplayName() {
		return "Spicy";
	}
	
	//----------------------
	
	public SpicySkin() {
		super();
		loadAllConfigs(Config.getPath("configFolder").resolve("skin_spicy.properties"));
	}
}