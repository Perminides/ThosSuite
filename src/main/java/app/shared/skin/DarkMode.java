package app.shared.skin;

import app.shared.Config;

public class DarkMode extends Skin {

	@Override
	public String getDisplayName() {
		return "Dark Mode";
	}
	
	public DarkMode() {
		super();
		loadAllConfigs(Config.getPath("configFolder").resolve("skin_darkmode.properties"));
	}
	
}
