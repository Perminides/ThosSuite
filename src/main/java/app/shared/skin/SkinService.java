package app.shared.skin;

import java.util.ArrayList;
import java.util.List;

import app.shared.Config;

public final class SkinService {

	// 1. Liste muss veränderbar (mutable) sein wegen des refreshs, daher wrapping in ArrayList
    private static final List<Skin> AVAILABLE_SKINS = new ArrayList<>(List.of(
            new BlueGradientSkin(),
            new RedGradientSkin(),
            new FlatWebSkin(),
            new FlowerSkin(),
            new SpicySkin(),
            new DarkMode()
    ));
    private static Skin current = AVAILABLE_SKINS.get(0); // !Erweiterung Später über config-Datei...
    
    // Statischer Initializer lädt gespeichertes Skin
    static {
        String savedSkinClass = Config.get("pref.skinClass");
        
        if (savedSkinClass != null) {
            // Suche Skin in der Liste
            current = null;
            for (Skin skin : AVAILABLE_SKINS)
                if (skin.getClass().getSimpleName().equals(savedSkinClass)) {
                    current = skin;
                    break;
                }
            if (current == null)
                throw new RuntimeException("Ungültiges Skin in der Konfiguration: " + savedSkinClass);
        } else {
            // Fallback auf erstes Skin
            current = AVAILABLE_SKINS.get(0);
        }
    }

    private SkinService() {} // keine Instanz erlaubt

    public static void set(Skin skin) {
    	if (current == skin)
    		return;
    	
        current = skin;
        Config.set("pref.skinClass", skin.getClass().getSimpleName());
    }

    public static Skin get() {
        return current;
    }
    
    public static List<Skin> getAllSkins() {
        return AVAILABLE_SKINS;
    }
    
 // NEU: Die Methode für den Refresh
    public static void refresh() {
        try {
            // 1. Index finden
            int index = AVAILABLE_SKINS.indexOf(current);
            if (index == -1)
            	throw new RuntimeException("What the heck?");
            
            // 2. Neue Instanz der aktuellen Skin-Klasse erzeugen (ruft Konstruktor & loadConfig neu auf)
            Skin newInstance = current.getClass().getDeclaredConstructor().newInstance();
            
            // 3. In der Liste austauschen
            AVAILABLE_SKINS.set(index, newInstance);
            
            // 4. Als aktuell setzen
            current = newInstance;
            
        } catch (Exception e) {
            // Fangen wir generisch, da Reflection viele Exceptions werfen kann (Instantiation, IllegalAccess, etc.)
            throw new RuntimeException("Fehler beim Reload des Skins: " + current.getClass().getSimpleName(), e);
        }
    }
}
