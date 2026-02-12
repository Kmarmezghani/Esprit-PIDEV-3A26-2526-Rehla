package Controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import models.Post;
import models.Personne;
import services.PostService;

import java.time.LocalDate;

public class AddPostController {

    @FXML private TextField txtTitre;
    @FXML private TextArea txtContenu;
    @FXML private TextField txtPopularite;
    @FXML private TextField txtAuteurId;

    private PostService postService = new PostService();

    @FXML
    private void addPost() {

        try {
            Post post = new Post();
            post.setTitre(txtTitre.getText());
            post.setContenu(txtContenu.getText());
            post.setDatePublication(LocalDate.now());
            post.setPopularite(Integer.parseInt(txtPopularite.getText()));

            Personne auteur = new Personne();
            auteur.setId(Integer.parseInt(txtAuteurId.getText()));
            post.setAuteur(auteur);

            postService.add(post);

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setContentText("Post ajouté avec succès !");
            alert.showAndWait();

            // fermer la fenêtre
            Stage stage = (Stage) txtTitre.getScene().getWindow();
            stage.close();

        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage()).showAndWait();
        }
    }
}
