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
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import models.*;
import services.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.Cursor;
import javafx.scene.layout.HBox;

import javafx.scene.control.Button;
import javafx.scene.control.TableCell;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import util.Session;

import java.io.File;
import java.io.InputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;

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

    // TableView

    @FXML private TableView<?> tableactivite1111;
    @FXML private TableView<?> tabledestination;
    @FXML private TableView<Post> tablepost;

    @FXML private TableView<Commentaire> tableCommentaire;

    @FXML private ToggleButton dashactbut;
    @FXML private ToggleButton dashdesbut;
    @FXML private ToggleButton dashpostbut;
    @FXML private ToggleButton dashresbut;
    @FXML private ToggleButton dashuserbut;

    @FXML
    private Button btnNotif;
    private final ToggleGroup dashboardGroup = new ToggleGroup();


    @FXML private TableColumn<Post, Integer> colIdPost;
    @FXML private TableColumn<Post, String> colTitrePost;
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
    @FXML private TableColumn<Commentaire, Integer> colAuteurCom;
    @FXML private TableColumn<Commentaire, Integer> colPostCom;


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
    private notificationService notificationService = new notificationService();

    private CommentaireService commentaireService = new CommentaireService();
    private ObservableList<Commentaire> commentaireList = FXCollections.observableArrayList();
    private PostService postService = new PostService();
    private ObservableList<Post> postList = FXCollections.observableArrayList();

    Personne currentUser = Session.getCurrentUser();
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
        initUsersTable();
        initPreferencesTable();

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

        Platform.runLater(() -> {
            Scene scene = dashuserbut.getScene();
            Stage stage = (Stage) scene.getWindow();

            scene.setOnKeyPressed(event -> {
                if (event.getCode() == javafx.scene.input.KeyCode.ESCAPE && stage.isMaximized()) {
                    stage.setMaximized(false);
                }
            });
        });
        loadPosts();
        loadCommentaires();
        loadFavoris();
        btnNotif.setOnAction(e -> toggleNotifications());

        addEditDeleteButtonsToColumn(colAction,
                post -> openUpdatePopup((Post) post),
                post -> deletePost((Post) post)
        );
        addEditDeleteButtonsToColumn(colAction2,
                com -> openUpdateCommentPopup((Commentaire) com),
                com -> deleteComment((Commentaire) com)
        );

        addImageColumn();



    }
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
/*---------------------------------------posts-------------------------------------------*/
    private void loadPosts() {


        colTitrePost.setCellValueFactory(new PropertyValueFactory<>("titre"));
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
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/FormulaireAddPost.fxml"));
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
    private <T> void addEditDeleteButtonsToColumn(TableColumn<T, Void> col, java.util.function.Consumer<T> onEdit, java.util.function.Consumer<T> onDelete) {
        col.setCellFactory(param -> new TableCell<>() {

            private final Button btnEdit = new Button();
            private final Button btnDelete = new Button();
            private final HBox hbox = new HBox(10, btnEdit, btnDelete);

            {
                // EDIT
                Image editImage = new Image(getClass().getResourceAsStream("/icons/edit.png"));
                ImageView editView = new ImageView(editImage);
                editView.setFitWidth(16);
                editView.setFitHeight(16);
                editView.setPreserveRatio(true);
                btnEdit.setGraphic(editView);
                btnEdit.setStyle("-fx-background-color: transparent;");
                btnEdit.setCursor(Cursor.HAND);
                btnEdit.setOnAction(event -> {
                    T item = getTableView().getItems().get(getIndex());
                    onEdit.accept(item);
                });

                // DELETE
                Image deleteImage = new Image(getClass().getResourceAsStream("/icons/poubelle.png"));
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
                    getClass().getResource("/FormulaireUpdatePost.fxml")
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

        // Auteur ID
        colAuteurCom.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleIntegerProperty(
                        cellData.getValue().getAuteur().getId()
                ).asObject()
        );

        // Post ID
        colPostCom.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleIntegerProperty(
                        cellData.getValue().getPost().getId()
                ).asObject()
        );

        commentaireList.setAll(commentaireService.getAll());
        tableCommentaire.setItems(commentaireList);
    }
    @FXML
    private void openAddCommentForm() {

        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/FormulaireAddComment.fxml")
            );

            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("Ajouter Commentaire");
            stage.setScene(new Scene(root));
            stage.showAndWait();
            loadCommentaires();


        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void openUpdateCommentPopup(Commentaire com) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/FormulaireUpdateComment.fxml"));
            Parent root = loader.load();

            UpdateCommentController controller = loader.getController();
            controller.setCommentToEdit(com);

            Stage stage = new Stage();
            stage.setTitle("Modifier Commentaire");
            stage.setScene(new Scene(root));
            stage.showAndWait();

            // rafraîchir TableView après modification
            commentaireList.setAll(commentaireService.getAll());
            tableCommentaire.refresh();

        } catch (Exception e) {
            e.printStackTrace();
        }
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
