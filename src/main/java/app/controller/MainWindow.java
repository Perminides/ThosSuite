package app.controller;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import app.controller.model.PlayMenuItem;
import app.controller.model.PlayMenuNode;
import app.learn.model.LearnSessionInfo;
import app.shared.Log;
import app.shared.model.CardSortOrder;
import app.shared.skin.Skin;
import app.shared.skin.SkinService;
import javafx.collections.ObservableList;
import javafx.geometry.Dimension2D;
import javafx.scene.Scene;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HeaderBar;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

/**
 * A wrapper around the stage (the window) of the application. Holds a StackPane as its contentPane, the anchor.
 * The Stack Pane is sized correctly according to the current skin. All children will be sized by the contentPane.
 * CSS-classes:
 * 		root 		= "my-root",
 * 		headerBar 	= "my-header-bar",
 * 		titleLabel	= "my-title"
 */
@SuppressWarnings("deprecation")
public class MainWindow {

	private final StackPane contentPane;
	
    private MenuItem itemSave = null;
    private Menu menuLearn = null;
    private Menu menuOptions = null;
    private Menu menuSort = null;
    private Menu menuPlay = null;
    private List<PlayMenuNode> playMenuNodes;
    private List<LearnSessionInfo> todaysLearnSessions;
    
    private Consumer<LearnSessionInfo> onSessionSelected = null;
    private Consumer<CardSortOrder> onSortSelected = null;
    private Consumer<Skin> onNewSkinSelected = null;
    private Runnable onDiaryCreateSelected = null;
    private Runnable onDiaryViewSelected = null;
    private Runnable onWeekdaySelected = null;
    private Runnable onMattressSelected = null;
    private Runnable onExportSelected = null;
    private Runnable onMovieSelected = null;
    private Runnable onMovieAdditionalRunSelected = null;
    private Supplier<CardSortOrder> sortOrderSupplier = null;
    private Runnable onSaveSelected = null;
    private Runnable onEscPressed = null;
    private Runnable onPausePressed = null;
    private Consumer<PlayMenuItem> onPlayItemSelected = null;
    private Runnable onReloadSkin = null;
    private Consumer<String> onStatisticsSelected = null;
    
    private Stage stage;
    private HeaderBar headerBar;
    private BorderPane root;
    
    private long lastEscapeTime;

    public MainWindow(Stage stage) {
    	this.stage = stage;
        // 1. Fenster-Basics (unabhängig vom Skin)
        stage.initStyle(StageStyle.EXTENDED);
        stage.setTitle("Thos Suite"); // → Für Windows, nicht für unsere Anzeige. Wenn Du über das Taskleisten-Icon hoverst, liest Du das, was hier steht...
        stage.setResizable(false);
        
        // 2. Struktur anlegen (Container überleben den Skin-Wechsel)
        root = new BorderPane();
        root.getStyleClass().add("my-root");
        
        // 3. Unseren Anker direkt hier bauen und einhängen
        contentPane = new StackPane();
        root.setCenter(contentPane);
        
        // 4. Scene erstellen
        Scene scene = new Scene(root);
        stage.setScene(scene);
        
        // 5. Globale Handler
        initKeyBindings();
    }
    
    /**
     * Baut die UI-Inhalte neu auf basierend auf dem aktuellen Skin.
     * Wird in der Startklasse und bei Skin-Wechsel aufgerufen.
     */
    public void buildStyledUi() {
        Skin skin = SkinService.get();
        
        // A. Sizing unserer contentPane
        Dimension2D size = skin.getContentSize();
        contentPane.setPrefSize(size.getWidth(), size.getHeight());
        contentPane.setMinSize(size.getWidth(), size.getHeight());
        contentPane.setMaxSize(size.getWidth(), size.getHeight());
        
        // B. Styling
        skin.styleScene(stage.getScene());
        
        // C. Header & Menü (Neu erstellen & setzen)
        // Hinweis: createMenuBar müssen wir noch so anpassen, dass es die Bar zurückgibt!
        MenuBar menuBar = buildMenuBar(skin); 
        
        headerBar = skin.createMainWindowHeaderBar(stage, menuBar);
        headerBar.getStyleClass().add("my-header-bar");
        
        // D. Sicherstellen, dass Minimize und Close-Button die ganze Höhe ausnutzen...
        headerBar.heightProperty().addListener((_, _, newVal) ->
            HeaderBar.setPrefButtonHeight(stage, newVal.doubleValue())
        );
        
        root.setTop(headerBar);
    }
    
