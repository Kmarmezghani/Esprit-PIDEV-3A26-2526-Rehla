package Controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import models.Commentaire;
import models.Personne;
import models.Post;
import services.CommentaireService;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

public class CommentPopupController {

    @FXML
    private VBox commentsContainer;

    @FXML
    private TextField txtComment;

    private Post post;

    private Runnable onClose;

    private CommentaireService commentaireService = new CommentaireService();

    public void setOnClose(Runnable onClose) {
        this.onClose = onClose;
    }

    /** Initialiser le popup avec un post existant */
    public void setPost(Post post) {
        this.post = post;

        // Récupérer tous les commentaires depuis la base
        List<Commentaire> comments = commentaireService.getCommentairesByPost(post);
        commentsContainer.getChildren().clear();
        for (Commentaire c : comments) {
            String auteur = (c.getAuteur() != null ? c.getAuteur().getPrenom() + " " + c.getAuteur().getNom() : "Inconnu");
            addComment(auteur, c.getContenu(), c.getDateCommentaire().toString(), c);
        }
    }

    @FXML
    private void closePopup() {
        if (onClose != null) {
            onClose.run();
        }
    }


    /** Envoyer un nouveau commentaire */
    @FXML
    private void sendComment() {
        if(post == null) return;

        String text = txtComment.getText().trim();
        if(text.isEmpty()) return;

        Commentaire newComment = new Commentaire();
        newComment.setContenu(text);
        newComment.setDateCommentaire(LocalDateTime.now().toLocalDate()); // date du jour
        newComment.setPost(post);

        // Ici tu peux mettre la vraie personne connectée
        Personne auteur = new Personne();
        auteur.setId(1); // exemple, remplacer par utilisateur réel
        auteur.setPrenom("Vous"); // pour l'affichage
        auteur.setNom("");         // pour l'affichage
        newComment.setAuteur(auteur);

        commentaireService.add(newComment);

        // Ajouter immédiatement à l'UI
        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm"));
        addComment(auteur.getPrenom() + " " + auteur.getNom(), text, now, newComment);

        txtComment.clear();
    }

    /** Ajouter un commentaire dans l'UI */
    private void addComment(String author, String message, String date, Commentaire commentObj) {
        VBox commentBox = new VBox(5);
        commentBox.getStyleClass().add("comment-item");

        // ================= HEADER =================
        HBox header = new HBox(10);

        Circle avatar = new Circle(15);
        avatar.getStyleClass().add("avatar-circle");

        VBox nameDate = new VBox(2);

        Label lblName = new Label(author);
        lblName.getStyleClass().add("comment-name");

        Label lblDate = new Label(date);
        lblDate.getStyleClass().add("comment-date");

        nameDate.getChildren().addAll(lblName, lblDate);
        header.getChildren().addAll(avatar, nameDate);

        // ================= MESSAGE =================
        Label lblMessage = new Label(message);
        lblMessage.getStyleClass().add("comment-text");
        lblMessage.setWrapText(true);

        // ================= ACTIONS =================
        // ================= ACTIONS =================
        HBox actionBox = new HBox(10);
        actionBox.setAlignment(Pos.CENTER_RIGHT);

// Image Update
        ImageView updateImg = new ImageView(new Image(getClass().getResourceAsStream("/icons/editblue.png")));
        updateImg.setFitWidth(20);
        updateImg.setFitHeight(20);
        updateImg.setPreserveRatio(true);
        updateImg.setSmooth(true);
        updateImg.setStyle("-fx-cursor: hand;");
        updateImg.setOnMouseClicked(e -> {
            String newContent = promptForUpdate(commentObj.getContenu());
            if (newContent != null && !newContent.isBlank()) {
                commentObj.setContenu(newContent);
                commentaireService.update(commentObj);
                lblMessage.setText(newContent);
            }
        });

// Image Delete
        ImageView deleteImg = new ImageView(new Image(getClass().getResourceAsStream("/icons/delete.png")));
        deleteImg.setFitWidth(20);
        deleteImg.setFitHeight(20);
        deleteImg.setPreserveRatio(true);
        deleteImg.setSmooth(true);
        deleteImg.setStyle("-fx-cursor: hand;");
        deleteImg.setOnMouseClicked(e -> {
            commentaireService.delete(commentObj);
            commentsContainer.getChildren().remove(commentBox);
        });

        actionBox.getChildren().addAll(updateImg, deleteImg);
        commentBox.getChildren().addAll(header, lblMessage, actionBox);
        commentsContainer.getChildren().add(commentBox);
    }
    private String promptForUpdate(String currentContent) {
        TextInputDialog dialog = new TextInputDialog(currentContent);
        dialog.setTitle("Modifier le commentaire");
        dialog.setHeaderText(null);
        dialog.setContentText("Éditer le commentaire :");
        return dialog.showAndWait().orElse(null);
    }
}