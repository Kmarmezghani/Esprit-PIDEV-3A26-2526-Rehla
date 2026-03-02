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

import models.Saison;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.function.UnaryOperator;
import models.TypeTourisme;
import java.util.stream.Collectors;
import java.util.Arrays;

public class EditVilleController implements Initializable {

    // ===== FXML Fields =====
    @FXML private TextField TFnomVille;
    @FXML private ComboBox<Pays> CBpays;
    @FXML private TextField TFregion;
    @FXML private MenuButton MBtypeTourisme;
    @FXML private ComboBox<Saison> CBsaison;

    @FXML private Label nomError;
    @FXML private Label regionError;
    @FXML private Label typeError;
    @FXML private Label saisonError;

    private Ville currentVille;
    private final VilleService villeService = new VilleService();
    private final PaysService paysService = new PaysService();

    // ===== Constants =====
    private static final int MAX_NAME = 100;
    private static final int MAX_REGION = 50;
    private static final int MAX_TYPE = 50;
    private static final int MAX_SAISON = 30;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        loadPays();
        loadSaisons();
        setupTypeMenu();
        setupFilters();
        setupListeners();
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

    // ===== Filters =====
    private void setupFilters() {
        UnaryOperator<TextFormatter.Change> textFilter = c -> 
            c.getControlNewText().matches("[a-zA-Z \\-']*") ? c : null;

        TFnomVille.setTextFormatter(new TextFormatter<>(textFilter));
        /* Removed TFtypeTourisme filter */
    }

    // ===== Listeners =====
    private void setupListeners() {
        TFnomVille.textProperty().addListener((obs, oldVal, newVal) -> validateLength(TFnomVille, nomError, newVal, 3, MAX_NAME));
        // TFtypeTourisme listener removed
        
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

    // ===== Validation Helpers =====
    private void validateLength(TextField field, Label label, String value, int min, int max) {
        if (value.isEmpty()) setFieldError(field, label, "X Required", "red");
        else if (value.length() < min) setFieldError(field, label, "! Min " + min, "orange");
        else if (value.length() > max) setFieldError(field, label, "X Max " + max, "red");
        else setFieldError(field, label, "V Valid", "green");
    }

    private void setFieldError(TextField field, Label label, String message, String color) {
        if (label != null) {
            label.setText(message);
            label.setStyle("-fx-text-fill: " + color + ";");
        }
        field.setStyle("-fx-border-color: " + color + "; -fx-border-width: 2;");
    }

    // ===== Set Data =====
    public void setVille(Ville ville) {
        this.currentVille = ville;
        TFnomVille.setText(ville.getNom());
        
        // Set selected types
        String[] types = ville.getTypeTourisme().split(",");
        for (MenuItem item : MBtypeTourisme.getItems()) {
            CheckMenuItem checkItem = (CheckMenuItem) item;
            if (Arrays.asList(types).contains(checkItem.getText())) {
                checkItem.setSelected(true);
            }
        }
        
        Saison s = Saison.fromString(ville.getSaison());
        if (s != null) CBsaison.setValue(s);
        
        for (Pays pays : CBpays.getItems()) {
            if (pays.getId() == ville.getPaysId()) {
                CBpays.setValue(pays);
                break;
            }
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

    // ===== Update =====
    @FXML
    void updateVille(ActionEvent event) {
        String nom = TFnomVille.getText().trim();
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

        // Duplicate check (ignore self)
        if (villeService.getAll().stream().anyMatch(v -> v.getNom().equalsIgnoreCase(nom) && v.getPaysId() == pays.getId() && v.getId() != currentVille.getId())) {
            showError("Duplicate City", "Another city with this name already exists in " + pays.getNom());
            return;
        }

        currentVille.setNom(nom); currentVille.setPaysId(pays.getId());
        currentVille.setTypeTourisme(type);
        currentVille.setSaison(saison.toString());

        villeService.update(currentVille);

        showSuccess("City Updated", "Successfully updated!");
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
    private void closeWindow() { Stage stage = (Stage) TFnomVille.getScene().getWindow(); stage.close(); }
}