    // Refactoring: Gibt MenuBar zurück statt void
	private MenuBar buildMenuBar(Skin skin) {
        MenuBar menuBar = skin.createMenuBar();
        HeaderBar.setDragType(menuBar, null);
        
        // DATEI-MENÜ
        Menu menuFile = skin.createMenu("Datei");
        itemSave = skin.createMenuItem("Speichern und beenden");
        itemSave.setOnAction(_ -> onSaveSelected.run());
        itemSave.setDisable(true);
        menuFile.getItems().add(itemSave);
        
        // OPTIONEN-MENÜ
        menuOptions = skin.createMenu("Optionen");
        menuSort = skin.createMenu("Anzeigereihenfolge");
        
        // !Sofort. Boah. Ok, also ich muss bei Skinwechsel nicht mehr im Controller setCurrentSortOrder aufrufen, was gut ist. Dafür habe ich hier diesen Hack, weil bei der allerersten Initialisierung der Controller noch nicht existiert, was eher eklig ist tbh.
        CardSortOrder currentSortOrder = null;
        if (sortOrderSupplier != null)
        	currentSortOrder = sortOrderSupplier.get();
        for (CardSortOrder order : CardSortOrder.values()) {
            MenuItem item = skin.createMenuItem(order.getDisplayName());
            item.setOnAction(e -> {
                onSortSelected.accept(order);
                // Alle anderen enablen, dieses disablen
                for (MenuItem menuItem : menuSort.getItems()) {
                    menuItem.setDisable(menuItem == e.getSource());
                }
            });
            if (order == currentSortOrder)
            	item.setDisable(true);
            menuSort.getItems().add(item);
        }
        menuOptions.getItems().add(menuSort);
        
        // LERNEN-MENÜ
        menuLearn = skin.createMenu("Lernen");
        if (todaysLearnSessions != null) {
            updateLearnMenuItems();
        }
        
        // SPIELEN-MENÜ
        menuPlay = skin.createMenu("Spielen");
        if (playMenuNodes != null) {
            updatePlayMenuItems();
        }
        
        // STATISTIK-MENÜ
        Menu menuStatistics = skin.createMenu("Statistik");
        MenuItem itemDashboard = skin.createMenuItem("Dashboard");
        itemDashboard.setOnAction(_ -> onStatisticsSelected.accept("Dashboard"));
        menuStatistics.getItems().add(itemDashboard);
        MenuItem itemFitbit = skin.createMenuItem("Fitbit");
        itemFitbit.setOnAction(_ -> onStatisticsSelected.accept("Fitbit"));
        menuStatistics.getItems().add(itemFitbit);
        MenuItem itemAlc = skin.createMenuItem("Alkohol");
        itemAlc.setOnAction(_ -> onStatisticsSelected.accept("Alkohol"));
        menuStatistics.getItems().add(itemAlc);
        
        // MODULE-MENÜ
        Menu menuModule = skin.createMenu("Module");
        MenuItem exportItem = skin.createMenuItem("Export");
        exportItem.setOnAction(_ -> onExportSelected.run());
        menuModule.getItems().add(exportItem);
        MenuItem movieItem = skin.createMenuItem("Filme");
        movieItem.setOnAction(_ -> onMovieSelected.run());
        menuModule.getItems().add(movieItem);
        MenuItem diaryViewItem = skin.createMenuItem("Tagebuch lesen");
        diaryViewItem.setOnAction(_ -> onDiaryViewSelected.run());
        menuModule.getItems().add(diaryViewItem);
        MenuItem diaryItem = skin.createMenuItem("Tagebucheintrag erstellen");
        diaryItem.setOnAction(_ -> onDiaryCreateSelected.run());
        menuModule.getItems().add(diaryItem);
        MenuItem additionalMovieItem = skin.createMenuItem("Erweiterter TMDB-Import");
        additionalMovieItem.setOnAction(_ -> onMovieAdditionalRunSelected.run());
        menuModule.getItems().add(additionalMovieItem);
        MenuItem weekdayItem = skin.createMenuItem("Wochentagsberechnung");
        weekdayItem.setOnAction(_ -> onWeekdaySelected.run());
        menuModule.getItems().add(weekdayItem);
        MenuItem mattressItem = skin.createMenuItem("Matratze");
        mattressItem.setOnAction(_ -> onMattressSelected.run());
        menuModule.getItems().add(mattressItem);

        
        // ANSICHT-MENÜ
        Menu menuView = skin.createMenu("Ansicht");
        Skin currentSkin = SkinService.get();
        
        for (Skin availableSkin : SkinService.getAllSkins()) {
            String displayName = availableSkin.getDisplayName();
            boolean isCurrentSkin = availableSkin.getClass() == currentSkin.getClass();
            String menuText = (isCurrentSkin ? "✓ " : "") + displayName;
            
            MenuItem item = skin.createMenuItem(menuText);
            item.setOnAction(_ -> {
                if (availableSkin == currentSkin) return;
                onNewSkinSelected.accept(availableSkin);
            });
            menuView.getItems().add(item);
        }
        // Trenner und Aktualisieren-Button.
        // Ich habe es nicht hinbekommen, dem Trenner über CSS mehr vertikales Padding zu geben, deswegen der Hack...
        SeparatorMenuItem separator = new SeparatorMenuItem();
        MenuItem spacer = new MenuItem("");
        spacer.setDisable(true);
        spacer.getStyleClass().add("my-spacer");
        menuView.getItems().add(spacer);
        menuView.getItems().add(separator);
        MenuItem spacer2 = new MenuItem("");
        spacer2.setDisable(true);
        spacer2.getStyleClass().add("my-spacer");
        menuView.getItems().add(spacer2);
        MenuItem itemReload = skin.createMenuItem("Aktualisieren");
        itemReload.setOnAction(_ -> {
            if (onReloadSkin != null) onReloadSkin.run();
        });
        menuView.getItems().add(itemReload);
        
        // Menüs zur MenuBar hinzufügen
        menuBar.getMenus().addAll(menuFile, menuOptions, menuLearn, menuPlay, menuStatistics, menuModule, menuView);        
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
    
 // Methoden
    public void setPlayItems(List<PlayMenuNode> nodes) {
        playMenuNodes = nodes;
        updatePlayMenuItems();
    }

    private void updatePlayMenuItems() {
        if (menuPlay == null) return;
        
        menuPlay.getItems().clear();
        for (PlayMenuNode node : playMenuNodes) {
            PlayMenuItem item = (PlayMenuItem) node; // Cast ist sicher dank sealed interface
            MenuItem menuItem = new MenuItem(item.label());
            menuItem.setOnAction(_ -> onPlayItemSelected.accept(item));
            menuPlay.getItems().add(menuItem);
        }
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
    
    public void showPane(Pane view) {
    	contentPane.getChildren().setAll(view);
    }
    
    private void initKeyBindings() {
    	stage.getScene().setOnKeyPressed(event -> {
            switch (event.getCode()) {
                case ESCAPE: {
                	long now = System.currentTimeMillis();
                    // Ignoriere wenn weniger als 300ms seit letztem ESC
                    if (now - lastEscapeTime < 500) {
                    	Log.debug(this, "Oha, wohl ein Tastatur-Glitsch, so schnell hintereinander 2x ESC...");
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
    

    public void setPlayItemConsumer(Consumer<PlayMenuItem> consumer) {
        this.onPlayItemSelected = consumer;
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
    
    public void setReloadSkinRunnable(Runnable action) {
        this.onReloadSkin = action;
    }
    
    public void setStatisticsConsumer(Consumer<String> consumer) {
        this.onStatisticsSelected = consumer;
    }
    
    public void setSortOrderSupplier(Supplier<CardSortOrder> supplier) {
    	this.sortOrderSupplier = supplier;
    }
    
    public void setDiaryCreateRunnable(Runnable runner) {
    	this.onDiaryCreateSelected = runner;
    }
    
    public void setDiaryViewRunnable(Runnable runner) {
    	this.onDiaryViewSelected = runner;
    }
    
    public void setWeekdayRunnable(Runnable runner) {
    	this.onWeekdaySelected = runner;
    }
    
    public void setMattressRunnable(Runnable runner) {
    	this.onMattressSelected = runner;
    }
    
    public void setExportRunnable(Runnable runner) {
    	this.onExportSelected = runner;
    }
    
    public void setMovieRunnable(Runnable runner) {
    	this.onMovieSelected = runner;
    }
    
    public void setExtraTmdbImportRunnable(Runnable runner) {
    	this.onMovieAdditionalRunSelected = runner;
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
	
	/**
	 * Setzt die aktuelle SortOrder und disabled das entsprechende MenuItem
	 * Wird initial ein Mal vom Controller nach Lesen der config aufgerufen
	 */
	public void setCurrentSortOrder(CardSortOrder sortOrder) {
	    if (menuSort == null) return; // Noch nicht initialisiert
	    
	    for (MenuItem item : menuSort.getItems()) {
	        // Vergleich über DisplayName (das steht im MenuItem-Text)
	        boolean isCurrentOrder = item.getText().equals(sortOrder.getDisplayName());
	        item.setDisable(isCurrentOrder);
	    }
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