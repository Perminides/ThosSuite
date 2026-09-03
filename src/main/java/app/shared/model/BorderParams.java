package app.shared.model;

import javafx.geometry.Insets;
import javafx.scene.paint.Color;

public record BorderParams (
		int width,
		Color color, // For TextField: When shown but not in focus
		Insets insets,
		int arc,

		//Use factory methods if you do not want to specify all of the following parameters
		int focusWidth, // You can control the width of the outer(!) focus border in all themes with focusWidth
		Color focusedColor, // TextFields are probably in focus most of the time?
		Color disabledColor // For TextField: No input expected.
		) {
	
	 public static BorderParams of(int width, Color color) {
	        return new BorderParams(width, color, new Insets(0, 0, 0, 0), 0, 0, color, color);
	    }
	 
	 public static BorderParams of(int width, Color color, int arc) {
	        return new BorderParams(width, color, new Insets(0, 0, 0, 0), arc, 0, color, color);
	    }
	 
	 public static BorderParams of(int width, Color color, Insets insets, int arc) {
	        return new BorderParams(width, color, insets, arc, 0, color, color);
	    }
	 
	 public static BorderParams of(int width, Color color, Insets insets, int arc, int focusWidth, Color focusedColor, Color disabledColor) {
	        return new BorderParams(width, color, insets, arc, 0, focusedColor, disabledColor);
	    } 
	 
	 /**
	  * Füllt fehlende Farben aus einem Ersatzwert.
	  *
	  * <p>Bleibt der Farb-Slot in den properties leer ({@code 1,,10,20,10,20,10}), steht hier
	  * {@code null} — dann greift die globale {@code borderColor}. Wer eine Farbe angibt, behält sie,
	  * auch wenn sie von der globalen abweicht: ein Skin mit unterschiedlich gefärbten Rändern bleibt
	  * damit möglich.</p>
	  *
	  * <p>Aufgelöst wird das <b>nicht</b> beim Parsen, sondern erst im Vorgaben-Durchlauf von
	  * {@code buildCss}. Der Loader läuft die Felder in Deklarationsreihenfolge ab und
	  * {@code borderColor} steht dort <em>nach</em> den BorderParams; bei einem abgeleiteten Skin
	  * kommt sie womöglich sogar erst aus der Eltern-Datei. Beim Parsen wäre sie also noch nicht da.</p>
	  */
	 public BorderParams withFallbackColor(Color fallback) {
	        if (color != null && focusedColor != null && disabledColor != null)
	            return this;
	        return new BorderParams(width,
	                color != null ? color : fallback,
	                insets, arc, focusWidth,
	                focusedColor != null ? focusedColor : fallback,
	                disabledColor != null ? disabledColor : fallback);
	 }

	 /**public BorderParams withInsets(Insets newInsets) {
	        return of(this.width, this.color, newInsets, this.arc);
	 }**/
}
