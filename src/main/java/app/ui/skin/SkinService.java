package app.ui.skin;

import java.util.List;

public final class SkinService {
	
	// !Erweiterung: Eine refresh-Möglichkeit einbauen, die die props neu einliest. Sollte aber nicht per se bei jedem skinwechsel passieren, oder vielleicht doch?

    private static final List<Skin> AVAILABLE_SKINS = List.of(
            new BlueGradientSkin(),
            new RedGradientSkin(),
            new FlatWebSkin(),
            new SpicySkin(),
            new DarkMode()
            // Neuer Skin? Eine Zeile hier hinzufügen!
        );
    private static Skin current = AVAILABLE_SKINS.get(0); // !Erweiterung Später über config-Datei...
    static {
        // Beim Klassenloading ersten Skin aktivieren
        current = AVAILABLE_SKINS.get(0);
    }

    private SkinService() {} // keine Instanz erlaubt

    public static void set(Skin skin) {
    	if (current == skin)
    		return;
    	
        current = skin;
    }

    public static Skin get() {
        return current;
    }
    
    public static List<Skin> getAllSkins() {
        return AVAILABLE_SKINS;
    }
}
