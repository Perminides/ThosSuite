package app.ui;

import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Consumer;

import app.data.CardSortOrder;
import app.data.LearnSessionInfo;
import app.ui.skin.Skin;
import app.ui.skin.SkinService;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HeaderBar;
import javafx.scene.layout.HeaderDragType;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

public class MainWindow {

    private MenuItem itemSave = null;
    private Menu menuLearn = null;
    private Menu menuOptions = null;
    private Menu menuSort = null;
    private List<LearnSessionInfo> todaysLearnSessions;
    
    private Consumer<LearnSessionInfo> onSessionSelected = null;
    private Consumer<CardSortOrder> onSortSelected = null;
    private Consumer<Skin> onNewSkinSelected = null;
    private Runnable onSaveSelected = null;
    private Runnable onEscPressed = null;
    private Runnable onPausePressed = null;
    
    private Stage stage;
    private HeaderBar headerBar;
    private BorderPane root;
    private Pane contentPane;
    
    private long lastEscapeTime;

    public MainWindow(Stage stage) {
    	this.stage = stage;
        // 1. Fenster-Basics (unabhängig vom Skin)
        stage.initStyle(StageStyle.EXTENDED);
        stage.setTitle("Thos Suite"); // → Für Windows, nicht für unsere Anzeige. Wenn Du über das Taskleisten-Icon hoverst, liest Du das, was hier steht...
        stage.setResizable(false);
        
        // 2. Struktur anlegen (Container überleben den Skin-Wechsel)
        root = new BorderPane();
        root.getStyleClass().add("thorstens-root");
        
        // 3. Scene erstellen
        Scene scene = new Scene(root);
        stage.setScene(scene);
        
        // 4. Globale Handler
        initKeyBindings();
        
        // 5. Initiales Füllen der UI
        buildStyledUi();
    }
    
    /**
     * Baut die UI-Inhalte neu auf basierend auf dem aktuellen Skin.
     * Wird im Konstruktor und bei Skin-Wechsel aufgerufen.
     */
    public void buildStyledUi() {
        Skin skin = SkinService.get();
        
        // A. Styling
        skin.styleScene(stage.getScene());
        
        // C. Header & Menü (Neu erstellen & setzen)
        // Hinweis: createMenuBar müssen wir noch so anpassen, dass es die Bar zurückgibt!
        MenuBar menuBar = buildMenuBar(skin); 
        
        headerBar = skin.createHeaderBar(stage, menuBar);
        headerBar.getStyleClass().add("thorstens-bar");
        
        // D. Sicherstellen, dass Minimize und Close-Button die ganze Höhe ausnutzen...
        headerBar.heightProperty().addListener((obs, oldVal, newVal) -> 
            HeaderBar.setPrefButtonHeight(stage, newVal.doubleValue())
        );
        
        root.setTop(headerBar);
    }
    
 // Refactoring: Gibt MenuBar zurück statt void
    private MenuBar buildMenuBar(Skin skin) {
        MenuBar menuBar = skin.createMenuBar();
        HeaderBar.setDragType(menuBar, HeaderDragType.DRAGGABLE_SUBTREE);
        
        // DATEI-MENÜ
        Menu menuFile = skin.createMenu("Datei");
        itemSave = skin.createMenuItem("Speichern und beenden");
        itemSave.setOnAction(_ -> onSaveSelected.run());
        itemSave.setDisable(true);
        menuFile.getItems().add(itemSave);
        
        // OPTIONEN-MENÜ
        menuOptions = skin.createMenu("Optionen");
        menuSort = skin.createMenu("Anzeigereihenfolge");
        
        for (CardSortOrder order : CardSortOrder.values()) {
            MenuItem item = skin.createMenuItem(order.getDisplayName());
            if (order == CardSortOrder.BY_WRONG_COUNT_DESC) {
                item.setDisable(true);
            }
            item.setOnAction(e -> {
                onSortSelected.accept(order);
                // Alle anderen enablen, dieses disablen
                for (MenuItem menuItem : menuSort.getItems()) {
                    menuItem.setDisable(menuItem == e.getSource());
                }
            });
            menuSort.getItems().add(item);
        }
        menuOptions.getItems().add(menuSort);
        
        // LERNEN-MENÜ
        menuLearn = skin.createMenu("Lernen");
        if (todaysLearnSessions != null) {
            updateLearnMenuItems();
        }
        
        // ANSICHT-MENÜ
        Menu menuView = skin.createMenu("Ansicht");
        Skin currentSkin = SkinService.get();
        
        for (Skin availableSkin : SkinService.getAllSkins()) {
            String displayName = availableSkin.getDisplayName();
            boolean isCurrentSkin = availableSkin.getClass() == currentSkin.getClass();
            String menuText = (isCurrentSkin ? "✓ " : "  ") + displayName;
            
            MenuItem item = skin.createMenuItem(menuText);
            item.setOnAction(_ -> {
                if (availableSkin == currentSkin) return;
                onNewSkinSelected.accept(availableSkin);
            });
            menuView.getItems().add(item);
        }
        
        // Menüs zur MenuBar hinzufügen
        menuBar.getMenus().addAll(menuFile, menuOptions, menuLearn, menuView);        
        return menuBar;
    }
    
