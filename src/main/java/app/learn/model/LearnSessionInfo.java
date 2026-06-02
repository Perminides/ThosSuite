package app.learn.model;

/**
 * Wie viel Karten waren heute fällig und wie viel sind noch fällig? Für die Anzeige im Menü.
 */
public abstract class LearnSessionInfo {
    public abstract String formatForMenu();
    //public abstract DeckCategory getCategory();
    public abstract boolean isStillDueToday();
}