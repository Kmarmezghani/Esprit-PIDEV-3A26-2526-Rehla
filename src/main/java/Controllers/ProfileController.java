package Controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import models.Personne;
import models.Preference;
import services.PersonneService;
import services.PreferenceService;
import util.Session;
import util.ValidationUtil;

import java.io.IOException;

public class ProfileController {

    @FXML private TextField fieldNom;
    @FXML private TextField fieldPrenom;
    @FXML private TextField fieldEmail;
    @FXML private TextField fieldPassword;
    @FXML private TextField fieldBudgetMin;
    @FXML private TextField fieldBudgetMax;
    @FXML private TextField fieldTypesVoyage;
    @FXML private TextField fieldCentresInteret;
    @FXML private Button btnSave;

    private final PersonneService personneService = new PersonneService();
    private final PreferenceService preferenceService = new PreferenceService();

    private Personne currentUser;
    private Preference currentPreference;

    @FXML
    public void initialize() {
        currentUser = Session.getCurrentUser();
        if (currentUser == null) {
            showAlert(Alert.AlertType.WARNING, "Not logged in", "Please log in to view your profile.");
            return;
        }
        fieldNom.setText(currentUser.getNom());
        fieldPrenom.setText(currentUser.getPrenom());
        fieldEmail.setText(currentUser.getEmail());

        currentPreference = preferenceService.getByPersonneId(currentUser.getId());
        if (currentPreference != null) {
            if (currentPreference.getBudgetMin() != null)
                fieldBudgetMin.setText(String.valueOf(currentPreference.getBudgetMin()));
            if (currentPreference.getBudgetMax() != null)
                fieldBudgetMax.setText(String.valueOf(currentPreference.getBudgetMax()));
            fieldTypesVoyage.setText(currentPreference.getTypesVoyage());
            fieldCentresInteret.setText(currentPreference.getCentresInteret());
        }
    }

    @FXML
    void handleSave(ActionEvent event) {
        if (currentUser == null) return;

        String nom = fieldNom.getText() != null ? fieldNom.getText().trim() : "";
        String prenom = fieldPrenom.getText() != null ? fieldPrenom.getText().trim() : "";
        String email = fieldEmail.getText() != null ? fieldEmail.getText().trim() : "";
        String newPassword = fieldPassword.getText();

        if (nom.isEmpty() || prenom.isEmpty() || email.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validation", "Nom, Prénom and Email are required.");
            return;
        }

        if (!ValidationUtil.nameDoesNotStartWithNumber(nom)) {
            showAlert(Alert.AlertType.WARNING, "Validation", "Nom: " + ValidationUtil.nameErrorMessage());
            return;
        }
        if (!ValidationUtil.nameDoesNotStartWithNumber(prenom)) {
            showAlert(Alert.AlertType.WARNING, "Validation", "Prénom: " + ValidationUtil.nameErrorMessage());
            return;
        }
        if (!ValidationUtil.isValidEmail(email)) {
            showAlert(Alert.AlertType.WARNING, "Validation", ValidationUtil.emailErrorMessage());
            return;
        }
        if (newPassword != null && !newPassword.isEmpty()) {
            if (!ValidationUtil.isStrongPassword(newPassword)) {
                showAlert(Alert.AlertType.WARNING, "Validation", "New password: " + ValidationUtil.strongPasswordErrorMessage());
                return;
            }
            currentUser.setMotDePasse(newPassword);
        }

        currentUser.setNom(nom);
        currentUser.setPrenom(prenom);
        currentUser.setEmail(email);
        try {
            personneService.update(currentUser);
        } catch (RuntimeException e) {
            showAlert(Alert.AlertType.ERROR, "Database error", "Could not update profile.\n\n" + e.getMessage());
            return;
        }
        Session.setCurrentUser(currentUser);

        Double budgetMin = parseDouble(fieldBudgetMin.getText());
        Double budgetMax = parseDouble(fieldBudgetMax.getText());
        String typesVoyage = fieldTypesVoyage.getText() != null ? fieldTypesVoyage.getText().trim() : null;
        String centresInteret = fieldCentresInteret.getText() != null ? fieldCentresInteret.getText().trim() : null;

        try {
            if (currentPreference != null) {
                currentPreference.setBudgetMin(budgetMin);
                currentPreference.setBudgetMax(budgetMax);
                currentPreference.setTypesVoyage(typesVoyage);
                currentPreference.setCentresInteret(centresInteret);
                preferenceService.update(currentPreference);
            } else {
                Preference pref = new Preference(budgetMin, budgetMax, typesVoyage, centresInteret, currentUser.getId());
                preferenceService.add(pref);
            }
        } catch (RuntimeException e) {
            showAlert(Alert.AlertType.ERROR, "Database error", "Profile updated but preferences failed.\n\n" + e.getMessage());
            return;
        }

        showAlert(Alert.AlertType.INFORMATION, "Saved", "Profile and preferences updated.");
    }

    @FXML
    void goBack(ActionEvent event) {
        navigateToHome();
    }

    @FXML
    void handleDeleteAccount(ActionEvent event) {
        if (currentUser == null) return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete account");
        confirm.setHeaderText("Are you sure?");
        confirm.setContentText("Your account and preferences will be permanently deleted. You will be redirected to the login page.");
        confirm.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                preferenceService.deleteByPersonneId(currentUser.getId());
                personneService.delete(currentUser);
                Session.clear();
                navigateToLogin();
            }
        });
    }

    private void navigateToHome() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/HomePage.fxml"));
            Stage stage = (Stage) fieldNom.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void navigateToLogin() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/loginPage.fxml"));
            Stage stage = (Stage) fieldNom.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Double parseDouble(String s) {
        if (s == null || s.trim().isEmpty()) return null;
        try {
            return Double.parseDouble(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