    private void updateLearnMenuItems() {
        menuLearn.getItems().clear();
        for (LearnSessionInfo info : todaysLearnSessions) {
            MenuItem item = new MenuItem(info.formatForMenu());
            if (!info.isStillDueToday()) {
                item.setDisable(true);
            } else {
                item.setOnAction(_ -> onSessionSelected.accept(info));
            }
            menuLearn.getItems().add(item);
        }
    }
    
    public void showPane(Pane panel) {
        root.setCenter(panel);
    }
    
    public void setLearnItems(List<LearnSessionInfo> infoList) {
        todaysLearnSessions = infoList;
        updateLearnMenuItems();
    }
    
    public void addLearnItems(List<LearnSessionInfo> infoList) {
        todaysLearnSessions.addAll(infoList);
        updateLearnMenuItems();
    }
    
    public void updateLearnItems(List<LearnSessionInfo> infoList) {
        todaysLearnSessions = infoList;
        updateLearnMenuItems();
    }
    
    public void showSaveSession(boolean enabled) {
        itemSave.setDisable(!enabled);
    }
    
    public void showView(Pane view) {
        // Wir tauschen den Inhalt im Zentrum des BorderPane aus
        root.setCenter(view);
        
        // Optional: Fokus anfordern, damit KeyListener (z.B. für Input) greifen
        view.requestFocus();
    }
    
    private void initKeyBindings() {
    	final String instanceId = Integer.toHexString(System.identityHashCode(this));
    	stage.getScene().setOnKeyPressed(event -> {
            switch (event.getCode()) {
                case ESCAPE: {
                	long now = System.currentTimeMillis();
                	System.out.println("[Instance: " + instanceId + "] Escaped pressed: " + LocalDateTime.now());
                    // Ignoriere wenn weniger als 300ms seit letztem ESC
                    if (now - lastEscapeTime < 500) {
                    	System.out.println("Oha, wohl ein Tastatur-Glitsch...");
                        event.consume();
                        return;
                    }
                    lastEscapeTime = now;
                    if (onEscPressed != null) onEscPressed.run();
                    break;
                }
                case PAUSE:
                    if (onPausePressed != null) onPausePressed.run();
                    break;
                default:
                    break;
            }
        });
    }
    
    // Callback Setter
    public void setSaveRunnable(Runnable action) {
        this.onSaveSelected = action;
    }
    
    public void setLearnSessionConsumer(Consumer<LearnSessionInfo> consumer) {
        this.onSessionSelected = consumer;
    }
    
    public void setSkinChangeConsumer(Consumer<Skin> consumer) {
        this.onNewSkinSelected = consumer;
    }
    
    public void setSortConsumer(Consumer<CardSortOrder> consumer) {
        this.onSortSelected = consumer;
    }
    
    public void setEscPressedRunnable(Runnable action) {
        this.onEscPressed = action;
    }

    public void setPausePressedRunnable(Runnable action) {
        this.onPausePressed = action;
    }

	public void show() {
		stage.show();		
	}

	public void setWidth(int width) {
		stage.setWidth(width);
	}

	public void setHeight(int height) {
		stage.setHeight(height);
	}

	public ObservableList<Image> getIcons() {
		return stage.getIcons();
	}

	public void centerOnScreen() {
		stage.centerOnScreen();
	}

	public Window getStage() {
		return stage;
	}
}