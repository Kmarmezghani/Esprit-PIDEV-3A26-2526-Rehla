package Controllers;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Side;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import models.*;
import models.stats.AvgNoteRow;
import models.stats.NoteDistributionRow;
import models.stats.TopActiviteRow;
import services.*;
import services.PaysService;
import services.VilleService;
import services.AttractionService;
import Controllers.EditPaysController;
import Controllers.EditVilleController;
import Controllers.EditAttractionController;

import java.io.File;
import java.io.InputStream;
import java.sql.Date;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Missing imports added
import javafx.scene.layout.HBox;
import javafx.geometry.Pos;
import util.Session;

public class DashboardController {

    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

    @FXML private Tab activitetab;
    @FXML private Tab admintab;
    @FXML private Tab attractiontab;
    @FXML private Tab commentairetab;
    @FXML private Tab destinationtab;
    @FXML private Tab posttab;
    @FXML private Tab reservationstab;
    @FXML private Tab reviewtab;
    @FXML private Tab ticketstab;
    @FXML private Tab usertab;
    @FXML private Tab statstab;

    @FXML private TabPane activitetabpanmain;
    @FXML private TabPane destinationtabpanmain;
    @FXML private TabPane posttabpanmain;
    @FXML private TabPane reservationtabpanmain;
    @FXML private TabPane usertabpanmain;

    @FXML private TableView<Activite> tableactivite;

    @FXML private TableColumn<Activite, String> colnameactivite;
    @FXML private TableColumn<Activite, String> colimagectivite;
    @FXML private TableColumn<Activite, String> coldescriptionactivite;
    @FXML private TableColumn<Activite, Double> colpriceactivite;
    @FXML private TableColumn<Activite, String> coltypeactivite;
    @FXML private TableColumn<Activite, Double> colavgratactivite;
    @FXML private TableColumn<Activite, String> colguideactivite;
    @FXML private TableColumn<Activite, String> colDestinationactivite;
    @FXML private TableColumn<Activite, String> coldateDactivite;
    @FXML private TableColumn<Activite, String> coldateFactivite;
    @FXML private TableColumn<Activite, Void> colDeleteactivite;
    @FXML private TableColumn<Activite, String> colMaxPlaces;

    @FXML private TableView<Review> tableReview;
    @FXML private TableColumn<Review, String> coluserReview;
    @FXML private TableColumn<Review, String> colcommentReview;
    @FXML private TableColumn<Review, Double> colnoteReview;
    @FXML private TableColumn<Review, String> coldateReview;
    @FXML private TableColumn<Review, Void> colDeleteReview;

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

    @FXML private TableView<Ticket> tableTicket;
    @FXML private TableColumn<Ticket, String> colStatut1;
    @FXML private TableColumn<Ticket, Double> colPrix;
    @FXML private TableColumn<Ticket, String> colType;
    @FXML private TableColumn<Ticket, Void> colDeleteTicket;
    @FXML private TableColumn<Ticket, String> colDestinationTicket;



    @FXML private BarChart<String, Number> barTopActivities;
    @FXML private CategoryAxis xTopActivities;
    @FXML private NumberAxis yTopActivities;

    @FXML private PieChart pieNotes;

    @FXML private TableView<AvgNoteRow> tableAvgNotes;
    @FXML private TableColumn<AvgNoteRow, String> colNom;
    @FXML private TableColumn<AvgNoteRow, Double> colAvg;
    @FXML private TableColumn<AvgNoteRow, Integer> colCount;

    // Destination Tables
    @FXML private TableView<Pays> tablePays;
    @FXML private TableView<Ville> tableVille;
    @FXML private TableView<Attraction> tableAttraction;

    // Pays Columns
    @FXML private TableColumn<Pays, String> colNomPays;
    @FXML private TableColumn<Pays, String> colDescriptionPays;
    @FXML private TableColumn<Pays, Void> colActionsPays;

    // Ville Columns
    @FXML private TableColumn<Ville, String> colNomVille;
    @FXML private TableColumn<Ville, String> colPaysVille;
    @FXML private TableColumn<Ville, String> colTypeTourismeVille;
    @FXML private TableColumn<Ville, String> colSaisonVille;
    @FXML private TableColumn<Ville, Void> colActionsVille;

    // Attraction Columns
    @FXML private TableColumn<Attraction, String> colNomAttraction;
    @FXML private TableColumn<Attraction, String> colDescriptionAttraction;
    @FXML private TableColumn<Attraction, String> colTypeAttraction;
    @FXML private TableColumn<Attraction, Double> colPrixAttraction;
    @FXML private TableColumn<Attraction, String> colHorairesAttraction;
    @FXML private TableColumn<Attraction, String> colVilleAttraction;
    @FXML private TableColumn<Attraction, Void> colActionsAttraction;

    @FXML private ToggleButton dashactbut;
    @FXML private ToggleButton dashdesbut;
    @FXML private ToggleButton dashpostbut;
    @FXML private ToggleButton dashresbut;
    @FXML private ToggleButton dashuserbut;

    private final ToggleGroup dashboardGroup = new ToggleGroup();

    // Destination Services
    private final PaysService paysService = new PaysService();
    private final VilleService villeService = new VilleService();
    private final AttractionService attractionService = new AttractionService();

    // Destination Lists
    private final ObservableList<Pays> paysList = FXCollections.observableArrayList();
    private final ObservableList<Ville> villeList = FXCollections.observableArrayList();
    private final ObservableList<Attraction> attractionList = FXCollections.observableArrayList();

    private final ActiviteService activiteService = new ActiviteService();
    private final ObservableList<Activite> activiteList = FXCollections.observableArrayList();
    private Activite selectedActivite;

    private final ReviewService reviewService = new ReviewService();
    private final ObservableList<Review> reviewList = FXCollections.observableArrayList();

    private final TicketService ticketService = new TicketService();
    private final ReservationService reservationService = new ReservationService();

    private final ObservableList<Reservation> reservationList = FXCollections.observableArrayList();
    private final ObservableList<Ticket> ticketList = FXCollections.observableArrayList();

    private Reservation selectedReservation;
    private Reservation currentReservationForTickets;

    private final StatsService statsService = new StatsService();
    @FXML private TableView<Commentaire> tableCommentaire;



    @FXML
    private Button btnNotif;


    @FXML private TableView<Post> tablepost;

    @FXML private TableColumn<Post, Integer> colIdPost;

