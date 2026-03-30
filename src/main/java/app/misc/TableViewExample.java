package app.misc;

import java.util.concurrent.atomic.AtomicBoolean;

import app.config.Config;
import app.ui.skin.SkinService;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class TableViewExample extends Application {

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
    public void start(final Stage primaryStage) {
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
        }

        for (TableColumn<Person, ?> column : tableView.getColumns()) {
            column.setEditable(true);
            ((TableColumn<Person, String>) column).setCellFactory(TextFieldTableCell.forTableColumn());
        }

        VBox root = new VBox(tableView);
        Scene scene = new Scene(root, -1, -1);

        Config.init("C:/Users/Markgraf/OneDrive/ThosSuite/");
        SkinService.get().styleScene(scene);
        
        primaryStage.setScene(scene);
        primaryStage.setTitle("TableView Example");
        
     // Start
        primaryStage.setWidth(2000);
        primaryStage.setOpacity(0);
        primaryStage.show();

        AtomicBoolean measured = new AtomicBoolean(false);

        Runnable[] listenerRef = new Runnable[1];
        double[] lastWidth = {-1};

        listenerRef[0] = () -> {
            double currentWidth = tableView.getWidth();
            
            // Warte bis TableView sich stabilisiert hat
            if (Math.abs(currentWidth - lastWidth[0]) >= 1) {
                lastWidth[0] = currentWidth;
                return;
            }
            
            if (measured.get()) return;
            
            // Jetzt messen (TableView ist stabil)
            double spaltenSumme = tableView.getColumns().stream()
                .mapToDouble(TableColumn::getWidth)
                .sum();
            
            System.out.println(spaltenSumme);
            
            double tableOverhead = tableView.getWidth() - spaltenSumme;
            double windowOverhead = primaryStage.getWidth() - tableView.getWidth();
            
            double targetWidth = spaltenSumme + windowOverhead;
            primaryStage.setWidth(targetWidth);
            
            measured.set(true);
            scene.removePostLayoutPulseListener(listenerRef[0]);
            primaryStage.setOpacity(1);
            
            System.out.println("Spaltensumme: " + spaltenSumme);
            System.out.println("tableOverhead: " + tableOverhead);
            System.out.println("windowOverhead: " + windowOverhead);
            System.out.println("targetWidth: " + targetWidth);
            System.out.println("Stage-Breite nach setWidth: " + primaryStage.getWidth());
            
        };
        
        
        
        scene.addPostLayoutPulseListener(listenerRef[0]);
    }

    public static void main(String[] args) {
        launch(args);
    }
    
    public static ScrollBar getHorizontalScrollbar(TableView<?> table) {
        for (Node n : table.lookupAll(".scroll-bar")) {
            if (n instanceof ScrollBar) {
                ScrollBar bar = (ScrollBar) n;
                if (bar.getOrientation() == Orientation.HORIZONTAL) {
                    return bar;
                }
            }
        }
        return null;
    }
}