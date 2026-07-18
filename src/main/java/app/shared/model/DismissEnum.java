package app.shared.model;

public enum DismissEnum {
    NORMAL,     // ESC schließt, X schließt
    NO_ESC,     // ESC blockiert, X erlaubt
    NO_X,       // ESC erlaubt, X blockiert
    MANDATORY   // ESC + X blockiert
}