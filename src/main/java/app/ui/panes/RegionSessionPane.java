package app.ui.panes;

import java.util.Set;

import app.data.Deck;
import app.presenter.RegionSessionPresenter;
import app.ui.components.SessionInfoLabel;
import app.ui.components.ShapeMapPane;
import app.ui.components.ShapeMapPane.ShapeMapState;
import app.ui.skin.Skin;
import app.ui.skin.SkinService;
import javafx.scene.control.TextField;
import javafx.scene.layout.Background;
import javafx.scene.layout.Pane;

public class RegionSessionPane extends Pane {
	private final RegionSessionPresenter presenter;
	private final Deck deckType;
    private ShapeMapPane karte;
    private SessionInfoLabel questionArea;
    private TextField textInputField;
	
	public RegionSessionPane(RegionSessionPresenter presenter, Deck deckType, boolean questionAreaVisible) {
        this.presenter = presenter;
        this.deckType = deckType;
        this.setBackground(new Background(SkinService.get().getBackgroundImage(deckType)));
        initUI(questionAreaVisible);
	}
	
	private void initUI(boolean questionAreaVisible) {
        Skin skin = SkinService.get();
        
        karte = skin.createShapeMapPane(deckType);
        karte.setListener(id -> mapElementClicked(id));
    	getChildren().add(karte);
    	
    	// !Sofoert: Hier wäre allerdings CENTER schon angesagt
    	if (questionAreaVisible) {
        	questionArea = skin.createSessionInfoLabel(deckType, Skin.TextLabelType.QUESTION);
        	questionArea.setText(""); // Initial leer
            getChildren().add(questionArea);
    	} else {
            textInputField = skin.createInputField(deckType);
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
