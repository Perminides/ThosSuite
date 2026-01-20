package app.data.persistence.anki;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import app.config.Config;
import app.data.AnkiCard;
import app.data.DeckCategory;
import app.data.Deck;

class CsvAnkiDeckCardSource {
		private final Map<Deck, File> bundles = new EnumMap<>(Deck.class);
		
		CsvAnkiDeckCardSource() {
			File dataDir = new File(Config.get("deckFolder"));
			for (Deck type : Deck.values()) {
				if (type.getCategory() == DeckCategory.ANKI_DECK)
					bundles.put(type, new File(dataDir, type.getDeckFileName()));
			}
		}
		
		List<AnkiCard> loadAll(Deck type) {
	        List<AnkiCard> result = new ArrayList<>();
	        File deckFile = bundles.get(type);

	        // Das Set merkt sich alle IDs, die wir in diesem Durchlauf schon gesehen haben
	        Set<Integer> seenIds = new HashSet<>();

	        // try-with-resources schließt den Reader automatisch am Ende
	        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(deckFile), Charset.forName("UTF-8")))) {
	            br.lines().skip(1).forEach(p -> {
	                String[] tokens = p.split(";");
	                
	                // 1. Wir bauen erst das Karten-Objekt (dabei wird die ID geparst)
	                AnkiCard card = new AnkiCard(Arrays.asList(tokens));
	                
	                // 2. add() liefert 'false', wenn die ID schon drin war!
	                if (!seenIds.add(card.getId())) {
	                    throw new RuntimeException("Daten-Fehler: Doppelte ID " + card.getId() + " in Datei " + deckFile.getName() + " gefunden!");
	                }
	                
	                // 3. Wenn alles gut ging, ab in die Ergebnisliste
	                result.add(card);
	            });
	        } catch (Exception e) {
	            throw new RuntimeException("Fehler beim Lesen von " + deckFile, e);
	        }
	        return result;
	    }
	}