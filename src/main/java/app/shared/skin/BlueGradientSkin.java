package app.shared.skin;

import app.shared.Config;

public class BlueGradientSkin extends BaseColorSkin{
		
	@Override
	public String getDisplayName() {
		return "Moonlight";
	}
	
	//----------------------
	
	public BlueGradientSkin() {
		super();
		loadAllConfigs(Config.getPath("configFolder").resolve("skin_moonlight.properties"));
	}
}