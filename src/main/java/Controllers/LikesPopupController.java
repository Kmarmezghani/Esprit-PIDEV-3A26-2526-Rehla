package Controllers;

import javafx.fxml.FXML;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.control.Label;
import javafx.scene.shape.Circle;
import javafx.geometry.Pos;
import models.Personne;
import models.Post;
import services.LikeService;

import java.util.List;

public class LikesPopupController {

    @FXML
    private VBox likesContainer;

    private LikeService likeService = new LikeService();

    private Runnable onClose;
    @FXML
    private ImageView likeIcon;
    private final Image defaultAvatar = new Image(getClass().getResourceAsStream("/Backoffice/icons/usericon.png"));
    private final Image like_Icon=new Image(getClass().getResourceAsStream("/Backoffice/icons/HeartRed.png"));
    @FXML
    public void initialize() {
        likeIcon.setImage(like_Icon);
    }
    public void setOnClose(Runnable onClose) {
        this.onClose = onClose;
    }

    public void setPost(Post post) {

        List<Personne> users = likeService.getPersonsOfLike(post);
        likesContainer.getChildren().clear();

        if(users.isEmpty()) {
            Label empty = new Label("Aucun like pour le moment");
            empty.getStyleClass().add("comment-text");
            likesContainer.getChildren().add(empty);
            return;
        }

        for(Personne p : users) {
            addUser(p);
        }
    }

    private void addUser(Personne p) {

        HBox userBox = new HBox(10);
        userBox.setAlignment(Pos.CENTER_LEFT);
        userBox.getStyleClass().add("comment-item");

        ImageView avatarView = new ImageView(defaultAvatar);

        double size = 36;
        avatarView.setFitWidth(size);
        avatarView.setFitHeight(size);
        avatarView.setPreserveRatio(true);
        avatarView.setSmooth(true);

        // Clip pour garder l'aspect circulaire
        Circle clip = new Circle(size / 2, size / 2, size / 2);
        avatarView.setClip(clip);

        Label name = new Label(p.getPrenom() + " " + p.getNom());
        name.getStyleClass().add("comment-name");

        userBox.getChildren().addAll(avatarView, name);
        likesContainer.getChildren().add(userBox);
    }

    @FXML
    private void closePopup() {
        if(onClose != null) {
            onClose.run();
        }
    }
}