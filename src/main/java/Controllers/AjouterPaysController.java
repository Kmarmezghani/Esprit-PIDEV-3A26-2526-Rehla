package Controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import models.Pays;
import services.PaysService;

public class AjouterPaysController {

    @FXML
    private TextField TFnomPays;

    @FXML
    private TextField TFcontinent;

    @FXML
    private TextArea TAdescription;

    private final PaysService paysService = new PaysService();

    @FXML
    void ajouterPays(ActionEvent event) {
        // Validation
        if (TFnomPays.getText().isEmpty() || TFcontinent.getText().isEmpty() || TAdescription.getText().isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Validation Error");
            alert.setHeaderText("Missing Information");
            alert.setContentText("Please fill in all fields!");
            alert.showAndWait();
            return;
        }

        // Create Pays object
        Pays pays = new Pays();
        pays.setNom(TFnomPays.getText());
        pays.setContinent(TFcontinent.getText());
        pays.setDescription(TAdescription.getText());

        // Add to database
        paysService.add(pays);

        // Success message
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setHeaderText("Country Added");
        alert.setContentText("The country has been added successfully!");
        alert.showAndWait();

        // Close the window
        closeWindow();
    }

    @FXML
    void handleCancel(ActionEvent event) {
        closeWindow();
    }

    private void closeWindow() {
        Stage stage = (Stage) TFnomPays.getScene().getWindow();
        stage.close();
    }
}
