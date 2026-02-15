package Controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;
import models.Commentaire;
import services.CommentaireService;

public class UpdateCommentController {

    @FXML
    private TextArea txtContenu;

    @FXML
    private Button btnSave;

    private Commentaire commentToEdit;
    private CommentaireService commentaireService = new CommentaireService();

    // Méthode appelée depuis DashboardController
    public void setCommentToEdit(Commentaire com) {
        this.commentToEdit = com;
        txtContenu.setText(com.getContenu()); // remplir le champ avec l'ancien contenu
    }


    @FXML
    private void updateComment() {

        if (commentToEdit == null) {
            return;
        }

        // 🔹 Contrôle de saisie
        if (txtContenu.getText() == null || txtContenu.getText().trim().isEmpty()) {
            showError("Le contenu du commentaire ne peut pas être vide !");
            return;
        }

        try {
            commentToEdit.setContenu(txtContenu.getText().trim());
            commentaireService.update(commentToEdit);

            new Alert(Alert.AlertType.INFORMATION,
                    "Commentaire modifié avec succès !")
                    .showAndWait();

            Stage stage = (Stage) btnSave.getScene().getWindow();
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

}
