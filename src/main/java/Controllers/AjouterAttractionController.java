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
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.function.UnaryOperator;
import models.TypeAttraction;
import java.util.stream.Collectors;

public class AjouterAttractionController implements Initializable {

    // ===== FXML Fields =====
    @FXML private TextField TFnomAttraction;
    @FXML private TextArea TAdescription;
    @FXML private MenuButton MBtype;
    @FXML private TextField TFprix;
    @FXML private TextField TFheureOuverture;
    @FXML private TextField TFheureFermeture;
    @FXML private CheckBox CBestFerme;
    @FXML private ComboBox<Ville> CBville;

    @FXML private Label nomError;
    @FXML private Label prixError;
    @FXML private Label heureOuvertureError;
    @FXML private Label heureFermetureError;
    @FXML private Label descriptionCounter;

    private final AttractionService attractionService = new AttractionService();
    private final VilleService villeService = new VilleService();

    // ===== Constants =====
    private static final int MAX_NAME = 100;
    private static final int MAX_TYPE = 30;
    private static final int MAX_DESC = 500;
    private static final double MAX_PRICE = 10000;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        loadVilles();
        setupTypeMenu();
        setupFilters();
        setupListeners();
    }

    private void setupTypeMenu() {
        for (TypeAttraction type : TypeAttraction.values()) {
            CheckMenuItem item = new CheckMenuItem(type.toString());
            item.setUserData(type);
            MBtype.getItems().add(item);
        }
    }

    // ===== Filters to block invalid typing =====
    private void setupFilters() {
        // Name: letters, digits, space, hyphen, apostrophe
        TFnomAttraction.setTextFormatter(new TextFormatter<>((UnaryOperator<TextFormatter.Change>) c ->
            c.getControlNewText().matches("[a-zA-ZÀ-ÿ0-9 \\-']*") ? c : null
        ));



        // Price: digits + max 2 decimals (allows dot OR comma, starts empty)
        UnaryOperator<TextFormatter.Change> priceFilter = c -> {
            if (c.getControlNewText().matches("\\d*([.,]\\d{0,2})?")) {
                return c;
            }
            return null;
        };
        TFprix.setTextFormatter(new TextFormatter<>(priceFilter));

        // Time fields: HH:mm (digits + colon)
        UnaryOperator<TextFormatter.Change> timeFilter = c -> c.getControlNewText().matches("[0-9:]*") ? c : null;
        TFheureOuverture.setTextFormatter(new TextFormatter<>(timeFilter));
        TFheureFermeture.setTextFormatter(new TextFormatter<>(timeFilter));
    }

    // ===== Real-time listeners =====
    private void setupListeners() {
        // Name, Type, Price real-time validation
        TFnomAttraction.textProperty().addListener((obs, oldVal, newVal) -> validateLength(TFnomAttraction, nomError, newVal, 3, MAX_NAME));

        TFprix.textProperty().addListener((obs, oldVal, newVal) -> validatePrice(TFprix, prixError, newVal));

        // Description counter
        TAdescription.textProperty().addListener((obs, oldVal, newVal) -> {
            descriptionCounter.setText(newVal.length() + " / " + MAX_DESC);
            if (newVal.isEmpty() || newVal.length() > MAX_DESC)
                TAdescription.setStyle("-fx-border-color: red; -fx-border-width: 2;");
            else
                TAdescription.setStyle("-fx-border-color: green; -fx-border-width: 2;");
        });

        // Time fields
        Map<TextField, Label> timeFields = Map.of(TFheureOuverture, heureOuvertureError, TFheureFermeture, heureFermetureError);
        timeFields.forEach((field, label) -> field.textProperty().addListener((obs, oldVal, newVal) -> validateTimeField(field, label, newVal)));

        // Closed checkbox
        CBestFerme.selectedProperty().addListener((obs, oldVal, isClosed) -> {
            TFheureOuverture.setDisable(isClosed);
            TFheureFermeture.setDisable(isClosed);
            if (isClosed) {
                TFheureOuverture.clear();
                TFheureFermeture.clear();
                heureOuvertureError.setText("");
                heureFermetureError.setText("");
                TFheureOuverture.setStyle("");
                TFheureFermeture.setStyle("");
            }
        });
    }

    // ===== Validation Helpers =====
    private void validateLength(TextField field, Label label, String value, int min, int max) {
        if (value.isEmpty()) setFieldError(field, label, "❌ Required", "red");
        else if (value.length() < min) setFieldError(field, label, "⚠ Min " + min, "orange");
        else if (value.length() > max) setFieldError(field, label, "❌ Max " + max, "red");
        else setFieldError(field, label, "✅ Valid", "green");
    }

    private void validatePrice(TextField field, Label label, String value) {
        try {
            double p = Double.parseDouble(value.replace(",", "."));
            if (p < 0) setFieldError(field, label, "❌ Cannot be negative", "red");
            else if (p > MAX_PRICE) setFieldError(field, label, "❌ Too high", "red");
            else setFieldError(field, label, "✅ Valid", "green");
        } catch (NumberFormatException e) {
            setFieldError(field, label, "❌ Invalid number", "red");
        }
    }

    private void validateTimeField(TextField field, Label label, String value) {
        if (value.isEmpty()) setFieldError(field, label, "❌ Required", "red");
        else if (!value.matches("([01]?[0-9]|2[0-3]):[0-5][0-9]")) setFieldError(field, label, "⚠ Format HH:mm", "orange");
        else setFieldError(field, label, "✅ Valid", "green");
    }

    private void setFieldError(TextField field, Label label, String message, String color) {
        label.setText(message);
        label.setStyle("-fx-text-fill: " + color + ";");
        field.setStyle("-fx-border-color: " + color + "; -fx-border-width: 2;");
    }

    // ===== Load Cities =====
    private void loadVilles() {
        List<Ville> villeList = villeService.getAll();
        ObservableList<Ville> villeObs = FXCollections.observableArrayList(villeList);
        CBville.setItems(villeObs);
        CBville.setConverter(new javafx.util.StringConverter<Ville>() {
            @Override public String toString(Ville ville) { return ville == null ? null : ville.getNom(); }
            @Override public Ville fromString(String s) { return null; }
        });
    }

    // ===== Final Validation & Save =====
    @FXML
    void ajouterAttraction(ActionEvent event) {
        String nom = TFnomAttraction.getText().trim();
        
        String type = MBtype.getItems().stream()
                .filter(item -> ((CheckMenuItem) item).isSelected())
                .map(item -> item.getText())
                .collect(Collectors.joining(","));

        String desc = TAdescription.getText().trim();
        String prixText = TFprix.getText().trim();
        boolean estFerme = CBestFerme.isSelected();
        String heureOuvStr = TFheureOuverture.getText().trim();
        String heureFermStr = TFheureFermeture.getText().trim();
        Ville ville = CBville.getValue();

        // Required fields
        if (nom.isEmpty() || type.isEmpty() || desc.isEmpty() || prixText.isEmpty() || ville == null) {
            showError("Missing Information", "Please fill all fields!");
            return;
        }

        // Parse price
        double prix;
        try { prix = Double.parseDouble(prixText.replace(",", ".")); }
        catch (NumberFormatException e) { showError("Invalid Price", "Enter a valid number"); return; }
        if (prix < 0 || prix > MAX_PRICE) { showError("Invalid Price", "Out of range"); return; }

        // Parse times
        LocalTime heureOuv = null, heureFerm = null;
        if (!estFerme) {
            if (heureOuvStr.isEmpty() || heureFermStr.isEmpty()) {
                showError("Missing Time", "Enter opening and closing times");
                return;
            }
            try {
                // Auto-fix single digit hour if needed (e.g. 9:00 -> 09:00)
                if (heureOuvStr.matches("\\d:\\d\\d")) heureOuvStr = "0" + heureOuvStr;
                if (heureFermStr.matches("\\d:\\d\\d")) heureFermStr = "0" + heureFermStr;
                
                heureOuv = LocalTime.parse(heureOuvStr);
                heureFerm = LocalTime.parse(heureFermStr);
            } catch (Exception e) {
                showError("Invalid Time", "Use HH:mm format");
                return;
            }
            // Removed check for overnight hours to allow 09:00 -> 06:00
        }

        // Duplicate check
        if (attractionService.getAll().stream().anyMatch(a -> a.getNom().equalsIgnoreCase(nom) && a.getVilleId() == ville.getId())) {
            showError("Duplicate Attraction", "An attraction with this name already exists in " + ville.getNom());
            return;
        }

        // Save
        Attraction attraction = new Attraction();
        attraction.setNom(nom); attraction.setType(type); attraction.setDescription(desc);
        attraction.setPrix(prix);
        attraction.setHeureOuverture(estFerme ? null : java.sql.Time.valueOf(heureOuv));
        attraction.setHeureFermeture(estFerme ? null : java.sql.Time.valueOf(heureFerm));
        attraction.setEstFerme(estFerme); attraction.setVilleId(ville.getId());
        attractionService.add(attraction);

        showSuccess("Attraction Added", "Successfully added!");
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

    @FXML
    void handleCancel(ActionEvent event) { closeWindow(); }

    private void closeWindow() { Stage stage = (Stage) TFnomAttraction.getScene().getWindow(); stage.close(); }
}