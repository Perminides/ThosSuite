package app.controller;

import app.data.CardSortOrder;

public interface Session {

	public void start();
	public void cancel();
	public void end();
	public void refresh();
	
	public default void endPause() {};
	public default void sort(CardSortOrder order) {}; // !Architektur: Wieso Card?
}
