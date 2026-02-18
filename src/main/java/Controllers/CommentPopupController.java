package Controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

public class CommentPopupController implements Initializable {

    @FXML
    private VBox commentsContainer;

    @FXML
    private TextField txtComment;

    private Stage stage;

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    /* ================= CLOSE FIX =================
    @FXML
    private void closePopup() {
        if (stage != null) {
            stage.close();
        } else {
            // fallback solution
            Stage currentStage = (Stage) commentsContainer.getScene().getWindow();
            currentStage.close();
        }
    }*/
    private Runnable onClose;

    public void setOnClose(Runnable onClose) {
        this.onClose = onClose;
    }
    @FXML
    private void closePopup() {
        if (onClose != null) {
            onClose.run();
        }
    }


    /* ================= INITIAL STATIC COMMENTS ================= */
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

        addComment("Sarra Ben Ali", "Super activité 😍 j’ai adoré l’ambiance !", "Il y a 2 h");
        addComment("Mohamed Trabelsi", "Très bien organisée 👏", "Il y a 5 h");
        addComment("Nour Haddad", "On refait ça quand ? 🔥", "Hier à 21:34");
    }

    /* ================= SEND COMMENT ================= */
    @FXML
    private void sendComment() {

        String text = txtComment.getText().trim();
        if(text.isEmpty()) return;

        String now = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm"));

        addComment("Vous", text, now);

        txtComment.clear();
    }

    /* ================= COMMENT CREATION ================= */
    private void addComment(String author, String message, String date) {

        VBox commentBox = new VBox(5);
        commentBox.getStyleClass().add("comment-item");

        // Header (Name + Date)
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

        // Message
        Label lblMessage = new Label(message);
        lblMessage.getStyleClass().add("comment-text");
        lblMessage.setWrapText(true);

        commentBox.getChildren().addAll(header, lblMessage);

        commentsContainer.getChildren().add(commentBox);
    }
}
