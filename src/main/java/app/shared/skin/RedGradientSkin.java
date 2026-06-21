package app.shared.skin;

import app.shared.Config;

public class RedGradientSkin extends BaseColorSkin{
		
	@Override
	public String getDisplayName() {
		return "Drive";
	}
	
	//----------------------
	
	public RedGradientSkin() {
		super();
		loadAllConfigs(Config.getPath("configFolder").resolve("skin_drive.properties"));
	}
}