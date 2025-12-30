package app.controller;

import app.data.CardSortOrder;
import javafx.scene.layout.Pane;

public interface Session {

	public void start();
	public void cancel();
	public void end();
	public void refresh();
	public Pane getView();
	
	public default void endPause() {};
	public default void sort(CardSortOrder order) {};
}
