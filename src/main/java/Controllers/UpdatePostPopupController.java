package Controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import models.Post;
import services.PostService;

import java.net.URL;
import java.util.ResourceBundle;

public class UpdatePostPopupController implements Initializable {

    @FXML
    private TextField txtContent;

    @FXML
    private TextField txtImage;

    private Post post;

    private Runnable onClose;

    public void setPost(Post post) {
        this.post = post;
        txtContent.setText(post.getContenu());
        txtImage.setText(post.getImage() != null ? post.getImage() : "");
    }

    public void setOnClose(Runnable onClose) {
        this.onClose = onClose;
    }

    @FXML
    private void closePopup() {
        if(onClose != null) onClose.run();
    }

    @FXML
    private void updatePost() {
        if(post != null) {
            post.setContenu(txtContent.getText());
            String img = txtImage.getText().isBlank() ? null : txtImage.getText();
            post.setImage(img);

            PostService postService = new PostService();
            postService.update(post); // assure-toi que ta méthode update existe dans PostService

            // fermer le popup
            if(onClose != null) onClose.run();
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // tu peux initialiser d'autres choses si besoin
    }

    @FXML
    private void chooseImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir une image");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg")
        );

        java.io.File selectedFile = fileChooser.showOpenDialog(txtContent.getScene().getWindow());
        if(selectedFile != null){
            txtImage.setText(selectedFile.getAbsolutePath());
        }
    }
}