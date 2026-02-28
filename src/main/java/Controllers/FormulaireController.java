package Controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

public class FormulaireController {

    @FXML
    private AnchorPane root;

    @FXML
    private void handleCancel(ActionEvent event) {
        Stage stage = (Stage) ((Node) event.getSource())
                .getScene()
                .getWindow();
        stage.close();
    }

    @FXML
    public void initialize() {
        // Initialisation du formulaire si nécessaire
        // (champs, listeners, etc.)
    }
}