    @FXML private TableColumn<Post, String> colContenuPost;
    @FXML private TableColumn<Post, java.time.LocalDate> colDatePost;
    @FXML private TableColumn<Post, Integer> colPopularitePost;
    @FXML private TableColumn<Post, String> colAuteurPost;
    @FXML private TableColumn<Post, Integer> colLikesPost;
    @FXML
    private TableColumn<Post, Void> colAction;
    @FXML
    private TableColumn<Commentaire, Void> colAction2;

    @FXML
    private TableColumn<Post, String> colImagePost;


    @FXML private TableColumn<Commentaire, Integer> colIdCom;
    @FXML private TableColumn<Commentaire, String> colContenuCom;
    @FXML private TableColumn<Commentaire, java.time.LocalDate> colDateCom;
    @FXML private TableColumn<Commentaire, String> colAuteurCom;
    @FXML private TableColumn<Commentaire, String> colPostCom;


    @FXML private Tab preferencetab;
    private ContextMenu notifMenu = new ContextMenu();
    @FXML private TableView<Personne> tableuser;
    @FXML private TableView<Preference> tablePreferences;
    //-------------------------------------------------------------------
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

//--------------------------------------------------------------

    @FXML private TableView<FavorisPost> tableFavoris;

    @FXML private TableColumn<FavorisPost, String> colFavUser;
    @FXML private TableColumn<FavorisPost, String> colFavPost;
    @FXML private TableColumn<FavorisPost, String> colFavDate;
    @FXML private TableColumn<FavorisPost, Void> colFavAction;

    private ObservableList<FavorisPost> favorisList = FXCollections.observableArrayList();
    private FavorisService favorisService = new FavorisService();


    private PersonneService personneService = new PersonneService();
    private PreferenceService preferenceService = new PreferenceService();
    private ObservableList<Personne> userList = FXCollections.observableArrayList();
    private ObservableList<Preference> preferenceList = FXCollections.observableArrayList();
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private NotificationService notificationService = new NotificationService();

    private CommentaireService commentaireService = new CommentaireService();
    private ObservableList<Commentaire> commentaireList = FXCollections.observableArrayList();
    private PostService postService = new PostService();
    private ObservableList<Post> postList = FXCollections.observableArrayList();

    Personne currentUser = Session.getCurrentUser();
    //----------------------------------------------------
    @FXML private Label lblTotalUsers;
    @FXML private Label lblTotalPosts;
    @FXML private Label lblTotalComments;
    @FXML private Label lblTotalReservations;

    private AnalyticsReservationService analyticsService = new AnalyticsReservationService();
    //-------------------------------------------------------

