package Controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;
import models.Pays;
import services.PaysService;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.function.UnaryOperator;

public class AjouterPaysController implements Initializable {

    // ===== FXML Fields =====
    @FXML private TextField TFnomPays;
    @FXML private TextArea TAdescription;

    @FXML private Label nomError;
    @FXML private Label descriptionCounter;

    private final PaysService paysService = new PaysService();

    // ===== Constants =====
    private static final int MAX_NAME = 50;
    private static final int MAX_DESC = 500;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupFilters();
        setupListeners();
    }

    // ===== Filters =====
    private void setupFilters() {
        UnaryOperator<TextFormatter.Change> textFilter = c -> 
            c.getControlNewText().matches("[a-zA-Z \\-']*") ? c : null;

        TFnomPays.setTextFormatter(new TextFormatter<>(textFilter));
    }

    // ===== Listeners =====
    private void setupListeners() {
        TFnomPays.textProperty().addListener((obs, oldVal, newVal) -> validateLength(TFnomPays, nomError, newVal, 3, MAX_NAME));

        TAdescription.textProperty().addListener((obs, oldVal, newVal) -> {
            descriptionCounter.setText(newVal.length() + " / " + MAX_DESC);
            if (newVal.isEmpty() || newVal.length() > MAX_DESC)
                TAdescription.setStyle("-fx-border-color: red; -fx-border-width: 2;");
            else
                TAdescription.setStyle("-fx-border-color: green; -fx-border-width: 2;");
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

    // ===== Save =====
    @FXML
    void ajouterPays(ActionEvent event) {
        String nom = TFnomPays.getText().trim();
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
    private void closeWindow() { Stage stage = (Stage) TFnomPays.getScene().getWindow(); stage.close(); }
}
