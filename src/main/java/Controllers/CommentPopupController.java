package Controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import models.Commentaire;
import models.Personne;
import models.Post;
import models.notification;
import org.json.JSONObject;
import services.CommentaireService;
import services.notificationService;
import util.Session;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

public class CommentPopupController {

    @FXML
    private VBox commentsContainer;
    @FXML
    private StackPane root;
    @FXML
    private TextField txtComment;

    private Post post;

    private Runnable onClose;

    private CommentaireService commentaireService = new CommentaireService();
    private Personne currentUser = Session.getCurrentUser();


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

    private double getToxicityScore(String text) {
        try {
            URL url = new URL("http://localhost:5000/predict");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setDoOutput(true);

            // Encodage JSON sûr
            String jsonInput = "{\"text\": \"" + text.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\"}";

            try (OutputStream os = conn.getOutputStream()) {
                os.write(jsonInput.getBytes("UTF-8"));
                os.flush();
            }

            int status = conn.getResponseCode();

            InputStream is = (status == 200) ? conn.getInputStream() : conn.getErrorStream();

            BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line);
            }

            if (status != 200) {
                System.err.println("Erreur Flask: HTTP " + status + " → " + response);
                return 0;
            }

            JSONObject obj = new JSONObject(response.toString());
            return obj.getDouble("score");

        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }
    @FXML
    private void sendComment() {

        if(post == null) return;

        String text = txtComment.getText().trim();

        if(text.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setHeaderText("Commentaire vide !");
            alert.showAndWait();
            return;
        }

        double score = getToxicityScore(text);

        if(score >= 0.7) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setHeaderText("Commentaire refusé !");
            alert.setContentText("Le contenu ne respecte pas nos règles.");
            alert.showAndWait();
            return;
        }

        if(currentUser == null) return;

        Commentaire newComment = new Commentaire();
        newComment.setContenu(text);
        newComment.setDateCommentaire(LocalDateTime.now().toLocalDate());
        newComment.setPost(post);
        newComment.setAuteur(currentUser);

        Commentaire savedComment = commentaireService.addAndReturn(newComment);

        if(savedComment == null){
            return;
        }

        if(score >= 0.1) {
            notifyAdmin(text, score, savedComment);
            showToast("Commentaire sensible publié (admin notifié)");
        } else {
            showToast("Commentaire publié !");
        }

        String now = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm"));

        addComment(
                currentUser.getPrenom() + " " + currentUser.getNom(),
                text,
                now,
                newComment
        );

        txtComment.clear();
    }


    private void notifyAdmin(String contenu, double score, Commentaire comment) {

        Personne auteur = comment.getAuteur();

        String message =
                "👤 " + auteur.getPrenom() + " " + auteur.getNom() +
                        " a publié dans le post ID=" + comment.getPost().getId() +
                        " un commentaire suspect (score: " +
                        String.format("%.2f", score) + ")";

        notification notif = new notification(
                message,
                "COMMENT",
                comment.getPost().getId(),
                comment.getId(),
                auteur.getId(),
                1   // admin
        );

        new notificationService().add(notif);

        System.out.println("Notification commentaire enregistrée !");
    }

    private void showToast(String message) {
        Label toast = new Label(message);
        toast.getStyleClass().add("toast");
        toast.setStyle(
                "-fx-background-color: rgba(0,0,0,0.7);" +
                        "-fx-text-fill: white;" +
                        "-fx-padding: 10px 20px;" +
                        "-fx-background-radius: 20;" +
                        "-fx-font-size: 14px;"
        );

        root.getChildren().add(toast);
        StackPane.setAlignment(toast, Pos.TOP_CENTER);

        // Faire disparaître après 2 secondes
        new Thread(() -> {
            try {
                Thread.sleep(7000);
            } catch (InterruptedException ignored) {}
            javafx.application.Platform.runLater(() -> root.getChildren().remove(toast));
        }).start();
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
        HBox actionBox = new HBox(10);
        actionBox.setAlignment(Pos.CENTER_RIGHT);
        if(currentUser != null &&
                commentObj.getAuteur() != null &&
                commentObj.getAuteur().getId() == currentUser.getId()) {

            // Image Update
            ImageView updateImg = new ImageView(
                    new Image(getClass().getResourceAsStream("/icons/editblue.png"))
            );

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
            ImageView deleteImg = new ImageView(
                    new Image(getClass().getResourceAsStream("/icons/delete.png"))
            );

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
        }

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