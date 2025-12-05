package app.ui.panels;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Set;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import app.data.DeckType;
import app.presenter.RegionSessionPresenter;
import app.ui.MainWindow;
import app.ui.MapElementListener;
import app.ui.components.BackgroundPanel;
import app.ui.components.CustomTextField;
import app.ui.components.CustomTextLabel;
import app.ui.components.ShapeMapPanel;
import app.ui.components.ShapeMapState;
import app.ui.skin.Skin;
import app.ui.skin.SkinService;

public class RegionSessionPanel extends JPanel{
	private static final long serialVersionUID = 1L;

	private final MainWindow mainWindow;
	private final RegionSessionPresenter presenter;
	private final DeckType deckType;
    private ShapeMapPanel karte;
    private CustomTextLabel questionArea;
    private CustomTextField textInputField;
    private boolean batch = false;
	
	public RegionSessionPanel(MainWindow mainWindow, RegionSessionPresenter presenter, DeckType deckType, boolean questionAreaVisible) {
        this.mainWindow = mainWindow;
        this.presenter = presenter;
        this.deckType = deckType;
        setLayout(null);
        setOpaque(false);
        setSize(SkinService.get().getContentSize());
        initUI(questionAreaVisible);
	}
	
	public void show() {
		BackgroundPanel bg = SkinService.get().createBackgroundPanel(DeckType.MC_CARDS); // !Später vielleicht eigenes Hintergrundbild? Platz wäre ja genug!
    	bg.add(this);
        mainWindow.showPanel(bg);
	}
	
	private void initUI(boolean questionAreaVisible) {
        Skin skin = SkinService.get();
    	karte = skin.createShapeMapPanel(deckType);
    	karte.setListener(new MapElementListener() {
    	    @Override
    	    public void mouseClicked(String id) {
    	        mapElementClicked(id);  // ← Über Helper
    	    }
    	});
    	add(karte);
    	
    	if (questionAreaVisible) {
    		questionArea = skin.createTextLabel(deckType, Skin.TextLabelType.QUESTION);
    		questionArea.setNameForDebug("QuestionLabel");
    		questionArea.setText("");
    		add(questionArea);
    	} else {
    		textInputField = skin.createInputField(deckType);
    		textInputField.setText("");
    		textInputField.setEnabled(true);
    		SwingUtilities.invokeLater(() -> textInputField.requestFocusInWindow()); // Darf erst aufgerufen werden nachdem das Panel gerendert wurde...
			textInputField.addKeyListener(new KeyAdapter() {
	    		@Override
	    		public void keyReleased(KeyEvent e) {
	    			textInputChanged();
	    		}
	    		
			});
    		add(textInputField);
    	}
    }
	
	/**
	 * ----- MAP -----
	 */
	
    public void beginTx() {
    	batch = true;
    }
    
    public void endTx() {
    	batch = false;
    	karte.repaint();
    }
    
	public void addIdsToActive(Set<String> ids) {
		karte.makeActive(ids);
		if (!batch)
    		karte.repaint();
	}
	
	public void addIdsToMarked(Set<String> ids) {
		karte.addToMarked(ids);
		if (!batch)
    		karte.repaint();
	}
	
	public void moveAllToActive() { // CLICK_REGION_BLANK
		karte.moveAllToActive();
		if (!batch)
    		karte.repaint();
	}
	
	public void moveCorrectToActive() {
		karte.moveCorrectToActive();
		if (!batch)
    		karte.repaint();
	}
	
	public void addIdsToCorrect(Set<String> elements) {
    	karte.addToCorrect(elements);
    	if (!batch)
    		karte.repaint();
    }
	
	public void setIdToIncorrect(String element) {
    	karte.addToIncorrect(element);
    	if (!batch)
    		karte.repaint();
	}
	
    public void setMapActive(boolean active) {
    	karte.setActive(active);
    	if (!batch)
    		karte.repaint();
    }
    
    public ShapeMapState getState() {
    	return karte.getState();
    }
    
    public void setState(ShapeMapState state) {
    	karte.setState(state);
    	if (!batch)
    		karte.repaint();
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
			textInputField.setEnabled(true);
			textInputField.requestFocusInWindow();
		} else {
			textInputField.setEnabled(false);
			// Text bleibt stehen (z.B. für Lösungs-Anzeige)
		}
	}
    
    public void setQuestion(String text) {
    	questionArea.setText(text); 
    }
    
	public String getQuestion() {
		return questionArea.getText();
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
