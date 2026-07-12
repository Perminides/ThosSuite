package app.shared.skin;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import javafx.scene.image.Image;

/**
 * Cacht große, skin-abhängige Bilder — und zwar nur für den aktuell aktiven Skin. Wechselt der Skin, wird der Cache
 * geleert (das alte, evtl. dreistellige MB-Bild wird freigegeben).
 *
 * <p>Bewusst generisch gehalten: die Klasse weiß nichts von Karten, Decks oder learn. Sie nimmt einen {@link Path}
 * herein und gibt ein {@link Image} zurück — mehr nicht. Damit importiert sie kein Feature-Paket (Regel 1:
 * zirkelfrei) und steht jedem Feature offen, das später große skin-abhängige Bilder braucht.</p>
 *
 * <p>Diese Klasse ist die einzige Stelle, an der solche Bilder von der Platte gelesen werden. Früher lag das
 * Laden im MapRepository und die Invalidierung im MapService — beides ist hierher konsolidiert.</p>
 */
public class SkinImageCache {

	private static SkinImageCache instance;

	private final Map<Path, Image> cache = new HashMap<>();
	private Skin cachedSkin;

	private SkinImageCache() {
	}

	public static SkinImageCache getInstance() {
		if (instance == null)
			instance = new SkinImageCache();
		return instance;
	}

	/**
	 * Liefert das Bild zum Pfad (gecacht). {@code null}-Pfad → {@code null} (Karte ohne inaktive Variante).
	 * Existiert die Datei nicht, ebenfalls {@code null} — dieselbe stille Semantik wie früher im MapRepository,
	 * bewusst so belassen (siehe Chat: gehört bei Gelegenheit auf FailFast geprüft).
	 */
	public Image get(Path path) {
		invalidateIfSkinChanged();
		if (path == null)
			return null;

		Image cached = cache.get(path);
		if (cached != null)
			return cached;

		Image loaded = load(path);
		if (loaded != null)
			cache.put(path, loaded);
		return loaded;
	}

	/**
	 * Lädt das Bild vorab in den Cache (Splash-Preload). Gibt bewusst nichts zurück, damit Aufrufer wie der
	 * MapService warmlaufen können, ohne ein {@link Image} anzufassen — sie bleiben so javafx-frei.
	 */
	public void warm(Path path) {
		get(path);
	}

	private void invalidateIfSkinChanged() {
		Skin current = SkinService.get();
		if (current != cachedSkin) {
			cache.clear();
			cachedSkin = current;
		}
	}

	private Image load(Path path) {
		File file = path.toFile();
		if (!file.exists())
			return null;

		// Image(InputStream) lädt synchron (wichtig, damit es sofort da ist).
		try (InputStream is = new FileInputStream(file)) {
			return new Image(is);
		} catch (IOException e) {
			throw new RuntimeException("Bild konnte nicht geladen werden: " + path, e);
		}
	}
}