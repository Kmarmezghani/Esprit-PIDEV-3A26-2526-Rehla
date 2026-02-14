package Controllers;

import javafx.application.Platform;
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
import javafx.stage.Stage;
import models.Commentaire;
import models.Post;
import services.CommentaireService;
import services.PostService;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.Cursor;
import javafx.scene.layout.HBox;

import javafx.scene.control.Button;
import javafx.scene.control.TableCell;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

import java.io.File;
import java.io.InputStream;

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
    @FXML private TableView<?> tableuser;
    @FXML private TableView<Commentaire> tableCommentaire;

    @FXML private ToggleButton dashactbut;
    @FXML private ToggleButton dashdesbut;
    @FXML private ToggleButton dashpostbut;
    @FXML private ToggleButton dashresbut;
    @FXML private ToggleButton dashuserbut;


    private final ToggleGroup dashboardGroup = new ToggleGroup();


    @FXML private TableColumn<Post, Integer> colIdPost;
    @FXML private TableColumn<Post, String> colTitrePost;
    @FXML private TableColumn<Post, String> colContenuPost;
    @FXML private TableColumn<Post, java.time.LocalDate> colDatePost;
    @FXML private TableColumn<Post, Integer> colPopularitePost;
    @FXML private TableColumn<Post, Integer> colAuteurPost;
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

    private CommentaireService commentaireService = new CommentaireService();
    private ObservableList<Commentaire> commentaireList = FXCollections.observableArrayList();

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
    private PostService postService = new PostService();
    private ObservableList<Post> postList = FXCollections.observableArrayList();




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

    private void loadPosts() {

        colIdPost.setCellValueFactory(new PropertyValueFactory<>("id"));
        colTitrePost.setCellValueFactory(new PropertyValueFactory<>("titre"));
        colContenuPost.setCellValueFactory(new PropertyValueFactory<>("contenu"));
        colDatePost.setCellValueFactory(new PropertyValueFactory<>("datePublication"));
        colPopularitePost.setCellValueFactory(new PropertyValueFactory<>("popularite"));
        colLikesPost.setCellValueFactory(new PropertyValueFactory<>("nbLikes"));

        // Auteur → on affiche juste son ID
        colAuteurPost.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleIntegerProperty(
                        cellData.getValue().getAuteur().getId()
                ).asObject()
        );
        colImagePost.setCellValueFactory(new PropertyValueFactory<>("image"));


        postList.setAll(postService.getAll());
        tablepost.setItems(postList);
    }

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
                postService.delete(post);      // suppression DB
                postList.remove(post);         // rafraîchir TableView
            }
        });
    }
    private void loadCommentaires() {

        colIdCom.setCellValueFactory(new PropertyValueFactory<>("id"));
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

}
