package app.ui.panes;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import app.data.DeckType;
import app.data.LearnStat;
import app.data.SessionProgress;
import app.presenter.AnkiSessionPresenter;
import app.ui.components.CustomTextLabel;
import app.ui.components.ImagePane;
import app.ui.components.MultipleChoicePane;
import app.ui.components.ShapeMapPane;
import app.ui.skin.Skin;
import app.ui.skin.SkinService;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.Background;
import javafx.scene.layout.Pane;

public class GermanySessionPane extends Pane implements AnkiSessionPane {
	private static final DeckType DECKTYPE = DeckType.GERMANY_CARDS; 

	private final AnkiSessionPresenter presenter;
    private TextField textInputField;
    private CustomTextLabel questionArea;
    private CustomTextLabel progressArea;
    private CustomTextLabel cardHistoryArea;
    private MultipleChoicePane mcPane;
    private Button backButton;
    private ImagePane imageComponent;
    private ShapeMapPane deutschlandkarte;

    public GermanySessionPane(AnkiSessionPresenter presenter) {
        this.presenter = presenter;
        this.setBackground(new Background(SkinService.get().getWallpaper(DECKTYPE)));
        initUI();
    }

    private void initUI() {
        Skin skin = SkinService.get();
        
    	deutschlandkarte = skin.createShapeMapPane(DECKTYPE);
    	deutschlandkarte.setListener(id -> mapElementClicked(id));
    	getChildren().add(deutschlandkarte);
    	deutschlandkarte.moveAllToActive();
    	
    	
    	questionArea = skin.createCustomTextLabel(DECKTYPE, Skin.TextLabelType.QUESTION);
    	questionArea.setText(""); // Initial leer
        getChildren().add(questionArea);
    	
        textInputField = skin.createInputField(DECKTYPE);
        textInputField.setOnKeyReleased(_ -> textInputChanged());
        getChildren().add(textInputField);
    	
    	imageComponent = skin.createImageComponent(DECKTYPE);
    	getChildren().add(imageComponent); 
    	
    	mcPane = skin.createMultipleChoicePane(DECKTYPE);
    	mcPane.addListener(
    		new Consumer<Integer>() {
				@Override
				public void accept(Integer i) {
					onAnswerSelected(i);
				}
    		}
    	);
    	getChildren().add(mcPane);
    	
    	progressArea = skin.createCustomTextLabel(DECKTYPE, Skin.TextLabelType.PROGRESS);
    	progressArea.setText("");
    	getChildren().add(progressArea);
    	
    	cardHistoryArea = skin.createCustomTextLabel(DECKTYPE, Skin.TextLabelType.CARD_HISTORY);
    	cardHistoryArea.setText("");
    	getChildren().add(cardHistoryArea);
    	
    	backButton = skin.createIconButton(DECKTYPE, Skin.IconButtonType.BACK);
    	backButton.setOnAction(_ -> backButtonClicked());
    	getChildren().add(backButton);
    }
	
	// ========================================
	// Called from presenter
	// ========================================
    
    // Question
	
    public void setQuestion(String text) {
        questionArea.setText(text);
    }
    
	// Image
	
    public void setImage(String imagePath) {
        imageComponent.setImage(imagePath);
    }
    
    // Multiple Choice
    
	public void setMultipleChoice(List<String> answers) {
		mcPane.initiateMultipleChoice(answers);
	}
	
	public void disableMcPanel() {
		mcPane.clearAndSetInactive();
	}
	
    public void setMcCorrect(int id, boolean correct) {
    	mcPane.setCorrect(id, correct);
    }
    
	public void setMcSolution(Set<Integer> correctIds) {
		mcPane.setCorrectAndInactive(correctIds);
	}
	
	// Map
	
    public void resetMarkers() {
    	deutschlandkarte.moveAllToActive();
    }
	
	public void addIdsToCorrect(Set<String> elements) {
    	deutschlandkarte.addToCorrect(elements);
    }
	
	@Override
	public void setMarkedIds(Set<String> elements) {
		deutschlandkarte.addToMarked(elements);
	}
    
    @Override
    public void setIdToIncorrect(String element) {
    	deutschlandkarte.addToIncorrect(element);
    }
    
    public void setMapActive(boolean active) {
    	deutschlandkarte.setInteractive(active);
    }
    
    // Input
    
    public void setTextFieldActive(boolean active) {
        if (active) {
            textInputField.setText("");
            textInputField.setDisable(false);
            textInputField.requestFocus();
        } else {
            textInputField.setDisable(true);
        }
    }
    
	public void setTextInTextField(String text) {
		textInputField.setText(text); 
	}	
	
	// ========================================
	// Called from components
	// ========================================
	
	public void onAnswerSelected(int index) {
		presenter.clickedMCAnswer(index);
	}
	
	private void textInputChanged() {
	    presenter.typedText(textInputField.getText());
	}

	private void mapElementClicked(String id) {
	    presenter.clickedMapElement(id);
	}
	
	private void backButtonClicked() {
	    presenter.clickedBack();
	}

	@Override
	public void sessionProgressChanged(SessionProgress progress) {
		String text = "Korrekt: " + progress.correct() + "\nFalsch: "
				+ progress.incorrect() + "\nOffen: "
				+ (progress.details().size()-progress.correct()-progress.incorrect());
		progressArea.setText(text);
	}


	@Override
	public void updateCardStats(LearnStat stats) {
		String text = "";
		if (stats != null) {
		text = "Zuletzt gespielt: " + stats.getLastPlayed()
			+ "\nLevel: " + stats.getCurrentLevel()
			+ "\nFalsch beantwortet: " + stats.getWrongCount();
		}
		cardHistoryArea.setText(text);
	}
	
	@Override
	public Pane asPane() {
		return this;
	}
}