package app.ui.panels;

import java.util.List;
import java.util.Set;

import javax.swing.JLabel;
import javax.swing.JTextField;

import app.data.DeckType;
import app.data.LearnStat;
import app.data.SessionProgress;
import app.presenter.AnkiSessionPresenter;
import app.ui.MainWindow;
import app.ui.components.CustomButtonLabel;
import app.ui.components.CustomButtonLabel.CLBState;
import app.ui.components.CustomTextLabel;
import app.ui.components.MultipleChoicePanel;
import app.ui.components.ShapeMapPane; // NEU: JavaFX Pane
import app.ui.skin.Skin;
import app.ui.skin.SkinService;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane; // NEU: JavaFX Basis

public class GermanySessionPane extends Pane implements AnkiSessionPanel { // Extends Pane (FX)
	// private static final long serialVersionUID = 1L; // Braucht Pane nicht
	private static final DeckType DECKTYPE = DeckType.GERMANY_CARDS; 

	private final MainWindow mainWindow;
	private final AnkiSessionPresenter presenter;
	
    // --- Swing Altlasten (stehen lassen, wie gewünscht) ---
    private JTextField textInputField;
    private Label questionArea;
    private Label progressArea;
    private Label cardHistoryArea;
    private MultipleChoicePanel mcPanel;
    private CustomButtonLabel backButton;
    private JLabel imageLabel;
    
    // --- NEU: Die JavaFX Map ---
    private ShapeMapPane deutschlandkarte; // Typ geändert!

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
        
        // 1. Hintergrund holen (Deine Methode!)
        Pane background = skin.createBackgroundPane(DECKTYPE);
        
        // 2. Uns selbst (die Map-Layer) darauf legen
        // Da 'background' ein Pane ist und wir (this) auch ein Pane sind
        // und absolute Koordinaten nutzen, passt das perfekt aufeinander.
        background.getChildren().add(this);
        
        // 3. Anzeigen
        mainWindow.showView(background);
    }  

    private void initUI() {
        Skin skin = SkinService.get();
        
        // --- START NEUER MAP CODE ---
    	// Wir nutzen die neue Factory im Skin (die wir eben besprochen haben)
    	deutschlandkarte = skin.createShapeMapPane(DECKTYPE);
    	
    	// Listener: JavaFX Consumer Style
    	deutschlandkarte.setListener(id -> mapElementClicked(id));
    	
    	// Hinzufügen zum SceneGraph
    	getChildren().add(deutschlandkarte);
    	
    	// Initialisierung: Alle aktiv
    	deutschlandkarte.makeEveryShapeActive();
    	// --- ENDE NEUER MAP CODE ---
    	
    	
    	questionArea = skin.createCustomTextLabel(DECKTYPE, Skin.TextLabelType.QUESTION);
    	questionArea.setText(""); // Initial leer
        getChildren().add(questionArea);
    	
    	/**
    	textInputField = skin.createInputField(DECKTYPE);
    	textInputField.setEnabled(false);
    	textInputField.addKeyListener(new KeyAdapter() {
    		@Override
    		public void keyReleased(KeyEvent e) {
    			textInputChanged();
    		}
    		
		});
    	add(textInputField); // FEHLER
    	
    	imageLabel = skin.createImageLabel(DECKTYPE);
    	add(imageLabel); // FEHLER
    	
    	mcPanel = skin.createMultipleChoicePanel(DECKTYPE);
    	mcPanel.addListener(
    		new Consumer<Integer>() {
				@Override
				public void accept(Integer i) {
					onAnswerSelected(i);
				}
    		}
    	);
    	mcPanel.disableAllButtons();
    	add(mcPanel); // FEHLER**/
    	
    	progressArea = skin.createCustomTextLabel(DECKTYPE, Skin.TextLabelType.PROGRESS);
    	progressArea.setText("");
    	getChildren().add(progressArea); // FEHLER
    	
    	cardHistoryArea = skin.createCustomTextLabel(DECKTYPE, Skin.TextLabelType.CARD_HISTORY);
    	cardHistoryArea.setText("");
    	getChildren().add(cardHistoryArea); // FEHLER
    	
    	/**backButton = skin.createIconButton(DECKTYPE, Skin.IconButtonType.BACK);
    	backButton.addMouseListener(new MouseInputAdapter() {
   		 @Override
            public void mousePressed(MouseEvent e) {
   			 	backButtonClicked();
            }
		});
    	add(backButton); // FEHLER**/
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
    	//imageLabel.setIcon(imagePath == null ? null : new ImageIcon(imagePath));
    }
    
    // Multiple Choice
    
	public void setMultipleChoice(List<String> answers) {
		//mcPanel.initiateMultipleChoice(answers);
	}
	
	public void setMCPanelActive(boolean active) {
		/**if (active)
			throw new RuntimeException("Das habe ich nicht vorhergesehen.");
		else
			mcPanel.inactivateAllActiveButtons();**/
	}
	
	public void disableMcPanel() {
		//mcPanel.disableAllButtons();
	}
	
    public void setMcCorrect(int id, boolean correct) {
    	if (correct)
    		mcPanel.setState(id, CLBState.CORRECT);
    	else
    		mcPanel.setState(id, CLBState.INCORRECT);
    }
    
	public void setMcSolution(Set<Integer> correctIds) {
		for (int id : correctIds) {
			mcPanel.setState(id, CLBState.CORRECT);
		}
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
    	deutschlandkarte.setInteractive(active); // Methode heißt jetzt setInteractive
    	// repaint entfällt
    }
    
    // Input
    
	public void setTextFieldActive(boolean active) {
		/**if (active) {
			textInputField.setText("");
			textInputField.setEnabled(true);
			textInputField.requestFocusInWindow();
		} else {
			textInputField.setEnabled(false);
		}**/
	}
    
	public void setTextInTextField(String text) {
		//textInputField.setText(text); 
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