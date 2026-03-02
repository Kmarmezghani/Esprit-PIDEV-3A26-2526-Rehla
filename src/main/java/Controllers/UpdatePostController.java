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
    @FXML private TextField txtAuteurId;
    @FXML private ImageView imagePreview;

    private File selectedImageFile;
    private Post postToEdit;

    private final PostService postService = new PostService();


    public void setPostToEdit(Post post) {
        this.postToEdit = post;

        txtTitre.setText(post.getTitre());
        txtContenu.setText(post.getContenu());
        txtAuteurId.setText(String.valueOf(post.getAuteur().getId()));

        if (post.getImage() != null && !post.getImage().isEmpty()) {
            File file = new File(post.getImage());
            if (file.exists()) {
                imagePreview.setImage(new Image(file.toURI().toString()));
            }
        }
    }


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

    @FXML
    private void updatePost() {

        try {

            //  CONTROLE DE SAISIE
            if (txtTitre.getText() == null || txtTitre.getText().trim().isEmpty()) {
                showError("Le titre ne peut pas être vide !");
                return;
            }

            if (txtContenu.getText() == null || txtContenu.getText().trim().isEmpty()) {
                showError("Le contenu ne peut pas être vide !");
                return;
            }


            // Mise à jour
            postToEdit.setTitre(txtTitre.getText().trim());
            postToEdit.setContenu(txtContenu.getText().trim());



            if (selectedImageFile != null) {
                String imagePath = copierImagePath(selectedImageFile);
                postToEdit.setImage(imagePath);
            }

            postService.update(postToEdit);

            new Alert(Alert.AlertType.INFORMATION, "Post modifié avec succès !").showAndWait();

            Stage stage = (Stage) txtTitre.getScene().getWindow();
            stage.close();

        } catch (Exception e) {
            showError(e.getMessage());
        }
    }
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setContentText(message);
        alert.showAndWait();
    }


    private String copierImagePath(File imageFile) throws IOException {

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
