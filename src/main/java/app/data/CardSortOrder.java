package app.data;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

// !Später auf das noch zu definierende CardInterface umswitchen, wir wollen ja alle Kartenlisten sortieren können, auch FastHints... 
public enum CardSortOrder {
	RANDOM("Zufällig") {
		@Override
		public void sort(List<AnkiCard> cards) {
			Collections.shuffle(cards);
		}
	},
	
	BY_LEVEL_DESC("Höchstes Level zuerst") {
		@Override
		public void sort(List<AnkiCard> cards) {
			Collections.sort(cards, new Comparator<AnkiCard>() {
				@Override
				public int compare(AnkiCard a, AnkiCard b) {
					int aLevel = a.getLearnStat() != null ? a.getLearnStat().getCurrentLevel() : 0;
					int bLevel = b.getLearnStat() != null ? b.getLearnStat().getCurrentLevel() : 0;
					return Integer.compare(bLevel, aLevel);
				}
			});
		}
	},
	
	BY_LEVEL_ASC("Niedrigstes Level zuerst") {
		@Override
		public void sort(List<AnkiCard> cards) {
			Collections.sort(cards, new Comparator<AnkiCard>() {
				@Override
				public int compare(AnkiCard a, AnkiCard b) {
					int aLevel = a.getLearnStat() != null ? a.getLearnStat().getCurrentLevel() : 0;
					int bLevel = b.getLearnStat() != null ? b.getLearnStat().getCurrentLevel() : 0;
					return Integer.compare(aLevel, bLevel);
				}
			});
		}
	},
	BY_WRONG_COUNT_DESC("Schwere zuerst") {
		@Override
		public void sort(List<AnkiCard> cards) {
			Collections.sort(cards, new Comparator<AnkiCard>() {
				@Override
				public int compare(AnkiCard a, AnkiCard b) {
					int aLevel = a.getLearnStat() != null ? a.getLearnStat().getWrongCount() : 0;
					int bLevel = b.getLearnStat() != null ? b.getLearnStat().getWrongCount() : 0;
					return Integer.compare(bLevel, aLevel);
				}
			});
		}
	},
	BY_WRONG_COUNT_ASC("Leichte zuerst") {
		@Override
		public void sort(List<AnkiCard> cards) {
			Collections.sort(cards, new Comparator<AnkiCard>() {
				@Override
				public int compare(AnkiCard a, AnkiCard b) {
					int aLevel = a.getLearnStat() != null ? a.getLearnStat().getWrongCount() : 0;
					int bLevel = b.getLearnStat() != null ? b.getLearnStat().getWrongCount() : 0;
					return Integer.compare(aLevel, bLevel);
				}
			});
		}
	};

	private final String displayName;

	CardSortOrder(String displayName) {
		this.displayName = displayName;
	}

	public String getDisplayName() {
		return displayName;
	}

	// Jede Enum muss diese Methode implementieren
	public abstract void sort(List<AnkiCard> cards);
}
