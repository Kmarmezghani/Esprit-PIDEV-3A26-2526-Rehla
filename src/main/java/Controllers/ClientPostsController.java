package Controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.control.MenuItem;

import javafx.scene.image.ImageView;
import javafx.scene.image.Image  ;

import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.stage.StageStyle;

import java.awt.*;


public class ClientPostsController {


    @FXML
    private BorderPane mainContent;

    @FXML
    private StackPane root;   // ajoute fx:id="root" au StackPane

    @FXML
    private ImageView likeIcon;

    @FXML
    private Button btnLike;

    private boolean isLiked = false;

    private Image heartEmpty;
    private Image heartFull;
    @FXML private ImageView commentIcon;

    private Image commentEmpty;

    @FXML
    private ImageView starIcon;

    @FXML
    private Button btnStar;

    private boolean isStarred = false;

    private Image starEmpty;
    private Image starFull;
    @FXML
    private void initialize() {

        heartEmpty = new Image(getClass().getResourceAsStream("/icons/heartwhite.png"));
        heartFull = new Image(getClass().getResourceAsStream("/icons/HeartRed.png"));

        likeIcon.setImage(heartEmpty);
        commentEmpty = new Image(getClass()
                .getResourceAsStream("/icons/comment.png"));
        commentIcon.setImage(commentEmpty);

        starEmpty = new Image(getClass().getResourceAsStream("/icons/whiteStar.png"));
        starFull = new Image(getClass().getResourceAsStream("/icons/yellowStar.png"));
        starIcon.setImage(starEmpty);
    }
    @FXML
    private void handleComment(ActionEvent event) {
        try {

            FXMLLoader loader =
                    new FXMLLoader(getClass().getResource("/CommentPopup.fxml"));

            Parent popup = loader.load();
            CommentPopupController controller = loader.getController();

            // 🔥 Blur seulement sur le blog
            GaussianBlur blur = new GaussianBlur(20);
            mainContent.setEffect(blur);

            // 🔥 Fond sombre
            StackPane overlay = new StackPane();
            overlay.setStyle("-fx-background-color: rgba(0,0,0,0.5);");

            // IMPORTANT: ne pas toucher la taille du popup
            overlay.getChildren().add(popup);
            StackPane.setAlignment(popup, Pos.CENTER);

            root.getChildren().add(overlay);

            // 🔥 Fermeture
            controller.setOnClose(() -> {
                mainContent.setEffect(null);
                root.getChildren().remove(overlay);
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void handleClose(ActionEvent event) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.close();
    }

    public void handleFullScreen(ActionEvent event) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setFullScreen(!stage.isFullScreen());
    }

    @FXML
    private void handleLike() {

        isLiked = !isLiked;

        if (isLiked) {
            likeIcon.setImage(heartFull);
            btnLike.setStyle("-fx-background-color: transparent;");
        } else {
            likeIcon.setImage(heartEmpty);
            btnLike.setStyle("-fx-background-color: transparent;");
        }
    }

    @FXML
    private void handleStar() {
        isStarred = !isStarred;

        if (isStarred) {
            starIcon.setImage(starFull);
            btnStar.setStyle("-fx-background-color: transparent;");
        } else {
            starIcon.setImage(starEmpty);
            btnStar.setStyle("-fx-background-color: transparent;");
        }
    }
    private void handleUpdatePost() {
        System.out.println("Update post...");



    }
    private void handleDeletePost() {

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText("Supprimer ce post ?");
        confirm.setContentText("Cette action est irréversible.");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                System.out.println("Post supprimé !");
                // ici tu mettras la suppression réelle
            }
        });
    }
    @FXML
    private void handlePostOptions(ActionEvent event) {

        Button sourceBtn = (Button) event.getSource();

        // Création du menu contextuel
        ContextMenu menu = new ContextMenu();

        MenuItem updateItem = new MenuItem("✏ Modifier");
        MenuItem deleteItem = new MenuItem("🗑 Supprimer");

        // ACTION UPDATE
        updateItem.setOnAction(e -> handleUpdatePost());

        // ACTION DELETE
        deleteItem.setOnAction(e -> handleDeletePost());

        menu.getItems().addAll(updateItem, deleteItem);

        // Afficher le menu sous le bouton
        menu.show(sourceBtn, Side.BOTTOM, 0, 0);
    }




}





