package app.ui.panels;

import java.io.File;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import app.data.DeckType;
import app.data.LearnStat;
import app.data.SessionProgress;
import app.presenter.AnkiSessionPresenter;
import app.ui.MainWindow;
import app.ui.components.ImagePane;
import app.ui.components.MultipleChoicePane;
import app.ui.components.ShapeMapPane; // NEU: JavaFX Pane
import app.ui.skin.Skin;
import app.ui.skin.SkinService;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane; // NEU: JavaFX Basis

public class GermanySessionPane extends Pane implements AnkiSessionPanel {
	private static final DeckType DECKTYPE = DeckType.GERMANY_CARDS; 

	private final MainWindow mainWindow;
	private final AnkiSessionPresenter presenter;
    private TextField textInputField;
    private Label questionArea;
    private Label progressArea;
    private Label cardHistoryArea;
    private MultipleChoicePane mcPane;
    private Button backButton;
    private ImagePane imageComponent;
    private ShapeMapPane deutschlandkarte;

    public GermanySessionPane(MainWindow mainWindow, AnkiSessionPresenter presenter) {
        this.mainWindow = mainWindow;
        this.presenter = presenter;
        // setLayout(null); // Gibt es in FX Pane nicht (ist default)
        // setOpaque(false); // Gibt es in FX Pane nicht
        // setSize(SkinService.get().getContentSize()); // Muss anders gelöst werden (in FX meist via PrefSize)
        initUI();
    }
    
    @Override
    public void show() {
        Skin skin = SkinService.get();
        Pane background = skin.createBackgroundPane(DECKTYPE);
        background.getChildren().add(this);
        mainWindow.showView(background);
    }  

    private void initUI() {
        Skin skin = SkinService.get();
        
    	deutschlandkarte = skin.createShapeMapPane(DECKTYPE);
    	deutschlandkarte.setListener(id -> mapElementClicked(id));
    	getChildren().add(deutschlandkarte);
    	deutschlandkarte.makeEveryShapeActive();
    	
    	
    	questionArea = skin.createCustomTextLabel(DECKTYPE, Skin.TextLabelType.QUESTION);
    	questionArea.setText(""); // Initial leer
        getChildren().add(questionArea);
    	
        textInputField = skin.createInputField(DECKTYPE);
        textInputField.setOnKeyReleased(_ -> textInputChanged());
        getChildren().add(textInputField);
    	
    	imageComponent = skin.createImageComponent(DECKTYPE);
    	getChildren().add(imageComponent); // FEHLER 
    	
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
    	getChildren().add(progressArea); // FEHLER
    	
    	cardHistoryArea = skin.createCustomTextLabel(DECKTYPE, Skin.TextLabelType.CARD_HISTORY);
    	cardHistoryArea.setText("");
    	getChildren().add(cardHistoryArea); // FEHLER
    	
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
	
	public void setMCPanelActive(boolean active) {
		if (active)
			throw new RuntimeException("Das habe ich nicht vorhergesehen.");
		else
			mcPane.clearAndSetInactive();
	}
	
    public void setMcCorrect(int id, boolean correct) {
    	mcPane.setCorrect(id, correct);
    }
    
	public void setMcSolution(Set<Integer> correctIds) {
		mcPane.setCorrectAndInactive(correctIds);
	}
	
	// Map - HIER WURDE ANGEPASST (Delegation an ShapeMapPane)
	
    public void resetMarkers() {
    	deutschlandkarte.makeEveryShapeActive();
    	// repaint entfällt
    }
	
	public void addIdsToCorrect(Set<String> elements) {
    	deutschlandkarte.addToCorrect(elements);
    	// repaint entfällt
    }
	
	@Override
	public void setMarkedIds(Set<String> elements) {
		deutschlandkarte.addToMarked(elements);
		// repaint entfällt
	}
    
    @Override
    public void setIdToIncorrect(String element) {
    	deutschlandkarte.addToIncorrect(element);
    	// repaint entfällt
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
		System.out.println(text);
		cardHistoryArea.setText(text);
	}
	
	public DeckType getType() {
		return DECKTYPE;
	}
}