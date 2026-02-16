package Controllers;

import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import models.Personne;
import models.Preference;
import models.Reservation;
import models.Ticket;
import services.PersonneService;
import services.PreferenceService;
import services.ReservationService;
import services.TicketService;
import util.Session;

import java.io.IOException;
import java.sql.Date;
import java.time.format.DateTimeFormatter;

public class DashboardController {


    @FXML private Tab activitetab;
    @FXML private TabPane activitetabpanmain;

    @FXML private Tab admintab;

    @FXML private Tab attractiontab;
    @FXML private Tab commentairetab;

    @FXML private Tab destinationtab;
    @FXML private TabPane destinationtabpanmain;

    @FXML private Tab posttab;
    @FXML private TabPane posttabpanmain;

    @FXML private Tab reservationstab;
    @FXML private TabPane reservationtabpanmain;

    @FXML private Tab reviewtab;

    @FXML private Tab ticketstab;

    @FXML private Tab usertab;
    @FXML private Tab preferencetab;
    @FXML private TabPane usertabpanmain;

    @FXML private TableView<?> tableactivite;
    @FXML private TableView<?> tableactivite1111;
    @FXML private TableView<?> tabledestination;
    @FXML private TableView<?> tablepost;
    @FXML private TableView<Personne> tableuser;
    @FXML private TableView<Preference> tablePreferences;

    @FXML private TableColumn<Personne, Integer> colUserId;
    @FXML private TableColumn<Personne, String> colUserNom;
    @FXML private TableColumn<Personne, String> colUserPrenom;
    @FXML private TableColumn<Personne, String> colUserEmail;
    @FXML private TableColumn<Personne, String> colUserRole;
    @FXML private TableColumn<Personne, String> colUserStatut;
    @FXML private TableColumn<Personne, String> colUserDateInsc;
    @FXML private TableColumn<Preference, String> colPrefUser;
    @FXML private TableColumn<Preference, Double> colPrefBudgetMin;
    @FXML private TableColumn<Preference, Double> colPrefBudgetMax;
    @FXML private TableColumn<Preference, String> colPrefTypes;
    @FXML private TableColumn<Preference, String> colPrefCentres;
    private TicketService ticketService = new TicketService();
    private PersonneService personneService = new PersonneService();
    private PreferenceService preferenceService = new PreferenceService();
    private ObservableList<Personne> userList = FXCollections.observableArrayList();
    private ObservableList<Preference> preferenceList = FXCollections.observableArrayList();
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    @FXML
    private TableView<Ticket> tableTicket;
    private Reservation selectedReservation;
    private Reservation currentReservationForTickets;



    @FXML private TableView<Reservation> tableReservation;

    @FXML private TableColumn<Reservation, Date> colDateReservation;
    @FXML private TableColumn<Reservation, Date> colDateDebut;
    @FXML private TableColumn<Reservation, Date> colDateFin;
    @FXML private TableColumn<Reservation, String> colStatut;
    @FXML private TableColumn<Reservation, Double> colCoutTotal;
    @FXML private TableColumn<Reservation, String> colDestination;
    @FXML private TableColumn<Reservation, Integer> colNbrTickets;
    @FXML private TableColumn<Reservation, Void> colDeleteReservation;
    @FXML private TableColumn<Reservation, Void> colTicketReservation;


    @FXML private TableColumn<Ticket, Date> colDateDebut1;
    @FXML private TableColumn<Ticket, Date> colDateFin1;
    @FXML private TableColumn<Ticket, String> colStatut1;
    @FXML private TableColumn<Ticket, Double> colPrix;
    @FXML private TableColumn<Ticket, String> colType;
    @FXML private TableColumn<Ticket, Void> colDeleteTicket;




    @FXML private ToggleButton dashactbut;
    @FXML private ToggleButton dashdesbut;
    @FXML private ToggleButton dashpostbut;
    @FXML private ToggleButton dashresbut;
    @FXML private ToggleButton dashuserbut;


