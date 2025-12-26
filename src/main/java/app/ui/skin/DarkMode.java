package app.ui.skin;

import app.config.Config;

public class DarkMode extends Skin {

	@Override
	public String getDisplayName() {
		return "Dark Mode";
	}
	
	public DarkMode() {
		super();
		loadAllConfigs(Config.get("configFolder") + "skin_darkmode.properties");
	}
	
}
