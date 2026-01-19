package app.misc;

import app.config.Config;
import app.ui.skin.SkinService;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.Window;

public class TableViewDialogTest extends Application {

    public static class Person {
        private final String firstName;
        private final String lastName;
        private final String city;
        private final String country;
        private final String age;

        public Person(String firstName, String lastName, String city, String country, String age) {
            this.firstName = firstName;
            this.lastName = lastName;
            this.city = city;
            this.country = country;
            this.age = age;
        }

        public String getFirstName() { return firstName; }
        public String getLastName() { return lastName; }
        public String getCity() { return city; }
        public String getCountry() { return country; }
        public String getAge() { return age; }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void start(Stage primaryStage) {
        Config.init("C:/Users/Markgraf/OneDrive/ThosSuite/");
        
        // Dialog erstellen (mit Parent, damit er nicht zentral positioniert wird)
        Dialog<?> dialog = SkinService.get().createDialog(null, "TableView im Dialog");
        
        // TableView erstellen (exakt wie in TableViewExample)
        TableView<Person> tableView = new TableView<>();
        tableView.setPrefWidth(TableView.USE_COMPUTED_SIZE);
        tableView.setPrefHeight(TableView.USE_COMPUTED_SIZE);
        tableView.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        tableView.getStyleClass().add("my-table-view");

        TableColumn<Person, String> firstNameCol = new TableColumn<>("Vorname");
        firstNameCol.setCellValueFactory(new PropertyValueFactory<>("firstName"));

        TableColumn<Person, String> lastNameCol = new TableColumn<>("Nachname");
        lastNameCol.setCellValueFactory(new PropertyValueFactory<>("lastName"));

        TableColumn<Person, String> cityCol = new TableColumn<>("Stadt");
        cityCol.setCellValueFactory(new PropertyValueFactory<>("city"));

        TableColumn<Person, String> countryCol = new TableColumn<>("Land");
        countryCol.setCellValueFactory(new PropertyValueFactory<>("country"));

        TableColumn<Person, String> ageCol = new TableColumn<>("Alter");
        ageCol.setCellValueFactory(new PropertyValueFactory<>("age"));

        tableView.getColumns().addAll(firstNameCol, lastNameCol, cityCol, countryCol, ageCol);

        ObservableList<Person> data = FXCollections.observableArrayList(
            new Person("Anna", "Müller", "Hamburg", "Deutschland", "28"),
            new Person("Max", "Schmidt", "München", "Deutschland", "34"),
            new Person("Sophie", "Wagner", "Berlin", "Deutschland", "42"),
            new Person("Lukas", "Becker", "Köln", "Deutschland", "25"),
            new Person("Emma", "Hoffmann", "Frankfurt am Main, also im Westen", "Deutschland", "31"),
            new Person("Felix", "Schäfer", "Stuttgart", "Deutschland", "29"),
            new Person("Mia", "Koch", "Düsseldorf", "Deutschland", "37")
        );

        tableView.setItems(data);
        tableView.setEditable(true);
        
        for (TableColumn<Person, ?> column : tableView.getColumns()) {
            column.setSortable(false);
            column.setEditable(true);
            ((TableColumn<Person, String>) column).setCellFactory(TextFieldTableCell.forTableColumn());
        }

        // VBox als Container (wie in TableViewExample)
        VBox content = new VBox(tableView);
        content.getStyleClass().add("my-dialog-vbox");
        
        // In Dialog einbauen
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.OK);
        
        // Versuch 1: Einfach anzeigen und schauen was passiert
        System.out.println("=== Versuch 1: Dialog ohne manuelle Anpassung ===");
        Platform.runLater(() -> {
            dialog.show();
            
            // Nach dem Show: Größen ausgeben
            Platform.runLater(() -> {
                Window window = dialog.getDialogPane().getScene().getWindow();
                System.out.println("Dialog Window: " + window.getWidth() + " x " + window.getHeight());
                System.out.println("DialogPane: " + dialog.getDialogPane().getWidth() + " x " + dialog.getDialogPane().getHeight());
                System.out.println("Content VBox: " + content.getWidth() + " x " + content.getHeight());
                System.out.println("TableView: " + tableView.getWidth() + " x " + tableView.getHeight());
                
                System.out.println("TableView Items: " + tableView.getItems().size());
                System.out.println("TableView PrefHeight: " + tableView.getPrefHeight());
                System.out.println("TableView MinHeight: " + tableView.getMinHeight());
                System.out.println("TableView MaxHeight: " + tableView.getMaxHeight());
                
                // Versuch 2: sizeToScene() aufrufen
                System.out.println("\n=== Versuch 2: sizeToScene() nach Show ===");
                window.sizeToScene();
                
                Platform.runLater(() -> {
                    System.out.println("Nach sizeToScene():");
                    System.out.println("Dialog Window: " + window.getWidth() + " x " + window.getHeight());
                    System.out.println("DialogPane: " + dialog.getDialogPane().getWidth() + " x " + dialog.getDialogPane().getHeight());
                    System.out.println("Content VBox: " + content.getWidth() + " x " + content.getHeight());
                    System.out.println("TableView: " + tableView.getWidth() + " x " + tableView.getHeight());
                });
            });
        });
        
        primaryStage.setTitle("Parent Stage (leer)");
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}