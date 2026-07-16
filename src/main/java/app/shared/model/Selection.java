package app.shared.model;

public enum Selection {
    ZERO, ONE, TWO, THREE, FOUR, FIVE;

    public int index() { return ordinal(); }
    public static Selection of(int index) { return values()[index]; }
}