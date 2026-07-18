package app.shared.model;

public enum SelectionEnum {
    ZERO, ONE, TWO, THREE, FOUR, FIVE;

    public int index() { return ordinal(); }
    public static SelectionEnum of(int index) { return values()[index]; }
}