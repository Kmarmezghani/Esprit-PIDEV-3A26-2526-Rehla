
package Controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
        import javafx.stage.Stage;
import models.Post;
import models.Personne;
import services.PostService;

import java.time.LocalDate;

public class UpdatePostController {

    @FXML private TextField txtTitre;
    @FXML private TextArea txtContenu;
    @FXML private TextField txtPopularite;
    @FXML private TextField txtAuteurId;

    private PostService postService = new PostService();

    private Post postToEdit = null;
    private boolean isUpdate = false;


    public void setPostToEdit(Post post) {

        this.postToEdit = post;
        this.isUpdate = true;

        txtTitre.setText(post.getTitre());
        txtContenu.setText(post.getContenu());
        txtPopularite.setText(String.valueOf(post.getPopularite()));
        txtAuteurId.setText(String.valueOf(post.getAuteur().getId()));
    }
    @FXML
    private void updatePost() {

        postToEdit.setTitre(txtTitre.getText());
        postToEdit.setContenu(txtContenu.getText());
        postToEdit.setPopularite(
                Integer.parseInt(txtPopularite.getText())
        );

        postService.update(postToEdit);

        Stage stage = (Stage) txtTitre.getScene().getWindow();
        stage.close();
    }

}