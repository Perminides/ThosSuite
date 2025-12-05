package app.ui.panels;

import java.util.List;
import java.util.Set;

import app.data.LearnStat;
import app.data.SessionProgress;

public interface AnkiSessionPanel extends DeckSessionPanel{

	public void show();
	
	public void setMcSolution(Set<Integer> correctIds);
	public void setMcCorrect(int id, boolean correct);
	public void disableMcPanel();
	public void setMCPanelActive(boolean active);
	public void setMultipleChoice(List<String> answers);
	
	public void setImage(String imagePath);
	
	public void setQuestion(String text);
	
	public void sessionProgressChanged(SessionProgress progress);
	public void updateCardStats(LearnStat stats);
	
	public default void setTextFieldActive(boolean active) {};
	public default void setTextInTextField(String text) {}
	
	// Alle folgenden nicht von Multiple Choice implementiert...
	public default void beginTx() {};
	public default void endTx() {};
	public default void setMapActive(boolean active) {};
	public default void setIdsInQuestion(Set<String> idsInQuestion) {}; // Deutschland nicht implementiert.
	public default void setIdToIncorrect(String element) {};
	public default void addIdsToCorrect(Set<String> elements) {}; // Z.B. nach falschem Click. Oder auch einzeln nach richtigem Click.
	public default void setMarkedIds(Set<String> left) {};
	public default void resetMarkers() {};
	
}
