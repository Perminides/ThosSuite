package app.shared.skin;

import app.shared.Config;

/**
 * Der helle Kachel-Skin: weiße Flächen auf fast weißem Grund, runde Ecken, keine Rahmen — was vorne
 * liegt, sagt es über seinen Schatten statt über eine Kontur.
 *
 * <p>Prototyp. Den Schatten tragen bisher nur die einfachen Bausteine (Knöpfe, Eingaben, Auswahlen,
 * Info-Felder, Kacheln, Karten des Tagebuchs, Tabellen); Karten, Diagramme und die Fenster von
 * Dialogen und Popups sind noch außen vor.</p>
 */
public class TileSkin extends Skin {

	@Override
	public String getDisplayName() {
		return "Tiles";
	}

	public TileSkin() {
		super();
		loadAllConfigs(Config.getPath("configFolder").resolve("skin_tile.properties"));
	}

}
