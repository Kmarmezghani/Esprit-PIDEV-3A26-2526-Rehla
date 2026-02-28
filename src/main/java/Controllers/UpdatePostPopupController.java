package Controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import models.Post;
import services.PostService;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;

public class UpdatePostPopupController implements Initializable {

    @FXML
    private TextField txtContent;

    @FXML
    private ImageView imgPreview;

    private File selectedFile;

    private Post post;

    private Runnable onClose;

    public void setPost(Post post) {
        this.post = post;
        txtContent.setText(post.getContenu());

        if (post.getImage() != null && !post.getImage().isBlank()) {
            File file = new File(post.getImage());
            if (file.exists()) {
                imgPreview.setImage(new Image(file.toURI().toString()));
                selectedFile = file;
            }
        }
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
        if(post != null){
            post.setContenu(txtContent.getText());

            if(selectedFile != null){
                post.setImage(selectedFile.getAbsolutePath());
            }

            PostService postService = new PostService();
            postService.update(post);

            if(onClose != null) onClose.run();
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // initialisation si besoin
    }

    @FXML
    private void chooseImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir une image");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg")
        );

        File file = fileChooser.showOpenDialog(txtContent.getScene().getWindow());
        if(file != null){
            selectedFile = file;
            imgPreview.setImage(new Image(file.toURI().toString()));
        }
    }
}