    private final ToggleGroup dashboardGroup = new ToggleGroup();



    @FXML
    public void initialize() {


        dashuserbut.setToggleGroup(dashboardGroup);
        dashdesbut.setToggleGroup(dashboardGroup);
        dashresbut.setToggleGroup(dashboardGroup);
        dashactbut.setToggleGroup(dashboardGroup);
        dashpostbut.setToggleGroup(dashboardGroup);


        dashboardGroup.selectedToggleProperty().addListener((obs, oldT, newT) -> {
            if (newT == null) {
                Platform.runLater(() -> dashboardGroup.selectToggle(oldT));
            }
            applySelectedStyles();
        });


        hideAllPanes();
        showPane(usertabpanmain);


        dashboardGroup.selectToggle(dashuserbut);
        applySelectedStyles();

        addDeleteButton();
        addTicketButton();
        initReservationTable();
        initTicketTable();
        addDeleteTicketButton();
        initUsersTable();
        initPreferencesTable();

        ticketstab.setOnSelectionChanged(event -> {
            if (ticketstab.isSelected()) {

                if (currentReservationForTickets == null) {
                    // 🔥 No reservation selected → show ALL tickets
                    refreshTicketTable();
                } else {
                    // Reservation selected → show its tickets
                    loadTicketsByReservation(currentReservationForTickets.getId());
                }
            }
        });
        reservationstab.setOnSelectionChanged(event -> {
            if (reservationstab.isSelected()) {
                currentReservationForTickets = null;
            }
        });



        Platform.runLater(() -> {
            Scene scene = dashuserbut.getScene();
            Stage stage = (Stage) scene.getWindow();

            scene.setOnKeyPressed(event -> {
                if (event.getCode() == javafx.scene.input.KeyCode.ESCAPE && stage.isMaximized()) {
                    stage.setMaximized(false);
                }
            });
        });
        tableReservation.widthProperty().addListener((obs, oldW, newW) -> {
            double w = newW.doubleValue();
            double available = w - 20;

            colDateReservation.setPrefWidth(available * 0.15);
            colDateDebut.setPrefWidth(available * 0.15);
            colDateFin.setPrefWidth(available * 0.15);
            colStatut.setPrefWidth(available * 0.12);
            colCoutTotal.setPrefWidth(available * 0.15);
            colDeleteReservation.setPrefWidth(available * 0.10);
        });
        tableTicket.widthProperty().addListener((obs, oldW, newW) -> {
            double w = newW.doubleValue();
            double available = w - 20;

            colType.setPrefWidth(available * 0.20);
            colPrix.setPrefWidth(available * 0.15);
            colStatut1.setPrefWidth(available * 0.15);
            colDateDebut1.setPrefWidth(available * 0.15);
            colDateFin1.setPrefWidth(available * 0.15);
            colDeleteTicket.setPrefWidth(available * 0.10);
        });


    }

    private ReservationService reservationService = new ReservationService();
    private ObservableList<Reservation> reservationList = FXCollections.observableArrayList();

