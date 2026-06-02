package app.shared.model;
public enum SessionSwitchStrategy {
    IMMEDIATE,       // Sofort wechseln (z.B. Play Session, oder frisch gestartet)
    OFFER_SAVE,      // Dialog: "Speichern und beenden?" (Anki)
    CONFIRM_DISCARD  // Dialog: "Fortschritt geht verloren! Wirklich abbrechen?" (Region)
}