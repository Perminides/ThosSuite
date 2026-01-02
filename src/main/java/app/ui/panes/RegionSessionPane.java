package app.ui.panes;

import java.util.Set;

import app.data.DeckType;
import app.data.MapMetadata;
import app.presenter.RegionSessionPresenter;
import app.ui.components.CustomTextLabel;
import app.ui.components.ShapeMapPane;
import app.ui.components.ShapeMapPane.ShapeMapState;
import app.ui.skin.Skin;
import app.ui.skin.SkinService;
import javafx.scene.control.TextField;
import javafx.scene.layout.Background;
import javafx.scene.layout.Pane;

public class RegionSessionPane extends Pane {
	private final RegionSessionPresenter presenter;
	private final DeckType deckType;
    private ShapeMapPane karte;
    private CustomTextLabel questionArea;
    private TextField textInputField;
	
	public RegionSessionPane(RegionSessionPresenter presenter, DeckType deckType, boolean questionAreaVisible) {
        this.presenter = presenter;
        // All germany regions can be styled for my skins by styling lk_bb. A hack, I know...
        this.deckType = deckType.getMapMetadata() == MapMetadata.GERMANY ? DeckType.BUNDESLAND_BRANDENBURG : deckType;
        this.setBackground(new Background(SkinService.get().getBackgroundImage(deckType)));
        initUI(questionAreaVisible);
	}
	
	private void initUI(boolean questionAreaVisible) {
        Skin skin = SkinService.get();
        
        karte = skin.createShapeMapPane(deckType);
        karte.resetAllStates();
        karte.setListener(id -> mapElementClicked(id));
    	getChildren().add(karte);
    	
    	if (questionAreaVisible) {
        	questionArea = skin.createCustomTextLabel(deckType, Skin.TextLabelType.QUESTION);
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
            textInputField.setText("");
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
