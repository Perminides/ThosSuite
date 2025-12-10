package app.ui.skin.params;

import java.awt.Insets;

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
	 
	 /**public BorderParams withInsets(Insets newInsets) {
	        return of(this.width, this.color, newInsets, this.arc);
	 }**/
}