    private void initReservationTable() {

        colDateReservation.setCellValueFactory(new PropertyValueFactory<>("dateReservation"));
        colDateDebut.setCellValueFactory(new PropertyValueFactory<>("dateDebut"));
        colDateFin.setCellValueFactory(new PropertyValueFactory<>("dateFin"));
        colStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));
        colCoutTotal.setCellValueFactory(cellData -> {
            int reservationId = cellData.getValue().getId();
            double total = ticketService.sumPrixByReservation(reservationId);
            return new SimpleDoubleProperty(total).asObject();
        });


        colDestination.setCellValueFactory(cellData -> {
            int destId = cellData.getValue().getDestinationId();
            String nomDestination = reservationService.getDestinationNomById(destId);
            return new SimpleStringProperty(nomDestination);
        });

        // ⚠️ colonne calculée (nbr tickets)
        colNbrTickets.setCellValueFactory(cellData -> {
            int reservationId = cellData.getValue().getId();
            int count = ticketService.countTicketsByReservation(reservationId);
            return new SimpleIntegerProperty(count).asObject();
        });

        tableReservation.setRowFactory(tv -> {
            TableRow<Reservation> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    Reservation r = row.getItem();
                    openEditPopup(r);
                }
            });
            return row;
        });
        tableReservation.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {

            selectedReservation = newSelection;


            if (newSelection != null) {
                loadTicketsByReservation(newSelection.getId());
            }
        });

        refreshReservationTable();
    }

    private void refreshReservationTable() {
        reservationList.setAll(reservationService.getAll());
        tableReservation.setItems(reservationList);

    }

    private ObservableList<Ticket> ticketList = FXCollections.observableArrayList();

    private void initTicketTable() {

        colType.setCellValueFactory(new PropertyValueFactory<>("type"));
        colPrix.setCellValueFactory(new PropertyValueFactory<>("prix"));
        colStatut1.setCellValueFactory(new PropertyValueFactory<>("statut"));
        colDateDebut1.setCellValueFactory(new PropertyValueFactory<>("dateDebut"));
        colDateFin1.setCellValueFactory(new PropertyValueFactory<>("dateFin"));


        // 🔹 Double click pour modifier
        tableTicket.setRowFactory(tv -> {
            TableRow<Ticket> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    Ticket t = row.getItem();
                    openEditTicketPopup(t);
                }
            });
            return row;
        });

        refreshTicketTable();
    }
    private void refreshTicketTable() {
        ticketList.setAll(ticketService.getAll());
        tableTicket.setItems(ticketList);
    }
    private void loadTicketsByReservation(int reservationId) {

        ticketList.setAll(
                ticketService.getTicketsByReservation(reservationId)
        );

        tableTicket.setItems(ticketList);
    }





    private void hideAllPanes() {
        hidePane(usertabpanmain);
        hidePane(activitetabpanmain);
        hidePane(destinationtabpanmain);
        hidePane(reservationtabpanmain);
        hidePane(posttabpanmain);
    }

    private void hidePane(TabPane p) {
        if (p == null) return;
        p.setVisible(false);
        p.setManaged(false);
    }

    private void showPane(TabPane paneToShow) {
        hideAllPanes();

        if (paneToShow == null) return;
        paneToShow.setVisible(true);
        paneToShow.setManaged(true);
        paneToShow.toFront();
    }

    private void applySelectedStyles() {
        styleToggle(dashuserbut);
        styleToggle(dashdesbut);
        styleToggle(dashresbut);
        styleToggle(dashactbut);
        styleToggle(dashpostbut);
    }

    private void styleToggle(ToggleButton b) {
        if (b == null) return;

        if (b.isSelected()) {
            b.setStyle(
                    "-fx-background-color: #98acd8;" +
                            "-fx-background-radius: 30 0 0 30;" +
                            "-fx-border-color: #18377C;" +
                            "-fx-border-width: 0 0 0 3;" +
                            "-fx-font-weight: 700;"
            );
        } else {
            b.setStyle("");
        }
    }

    @FXML
    void dashboardButtonClicked(ActionEvent event) {
        ToggleButton clicked = (ToggleButton) event.getSource();

        if (clicked == dashuserbut) {
            showPane(usertabpanmain);
        } else if (clicked == dashactbut) {
            showPane(activitetabpanmain);
        } else if (clicked == dashdesbut) {
            showPane(destinationtabpanmain);
        } else if (clicked == dashresbut) {
            showPane(reservationtabpanmain);
        } else if (clicked == dashpostbut) {
            showPane(posttabpanmain);
        }
    }

    @FXML
    void FXaddActivite(ActionEvent event) {

    }

    @FXML
    void closewindow(ActionEvent event) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.close();
    }

    @FXML
    void minwindow(ActionEvent event) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setIconified(true);
    }

    @FXML
    void maxwindow(ActionEvent event) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setMaximized(!stage.isMaximized());
    }

    @FXML
    void handleLogout(ActionEvent event) {
        Session.clear();
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/loginPage.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    void refreshUsersTable(ActionEvent event) {
        refreshUsersTable();
    }

    @FXML
    void refreshPreferencesTable(ActionEvent event) {
        refreshPreferencesTable();
    }

    private void initUsersTable() {
        colUserId.setCellValueFactory(cell -> new SimpleIntegerProperty(cell.getValue().getId()).asObject());
        colUserNom.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getNom()));
        colUserPrenom.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getPrenom()));
        colUserEmail.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getEmail()));
        colUserRole.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getRole()));
        colUserStatut.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getStatutCompte()));
        colUserDateInsc.setCellValueFactory(cell -> {
            var dt = cell.getValue().getDateInscription();
            String s = dt != null ? dt.format(DATE_FORMAT) : "";
            return new SimpleStringProperty(s);
        });
        userList.setAll(personneService.getAll());
        tableuser.setItems(userList);
    }

    private void refreshUsersTable() {
        userList.setAll(personneService.getAll());
        tableuser.setItems(userList);
    }

    private void initPreferencesTable() {
        colPrefUser.setCellValueFactory(cell -> {
            int pid = cell.getValue().getPersonneId();
            Personne p = personneService.getById(pid);
            String email = p != null ? p.getEmail() : "—";
            return new SimpleStringProperty(email);
        });
        colPrefBudgetMin.setCellValueFactory(cell -> {
            Double v = cell.getValue().getBudgetMin();
            return new SimpleDoubleProperty(v != null ? v : 0).asObject();
        });
        colPrefBudgetMax.setCellValueFactory(cell -> {
            Double v = cell.getValue().getBudgetMax();
            return new SimpleDoubleProperty(v != null ? v : 0).asObject();
        });
        colPrefTypes.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getTypesVoyage() != null ? cell.getValue().getTypesVoyage() : "—"));
        colPrefCentres.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getCentresInteret() != null ? cell.getValue().getCentresInteret() : "—"));
        preferenceList.setAll(preferenceService.getAll());
        tablePreferences.setItems(preferenceList);
    }

    private void refreshPreferencesTable() {
        preferenceList.setAll(preferenceService.getAll());
        tablePreferences.setItems(preferenceList);
    }
    @FXML
    private void openAddPopup(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(
                    getClass().getResource("/ajoutReservation.fxml")
            );

            Stage popupStage = new Stage();
            popupStage.setTitle("Add new reservation");

            // SET ICON
            popupStage.getIcons().add(
                    new Image(getClass().getResourceAsStream("/icons/logoblue.png"))
            );

            popupStage.initModality(Modality.APPLICATION_MODAL);

            // Attach popup to parent window (recommended)
            Stage parentStage = (Stage) ((Node) event.getSource())
                    .getScene().getWindow();
            popupStage.initOwner(parentStage);

            popupStage.setScene(new Scene(root));
            popupStage.setResizable(false); // optional
            popupStage.showAndWait();
            refreshReservationTable();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void addDeleteButton() {

        colDeleteReservation.setCellFactory(param -> new TableCell<>() {

            private final Button deleteBtn = new Button();

            {
                ImageView icon = new ImageView(new Image(
                        getClass().getResourceAsStream("/icons/poubelle.png")
                ));
                icon.setFitWidth(20);
                icon.setFitHeight(20);

                deleteBtn.setGraphic(icon);
                deleteBtn.setStyle("""
                -fx-background-color: transparent;
                -fx-padding: 0;
                -fx-cursor: hand;
            """);

                deleteBtn.setOnAction(e -> {
                    Reservation reservation = getTableView().getItems().get(getIndex());

                    reservationService.delete(reservation);      // DB
                    getTableView().getItems().remove(reservation); // UI
                    refreshReservationTable();
                    tableTicket.getItems().clear();




                    currentReservationForTickets = null;
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : deleteBtn);
            }
        });
    }
    private void openEditPopup(Reservation reservation) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ajoutReservation.fxml"));
            Parent root = loader.load();

            // Récupérer le controller du popup
            AjouterReservationController popupController = loader.getController();

            // Pré-remplir les champs
            popupController.setReservation(reservation);

            Stage popupStage = new Stage();
            popupStage.setTitle("Modifier Reservation");
            popupStage.initModality(Modality.APPLICATION_MODAL);
            popupStage.setScene(new Scene(root));
            popupStage.showAndWait();

            // Après fermeture, refresh TableView
            refreshReservationTable();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @FXML
    private void openAddTicketPopup(ActionEvent event) {

        // Use selectedReservation if no ticket button clicked
        if (currentReservationForTickets == null) {
            if (selectedReservation != null) {
                currentReservationForTickets = selectedReservation;
            } else {
                Alert alert = new Alert(Alert.AlertType.WARNING,
                        "Veuillez sélectionner une réservation !");
                alert.showAndWait();
                return;
            }
        }

        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/ajoutTicket.fxml")
            );

            Parent root = loader.load();
            TicketController popupController = loader.getController();

            // Pass the reservation
            popupController.setReservation(currentReservationForTickets);

            Stage popupStage = new Stage();
            popupStage.setTitle("Add Ticket");
            popupStage.initModality(Modality.APPLICATION_MODAL);
            popupStage.setScene(new Scene(root));
            popupStage.showAndWait();

            // refresh tickets
            loadTicketsByReservation(currentReservationForTickets.getId());

            refreshReservationTable();




        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void addTicketButton() {

        colTicketReservation.setCellFactory(param -> new TableCell<>() {

            private final Button ticketBtn = new Button();

            {
                ImageView icon = new ImageView(new Image(
                        getClass().getResourceAsStream("/icons/ticket.png")
                ));
                icon.setFitWidth(22);
                icon.setFitHeight(27);

                ticketBtn.setGraphic(icon);
                ticketBtn.setStyle("""
                -fx-background-color: transparent;
                -fx-padding: 0;
                -fx-cursor: hand;
            """);

                ticketBtn.setOnAction(e -> {

                    Reservation reservation =
                            getTableView().getItems().get(getIndex());

                    // 🔥 IMPORTANT
                    currentReservationForTickets = reservation;

                    // Switch to ticket tab
                    reservationtabpanmain.getSelectionModel().select(ticketstab);

                    // Load tickets
                    loadTicketsByReservation(reservation.getId());
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : ticketBtn);
            }
        });
    }




    private void addDeleteTicketButton() {

        colDeleteTicket.setCellFactory(param -> new TableCell<>() {

            private final Button deleteBtn = new Button();

            {
                ImageView icon = new ImageView(new Image(
                        getClass().getResourceAsStream("/icons/poubelle.png")
                ));
                icon.setFitWidth(20);
                icon.setFitHeight(20);

                deleteBtn.setGraphic(icon);
                deleteBtn.setStyle("""
                -fx-background-color: transparent;
                -fx-padding: 0;
                -fx-cursor: hand;
            """);

                deleteBtn.setOnAction(e -> {
                    Ticket ticket = getTableView().getItems().get(getIndex());

                    ticketService.delete(ticket);        // DB
                    getTableView().getItems().remove(ticket); // UI
                    if (currentReservationForTickets != null) {
                        loadTicketsByReservation(currentReservationForTickets.getId());
                    } else {
                        refreshTicketTable(); // show all tickets
                    }
                    refreshReservationTable();


                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : deleteBtn);
            }
        });
    }

    private void openEditTicketPopup(Ticket ticket) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ajoutTicket.fxml"));
            Parent root = loader.load();

            TicketController popupController = loader.getController();

            // Pré-remplir
            popupController.setTicket(ticket);

            Stage popupStage = new Stage();
            popupStage.setTitle("Modifier Ticket");
            popupStage.initModality(Modality.APPLICATION_MODAL);
            popupStage.setScene(new Scene(root));
            popupStage.showAndWait();

            refreshTicketTable(); // refresh après modification
            refreshReservationTable();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }




}
