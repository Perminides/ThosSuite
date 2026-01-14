package app.misc;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import app.config.Config;
import app.ui.skin.SkinService;
import javafx.application.Application;
import javafx.application.Platform;
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
        
        AtomicInteger currentWidth = new AtomicInteger(750);
        AtomicInteger upperBound = new AtomicInteger(2000);
        AtomicInteger lowerBound = new AtomicInteger(currentWidth.get()-10);
        
        primaryStage.setWidth(currentWidth.get());
        root.layout();
        root.applyCss();
        primaryStage.setOpacity(0);
        final long start = System.currentTimeMillis();
        // Achtung: Das ganze ist nicht blocking. Bei der Integration in die Suite wird das zu einem Problem!
        primaryStage.show();

        
        final double[] lastTableViewWidth = {-2};
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        ScheduledFuture<?> timeout = executor.schedule(() -> {
            Platform.runLater(() -> {
                primaryStage.close();
                // Error handling
                System.out.println("Also länger warte ich aber nicht...");
                executor.shutdown(); 
            });
        }, 5, TimeUnit.SECONDS);
        
        
        Runnable[] listenerRef = new Runnable[1];
        listenerRef[0] = () -> {
            System.out.print(System.currentTimeMillis() - start + "ms: ");
            
            double currentTableViewWidth = tableView.getWidth();
            // Hat sich die TableView-Breite angepasst? Das braucht manchmal einen Pulse mehr
            if (Math.abs(currentTableViewWidth - lastTableViewWidth[0]) < 1) { // Ist ein double. Kleiner 1 sehen wir als keine wirkliche Änderung.
                System.out.println("Wir setzen einen Pulse aus - Stage: " + primaryStage.getWidth() + ", TableView: " + currentTableViewWidth);
                return;
            }
            
            // Jetzt ist die TableView angepasst, wir können prüfen
            lastTableViewWidth[0] = currentTableViewWidth;
            ScrollBar sb = getHorizontalScrollbar(tableView);
            
            if (upperBound.get() - lowerBound.get() <= 2) { // Das reicht uns :-)
                // Differenz klein genug, nehme obere Grenze
            	timeout.cancel(false);
            	executor.shutdown();
                primaryStage.setWidth(upperBound.get());
                scene.removePostLayoutPulseListener(listenerRef[0]);
                primaryStage.setOpacity(1);
                System.out.println("Fertig - Width: " + upperBound.get());
            } else {
                if (sb != null && sb.isVisible()) {
                    // Zu schmal, brauchen mehr Platz
                    System.out.println("Zu schmal bei " + currentWidth.get() + ", bounds: [" + lowerBound.get() + ", " + upperBound.get() + "]");
                    lowerBound.set(currentWidth.get());
                } else {
                    // Passt (keine ScrollBar oder nicht sichtbar)
                    System.out.println("Passt bei " + currentWidth.get() + ", bounds: [" + lowerBound.get() + ", " + upperBound.get() + "]" + " (" + sb + ") " + tableView.getWidth());
                    upperBound.set(currentWidth.get());
                }
                
                // Nächster Versuch: Mitte zwischen lower und upper
                currentWidth.set((lowerBound.get() + upperBound.get()) / 2);
                primaryStage.setWidth(currentWidth.get());
            }
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