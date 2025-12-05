package app.data.persistence;

import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import app.config.Config;
import app.data.DeckType;
import app.data.GeoMap;
import app.data.MapMetadata;
import app.data.MapShape;
import app.data.MapType;
import app.ui.skin.Skin;

/**
 * Sollte nur(!) vom MapService benutzt werden!
 */
public class MapRepository {
    private final GeoJsonLoader loader;
    
    public MapRepository() {
        this.loader = new GeoJsonLoader();
    }
    
    public GeoMap load(DeckType type, Skin skin) {
    	MapMetadata meta = type.getMapMetadata();
        if (meta.getMapType() == MapType.SHAPE) {
        	
        	// 3. Laden
        	String geoJsonPath = Config.get("geoJsonFolder") + meta.getGeoJsonFiles()[0];
        	List<MapShape> shapes = loader.load(geoJsonPath);
        	return new GeoMap(shapes, MapType.SHAPE, null, null, null, null);
        } else {
        	try {
        		// !Sofort. Also a) musst Du Landkarten als png speichern. Oops, mache ich ja bereits. Du könntest auch bmp versuchen *lol*
        		// !Sofort b) musst Du verstehen, was hier mit getCompatibleIImage gemacht wird und das dann entfernen und das bIld schon kompatibel speichern!
        		// !Sofort c) musst Du die Zeiten messen, um naja, sollte klar sein warum, oder?
        		BufferedImage bg = getCompatibleImage(ImageIO.read(new File(skin.getMapImagePath(type))));
        		BufferedImage ov = getCompatibleImage(skin.getMapOverlayImagePath(type) == null ? null : ImageIO.read(new File(skin.getMapOverlayImagePath(type))));
        		BufferedImage bgia = getCompatibleImage(skin.getMapInactiveImagePath(type) == null ? null : ImageIO.read(new File(skin.getMapInactiveImagePath(type))));
        		BufferedImage ovia = getCompatibleImage(skin.getMapInactiveOverlayImagePath(type) == null ? null : ImageIO.read(new File(skin.getMapInactiveOverlayImagePath(type))));
        		
        		String folder = Config.get("geoJsonFolder");
        		String[] fileNames = meta.getGeoJsonFiles();
        		List<MapShape> shapes = new ArrayList<>();
        		for (String fileName : fileNames) {
        			shapes.addAll(loader.load(folder + fileName));
        		}
        		return new GeoMap(shapes, MapType.IMAGE, bg, ov, bgia, ovia);
        	} catch (Exception e) {
        		throw new RuntimeException("Probs beim Laden der MapBilder", e);
        	}
        }
    }
    
    /**
     * Das hier hat das Zeichnen des Hintergrundbildes zu Beginn der ersten Session 
     * von 280 ms max auf 80 ms max gedrosselt.
     * Noch mehr scheint der Schalter Dsun.java2d.d3d=false gebracht zu haben.
     * Ob das hier mit dem Schalter überhaupt noch notwendig ist, habe ich nicht getestet.
     * Es schadet sicher auch nicht.
     *  
     * @param bi
     * @return
     */
    private BufferedImage getCompatibleImage(BufferedImage bi) {
    	// Hole kompatibles Format vom Bildschirm
		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		GraphicsDevice gd = ge.getDefaultScreenDevice();
		GraphicsConfiguration gc = gd.getDefaultConfiguration();

		// Erstelle kompatibles Bild
		BufferedImage compatible = gc.createCompatibleImage(
				bi.getWidth(), 
				bi.getHeight(), 
		    Transparency.TRANSLUCENT  // Weil ABGR Alpha hat
		);

		// Kopiere Inhalt
		Graphics2D g = compatible.createGraphics();
		g.drawImage(bi, 0, 0, null);
		g.dispose();
		return compatible;
    }
}