package Controllers;

import javafx.fxml.FXML;
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
    private void saveComment() {
        if (commentToEdit != null) {
            commentToEdit.setContenu(txtContenu.getText());
            commentaireService.update(commentToEdit); // mettre à jour dans la base

            // Fermer la fenêtre
            Stage stage = (Stage) btnSave.getScene().getWindow();
            stage.close();
        }
    }
}
