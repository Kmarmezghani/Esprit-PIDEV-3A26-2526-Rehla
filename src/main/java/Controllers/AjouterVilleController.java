package Controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;
import models.Pays;
import models.Ville;
import services.PaysService;
import services.VilleService;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class AjouterVilleController implements Initializable {

    @FXML
    private TextField TFnomVille;

    @FXML
    private ComboBox<Pays> CBpays;

    @FXML
    private TextField TFregion;

    @FXML
    private TextField TFtypeTourisme;

    @FXML
    private TextField TFsaison;

    @FXML
    private Spinner<Integer> SPpopularite;

    private final VilleService villeService = new VilleService();
    private final PaysService paysService = new PaysService();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Initialize popularity spinner
        SpinnerValueFactory<Integer> valueFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 100, 50);
        SPpopularite.setValueFactory(valueFactory);

        // Load countries into ComboBox
        loadPays();
    }

    private void loadPays() {
        List<Pays> paysList = paysService.getAll();
        ObservableList<Pays> paysObservableList = FXCollections.observableArrayList(paysList);
        CBpays.setItems(paysObservableList);

        // Display country name in ComboBox
        CBpays.setCellFactory(param -> new ListCell<Pays>() {
            @Override
            protected void updateItem(Pays pays, boolean empty) {
                super.updateItem(pays, empty);
                if (empty || pays == null) {
                    setText(null);
                } else {
                    setText(pays.getNom());
                }
            }
        });

        CBpays.setButtonCell(new ListCell<Pays>() {
            @Override
            protected void updateItem(Pays pays, boolean empty) {
                super.updateItem(pays, empty);
                if (empty || pays == null) {
                    setText(null);
                } else {
                    setText(pays.getNom());
                }
            }
        });
    }

    @FXML
    void ajouterVille(ActionEvent event) {
        // Validation
        if (TFnomVille.getText().isEmpty() || CBpays.getValue() == null || 
            TFregion.getText().isEmpty() || TFtypeTourisme.getText().isEmpty() || 
            TFsaison.getText().isEmpty()) {
            
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Validation Error");
            alert.setHeaderText("Missing Information");
            alert.setContentText("Please fill in all fields!");
            alert.showAndWait();
            return;
        }

        // Create Ville object
        Ville ville = new Ville();
        ville.setNom(TFnomVille.getText());
        ville.setPaysId(CBpays.getValue().getId());
        ville.setRegion(TFregion.getText());
        ville.setTypeTourisme(TFtypeTourisme.getText());
        ville.setSaison(TFsaison.getText());
        ville.setPopularite(SPpopularite.getValue());

        // Add to database
        villeService.add(ville);

        // Success message
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setHeaderText("City Added");
        alert.setContentText("The city has been added successfully!");
        alert.showAndWait();

        // Close the window
        closeWindow();
    }

    @FXML
    void handleCancel(ActionEvent event) {
        closeWindow();
    }

    private void closeWindow() {
        Stage stage = (Stage) TFnomVille.getScene().getWindow();
        stage.close();
    }
}
