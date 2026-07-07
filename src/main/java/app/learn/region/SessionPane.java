package app.learn.region;

import java.util.Set;

import app.learn.MapService;
import app.learn.ShapeMapPane;
import app.learn.ShapeMapPane.ShapeMapState;
import app.learn.model.Deck;
import app.learn.model.GeoMap;
import app.shared.skin.Skin;
import app.shared.skin.SkinService;
import app.shared.ui.SessionInfoLabel;
import javafx.scene.control.TextField;
import javafx.scene.layout.Background;
import javafx.scene.layout.Pane;

public class SessionPane extends Pane {
	private final SessionPresenter presenter;
	private final Deck deckType;
    private ShapeMapPane karte;
    private SessionInfoLabel questionArea;
    private TextField textInputField;
	
	public SessionPane(SessionPresenter presenter, Deck deckType, boolean questionAreaVisible) {
        this.presenter = presenter;
        this.deckType = deckType;
        this.setBackground(new Background(SkinService.get().getBackgroundImage(deckType.getMapName(), deckType.getCategory().toString())));
        initUI(questionAreaVisible);
	}
	
	private void initUI(boolean questionAreaVisible) {
        Skin skin = SkinService.get();
        
        GeoMap map = MapService.getInstance().getMap(deckType);  // holt sich die SessionPane selbst (sie ist learn)
        karte = new ShapeMapPane(map, deckType.getMapName(), deckType.getCategory().toString());
        getChildren().add(karte.getView());
        karte.setListener(id -> mapElementClicked(id));
    	
    	// !Sofort: Hier wäre allerdings CENTER schon angesagt
    	if (questionAreaVisible) {
        	questionArea = skin.createSessionInfoLabel(deckType.getMapName(), deckType.getCategory().toString(), Skin.TextLabelType.QUESTION);
        	questionArea.setText(""); // Initial leer
            getChildren().add(questionArea);
    	} else {
            textInputField = skin.createInputField(deckType.getMapName(), deckType.getCategory().toString());
            textInputField.setOnKeyReleased(_ -> textInputChanged());
            getChildren().add(textInputField);
    	}
    }
	
	/**
	 * ----- MAP -----
	 */
    
	public void addIdsToActive(Set<String> ids) {
		karte.makeActive(ids);
	}
	
	public void addIdsToMarked(Set<String> ids) {
		karte.addToMarked(ids);
	}
	
	public void moveAllToActive() { // CLICK_REGION_BLANK
		karte.moveAllToActive();
	}
	
	public void moveCorrectToActive() {
		karte.moveCorrectToActive();
	}
	
	public void addIdsToCorrect(Set<String> elements) {
    	karte.addToCorrect(elements);
    }
	
	public void addIdsToInactive(Set<String> elements) {
    	karte.makeInactive(elements);
    }
	
	public void setIdToIncorrect(String element) {
    	karte.addToIncorrect(element);
	}
	
    public void setMapActive(boolean active) {
    	karte.setInteractive(active);
    }
    
    public ShapeMapState getState() {
    	return karte.getState();
    }
    
    public void setState(ShapeMapState state) {
    	karte.setState(state);
    }
    
    /**
     * ----- Text -----
     */
    
	public void setTextInTextField(String string) {
		textInputField.setText(string);
	}
	
    public void setTextFieldActive(boolean active) {
        if (active) {
            textInputField.setText(null);
            textInputField.setDisable(false);
            textInputField.requestFocus();
        } else {
            textInputField.setDisable(true); // Text bleibt stehen für die Lösung...
        }
    }
    
    public void setQuestion(String text) {
    	if (questionArea != null)
    		questionArea.setText(text); 
    }
    
	public String getQuestion() {
		if (questionArea != null)
			return questionArea.getText();
		else
			return null;
	}
    
    /**
     * ----- From Components -----
     */
	
	private void mapElementClicked(String id) {
		presenter.clickedMapElement(id);
	}
	
	private void textInputChanged() {
		presenter.typedText(textInputField.getText());
	}
}
