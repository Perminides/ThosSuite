package app.ui.panes;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import app.data.DeckType;
import app.data.LearnStat;
import app.data.SessionProgress;
import app.presenter.AnkiSessionPresenter;
import app.ui.MainWindow;
import app.ui.MapElementListener;
import app.ui.components.CustomTextLabel;
import app.ui.components.ImageMapPane;
import app.ui.components.ImagePane;
import app.ui.components.MultipleChoicePane;
import app.ui.skin.Skin;
import app.ui.skin.SkinService;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;

public class WorldSessionPane extends Pane implements AnkiSessionPane {
	private static final DeckType DECKTYPE = DeckType.WORLD_CARDS; 

	private final MainWindow mainWindow;
	private final AnkiSessionPresenter presenter;
    private TextField textInputField;
    private CustomTextLabel questionArea;
    private CustomTextLabel progressArea;
    private CustomTextLabel cardHistoryArea;
    private MultipleChoicePane mcPane;
    private Button backButton;
    private ImageMapPane weltkarte;
    private ImagePane imageComponent;

    public WorldSessionPane(MainWindow mainWindow, AnkiSessionPresenter presenter) {
        this.mainWindow = mainWindow;
        this.presenter = presenter;
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
        weltkarte = skin.createImageMapPanel(DECKTYPE);
    	weltkarte.setListener(new MapElementListener() {
    	    @Override
    	    public void mouseClicked(String id) {
    	        mapElementClicked(id);  // ← Über Helper
    	    }
    	});
    	getChildren().add(weltkarte);
    	
    	questionArea = skin.createCustomTextLabel(DECKTYPE, Skin.TextLabelType.QUESTION);
    	questionArea.setText("");
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
	
	/**
	 * Setzt aktive Buttons auf inaktiv. Nach falschem Klick z.B.
	 * @param active
	 */
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
	
	// Map
	
    public void resetMarkers() {
    	weltkarte.resetMarkers();
    }
    
    public void setMapActive(boolean active) {
    	weltkarte.setActive(active);
    }
    
	@Override
	public void setIdsInQuestion(Set<String> ids) {
		weltkarte.setToCheckShapes(ids);
	}
	
	@Override
	public void setMarkedIds(Set<String> ids) {
		weltkarte.setMarked(ids);
	}
	
    @Override
	public void addIdsToCorrect(Set<String> elements) {
		weltkarte.addToCorrect(elements);
    }
    
    @Override
	public void setIdToIncorrect(String id) { // Die id ist eh null, braucht uns nicht zu interessieren.
		weltkarte.resetMarkers(); // Wo war die EM 2021? Alle Länder werden grün. Wer gewann? Wenn ich falsch klicke und jetzt eins der 10 nochmal grün wird sehe ich nicht, welches!
    	weltkarte.markLastClickAsIncorrect();
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
		System.out.println("Clicked on Map!");
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
}
