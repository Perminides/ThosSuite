package app.learn.anki.model;

import java.util.List;
import java.util.Set;

import javafx.scene.layout.Pane;

public interface SessionPane {

	public Pane asPane();

	public void setMcSolution(Set<Integer> correctIds);
	public void setMcCorrect(int id, boolean correct);
	public void disableMcPanel();
	public void setMultipleChoice(List<String> answers);

	public void setImage(String imagePath);
	public void setQuestion(String text);

	public void setProgressText(String text);
	public void setCardHistoryText(String text);

	public default void setTextFieldActive(boolean active) {};
	public default void setTextInTextField(String text) {}

	// Alle folgenden nicht von Multiple Choice implementiert...
	public default void beginTx() {};
	public default void endTx() {};
	public default void setMapActive(boolean active) {};
	public default void setIdsInQuestion(Set<String> idsInQuestion) {};
	public default void setIdToIncorrect(String element) {};
	public default void addIdsToCorrect(Set<String> elements) {};
	public default void setMarkedIds(Set<String> left) {};
	public default void resetMarkers() {};
}