package app.learn.anki;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

import app.learn.anki.model.Card;
import app.shared.model.CardSortOrder;

public class CardSortService {

	public static Consumer<List<Card>> getSorter(CardSortOrder order) {
        return switch (order) {
            case RANDOM -> cards -> Collections.shuffle(cards);
            case BY_LEVEL_DESC -> cards -> Collections.sort(cards, new Comparator<Card>() {
				@Override
				public int compare(Card a, Card b) {
					int aLevel = a.getLearnStat() != null ? a.getLearnStat().getCurrentLevel() : 0;
					int bLevel = b.getLearnStat() != null ? b.getLearnStat().getCurrentLevel() : 0;
					return Integer.compare(bLevel, aLevel);
				}
			});
            case BY_LEVEL_ASC -> cards -> Collections.sort(cards, new Comparator<Card>() {
				@Override
				public int compare(Card a, Card b) {
					int aLevel = a.getLearnStat() != null ? a.getLearnStat().getCurrentLevel() : 0;
					int bLevel = b.getLearnStat() != null ? b.getLearnStat().getCurrentLevel() : 0;
					return Integer.compare(aLevel, bLevel);
				}
			});
            case BY_WRONG_COUNT_ASC -> cards -> Collections.sort(cards, new Comparator<Card>() {
				@Override
				public int compare(Card a, Card b) {
					int aLevel = a.getLearnStat() != null ? a.getLearnStat().getWrongCount() : 0;
					int bLevel = b.getLearnStat() != null ? b.getLearnStat().getWrongCount() : 0;
					return Integer.compare(aLevel, bLevel);
				}
			});
            case BY_WRONG_COUNT_DESC -> cards -> Collections.sort(cards, new Comparator<Card>() {
				@Override
				public int compare(Card a, Card b) {
					int aLevel = a.getLearnStat() != null ? a.getLearnStat().getWrongCount() : 0;
					int bLevel = b.getLearnStat() != null ? b.getLearnStat().getWrongCount() : 0;
					return Integer.compare(bLevel, aLevel);
				}
			});
        };
    } 
	
}
