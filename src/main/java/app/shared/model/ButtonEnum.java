package app.shared.model;

public enum ButtonEnum {
    OK("OK"),
    CANCEL("Abbrechen"),
    RESUME("Fortsetzen"),
	GREEN("Grün"),
	YELLOW("Gelb"),
	RED("Rot"),
	SAVE("Speichern"),
	DISCARD("Verwerfen"),
	END_ANYHOW("Trotzdem beenden"),
	IMPORT("Importieren"),
	BLACKLIST("Blacklist"),
	WHITELIST("Whitelist"),
	YES("Ja"),
	NO("Nein"),
	WHOLE_SEASON("Ganze Staffel"),
	EPISODE("Nur diese Episode"),
	DONE("Habe ich getan"),
	OTHER_DIRECTION("Andere Richtung genommen"),
	LATER("Mache ich später"),
	SUNDAY("Sonntag"),
	MONDAY("Montag"),
	TUESDAY("Dienstag"),
	WEDNESDAY("Mittwoch"),
	THURSDAY("Donnerstag"),
	FRIDAY("Freitag"),
	SATURDAY("Samstag");
	

    private final String text;

    ButtonEnum(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }
}