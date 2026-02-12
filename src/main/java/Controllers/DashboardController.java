package Controllers;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import models.Pays;
import models.Ville;
import models.Attraction;
import services.PaysService;
import services.VilleService;
import services.AttractionService;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;


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
    @FXML private TabPane usertabpanmain;

    // Activities TableView (managed by Activities module)
    @FXML
    private TableView<?> tableactivite;
    @FXML
    private TableColumn<?, ?> colnameactivite;
    @FXML
    private TableColumn<?, ?> coldescriptionactivite;
    @FXML
    private TableColumn<?, ?> colpriceactivite;
    @FXML
    private TableColumn<?, ?> coldureeactivite;
    @FXML
    private TableColumn<?, ?> coltypeactivite;
    @FXML
    private TableColumn<?, ?> colavgratactivite;
    @FXML
    private TableColumn<?, ?> colguideactivite;
    @FXML
    private TableColumn<?, ?> colDeleteactivite;

    @FXML private TableView<?> tableactivite1111;
    @FXML private TableView<?> tablepost;
    @FXML private TableView<?> tableuser;

    // Destination Tables
    @FXML private TableView<Pays> tablePays;
    @FXML private TableView<Ville> tableVille;
    @FXML private TableView<Attraction> tableAttraction;

    // Pays Columns
    @FXML private TableColumn<Pays, String> colNomPays;
    @FXML private TableColumn<Pays, String> colContinentPays;
    @FXML private TableColumn<Pays, String> colDescriptionPays;
    @FXML private TableColumn<Pays, Void> colActionsPays;

    // Ville Columns
    @FXML private TableColumn<Ville, String> colNomVille;
    @FXML private TableColumn<Ville, Integer> colPaysVille;
    @FXML private TableColumn<Ville, String> colRegionVille;
    @FXML private TableColumn<Ville, String> colTypeTourismeVille;
    @FXML private TableColumn<Ville, String> colSaisonVille;
    @FXML private TableColumn<Ville, Integer> colPopulariteVille;
    @FXML private TableColumn<Ville, Void> colActionsVille;

    // Attraction Columns
    @FXML private TableColumn<Attraction, String> colNomAttraction;
    @FXML private TableColumn<Attraction, String> colDescriptionAttraction;
    @FXML private TableColumn<Attraction, String> colTypeAttraction;
    @FXML private TableColumn<Attraction, Double> colPrixAttraction;
    @FXML private TableColumn<Attraction, String> colHorairesAttraction;
    @FXML private TableColumn<Attraction, Integer> colVilleAttraction;
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
        
        // Initialize Destination Tables
        initPaysTable();
        initVilleTable();
        initAttractionTable();
        Platform.runLater(() -> {
            Scene scene = dashuserbut.getScene();
            Stage stage = (Stage) scene.getWindow();

            scene.setOnKeyPressed(event -> {
                if (event.getCode() == javafx.scene.input.KeyCode.ESCAPE && stage.isMaximized()) {
                    stage.setMaximized(false);
                }
            });
        });

    }

    // Activities table initialization - handled by Activities module
    // Placeholder method to prevent errors
    private void initActiviteTable() {
        // Will be implemented by Activities module
    }

    private void refreshActiviteTable() {
        // Will be implemented by Activities module
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
    void openAddPopupactivite(ActionEvent event) {
        // Will be implemented by Activities module
        System.out.println("Add Activity - handled by Activities module");
    }

    // ========== DESTINATION MODULE METHODS ==========

    // --- PAYS (Country) Methods ---
    private void initPaysTable() {
        colNomPays.setCellValueFactory(new PropertyValueFactory<>("nom"));
        colContinentPays.setCellValueFactory(new PropertyValueFactory<>("continent"));
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
            Parent root = FXMLLoader.load(getClass().getResource("/FormulaireAddPays.fxml"));
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
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/FormulaireEditPays.fxml"));
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
        colPaysVille.setCellValueFactory(new PropertyValueFactory<>("paysId"));
        colRegionVille.setCellValueFactory(new PropertyValueFactory<>("region"));
        colTypeTourismeVille.setCellValueFactory(new PropertyValueFactory<>("typeTourisme"));
        colSaisonVille.setCellValueFactory(new PropertyValueFactory<>("saison"));
        colPopulariteVille.setCellValueFactory(new PropertyValueFactory<>("popularite"));

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
            Parent root = FXMLLoader.load(getClass().getResource("/FormulaireAddVille.fxml"));
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
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/FormulaireEditVille.fxml"));
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
        colHorairesAttraction.setCellValueFactory(new PropertyValueFactory<>("horaires"));
        colVilleAttraction.setCellValueFactory(new PropertyValueFactory<>("villeId"));

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
            Parent root = FXMLLoader.load(getClass().getResource("/FormulaireAddAttraction.fxml"));
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
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/FormulaireEditAttraction.fxml"));
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

}
