package Controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import models.Post;
import models.Personne;
import services.PersonneService;
import services.PostService;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class AddPostController {

    @FXML private TextField txtTitre;
    @FXML private TextArea txtContenu;
    @FXML private TextField txtPopularite;
    @FXML private ImageView imagePreview;

    private File selectedImageFile;

    private final PostService postService = new PostService();
    @FXML
    private ComboBox<Personne> comboAuteur; // ComboBox pour les auteurs

    private PersonneService userService = new PersonneService();

    @FXML
    public void initialize() {
        loadAuteurs();
    }

    private void loadAuteurs() {
        List<Personne> auteurs = userService.getAll(); // récupère tous les utilisateurs
        ObservableList<Personne> options = FXCollections.observableArrayList(auteurs);
        comboAuteur.setItems(options);

        // Optionnel : afficher seulement le nom dans le ComboBox
        comboAuteur.setCellFactory(param -> new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(Personne item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getNom()); // ou getFullName()
                }
            }
        });

        comboAuteur.setButtonCell(new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(Personne item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getNom());
                }
            }
        });
    }
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

            //  CONTROLE DE SAISIE

            if (txtTitre.getText() == null || txtTitre.getText().trim().isEmpty()) {
                showError("Le titre ne peut pas être vide !");
                return;
            }

            if (txtContenu.getText() == null || txtContenu.getText().trim().isEmpty()) {
                showError("Le contenu ne peut pas être vide !");
                return;
            }

            if (txtPopularite.getText() == null || txtPopularite.getText().trim().isEmpty()) {
                showError("La popularité est obligatoire !");
                return;
            }

            int popularite;
            try {
                popularite = Integer.parseInt(txtPopularite.getText().trim());
            } catch (NumberFormatException e) {
                showError("La popularité doit être un nombre !");
                return;
            }

            Personne auteur = comboAuteur.getSelectionModel().getSelectedItem();
            if (auteur == null) {
                showError("Veuillez sélectionner un auteur !");
                return;
            }

            // Création du post
            Post post = new Post();
            post.setTitre(txtTitre.getText().trim());
            post.setContenu(txtContenu.getText().trim());
            post.setDatePublication(LocalDateTime.now());
            post.setPopularite(popularite);
            post.setAuteur(auteur);

            if (selectedImageFile != null) {
                String imagePath = copierImagePath(selectedImageFile);
                post.setImage(imagePath);
            }

            postService.add(post);

            new Alert(Alert.AlertType.INFORMATION, "Post ajouté avec succès !").showAndWait();

            ((Stage) txtTitre.getScene().getWindow()).close();

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
