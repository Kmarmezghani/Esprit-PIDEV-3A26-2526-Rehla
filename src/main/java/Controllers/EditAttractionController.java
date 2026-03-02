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
import java.util.Arrays;

public class EditAttractionController implements Initializable {

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
    @FXML private Label typeError;
    @FXML private Label prixError;
    @FXML private Label heureOuvertureError;
    @FXML private Label heureFermetureError;
    @FXML private Label descriptionCounter;

    private Attraction currentAttraction;
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

    // ===== Filters =====
    private void setupFilters() {
        TFnomAttraction.setTextFormatter(new TextFormatter<>((UnaryOperator<TextFormatter.Change>) c ->
            c.getControlNewText().matches("[a-zA-Z0-9 \\-']*") ? c : null));

        // Price: digits + max 2 decimals (allows dot OR comma, starts empty)
        UnaryOperator<TextFormatter.Change> priceFilter = c -> {
            if (c.getControlNewText().matches("\\d*([.,]\\d{0,2})?")) {
                return c;
            }
            return null;
        };
        TFprix.setTextFormatter(new TextFormatter<>(priceFilter));

        UnaryOperator<TextFormatter.Change> timeFilter = c -> c.getControlNewText().matches("[0-9:]*") ? c : null;
        TFheureOuverture.setTextFormatter(new TextFormatter<>(timeFilter));
        TFheureFermeture.setTextFormatter(new TextFormatter<>(timeFilter));
    }

    // ===== Listeners =====
    private void setupListeners() {
        TFnomAttraction.textProperty().addListener((obs, oldVal, newVal) -> validateLength(TFnomAttraction, nomError, newVal, 3, MAX_NAME));
        // TFtype listener removed
        TFprix.textProperty().addListener((obs, oldVal, newVal) -> validatePrice(TFprix, prixError, newVal));

        TAdescription.textProperty().addListener((obs, oldVal, newVal) -> {
            descriptionCounter.setText(newVal.length() + " / " + MAX_DESC);
            if (newVal.isEmpty() || newVal.length() > MAX_DESC)
                TAdescription.setStyle("-fx-border-color: red; -fx-border-width: 2;");
            else
                TAdescription.setStyle("-fx-border-color: green; -fx-border-width: 2;");
        });

        Map<TextField, Label> timeFields = Map.of(TFheureOuverture, heureOuvertureError, TFheureFermeture, heureFermetureError);
        timeFields.forEach((field, label) -> field.textProperty().addListener((obs, oldVal, newVal) -> validateTimeField(field, label, newVal)));

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
        if (value.isEmpty()) setFieldError(field, label, "X Required", "red");
        else if (value.length() < min) setFieldError(field, label, "! Min " + min, "orange");
        else if (value.length() > max) setFieldError(field, label, "X Max " + max, "red");
        else setFieldError(field, label, "V Valid", "green");
    }

    private void validatePrice(TextField field, Label label, String value) {
        try {
            double p = Double.parseDouble(value.replace(",", "."));
            if (p < 0) setFieldError(field, label, "X Cannot be negative", "red");
            else if (p > MAX_PRICE) setFieldError(field, label, "X Too high", "red");
            else setFieldError(field, label, "V Valid", "green");
        } catch (NumberFormatException e) {
            setFieldError(field, label, "X Invalid number", "red");
        }
    }

    private void validateTimeField(TextField field, Label label, String value) {
        if (value.isEmpty()) setFieldError(field, label, "X Required", "red");
        else if (!value.matches("([01]?[0-9]|2[0-3]):[0-5][0-9]")) setFieldError(field, label, "! Format HH:mm", "orange");
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
    public void setAttraction(Attraction attraction) {
        this.currentAttraction = attraction;
        TFnomAttraction.setText(attraction.getNom());
        TAdescription.setText(attraction.getDescription());
        
        // Set selected types
        String[] types = attraction.getType().split(",");
        for (MenuItem item : MBtype.getItems()) {
            CheckMenuItem checkItem = (CheckMenuItem) item;
            if (Arrays.asList(types).contains(checkItem.getText())) {
                checkItem.setSelected(true);
            }
        }
        
        TFprix.setText(String.format("%.2f", attraction.getPrix()));
        
        if (attraction.isEstFerme()) {
            CBestFerme.setSelected(true);
        } else {
            CBestFerme.setSelected(false);
            if (attraction.getHeureOuverture() != null) TFheureOuverture.setText(attraction.getHeureOuverture().toString().substring(0, 5));
            if (attraction.getHeureFermeture() != null) TFheureFermeture.setText(attraction.getHeureFermeture().toString().substring(0, 5));
        }

        for (Ville ville : CBville.getItems()) {
            if (ville.getId() == attraction.getVilleId()) {
                CBville.setValue(ville);
                break;
            }
        }
    }

    // ===== Update =====
    @FXML
    void updateAttraction(ActionEvent event) {
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

        if (nom.isEmpty() || type.isEmpty() || desc.isEmpty() || prixText.isEmpty() || ville == null) {
            showError("Missing Information", "Please fill all fields!");
            return;
        }

        // Duplicate check (ignore self)
        if (attractionService.getAll().stream().anyMatch(a -> a.getNom().equalsIgnoreCase(nom) && a.getVilleId() == ville.getId() && a.getId() != currentAttraction.getId())) {
            showError("Duplicate Attraction", "Another attraction with this name already exists in " + ville.getNom());
            return;
        }

        double prix;
        try { prix = Double.parseDouble(prixText.replace(",", ".")); }
        catch (NumberFormatException e) { showError("Invalid Price", "Enter a valid number"); return; }
        if (prix < 0 || prix > MAX_PRICE) { showError("Invalid Price", "Out of range"); return; }

        LocalTime heureOuv = null, heureFerm = null;
        if (!estFerme) {
            if (heureOuvStr.isEmpty() || heureFermStr.isEmpty()) {
                showError("Missing Time", "Enter opening and closing times");
                return;
            }
            try {
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

        currentAttraction.setNom(nom); currentAttraction.setType(type); currentAttraction.setDescription(desc);
        currentAttraction.setPrix(prix);
        currentAttraction.setHeureOuverture(estFerme ? null : java.sql.Time.valueOf(heureOuv));
        currentAttraction.setHeureFermeture(estFerme ? null : java.sql.Time.valueOf(heureFerm));
        currentAttraction.setEstFerme(estFerme); currentAttraction.setVilleId(ville.getId());

        attractionService.update(currentAttraction);

        showSuccess("Attraction Updated", "Successfully updated!");
        closeWindow();
    }

    private void loadVilles() {
        List<Ville> villeList = villeService.getAll();
        ObservableList<Ville> villeObs = FXCollections.observableArrayList(villeList);
        CBville.setItems(villeObs);
        CBville.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(Ville ville) { return ville == null ? null : ville.getNom(); }
            @Override public Ville fromString(String s) { return null; }
        });
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
    private void closeWindow() { Stage stage = (Stage) TFnomAttraction.getScene().getWindow(); stage.close(); }
}
