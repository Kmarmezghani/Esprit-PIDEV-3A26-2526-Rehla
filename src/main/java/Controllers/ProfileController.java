package Controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import models.Personne;
import models.Preference;
import models.stats.CarbonFootprintSummary;
import models.stats.UserReputationSummary;
import services.CarbonFootprintService;
import services.PersonneService;
import services.PreferenceService;
import services.ReputationService;
import util.Session;
import util.ValidationUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class ProfileController {

    @FXML private ImageView profilePhotoView;
    @FXML private TextField fieldNom;
    @FXML private TextField fieldPrenom;
    @FXML private TextField fieldEmail;
    @FXML private TextField fieldTelephone;
    @FXML private TextField fieldPassword;
    @FXML private TextField fieldHeureNotif;
    @FXML private CheckBox checkNotifSms;
    @FXML private TextField fieldBudgetMin;
    @FXML private TextField fieldBudgetMax;
    @FXML private TextField fieldTypesVoyage;
    @FXML private TextField fieldCentresInteret;
    @FXML private Button btnSave;
    @FXML private javafx.scene.control.Label labelCo2Total;
    @FXML private javafx.scene.control.Label labelCo2Average;
    @FXML private javafx.scene.control.Label labelReputation;

    private final PersonneService personneService = new PersonneService();
    private final PreferenceService preferenceService = new PreferenceService();
    private final CarbonFootprintService carbonFootprintService = new CarbonFootprintService();
    private final ReputationService reputationService = new ReputationService();

    private Personne currentUser;
    private Preference currentPreference;
    private File selectedPhotoFile;

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
        if (fieldTelephone != null) fieldTelephone.setText(currentUser.getTelephone());
        if (fieldHeureNotif != null && currentUser.getHeureNotif() != null)
            fieldHeureNotif.setText(currentUser.getHeureNotif().format(DateTimeFormatter.ofPattern("HH:mm")));
        if (checkNotifSms != null) checkNotifSms.setSelected(currentUser.getNotifSmsActive() != null ? currentUser.getNotifSmsActive() : true);
        loadProfilePhoto(currentUser.getProfilePhoto());

        currentPreference = preferenceService.getByPersonneId(currentUser.getId());
        if (currentPreference != null) {
            if (currentPreference.getBudgetMin() != null)
                fieldBudgetMin.setText(String.valueOf(currentPreference.getBudgetMin()));
            if (currentPreference.getBudgetMax() != null)
                fieldBudgetMax.setText(String.valueOf(currentPreference.getBudgetMax()));
            fieldTypesVoyage.setText(currentPreference.getTypesVoyage());
            fieldCentresInteret.setText(currentPreference.getCentresInteret());
        }

        loadAdvancedMetrics();
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
        if (fieldTelephone != null) currentUser.setTelephone(fieldTelephone.getText() != null ? fieldTelephone.getText().trim() : null);
        if (fieldHeureNotif != null && fieldHeureNotif.getText() != null && !fieldHeureNotif.getText().trim().isEmpty()) {
            try {
                currentUser.setHeureNotif(LocalTime.parse(fieldHeureNotif.getText().trim(), DateTimeFormatter.ofPattern("HH:mm")));
            } catch (DateTimeParseException ignored) {}
        }
        if (checkNotifSms != null) currentUser.setNotifSmsActive(checkNotifSms.isSelected());
        if (selectedPhotoFile != null) {
            try {
                String path = copyPhotoToUploads(selectedPhotoFile);
                currentUser.setProfilePhoto(path);
                selectedPhotoFile = null;
            } catch (IOException e) {
                showAlert(Alert.AlertType.WARNING, "Photo", "Could not save profile photo: " + e.getMessage());
            }
        }
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

    private void loadAdvancedMetrics() {
        if (currentUser == null) return;

        try {
            CarbonFootprintSummary summary = carbonFootprintService.getFootprintForUser(currentUser.getId());
            if (labelCo2Total != null) {
                if (summary.reservationsCount() == 0) {
                    labelCo2Total.setText("No confirmed trips yet");
                    labelCo2Average.setText("");
                } else {
                    labelCo2Total.setText(String.format("%.0f kg CO₂e total", summary.totalKg()));
                    labelCo2Average.setText(String.format("≈ %.0f kg CO₂e / trip", summary.averagePerReservationKg()));
                }
            }
        } catch (Exception ignored) {}

        try {
            UserReputationSummary rep = reputationService.computeForUser(currentUser.getId());
            if (labelReputation != null) {
                labelReputation.setText(rep.levelLabel());
                labelReputation.setStyle(
                        "-fx-background-color: " + rep.levelColorHex() + ";" +
                        "-fx-text-fill: white;" +
                        "-fx-padding: 4 10;" +
                        "-fx-background-radius: 20;" +
                        "-fx-font-weight: bold;"
                );
            }
        } catch (Exception ignored) {}
    }

    @FXML
    void handleChangePhoto(ActionEvent event) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Select profile photo");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.webp"));
        Stage stage = (Stage) (fieldNom != null ? fieldNom.getScene().getWindow() : profilePhotoView.getScene().getWindow());
        File f = fc.showOpenDialog(stage);
        if (f != null) {
            selectedPhotoFile = f;
            profilePhotoView.setImage(new Image(f.toURI().toString(), true));
        }
    }

    private void loadProfilePhoto(String path) {
        if (profilePhotoView == null) return;
        if (path != null && !path.isEmpty()) {
            File f = new File(path);
            if (f.exists()) profilePhotoView.setImage(new Image(f.toURI().toString(), true));
            else setDefaultAvatar();
        } else setDefaultAvatar();
    }

    private void setDefaultAvatar() {
        try {
            java.net.URL url = getClass().getResource("/icons/usericon.png");
            if (url == null) url = getClass().getResource("/Backoffice/icons/activity_placeholder.png");
            if (url != null) profilePhotoView.setImage(new Image(url.toExternalForm(), true));
        } catch (Exception ignored) {}
    }

    private String copyPhotoToUploads(File imageFile) throws IOException {
        String folder = System.getProperty("user.home") + "/myapp/uploads/profile/";
        Files.createDirectories(Paths.get(folder));
        String ext = imageFile.getName().contains(".") ? imageFile.getName().substring(imageFile.getName().lastIndexOf('.')) : ".jpg";
        String fileName = "profile_" + currentUser.getId() + "_" + System.currentTimeMillis() + ext;
        Path dest = Paths.get(folder + fileName);
        Files.copy(imageFile.toPath(), dest, StandardCopyOption.REPLACE_EXISTING);
        return dest.toAbsolutePath().toString();
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
            Parent root = FXMLLoader.load(getClass().getResource("/Frontoffice/HomePage.fxml"));
            Stage stage = (Stage) fieldNom.getScene().getWindow();
            util.NavigationUtil.switchScene(stage, root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void navigateToLogin() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/Frontoffice/loginPage.fxml"));
            Stage stage = (Stage) fieldNom.getScene().getWindow();
            util.NavigationUtil.switchScene(stage, root);
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

