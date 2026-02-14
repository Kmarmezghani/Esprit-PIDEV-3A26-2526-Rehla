package Controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import models.Commentaire;
import models.Personne;
import models.Post;
import services.CommentaireService;
import services.PersonneService;
import services.PostService;

import java.time.LocalDate;

public class AddCommentController {

    @FXML private TextArea txtContenu;
    @FXML private ComboBox<Personne> cbAuteur;
    @FXML private ComboBox<Post> cbPost;

    private final CommentaireService commentaireService = new CommentaireService();
    private final PersonneService personneService = new PersonneService();
    private final PostService postService = new PostService();

    @FXML
    public void initialize() {

        // Charger auteurs
        cbAuteur.setItems(
                FXCollections.observableArrayList(
                        personneService.getAllPersonnes()
                )
        );

        // Charger posts
        cbPost.setItems(
                FXCollections.observableArrayList(
                        postService.getAll()
                )
        );

        // Affichage auteur (nom au lieu de toString default)
        cbAuteur.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(Personne p, boolean empty) {
                super.updateItem(p, empty);
                setText(empty || p == null ? null :
                        p.getNom() + " " + p.getPrenom());
            }
        });

        cbAuteur.setButtonCell(cbAuteur.getCellFactory().call(null));

        // Affichage post (titre au lieu de toString)
        cbPost.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(Post post, boolean empty) {
                super.updateItem(post, empty);
                setText(empty || post == null ? null :
                        post.getTitre());
            }
        });

        cbPost.setButtonCell(cbPost.getCellFactory().call(null));
    }

    @FXML
    private void addCommentaire() {

        try {

            Commentaire c = new Commentaire();
            c.setContenu(txtContenu.getText());
            c.setDateCommentaire(LocalDate.now());
            c.setAuteur(cbAuteur.getValue());
            c.setPost(cbPost.getValue());

            commentaireService.add(c);

            new Alert(Alert.AlertType.INFORMATION,
                    "Commentaire ajouté avec succès !")
                    .showAndWait();

            Stage stage = (Stage) txtContenu.getScene().getWindow();
            stage.close();

        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR,
                    e.getMessage()).showAndWait();
        }
    }
}
