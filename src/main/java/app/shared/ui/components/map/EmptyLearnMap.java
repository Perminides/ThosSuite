package app.shared.ui.components.map;

import java.util.Set;

import javafx.scene.Group;
import javafx.scene.Node;

/**
 * Die Karte einer Lernform, die keine hat — die MC-Session.
 *
 * <p>Statt überall {@code if (karte != null)} zu schreiben, gibt es hier eine Karte, die nichts tut.
 * Sie ist eine leere {@link Group}: hängt im Szenengraphen, zeichnet nichts und nimmt in der absolut
 * positionierenden Host-Pane keinen Platz.</p>
 */
public class EmptyLearnMap extends Group implements LearnMap {

	@Override public void reset()                           { }
	@Override public void setActive(boolean active)         { }
	@Override public void markCorrect(Set<String> ids)      { }
	@Override public void markIncorrect(String id)          { }
	@Override public void setClickTargets(Set<String> ids)  { }
	@Override public void mark(Set<String> ids)             { }

	@Override public Node getView() { return this; }
}
