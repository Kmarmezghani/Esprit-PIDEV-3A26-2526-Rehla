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
import services.CountriesNowService;
import services.PaysService;
import services.VilleService;

import models.Saison;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import models.TypeTourisme;
import java.util.stream.Collectors;

public class AjouterVilleController implements Initializable {

    // ===== FXML Fields =====
    @FXML private ComboBox<String> CBNomVille;
    @FXML private ComboBox<Pays> CBpays;
    @FXML private MenuButton MBtypeTourisme;
    @FXML private ComboBox<Saison> CBsaison;

    @FXML private Label nomError;
    @FXML private Label typeError;
    @FXML private Label saisonError;

    private final VilleService villeService = new VilleService();
    private final PaysService paysService = new PaysService();
    private final CountriesNowService countriesNowService = new CountriesNowService();

    // ===== Constants =====
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        loadPays();
        loadSaisons();
        setupTypeMenu();
        setupListeners();
        CBNomVille.setDisable(true);
    }

    private void setupTypeMenu() {
        for (TypeTourisme type : TypeTourisme.values()) {
            CheckMenuItem item = new CheckMenuItem(type.toString());
            item.setUserData(type);
            MBtypeTourisme.getItems().add(item);
        }
    }

    private void loadSaisons() {
        CBsaison.getItems().setAll(Saison.values());
    }

    // ===== Listeners =====
    private void setupListeners() {
        CBNomVille.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null || newVal.isBlank()) {
                nomError.setText("X Required");
                nomError.setStyle("-fx-text-fill: red;");
                CBNomVille.setStyle("-fx-border-color: red; -fx-border-width: 2;");
            } else {
                nomError.setText("V Valid");
                nomError.setStyle("-fx-text-fill: green;");
                CBNomVille.setStyle("-fx-border-color: green; -fx-border-width: 2;");
            }
        });

        CBpays.valueProperty().addListener((obs, oldVal, newVal) -> loadCitiesForSelectedCountry(newVal));
        
        CBsaison.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) {
                saisonError.setText("X Required");
                saisonError.setStyle("-fx-text-fill: red;");
                CBsaison.setStyle("-fx-border-color: red; -fx-border-width: 2;");
            } else {
                saisonError.setText("V Valid");
                saisonError.setStyle("-fx-text-fill: green;");
                CBsaison.setStyle("-fx-border-color: green; -fx-border-width: 2;");
            }
        });
    }

    private void loadCitiesForSelectedCountry(Pays selectedPays) {
        CBNomVille.getItems().clear();
        CBNomVille.setValue(null);
        CBNomVille.setStyle("");

        if (selectedPays == null) {
            CBNomVille.setDisable(true);
            return;
        }

        List<String> cities = countriesNowService.getCitiesByCountry(selectedPays.getNom());
        CBNomVille.getItems().setAll(cities);
        CBNomVille.setDisable(cities.isEmpty());

        if (cities.isEmpty()) {
            nomError.setText("X No cities from API");
            nomError.setStyle("-fx-text-fill: red;");
            CBNomVille.setStyle("-fx-border-color: red; -fx-border-width: 2;");
        } else {
            nomError.setText("");
            CBNomVille.setStyle("");
        }
    }

    // ===== Load Data =====
    private void loadPays() {
        List<Pays> paysList = paysService.getAll();
        ObservableList<Pays> paysObs = FXCollections.observableArrayList(paysList);
        CBpays.setItems(paysObs);
        CBpays.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(Pays pays) { return pays == null ? null : pays.getNom(); }
            @Override public Pays fromString(String s) { return null; }
        });
    }

    // ===== Save =====
    @FXML
    void ajouterVille(ActionEvent event) {
        String nom = CBNomVille.getValue() == null ? "" : CBNomVille.getValue().trim();
        Pays pays = CBpays.getValue();
        
        String type = MBtypeTourisme.getItems().stream()
                .filter(item -> ((CheckMenuItem) item).isSelected())
                .map(item -> item.getText())
                .collect(Collectors.joining(","));
                
        Saison saison = CBsaison.getValue();

        if (nom.isEmpty() || pays == null || type.isEmpty() || saison == null) {
            showError("Missing Information", "Please fill all fields!");
            return;
        }

        // Duplicate check
        if (villeService.getAll().stream().anyMatch(v -> v.getNom().equalsIgnoreCase(nom) && v.getPaysId() == pays.getId())) {
            showError("Duplicate City", "A city with this name already exists in " + pays.getNom());
            return;
        }

        Ville ville = new Ville();
        ville.setNom(nom); ville.setPaysId(pays.getId());
        ville.setTypeTourisme(type);
        ville.setSaison(saison.toString());

        villeService.add(ville);

        showSuccess("City Added", "Successfully added!");
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
    private void closeWindow() { Stage stage = (Stage) CBNomVille.getScene().getWindow(); stage.close(); }
}
