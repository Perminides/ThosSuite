package app.ui.skin;

import app.shared.Config;

// !Sofort: Leider ist DarkMode sauhässlich. Da musst Du nochmal ran...
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
