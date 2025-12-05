package app.data.persistence.anki;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import app.config.Config;
import app.data.AnkiCard;
import app.data.DeckCategory;
import app.data.DeckType;

class CsvAnkiDeckCardSource {
		private final Map<DeckType, File> bundles = new EnumMap<>(DeckType.class);
		
		CsvAnkiDeckCardSource() {
			File dataDir = new File(Config.get("deckFolder"));
			for (DeckType type : DeckType.values()) {
				if (type.getCategory() == DeckCategory.ANKI_DECK)
					bundles.put(type, new File(dataDir, type.getDeckFileName()));
			}
		}
		
		// !Sofort: Doppelte und fehlende IDs sind schon kein so ganz seltener Fehler. Die sollten dann auch ne Exception werfen. Tun sie das?
		List<AnkiCard> loadAll(DeckType type) {
			List<AnkiCard> result = new ArrayList<AnkiCard>();
			File deckFile = bundles.get(type);
			try  {
				BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(deckFile), Charset.forName("Cp1252")));
				br.lines().skip(1).forEach(p -> {
					String[] tokens = p.split(";");
					result.add(new AnkiCard(Arrays.asList(tokens)));
				});
				br.close();
			} catch (Exception e) {
				throw new RuntimeException("Fehler beim Lesen von " + deckFile, e);
			}
			return result;
		}
	}