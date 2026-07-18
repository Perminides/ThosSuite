package app.shared.ui.contracts;

import javafx.scene.Node;

/**
 * Schmale Fassade, über die der ComponentHost eine Komponente in den Scene-Graph hängt, ohne
 * ihren konkreten javafx-Typ zu kennen. Die learn-Seite hält Komponenten über ihre fachliche
 * Schnittstelle und reicht sie als SessionComponent an den Canvas — getView() ruft nur der Canvas.
 */
public interface UiComponent {
	/**
	 * Sollte nur vom Host aufgerufen werden, falls diese Komponenten von einem Host verwaltet wird.
	 * Ist niemnals aus einem Feature aufzurufen!
	 * 
	 * @return
	 */
	Node getView();
}