    @FXML
    public void initialize() {
        System.out.println("Current user: " + currentUser);
        if(currentUser == null){
            btnNotif.setVisible(false);
            btnNotif.setManaged(false);
            return;
        }

        dashuserbut.setToggleGroup(dashboardGroup);
        dashdesbut.setToggleGroup(dashboardGroup);
        dashresbut.setToggleGroup(dashboardGroup);
        dashactbut.setToggleGroup(dashboardGroup);
        dashpostbut.setToggleGroup(dashboardGroup);

        dashboardGroup.selectedToggleProperty().addListener((obs, oldT, newT) -> {
            if (newT == null) Platform.runLater(() -> dashboardGroup.selectToggle(oldT));
            applySelectedStyles();
        });

        hideAllPanes();
        showPane(usertabpanmain);

        dashboardGroup.selectToggle(dashuserbut);
        applySelectedStyles();
        Platform.runLater(() -> {
            Scene scene = dashuserbut.getScene();
            Stage stage = (Stage) scene.getWindow();

            scene.setOnKeyPressed(event -> {
                if (event.getCode() == javafx.scene.input.KeyCode.ESCAPE && stage.isMaximized()) {
                    stage.setMaximized(false);
                }
            });
        });

        initActiviteTable();
        initReviewTable();
        initReservationTable();
        initTicketTable();

        // Initialize Destination Tables
        initPaysTable();
        initVilleTable();
        initAttractionTable();
        loadPosts();
        loadCommentaires();
        loadFavoris();
        initUsersTable();
        initPreferencesTable();

        btnNotif.setOnAction(e -> toggleNotifications());

        addDeleteButtonsToColumn(colAction,
                post -> deletePost((post)
        ));
        addDeleteButtonsToColumn(colAction2,
                com -> deleteComment((com)
        ));

        addImageColumn();

        addDeleteReservationButton();
        addTicketButton();
        addDeleteTicketButton();

        initStatsTables();

        ticketstab.setOnSelectionChanged(event -> {
            if (ticketstab.isSelected()) {
                if (currentReservationForTickets == null) refreshTicketTable();
                else loadTicketsByReservation(currentReservationForTickets.getId());
            }
        });

        reservationstab.setOnSelectionChanged(event -> {
            if (reservationstab.isSelected()) currentReservationForTickets = null;
        });

        tableactivite.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            selectedActivite = newSelection;
        });

        reviewtab.setOnSelectionChanged(event -> {
            if (reviewtab.isSelected()) loadReviewsForSelectedActivite();
        });

        if (statstab != null) {
            statstab.setOnSelectionChanged(e -> {
                if (statstab.isSelected()) refreshActivityStats(null);
            });
        }

        tableactivite.setRowFactory(tv -> {
            TableRow<Activite> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) openEditPopup(row.getItem());
            });
            return row;
        });

        tableReservation.widthProperty().addListener((obs, o, n) -> {
            double available = n.doubleValue() - 20;
            colDateReservation.setPrefWidth(available * 0.15);
            colDateDebut.setPrefWidth(available * 0.15);
            colDateFin.setPrefWidth(available * 0.15);
            colStatut.setPrefWidth(available * 0.12);
            colCoutTotal.setPrefWidth(available * 0.15);
            colDeleteReservation.setPrefWidth(available * 0.10);
        });

        tableTicket.widthProperty().addListener((obs, o, n) -> {
            double available = n.doubleValue() - 20;
            colType.setPrefWidth(available * 0.20);
            colPrix.setPrefWidth(available * 0.15);
            colStatut1.setPrefWidth(available * 0.15);

            colDeleteTicket.setPrefWidth(available * 0.10);
            colDestinationTicket.setPrefWidth(available * 0.10);
        });

        tableactivite.widthProperty().addListener((obs, oldW, newW) -> {
            double available = newW.doubleValue() - 20;

            colnameactivite.setPrefWidth(available * 0.10);
            colimagectivite.setPrefWidth(available * 0.08);
            coldescriptionactivite.setPrefWidth(available * 0.20);
            coldateDactivite.setPrefWidth(available * 0.10);
            coldateFactivite.setPrefWidth(available * 0.10);
            colpriceactivite.setPrefWidth(available * 0.08);
            coltypeactivite.setPrefWidth(available * 0.09);
            colavgratactivite.setPrefWidth(available * 0.09);
            colDestinationactivite.setPrefWidth(available * 0.10);
            colguideactivite.setPrefWidth(available * 0.09);
            colMaxPlaces.setPrefWidth(available * 0.07);
            colDeleteactivite.setPrefWidth(available * 0.02);
        });

        tableReview.widthProperty().addListener((obs, oldW, newW) -> {
            double available = newW.doubleValue() - 20;
            coluserReview.setPrefWidth(available * 0.15);
            colcommentReview.setPrefWidth(available * 0.45);
            colnoteReview.setPrefWidth(available * 0.10);
            coldateReview.setPrefWidth(available * 0.20);
            colDeleteReview.setPrefWidth(available * 0.10);
        });

        if (tableAvgNotes != null) {
            tableAvgNotes.widthProperty().addListener((obs, oldW, newW) -> {
                double available = newW.doubleValue() - 20;
                colNom.setPrefWidth(available * 0.60);
                colAvg.setPrefWidth(available * 0.20);
                colCount.setPrefWidth(available * 0.20);
            });
        }

        Platform.runLater(() -> {
            Scene scene = dashuserbut.getScene();
            Stage stage = (Stage) scene.getWindow();

            scene.setOnKeyPressed(event -> {
                if (event.getCode() == javafx.scene.input.KeyCode.ESCAPE && stage.isMaximized()) {
                    stage.setMaximized(false);
                }
            });
        });

        lblTotalUsers.setText(String.valueOf(
                analyticsService.getTotalUsers()
        ));

        lblTotalPosts.setText(String.valueOf(
                analyticsService.getTotalPosts()
        ));

        lblTotalComments.setText(String.valueOf(
                analyticsService.getTotalComments()
        ));

        lblTotalReservations.setText(String.valueOf(
                analyticsService.getTotalReservations()
        ));
    }

    // =========================
    // ACTIVITES
    // =========================

    private void initActiviteTable() {
        colnameactivite.setCellValueFactory(new PropertyValueFactory<>("nom"));
        coldescriptionactivite.setCellValueFactory(new PropertyValueFactory<>("description"));
        colpriceactivite.setCellValueFactory(new PropertyValueFactory<>("prix"));
        coltypeactivite.setCellValueFactory(new PropertyValueFactory<>("typeActivite"));
        colavgratactivite.setCellValueFactory(new PropertyValueFactory<>("noteMoyenne"));

        // ✅✅✅ IMAGE COLUMN (comme ton ami) + support resources + relatif + absolu
        colimagectivite.setCellValueFactory(new PropertyValueFactory<>("image"));

        colimagectivite.setCellFactory(param -> new TableCell<>() {

            private final ImageView imageView = new ImageView();

            {
                imageView.setFitWidth(80);
                imageView.setFitHeight(60);
                imageView.setPreserveRatio(true);
                imageView.setSmooth(true);
            }

            @Override
            protected void updateItem(String imagePath, boolean empty) {
                super.updateItem(imagePath, empty);

                if (empty) {
                    setGraphic(null);
                    return;
                }

                // (Optionnel) pour éviter que les anciennes images restent affichées
                imageView.setImage(null);

                String p = (imagePath == null) ? "" : imagePath.trim();
                Image img = null;

                try {
                    // 1) resources (classpath): "/Backoffice/..." ou "/images/..."
                    if (!p.isEmpty() && p.startsWith("/")) {
                        try (InputStream is = getClass().getResourceAsStream(p)) {
                            if (is != null) {
                                img = new Image(is);
                            } else {
                                System.out.println("[IMG] Resource introuvable: " + p);
                            }
                        }
                    }

                    // 2) file system: "C:\..." ou "relative/path.png"
                    if (img == null && !p.isEmpty() && !p.startsWith("/")) {
                        File file = new File(p);

                        if (!file.exists()) {
                            file = new File(System.getProperty("user.dir"), p);
                        }

                        if (file.exists()) {
                            img = new Image(file.toURI().toString());
                        } else {
                            System.out.println("[IMG] File introuvable: " + file.getAbsolutePath());
                        }
                    }

                    // 3) placeholder obligatoire
                    if (img == null) {
                        try (InputStream is2 = getClass().getResourceAsStream("/Backoffice/icons/activity_placeholder.png")) {
                            if (is2 != null) {
                                img = new Image(is2);
                            } else {
                                System.out.println("[IMG] Placeholder introuvable: /Backoffice/icons/activity_placeholder.png");
                            }
                        }
                    }

                    if (img != null) {
                        imageView.setFitWidth(60);
                        imageView.setFitHeight(45);
                        imageView.setPreserveRatio(true);
                        imageView.setSmooth(true);

                        imageView.setImage(img);
                        setGraphic(imageView);
                    } else {
                        setGraphic(null); // si même le placeholder manque
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    setGraphic(null);
                }
            }
        });

        colguideactivite.setCellValueFactory(cellData -> {
            Activite a = cellData.getValue();
            String guideName = activiteService.getGuideNameByActiviteId(a.getGuideId());
            return new ReadOnlyStringWrapper(guideName);
        });

        colDestinationactivite.setCellValueFactory(cellData -> {
            Activite a = cellData.getValue();
            String label = activiteService.getDestinationNameById(a.getDestinationId());
            return new ReadOnlyStringWrapper(label);
        });

        coldateDactivite.setCellValueFactory(cellData -> {
            Activite a = cellData.getValue();
            String v = (a.getDateDebut() != null) ? a.getDateDebut().format(DT) : "";
            return new ReadOnlyStringWrapper(v);
        });

        coldateFactivite.setCellValueFactory(cellData -> {
            Activite a = cellData.getValue();
            String v = (a.getDateFin() != null) ? a.getDateFin().format(DT) : "";
            return new ReadOnlyStringWrapper(v);
        });

        colMaxPlaces.setCellValueFactory(cellData -> {
            Activite a = cellData.getValue();
            Integer mp = a.getMaxPlaces();
            String v = (mp == null) ? "No limit" : String.valueOf(mp);
            return new ReadOnlyStringWrapper(v);
        });

        tableactivite.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);

        refreshActiviteTable();
        addDeleteButton();
    }

    private void refreshActiviteTable() {
        activiteList.setAll(activiteService.getAll());
        tableactivite.setItems(activiteList);
    }

    private void addDeleteButton() {
        colDeleteactivite.setCellFactory(param -> new TableCell<>() {
            private final Button deleteBtn = new Button();

            {
                ImageView icon = new ImageView(new Image(getClass().getResourceAsStream("/Backoffice/icons/poubelle.png")));
                icon.setFitWidth(20);
                icon.setFitHeight(20);

                deleteBtn.setGraphic(icon);
                deleteBtn.setStyle("""
                    -fx-background-color: transparent;
                    -fx-padding: 0;
                    -fx-cursor: hand;
                """);

                deleteBtn.setOnAction(e -> {
                    Activite a = getTableView().getItems().get(getIndex());

                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                    confirm.setTitle("Confirmation");
                    confirm.setHeaderText("Delete activity");
                    confirm.setContentText("Are you sure you want to delete: " + a.getNom() + " ?");

                    confirm.showAndWait().ifPresent(btn -> {
                        if (btn == ButtonType.OK) {
                            activiteService.delete(a);
                            getTableView().getItems().remove(a);
                        }
                    });
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : deleteBtn);
            }
        });
    }

    @FXML
    private void openAddPopupactivite(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Backoffice/FormulaireAddActivite.fxml"));
            Parent root = loader.load();

            AjouterActiviteController controller = loader.getController();
            controller.setAdminMode(true);

            Stage popupStage = new Stage();
            popupStage.setTitle("Add new activity");
            popupStage.initModality(Modality.APPLICATION_MODAL);
            popupStage.setScene(new Scene(root));
            popupStage.showAndWait();

            refreshActiviteTable();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void openEditPopup(Activite activite) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Backoffice/FormulaireAddActivite.fxml"));
            Parent root = loader.load();

            AjouterActiviteController controller = loader.getController();
            controller.setAdminMode(true);
            controller.setActiviteToEdit(activite);

            Stage stage = new Stage();
            stage.setTitle("Edit Activity");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.showAndWait();

            refreshActiviteTable();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // =========================
    // REVIEWS
    // =========================

    private void initReviewTable() {
        coluserReview.setCellValueFactory(cellData ->
                new ReadOnlyStringWrapper(cellData.getValue().getUserName())
        );
        colcommentReview.setCellValueFactory(new PropertyValueFactory<>("commentaire"));
        colnoteReview.setCellValueFactory(new PropertyValueFactory<>("note"));

        coldateReview.setCellValueFactory(cellData -> {
            Review r = cellData.getValue();
            String dateStr = r.getDateAvis() != null ? r.getDateAvis().format(DT) : "";
            return new ReadOnlyStringWrapper(dateStr);
        });

        tableReview.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        addDeleteReviewButton();
        tableReview.setItems(reviewList);
    }

    private void addDeleteReviewButton() {
        colDeleteReview.setCellFactory(param -> new TableCell<>() {
            private final Button deleteBtn = new Button();

            {
                ImageView icon = new ImageView(new Image(getClass().getResourceAsStream("/Backoffice/icons/poubelle.png")));
                icon.setFitWidth(20);
                icon.setFitHeight(20);
                deleteBtn.setGraphic(icon);
                deleteBtn.setStyle("-fx-background-color: transparent; -fx-padding: 0; -fx-cursor: hand;");

                deleteBtn.setOnAction(e -> {
                    Review review = getTableView().getItems().get(getIndex());

                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                    confirm.setTitle("Confirmation");
                    confirm.setHeaderText("Delete review");
                    confirm.setContentText("Are you sure you want to delete this review?");

                    confirm.showAndWait().ifPresent(btn -> {
                        if (btn == ButtonType.OK) {
                            reviewService.delete(review);
                            getTableView().getItems().remove(review);
                        }
                    });
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : deleteBtn);
            }
        });
    }

    private void loadReviewsForSelectedActivite() {
        if (selectedActivite == null) {
            reviewList.clear();
            return;
        }
        reviewList.setAll(reviewService.getReviewsByActiviteId(selectedActivite.getId()));
    }

    // =========================
    // RESERVATIONS
    // =========================

    private void initReservationTable() {
        colDateReservation.setCellValueFactory(new PropertyValueFactory<>("dateReservation"));
        colDateDebut.setCellValueFactory(new PropertyValueFactory<>("dateDebut"));
        colDateFin.setCellValueFactory(new PropertyValueFactory<>("dateFin"));
        colStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));

        colCoutTotal.setCellValueFactory(cell -> {
            double total = ticketService.sumPrixByReservation(cell.getValue().getId());
            return new SimpleDoubleProperty(total).asObject();
        });

        colDestination.setCellValueFactory(cell -> {
            String nom = reservationService.getDestinationNomById(cell.getValue().getDestinationId());
            return new SimpleStringProperty(nom);
        });

        colNbrTickets.setCellValueFactory(cell -> {
            int count = ticketService.countTicketsByReservation(cell.getValue().getId());
            return new SimpleIntegerProperty(count).asObject();
        });

        tableReservation.setRowFactory(tv -> {
            TableRow<Reservation> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) openEditPopup(row.getItem());
            });
            return row;
        });

        tableReservation.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            selectedReservation = newV;
            if (newV != null) loadTicketsByReservation(newV.getId());
        });

        refreshReservationTable();
    }

    private void refreshReservationTable() {
        reservationList.setAll(reservationService.getAll());
        tableReservation.setItems(reservationList);
    }

    // =========================
    // TICKETS
    // =========================

    private void initTicketTable() {
        colType.setCellValueFactory(new PropertyValueFactory<>("type"));
        colPrix.setCellValueFactory(new PropertyValueFactory<>("prix"));
        colStatut1.setCellValueFactory(new PropertyValueFactory<>("statut"));

        colDestinationTicket.setCellValueFactory(new PropertyValueFactory<>("destinationNom"));

        tableTicket.setRowFactory(tv -> {
            TableRow<Ticket> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) openEditTicketPopup(row.getItem());
            });
            return row;
        });

        refreshTicketTable();
    }

    private void refreshTicketTable() {
        ticketList.setAll(ticketService.getAll());
        tableTicket.setItems(ticketList);
    }

    private void loadTicketsByReservation(int id) {
        ticketList.setAll(ticketService.getTicketsByReservation(id));
        tableTicket.setItems(ticketList);
    }

    // =========================
    // STATS
    // =========================

    private void initStatsTables() {
        if (tableAvgNotes == null) return;
        colNom.setCellValueFactory(new PropertyValueFactory<>("nom"));
        colAvg.setCellValueFactory(new PropertyValueFactory<>("avgNote"));
        colCount.setCellValueFactory(new PropertyValueFactory<>("nbAvis"));
    }

    @FXML
    private void refreshActivityStats(ActionEvent event) {
        try {
            if (barTopActivities != null) {
                List<TopActiviteRow> top = statsService.getTopActivitiesByInscriptions(7);

                barTopActivities.getData().clear();

                XYChart.Series<String, Number> s = new XYChart.Series<>();
                for (TopActiviteRow r : top) {
                    s.getData().add(new XYChart.Data<>(r.nom(), r.nbAvis()));
                }
                barTopActivities.getData().add(s);

                int max = top.stream().mapToInt(TopActiviteRow::nbAvis).max().orElse(1);

                yTopActivities.setAutoRanging(false);
                yTopActivities.setLowerBound(0);
                yTopActivities.setUpperBound(max + 1);
                yTopActivities.setTickUnit(1);

                Platform.runLater(() -> {
                    barTopActivities.lookupAll(".default-color0.chart-bar").forEach(node ->
                            node.setStyle("-fx-bar-fill: #002b11;")
                    );
                });
            }

            if (pieNotes != null) {
                List<NoteDistributionRow> dist = statsService.getNoteDistribution();

                Map<Integer, Integer> map = new HashMap<>();
                for (NoteDistributionRow row : dist) map.put(row.getNote(), row.getNb());

                ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
                for (int note = 1; note <= 5; note++) {
                    int nb = map.getOrDefault(note, 0);
                    pieData.add(new PieChart.Data(note + " ★", nb));
                }
                pieNotes.setData(pieData);
            }

            if (tableAvgNotes != null) {
                List<AvgNoteRow> avg = statsService.getAvgNotesByActivity();
                tableAvgNotes.setItems(FXCollections.observableArrayList(avg));
            }

        } catch (SQLException e) {
            e.printStackTrace();
            Alert a = new Alert(Alert.AlertType.ERROR, "Erreur SQL: " + e.getMessage());
            a.showAndWait();
        }
    }

    // =========================
    // UI + POPUPS
    // =========================

    private void applySelectedStyles() {
        styleToggle(dashuserbut);
        styleToggle(dashdesbut);
        styleToggle(dashresbut);
        styleToggle(dashactbut);
        styleToggle(dashpostbut);
    }
    @FXML
    private void openAnalysisPopup() {

        try {

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/Back" +
                            "office/AdminDashboard.fxml")
            );

            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("Admin Analytics");
            stage.setScene(new Scene(root));

            // Makes it popup modal
            stage.initModality(Modality.APPLICATION_MODAL);

            stage.setWidth(900);
            stage.setHeight(600);

            stage.showAndWait();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void openEditPopup(Reservation reservation) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Backoffice/ajoutReservation.fxml"));
            Parent root = loader.load();

            AjouterReservationController popupController = loader.getController();
            popupController.setReservation(reservation);

            Stage popupStage = new Stage();
            popupStage.setTitle("Modifier Reservation");
            popupStage.initModality(Modality.APPLICATION_MODAL);
            popupStage.setScene(new Scene(root));
            popupStage.showAndWait();

            refreshReservationTable();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void openEditTicketPopup(Ticket ticket) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Backoffice/ajoutTicket.fxml"));
            Parent root = loader.load();

            TicketController popupController = loader.getController();
            popupController.setTicket(ticket);

            Integer reservationId = ticket.getReservationId();
            if (reservationId != null) {
                Reservation reservation = reservationService.getById(reservationId);
                popupController.setReservation(reservation);
            }

            Stage popupStage = new Stage();
            popupStage.setTitle("Modifier Ticket");
            popupStage.initModality(Modality.APPLICATION_MODAL);
            popupStage.setScene(new Scene(root));
            popupStage.showAndWait();

            refreshTicketTable();
            refreshReservationTable();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void openAddTicketPopup(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Backoffice/formTicketBack.fxml"));
            Parent root = loader.load();

            TicketController popupController = loader.getController();

            if (currentReservationForTickets != null) popupController.setReservation(currentReservationForTickets);
            else if (selectedReservation != null) popupController.setReservation(selectedReservation);

            Stage popupStage = new Stage();
            popupStage.setTitle("Add Ticket");
            popupStage.initModality(Modality.APPLICATION_MODAL);
            popupStage.setScene(new Scene(root));
            popupStage.showAndWait();

            if (currentReservationForTickets != null) loadTicketsByReservation(currentReservationForTickets.getId());
            else refreshTicketTable();

            refreshReservationTable();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // =========================
    // BUTTONS FACTORY
    // =========================

    private void addDeleteReservationButton() {
        colDeleteReservation.setCellFactory(param -> new TableCell<>() {
            private final Button btn = new Button();

            {
                ImageView icon = new ImageView(new Image(getClass().getResourceAsStream("/Backoffice/icons/poubelle.png")));
                icon.setFitWidth(20);
                icon.setFitHeight(20);
                btn.setGraphic(icon);
                btn.setStyle("-fx-background-color: transparent;");

                btn.setOnAction(e -> {
                    Reservation r = getTableView().getItems().get(getIndex());

                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                    confirm.setTitle("Confirmation");
                    confirm.setHeaderText("Delete reservation");
                    confirm.setContentText("Are you sure you want to delete this reservation?");

                    confirm.showAndWait().ifPresent(b -> {
                        if (b == ButtonType.OK) {
                            reservationService.delete(r);
                            getTableView().getItems().remove(r);
                            refreshReservationTable();
                            tableTicket.getItems().clear();
                            currentReservationForTickets = null;
                        }
                    });
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
            }
        });
    }

    private void addTicketButton() {
        colTicketReservation.setCellFactory(param -> new TableCell<>() {
            private final Button btn = new Button();

            {
                ImageView icon = new ImageView(new Image(getClass().getResourceAsStream("/Backoffice/icons/ticket.png")));
                icon.setFitWidth(22);
                icon.setFitHeight(27);
                btn.setGraphic(icon);
                btn.setStyle("-fx-background-color: transparent;");

                btn.setOnAction(e -> {
                    Reservation r = getTableView().getItems().get(getIndex());
                    currentReservationForTickets = r;
                    reservationtabpanmain.getSelectionModel().select(ticketstab);
                    loadTicketsByReservation(r.getId());
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
            }
        });
    }

    private void addDeleteTicketButton() {
        colDeleteTicket.setCellFactory(param -> new TableCell<>() {
            private final Button btn = new Button();

            {
                ImageView icon = new ImageView(new Image(getClass().getResourceAsStream("/Backoffice/icons/poubelle.png")));
                icon.setFitWidth(20);
                icon.setFitHeight(20);
                btn.setGraphic(icon);
                btn.setStyle("-fx-background-color: transparent;");

                btn.setOnAction(e -> {

                    Ticket t = getTableView().getItems().get(getIndex());

                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                    confirm.setTitle("Confirmation");
                    confirm.setHeaderText("Delete Ticket");
                    confirm.setContentText("Are you sure?");

                    confirm.showAndWait().ifPresent(b -> {

                        if (b == ButtonType.OK) {

                            Integer resId = t.getReservationId();

                            // 🔥 CASE 1 → Ticket belongs to reservation
                            if (resId != null) {

                                ticketService.releaseTicket(t.getId());

                                // recalcul stats réservation
                                reservationService.updateReservationStats(resId);

                            }
                            // 🔥 CASE 2 → Ticket is free
                            else {

                                ticketService.delete(t);

                            }

                            // 🔄 REFRESH TABLES
                            if (currentReservationForTickets != null)
                                loadTicketsByReservation(currentReservationForTickets.getId());
                            else
                                refreshTicketTable();

                            refreshReservationTable();
                        }
                    });
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
            }
        });
    }

    // =========================
    // NAVIGATION PANES
    // =========================

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

        if (clicked == dashuserbut) showPane(usertabpanmain);
        else if (clicked == dashactbut) showPane(activitetabpanmain);
        else if (clicked == dashdesbut) showPane(destinationtabpanmain);
        else if (clicked == dashresbut) showPane(reservationtabpanmain);
        else if (clicked == dashpostbut) showPane(posttabpanmain);
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
    private void FXaddActivite(ActionEvent event) {
        // empty (ton code)
    }

    // ========== DESTINATION MODULE METHODS ==========

    // --- PAYS (Country) Methods ---
    private void initPaysTable() {
        colNomPays.setCellValueFactory(new PropertyValueFactory<>("nom"));
        colDescriptionPays.setCellValueFactory(new PropertyValueFactory<>("description"));

        addPaysActionButtons();
        refreshPaysTable();
    }

    private void refreshPaysTable() {
        paysList.setAll(paysService.getAll());
        tablePays.setItems(paysList);
    }

    private void addPaysActionButtons() {
        colActionsPays.setCellFactory(param -> new TableCell<>() {
            private final Button editBtn = createEditButton();
            private final Button deleteBtn = createDeleteButton();
            private final HBox container = new HBox(10, editBtn, deleteBtn);

            {
                container.setAlignment(Pos.CENTER);

                editBtn.setOnAction(e -> {
                    Pays pays = getTableView().getItems().get(getIndex());
                    openEditPopupPays(pays);
                });

                deleteBtn.setOnAction(e -> {
                    Pays pays = getTableView().getItems().get(getIndex());
                    paysService.delete(pays);
                    refreshPaysTable();
                    refreshVilleTable(); // Refresh child table
                    refreshAttractionTable(); // Refresh grandchild table
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : container);
            }
        });
    }

    @FXML
    void openAddPopupPays(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/Backoffice/FormulaireAddPays.fxml"));
            Stage popupStage = new Stage();
            popupStage.setTitle("Add Country");
            popupStage.initModality(Modality.APPLICATION_MODAL);
            popupStage.setScene(new Scene(root));
            popupStage.showAndWait();
            refreshPaysTable();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void openEditPopupPays(Pays pays) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Backoffice/FormulaireEditPays.fxml"));
            Parent root = loader.load();

            EditPaysController controller = loader.getController();
            controller.setPays(pays);

            Stage popupStage = new Stage();
            popupStage.setTitle("Edit Country");
            popupStage.initModality(Modality.APPLICATION_MODAL);
            popupStage.setScene(new Scene(root));
            popupStage.showAndWait();
            refreshPaysTable();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // --- VILLE (City) Methods ---
    private void initVilleTable() {
        colNomVille.setCellValueFactory(new PropertyValueFactory<>("nom"));
        colPaysVille.setCellValueFactory(cellData -> {
            int paysId = cellData.getValue().getPaysId();
            String paysName = paysList.stream()
                    .filter(p -> p.getId() == paysId)
                    .map(Pays::getNom)
                    .findFirst()
                    .orElse("Unknown ID: " + paysId);
            return new SimpleStringProperty(paysName);
        });
        colTypeTourismeVille.setCellValueFactory(new PropertyValueFactory<>("typeTourisme"));
        colSaisonVille.setCellValueFactory(new PropertyValueFactory<>("saison"));

        addVilleActionButtons();
        refreshVilleTable();
    }

    private void refreshVilleTable() {
        villeList.setAll(villeService.getAll());
        tableVille.setItems(villeList);
    }

    private void addVilleActionButtons() {
        colActionsVille.setCellFactory(param -> new TableCell<>() {
            private final Button editBtn = createEditButton();
            private final Button deleteBtn = createDeleteButton();
            private final HBox container = new HBox(10, editBtn, deleteBtn);

            {
                container.setAlignment(Pos.CENTER);

                editBtn.setOnAction(e -> {
                    Ville ville = getTableView().getItems().get(getIndex());
                    openEditPopupVille(ville);
                });

                deleteBtn.setOnAction(e -> {
                    Ville ville = getTableView().getItems().get(getIndex());
                    villeService.delete(ville);
                    refreshVilleTable();
                    refreshAttractionTable(); // Refresh child table
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : container);
            }
        });
    }

    @FXML
    void openAddPopupVille(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/Backoffice/FormulaireAddVille.fxml"));
            Stage popupStage = new Stage();
            popupStage.setTitle("Add City");
            popupStage.initModality(Modality.APPLICATION_MODAL);
            popupStage.setScene(new Scene(root));
            popupStage.showAndWait();
            refreshVilleTable();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void openEditPopupVille(Ville ville) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Backoffice/FormulaireEditVille.fxml"));
            Parent root = loader.load();

            EditVilleController controller = loader.getController();
            controller.setVille(ville);

            Stage popupStage = new Stage();
            popupStage.setTitle("Edit City");
            popupStage.initModality(Modality.APPLICATION_MODAL);
            popupStage.setScene(new Scene(root));
            popupStage.showAndWait();
            refreshVilleTable();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // --- ATTRACTION Methods ---
    private void initAttractionTable() {
        colNomAttraction.setCellValueFactory(new PropertyValueFactory<>("nom"));
        colDescriptionAttraction.setCellValueFactory(new PropertyValueFactory<>("description"));
        colTypeAttraction.setCellValueFactory(new PropertyValueFactory<>("type"));
        colPrixAttraction.setCellValueFactory(new PropertyValueFactory<>("prix"));
        colHorairesAttraction.setCellValueFactory(cellData -> {
            Attraction a = cellData.getValue();
            if (a.isEstFerme()) {
                return new SimpleStringProperty("Closed");
            } else {
                String start = (a.getHeureOuverture() != null) ? a.getHeureOuverture().toString().substring(0, 5) : "?";
                String end = (a.getHeureFermeture() != null) ? a.getHeureFermeture().toString().substring(0, 5) : "?";
                return new SimpleStringProperty(start + " - " + end);
            }
        });
        colVilleAttraction.setCellValueFactory(cellData -> {
            int villeId = cellData.getValue().getVilleId();
            String villeName = villeList.stream()
                    .filter(v -> v.getId() == villeId)
                    .map(Ville::getNom)
                    .findFirst()
                    .orElse("Unknown ID: " + villeId);
            return new SimpleStringProperty(villeName);
        });

        addAttractionActionButtons();
        refreshAttractionTable();
    }

    private void refreshAttractionTable() {
        attractionList.setAll(attractionService.getAll());
        tableAttraction.setItems(attractionList);
    }

    private void addAttractionActionButtons() {
        colActionsAttraction.setCellFactory(param -> new TableCell<>() {
            private final Button editBtn = createEditButton();
            private final Button deleteBtn = createDeleteButton();
            private final HBox container = new HBox(10, editBtn, deleteBtn);

            {
                container.setAlignment(Pos.CENTER);

                editBtn.setOnAction(e -> {
                    Attraction attraction = getTableView().getItems().get(getIndex());
                    openEditPopupAttraction(attraction);
                });

                deleteBtn.setOnAction(e -> {
                    Attraction attraction = getTableView().getItems().get(getIndex());
                    attractionService.delete(attraction);
                    refreshAttractionTable();
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : container);
            }
        });
    }

    @FXML
    void openAddPopupAttraction(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/Backoffice/FormulaireAddAttraction.fxml"));
            Stage popupStage = new Stage();
            popupStage.setTitle("Add Attraction");
            popupStage.initModality(Modality.APPLICATION_MODAL);
            popupStage.setScene(new Scene(root));
            popupStage.showAndWait();
            refreshAttractionTable();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void openEditPopupAttraction(Attraction attraction) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Backoffice/FormulaireEditAttraction.fxml"));
            Parent root = loader.load();

            EditAttractionController controller = loader.getController();
            controller.setAttraction(attraction);

            Stage popupStage = new Stage();
            popupStage.setTitle("Edit Attraction");
            popupStage.initModality(Modality.APPLICATION_MODAL);
            popupStage.setScene(new Scene(root));
            popupStage.showAndWait();
            refreshAttractionTable();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // --- Helper Methods for Buttons ---
    private Button createEditButton() {
        Button btn = new Button("Edit");
        btn.setStyle("-fx-background-color: #3A5BC7; -fx-text-fill: white; -fx-cursor: hand;");
        return btn;
    }

    private Button createDeleteButton() {
        Button btn = new Button("Delete");
        btn.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-cursor: hand;");
        return btn;
    }
    /////////////////////notification
    private void toggleNotifications() {

        if (notifMenu.isShowing()) {
            notifMenu.hide();
            return;
        }
        loadNotifications();
        notifMenu.show(btnNotif, Side.BOTTOM, 0, 8);
    }

    private void loadNotifications() {

        if (currentUser == null) return;

        List<notification> list =
                notificationService.getByReceiver2(currentUser.getId()); // admin

        notifMenu.getItems().clear();
        notifMenu.getStyleClass().add("notif-dropdown");

        MenuItem header = new MenuItem("Notifications");
        header.setDisable(true);
        header.getStyleClass().add("notif-header");

        notifMenu.getItems().add(header);
        notifMenu.getItems().add(new SeparatorMenuItem());

        if (list.isEmpty()) {

            MenuItem empty = new MenuItem("Aucune notification");
            empty.setDisable(true);
            empty.getStyleClass().add("notif-item");
            notifMenu.getItems().add(empty);

        } else {

            for (notification n : list) {

                MenuItem item = new MenuItem(n.getMessage());
                item.getStyleClass().add("notif-item");
                item.setOnAction(ev -> handleNotificationClick(n));

                notifMenu.getItems().add(item);
            }

        }
    }
    private void handleNotificationClick(notification n) {
        notifMenu.hide();

        Platform.runLater(() -> {

            if (n.getCommentId() != null) {

                dashboardGroup.selectToggle(dashpostbut);
                showPane(posttabpanmain);

                Commentaire targetCom = commentaireList.stream()
                        .filter(c -> c.getId() == n.getCommentId())
                        .findFirst()
                        .orElse(null);

                if (targetCom != null) {

                    int index = commentaireList.indexOf(targetCom);

                    tableCommentaire.getSelectionModel().clearAndSelect(index);
                    tableCommentaire.scrollTo(index);
                    tableCommentaire.requestFocus();

                } else {
                    System.out.println("Commentaire non trouvé");
                }

            }

            else if (n.getPostId() != null) {

                dashboardGroup.selectToggle(dashpostbut);
                showPane(posttabpanmain);

                Post targetPost = postList.stream()
                        .filter(p -> p.getId() == n.getPostId())
                        .findFirst()
                        .orElse(null);

                if (targetPost != null) {

                    int index = postList.indexOf(targetPost);

                    tablepost.getSelectionModel().clearAndSelect(index);
                    tablepost.scrollTo(index);
                    tablepost.requestFocus();
                }
            }
        });
    }
    /*---------------------------------------posts-------------------------------------------*/
    private void loadPosts() {

        colContenuPost.setCellValueFactory(new PropertyValueFactory<>("contenu"));
        colDatePost.setCellValueFactory(new PropertyValueFactory<>("datePublication"));
        colPopularitePost.setCellValueFactory(new PropertyValueFactory<>("popularite"));
        colLikesPost.setCellValueFactory(new PropertyValueFactory<>("nbLikes"));

        colAuteurPost.setCellValueFactory(cellData -> {
            String nom = "";
            String prenom = "";

            if (cellData.getValue().getAuteur() != null) {
                nom = cellData.getValue().getAuteur().getNom() != null ? cellData.getValue().getAuteur().getNom() : "";
                prenom = cellData.getValue().getAuteur().getPrenom() != null ? cellData.getValue().getAuteur().getPrenom() : "";
            }

            String fullName = (nom + " " + prenom).trim();
            if (fullName.isEmpty()) {
                fullName = "Inconnu";
            }

            return new SimpleStringProperty(fullName);
        });

        postList.setAll(postService.getAll());
        tablepost.setItems(postList);
    }
    /*----------------appeler le formulaire FormulaireAddPost avec ajout d'un titre----------------- */
    @FXML
    private void openAddPostForm() {

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Backoffice/FormulaireAddPost.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("Ajouter Post");
            stage.setScene(new Scene(root));
            stage.showAndWait();

            loadPosts(); // refresh après fermeture

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private <T> void addDeleteButtonsToColumn(TableColumn<T, Void> col,java.util.function.Consumer<T> onDelete) {
        col.setCellFactory(param -> new TableCell<>() {

            private final Button btnDelete = new Button();
            private final HBox hbox = new HBox(10, btnDelete);

            {



                // DELETE
                Image deleteImage = new Image(getClass().getResourceAsStream("/Backoffice/icons/poubelle.png"));
                ImageView deleteView = new ImageView(deleteImage);
                deleteView.setFitWidth(16);
                deleteView.setFitHeight(16);
                deleteView.setPreserveRatio(true);
                btnDelete.setGraphic(deleteView);
                btnDelete.setStyle("-fx-background-color: transparent;");
                btnDelete.setCursor(Cursor.HAND);
                btnDelete.setOnAction(event -> {
                    T item = getTableView().getItems().get(getIndex());
                    onDelete.accept(item);
                });
            }
            /*---------------tableview appelle updateItem pour mettre le contenu--------------*/
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(hbox);
                }
            }
        });
    }

    private void addImageColumn() {

        colImagePost.setCellFactory(param -> new TableCell<>() {

            private final ImageView imageView = new ImageView();

            {
                imageView.setFitWidth(80);
                imageView.setFitHeight(60);
                imageView.setPreserveRatio(true);
            }

            @Override
            protected void updateItem(String imagePath, boolean empty) {
                super.updateItem(imagePath, empty);

                if (empty || imagePath == null || imagePath.trim().isEmpty()) {
                    setGraphic(null);
                    return;
                }

                try {
                    File file = new File(imagePath);

                    if (file.exists()) {
                        imageView.setImage(new Image(file.toURI().toString()));
                    } else {
                        imageView.setImage(null); // pas d’image si fichier absent
                    }

                    setGraphic(imageView);

                } catch (Exception e) {
                    e.printStackTrace();
                    setGraphic(null);
                }
            }
        });

        colImagePost.setCellValueFactory(
                new PropertyValueFactory<>("image")
        );
    }



    private void openUpdatePopup(Post post) {

        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/Backoffice/FormulaireUpdatePost.fxml")
            );

            Parent root = loader.load();

            UpdatePostController controller = loader.getController();
            controller.setPostToEdit(post);

            Stage stage = new Stage();
            stage.setTitle("Modifier Post");
            stage.setScene(new Scene(root));
            /*attendre jusqu'a la fermiture de la fenetre et puis récuperer tt les posts*/
            stage.showAndWait();

            postList.setAll(postService.getAll());
            tablepost.refresh();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void deletePost(Post post) {

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setHeaderText("Supprimer ce post ?");
        alert.setContentText("Cette action est irréversible !");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                postService.delete(post);
                postList.remove(post);
                loadCommentaires(); // rafraîchir TableView
            }
        });
    }
    /*-------------------------------------commentaires--------------------------------*/
    private void loadCommentaires() {


        colContenuCom.setCellValueFactory(new PropertyValueFactory<>("contenu"));
        colDateCom.setCellValueFactory(new PropertyValueFactory<>("dateCommentaire"));

        colAuteurCom.setCellValueFactory(cellData -> {

            if (cellData.getValue().getAuteur() == null) {
                return new javafx.beans.property.SimpleStringProperty("Inconnu");
            }

            String nom = cellData.getValue().getAuteur().getNom();
            String prenom = cellData.getValue().getAuteur().getPrenom();

            return new javafx.beans.property.SimpleStringProperty(prenom + " " + nom);
        });

        colPostCom.setCellValueFactory(cellData -> {

            if (cellData.getValue().getPost() == null) {
                return new javafx.beans.property.SimpleStringProperty("—");
            }

            String contenu = cellData.getValue().getPost().getContenu();

            if (contenu == null) {
                return new javafx.beans.property.SimpleStringProperty("—");
            }

            if (contenu.length() > 40) {
                contenu = contenu.substring(0, 40) + ".....";
            }

            return new javafx.beans.property.SimpleStringProperty(contenu);
        });

        commentaireList.setAll(commentaireService.getAll());
        tableCommentaire.setItems(commentaireList);
    }

    private void deleteComment(Commentaire com) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setHeaderText("Supprimer ce commentaire ?");
        alert.setContentText("Cette action est irréversible !");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                commentaireService.delete(com);
                commentaireList.remove(com); // rafraîchir TableView
            }
        });
    }
    /*--------------------------------------------Favoris---------------------------------------------------------------------------*/
    private void loadFavoris() {

        colFavUser.setCellValueFactory(cell ->
                new SimpleStringProperty(
                        cell.getValue().getFavoris().getPersonne().getNom()
                                + " " +
                                cell.getValue().getFavoris().getPersonne().getPrenom()
                )
        );

        colFavPost.setCellValueFactory(cell ->
                new SimpleStringProperty(
                        cell.getValue().getPost().getContenu()
                )
        );

        colFavDate.setCellValueFactory(cell ->
                new SimpleStringProperty(
                        cell.getValue().getDateAjout().toString()
                )
        );

        favorisList.setAll(favorisService.getAllFavorisPosts());
        tableFavoris.setItems(favorisList);
    }
    /*------------------------------------------Module users------------------------------------------*/

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
    /*------------------------------------------------------------------------*/


}
