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

public class UpdatePostController {

    @FXML private TextField txtTitre;
    @FXML private TextArea txtContenu;
    @FXML private TextField txtPopularite;
    @FXML private TextField txtAuteurId;
    @FXML private ImageView imagePreview;

    private File selectedImageFile;
    private Post postToEdit;

    private final PostService postService = new PostService();

    // 🔁 Charger les données du post
    public void setPostToEdit(Post post) {
        this.postToEdit = post;

        txtTitre.setText(post.getTitre());
        txtContenu.setText(post.getContenu());
        txtPopularite.setText(String.valueOf(post.getPopularite()));
        txtAuteurId.setText(String.valueOf(post.getAuteur().getId()));

        if (post.getImage() != null && !post.getImage().isEmpty()) {
            File file = new File(post.getImage());
            if (file.exists()) {
                imagePreview.setImage(new Image(file.toURI().toString()));
            }
        }
    }

    // 📂 Choisir nouvelle image
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

    // 💾 Update Post
    @FXML
    private void updatePost() {

        try {
            postToEdit.setTitre(txtTitre.getText());
            postToEdit.setContenu(txtContenu.getText());
            postToEdit.setPopularite(Integer.parseInt(txtPopularite.getText()));

            // 🔁 mettre à jour auteur aussi
            Personne auteur = new Personne();
            auteur.setId(Integer.parseInt(txtAuteurId.getText()));
            postToEdit.setAuteur(auteur);

            // 📸 si nouvelle image choisie
            if (selectedImageFile != null) {
                String imagePath = copierImage(selectedImageFile);
                postToEdit.setImage(imagePath);
            }

            postService.update(postToEdit);

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setContentText("Post modifié avec succès !");
            alert.showAndWait();

            Stage stage = (Stage) txtTitre.getScene().getWindow();
            stage.close();

        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage()).showAndWait();
        }
    }

    // 📁 Même méthode que AddPostController
    private String copierImage(File imageFile) throws IOException {

        String dossier = System.getProperty("user.home") + "/myapp/uploads/";
        Files.createDirectories(Paths.get(dossier));

        String extension = imageFile.getName()
                .substring(imageFile.getName().lastIndexOf("."));

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
