package Controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;
import models.Pays;
import services.CountriesNowService;
import services.DescriptionApiService;
import services.PaysService;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class AjouterPaysController implements Initializable {

    // ===== FXML Fields =====
    @FXML private ComboBox<String> CBNomPays;
    @FXML private TextArea TAdescription;

    @FXML private Label nomError;
    @FXML private Label descriptionCounter;

    private final PaysService paysService = new PaysService();
    private final CountriesNowService countriesNowService = new CountriesNowService();
    private final DescriptionApiService descriptionApiService = new DescriptionApiService();

    // ===== Constants =====
    private static final int MAX_DESC = 500;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        loadCountriesFromApi();
        setupListeners();
    }

    private void loadCountriesFromApi() {
        List<String> countries = countriesNowService.getCountries();
        CBNomPays.getItems().setAll(countries);
        if (countries.isEmpty()) {
            nomError.setText("X API unavailable");
            nomError.setStyle("-fx-text-fill: red;");
        }
    }

    // ===== Listeners =====
    private void setupListeners() {
        CBNomPays.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null || newVal.isBlank()) {
                nomError.setText("X Required");
                nomError.setStyle("-fx-text-fill: red;");
                CBNomPays.setStyle("-fx-border-color: red; -fx-border-width: 2;");
            } else {
                nomError.setText("V Valid");
                nomError.setStyle("-fx-text-fill: green;");
                CBNomPays.setStyle("-fx-border-color: green; -fx-border-width: 2;");
            }
        });

        TAdescription.textProperty().addListener((obs, oldVal, newVal) -> {
            descriptionCounter.setText(newVal.length() + " / " + MAX_DESC);
            if (newVal.isEmpty() || newVal.length() > MAX_DESC)
                TAdescription.setStyle("-fx-border-color: red; -fx-border-width: 2;");
            else
                TAdescription.setStyle("-fx-border-color: green; -fx-border-width: 2;");
        });
    }

    // ===== Save =====
    @FXML
    void handleGenerateDescription(ActionEvent event) {
        String country = CBNomPays.getValue();
        if (country == null || country.isBlank()) {
            showError("Country Required", "Please select a country first.");
            return;
        }

        try {
            String description = descriptionApiService.generateCountryDescription(country);
            TAdescription.setText(description);
        } catch (Exception e) {
            showError("Description API Error", e.getMessage());
        }
    }

    // ===== Save =====
    @FXML
    void ajouterPays(ActionEvent event) {
        String nom = CBNomPays.getValue() == null ? "" : CBNomPays.getValue().trim();
        String desc = TAdescription.getText().trim();

        if (nom.isEmpty() || desc.isEmpty()) {
            showError("Missing Information", "Please fill all fields!");
            return;
        }

        // Duplicate check
        if (paysService.getAll().stream().anyMatch(p -> p.getNom().equalsIgnoreCase(nom))) {
            showError("Duplicate Country", "A country with this name already exists!");
            return;
        }

        Pays pays = new Pays();
        pays.setNom(nom);
        pays.setDescription(desc);

        paysService.add(pays);

        showSuccess("Country Added", "Successfully added!");
        closeWindow();
    }

    private void showError(String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR); alert.setTitle("Validation Error");
        alert.setHeaderText(header); alert.setContentText(content); alert.showAndWait();
    }

    private void showSuccess(String header, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION); alert.setTitle("Success");
        alert.setHeaderText(header); alert.setContentText(content); alert.showAndWait();
    }

    @FXML void handleCancel(ActionEvent event) { closeWindow(); }
    private void closeWindow() { Stage stage = (Stage) CBNomPays.getScene().getWindow(); stage.close(); }
}
