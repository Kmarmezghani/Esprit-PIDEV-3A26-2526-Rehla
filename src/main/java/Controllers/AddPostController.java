package Controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import models.Post;
import models.Personne;
import services.PostService;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDate;

public class AddPostController {

    @FXML private TextField txtTitre;
    @FXML private TextArea txtContenu;
    @FXML private TextField txtPopularite;
    @FXML private TextField txtAuteurId;
    @FXML private ImageView imagePreview;

    private File selectedImageFile;

    private final PostService postService = new PostService();

    // 📂 Ouvrir la galerie
    @FXML
    private void choisirImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir une image");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(
                        "Images", "*.png", "*.jpg", "*.jpeg"
                )
        );

        selectedImageFile = fileChooser.showOpenDialog(null);

        if (selectedImageFile != null) {
            imagePreview.setImage(
                    new Image(selectedImageFile.toURI().toString())
            );
        }

    }

    // ➕ Ajouter post
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


            if (selectedImageFile != null) {
                String imagePath = copierImage(selectedImageFile);

                post.setImage(imagePath);
            }

            postService.add(post);

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setContentText("Post ajouté avec succès !");
            alert.showAndWait();

            Stage stage = (Stage) txtTitre.getScene().getWindow();
            stage.close();

        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage()).showAndWait();
        }
    }

    private String copierImage(File imageFile) throws IOException {


        String dossier = System.getProperty("user.home") + "/myapp/uploads/";
        Files.createDirectories(Paths.get(dossier));

        // extension
        String extension = imageFile.getName()
                .substring(imageFile.getName().lastIndexOf("."));

        // nom unique
        String fileName = "post_" + System.currentTimeMillis() + extension;

        Path destination = Paths.get(dossier + fileName);

        Files.copy(
                imageFile.toPath(),
                destination,
                StandardCopyOption.REPLACE_EXISTING
        );


        return destination.toAbsolutePath().toString();
    }

}
