package Controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;
import models.Attraction;
import models.Ville;
import services.AttractionService;
import services.VilleService;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class AjouterAttractionController implements Initializable {

    @FXML
    private TextField TFnomAttraction;

    @FXML
    private TextArea TAdescription;

    @FXML
    private TextField TFtype;

    @FXML
    private Spinner<Double> SPprix;

    @FXML
    private TextField TFhoraires;

    @FXML
    private ComboBox<Ville> CBville;

    private final AttractionService attractionService = new AttractionService();
    private final VilleService villeService = new VilleService();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Initialize price spinner
        SpinnerValueFactory<Double> valueFactory = new SpinnerValueFactory.DoubleSpinnerValueFactory(0.0, 10000.0, 0.0, 10.0);
        SPprix.setValueFactory(valueFactory);

        // Load cities into ComboBox
        loadVilles();
    }

    private void loadVilles() {
        List<Ville> villeList = villeService.getAll();
        ObservableList<Ville> villeObservableList = FXCollections.observableArrayList(villeList);
        CBville.setItems(villeObservableList);

        // Display city name in ComboBox
        CBville.setCellFactory(param -> new ListCell<Ville>() {
            @Override
            protected void updateItem(Ville ville, boolean empty) {
                super.updateItem(ville, empty);
                if (empty || ville == null) {
                    setText(null);
                } else {
                    setText(ville.getNom());
                }
            }
        });

        CBville.setButtonCell(new ListCell<Ville>() {
            @Override
            protected void updateItem(Ville ville, boolean empty) {
                super.updateItem(ville, empty);
                if (empty || ville == null) {
                    setText(null);
                } else {
                    setText(ville.getNom());
                }
            }
        });
    }

    @FXML
    void ajouterAttraction(ActionEvent event) {
        // Validation
        if (TFnomAttraction.getText().isEmpty() || TAdescription.getText().isEmpty() || 
            TFtype.getText().isEmpty() || TFhoraires.getText().isEmpty() || CBville.getValue() == null) {
            
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Validation Error");
            alert.setHeaderText("Missing Information");
            alert.setContentText("Please fill in all fields!");
            alert.showAndWait();
            return;
        }

        // Create Attraction object
        Attraction attraction = new Attraction();
        attraction.setNom(TFnomAttraction.getText());
        attraction.setDescription(TAdescription.getText());
        attraction.setType(TFtype.getText());
        attraction.setPrix(SPprix.getValue());
        attraction.setHoraires(TFhoraires.getText());
        attraction.setVilleId(CBville.getValue().getId());

        // Add to database
        attractionService.add(attraction);

        // Success message
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setHeaderText("Attraction Added");
        alert.setContentText("The attraction has been added successfully!");
        alert.showAndWait();

        // Close the window
        closeWindow();
    }

    @FXML
    void handleCancel(ActionEvent event) {
        closeWindow();
    }

    private void closeWindow() {
        Stage stage = (Stage) TFnomAttraction.getScene().getWindow();
        stage.close();
    }
}
