package app.learn.anki.repository;

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

import app.learn.anki.model.Card;
import app.learn.model.Deck;
import app.learn.model.DeckCategory;
import app.shared.Config;

class CsvDeckCardSource {
		// Der Deck-Dateiname darf eine mit ';' getrennte Liste sein — dann wird jede Datei geladen.
		private final Map<Deck, List<File>> bundles = new EnumMap<>(Deck.class);

		CsvDeckCardSource() {
			File dataDir = Config.getPath("deckFolder").toFile();
			for (Deck type : Deck.values()) {
				if (type.getCategory() == DeckCategory.ANKI_DECK) {
					List<File> files = new ArrayList<>();
					for (String name : type.getDeckFileName().split(";"))
						files.add(new File(dataDir, name.trim()));
					bundles.put(type, files);
				}
			}
		}
		
		List<Card> loadAll(Deck type) {
	        List<Card> result = new ArrayList<>();
	        // Das Set merkt sich alle IDs über ALLE Dateien des Decks — eine Kollision fällt sofort auf.
	        Set<Integer> seenIds = new HashSet<>();
	        for (File deckFile : bundles.get(type))
	            readInto(deckFile, result, seenIds);
	        return result;
	    }

		private void readInto(File deckFile, List<Card> result, Set<Integer> seenIds) {
	        // try-with-resources schließt den Reader automatisch am Ende
	        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(deckFile), Charset.forName("UTF-8")))) {
	            br.readLine(); // Kopfzeile ueberspringen
	            String line;
	            while ((line = br.readLine()) != null) {
	                if (line.isBlank())
	                    continue;
	                String[] tokens = line.split(";");

	                // 1. Wir bauen erst das Karten-Objekt (dabei wird die ID geparst)
	                Card card = new Card(Arrays.asList(tokens));

	                // 2. add() liefert 'false', wenn die ID schon drin war!
	                if (!seenIds.add(card.getId())) {
	                    throw new RuntimeException("Daten-Fehler: Doppelte ID " + card.getId() + " in Datei " + deckFile.getName() + " gefunden!");
	                }

	                // 3. Wenn alles gut ging, ab in die Ergebnisliste
	                result.add(card);
	            }
	        } catch (Exception e) {
	            throw new RuntimeException("Fehler beim Lesen von " + deckFile, e);
	        }
	    }
	}