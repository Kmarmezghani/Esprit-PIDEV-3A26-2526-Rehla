package Controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import models.Pays;
import services.PaysService;

public class EditPaysController {

    @FXML
    private TextField TFnomPays;

    @FXML
    private TextField TFcontinent;

    @FXML
    private TextArea TAdescription;

    private Pays currentPays;
    private final PaysService paysService = new PaysService();

    public void setPays(Pays pays) {
        this.currentPays = pays;
        TFnomPays.setText(pays.getNom());
        TFcontinent.setText(pays.getContinent());
        TAdescription.setText(pays.getDescription());
    }

    @FXML
    void updatePays(ActionEvent event) {
        // Validation
        if (TFnomPays.getText().isEmpty() || TFcontinent.getText().isEmpty() || TAdescription.getText().isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Validation Error");
            alert.setHeaderText("Missing Information");
            alert.setContentText("Please fill in all fields!");
            alert.showAndWait();
            return;
        }

        // Update Pays object
        currentPays.setNom(TFnomPays.getText());
        currentPays.setContinent(TFcontinent.getText());
        currentPays.setDescription(TAdescription.getText());

        // Update in database
        paysService.update(currentPays);

        // Success message
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setHeaderText("Country Updated");
        alert.setContentText("The country has been updated successfully!");
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
