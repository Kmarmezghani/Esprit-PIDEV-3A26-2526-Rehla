package Controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import models.Pays;
import services.PaysService;
import services.VilleService;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class CountryBrowseController implements Initializable {

    @FXML
    private TextField searchField;

    @FXML
    private FlowPane countriesFlowPane;

    @FXML
    private Label titleLabel;

    @FXML
    private Label subtitleLabel;

    @FXML
    private Label noResultsLabel;

    private final PaysService paysService = new PaysService();
    private final VilleService villeService = new VilleService();
    private List<Pays> allCountries;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        loadAllCountries();
    }

    public void setSearchText(String searchText) {
        searchField.setText(searchText);
        handleSearch(null);
    }

    private void loadAllCountries() {
        allCountries = paysService.getAll();
        displayCountries(allCountries);
    }

    private void displayCountries(List<Pays> countries) {
        countriesFlowPane.getChildren().clear();
        noResultsLabel.setVisible(false);

        if (countries.isEmpty()) {
            noResultsLabel.setVisible(true);
            return;
        }

        for (Pays pays : countries) {
            VBox countryCard = createCountryCard(pays);
            countriesFlowPane.getChildren().add(countryCard);
        }
    }

    private VBox createCountryCard(Pays pays) {
        VBox card = new VBox(15);
        card.setPrefSize(280, 200);
        card.setStyle("-fx-background-color: white; " +
                "-fx-border-radius: 15; " +
                "-fx-background-radius: 15; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.15), 10, 0, 0, 3);");
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(25));

        // Country Flag Emoji
        Label flagLabel = new Label(getCountryEmoji(pays.getNom()));
        flagLabel.setFont(Font.font("System", 60));

        Label nameLabel = new Label(pays.getNom());
        nameLabel.setFont(Font.font("System", FontWeight.BOLD, 24));
        nameLabel.setTextFill(Color.web("#223f91"));

        Label continentLabel = new Label("Explore");
        continentLabel.setFont(Font.font("System", 14));
        continentLabel.setTextFill(Color.GRAY);

        // Count cities
        long cityCount = villeService.getAll().stream()
                .filter(v -> v.getPaysId() == pays.getId())
                .count();
        
        Label cityCountLabel = new Label(cityCount + " cities");
        cityCountLabel.setFont(Font.font("System", FontWeight.BOLD, 12));
        cityCountLabel.setTextFill(Color.web("#3A5BC7"));

        card.getChildren().addAll(flagLabel, nameLabel, continentLabel, cityCountLabel);

        // Hover effect
        card.setOnMouseEntered(e -> {
            card.setStyle("-fx-background-color: linear-gradient(to bottom right, #e8eaf6, white); " +
                    "-fx-border-radius: 20; " +
                    "-fx-background-radius: 20; " +
                    "-fx-effect: dropshadow(gaussian, rgba(103,58,183,0.3), 25, 0, 0, 8); " +
                    "-fx-cursor: hand;");
            card.setScaleX(1.03);
            card.setScaleY(1.03);
        });
        
        card.setOnMouseExited(e -> {
            card.setStyle("-fx-background-color: white; " +
                    "-fx-border-radius: 20; " +
                    "-fx-background-radius: 20; " +
                    "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 20, 0, 0, 5);");
            card.setScaleX(1.0);
            card.setScaleY(1.0);
        });

        // Navigate to country detail
        card.setOnMouseClicked(e -> navigateToCountry(pays));

        return card;
    }

    private String getCountryEmoji(String countryName) {
        switch (countryName.toLowerCase()) {
            case "france": return "FR";
            case "tunisia": case "tunisie": return "TN";
            case "japan": case "japon": return "JP";
            case "italy": case "italie": return "IT";
            case "spain": case "espagne": return "ES";
            case "usa": case "united states": case "etats-unis": return "US";
            case "uk": case "united kingdom": case "royaume-uni": return "GB";
            case "germany": case "allemagne": return "DE";
            case "morocco": case "maroc": return "MA";
            case "egypt": case "egypte": return "EG";
            default: return "World";
        }
    }

    @FXML
    void handleSearch(ActionEvent event) {
        String searchText = searchField.getText().toLowerCase().trim();
        
        if (searchText.isEmpty()) {
            titleLabel.setText("All Destinations");
            subtitleLabel.setText("Explore countries around the world");
            displayCountries(allCountries);
            return;
        }

        List<Pays> filtered = allCountries.stream()
                .filter(p -> p.getNom().toLowerCase().contains(searchText) ||
                             p.getDescription().toLowerCase().contains(searchText))
                .collect(Collectors.toList());

        titleLabel.setText("Search Results");
        subtitleLabel.setText("Found " + filtered.size() + " " + 
                (filtered.size() == 1 ? "country" : "countries") + " matching \"" + searchText + "\"");
        
        displayCountries(filtered);
    }

    @FXML
    void handleReset(ActionEvent event) {
        searchField.clear();
        titleLabel.setText("All Destinations");
        subtitleLabel.setText("Explore countries around the world");
        displayCountries(allCountries);
    }

    @FXML
    void goToHome(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Frontoffice/HomePage.fxml"));
            Parent root = loader.load();
            
            Stage stage = (Stage) countriesFlowPane.getScene().getWindow();
            if (stage.getScene() == null) {
                stage.setScene(new Scene(root));
            } else {
                stage.getScene().setRoot(root);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void navigateToCountry(Pays pays) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Frontoffice/CountryDetailPage.fxml"));
            Parent root = loader.load();
            
            CountryDetailController controller = loader.getController();
            controller.setPays(pays);
            
            Stage stage = (Stage) countriesFlowPane.getScene().getWindow();
            if (stage.getScene() == null) {
                stage.setScene(new Scene(root));
            } else {
                stage.getScene().setRoot(root);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Window Controls
    @FXML
    void closewindow(ActionEvent event) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.close();
    }

    @FXML
    void minwindow(ActionEvent event) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setIconified(true);
    }

    @FXML
    void maxwindow(ActionEvent event) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setMaximized(!stage.isMaximized());
    }

    // Profile Menu Actions
    @FXML
    void goToMyProfile(ActionEvent event) {
        System.out.println("My Profile - to be implemented");
        showInfo("Profile", "My Profile page will show user information,\nprofile picture, and settings.");
    }

    @FXML
    void goToMyPosts(ActionEvent event) {
        System.out.println("My Posts - to be implemented");
        showInfo("My Posts", "This will show all blog posts\ncreated by the current user.");
    }

    @FXML
    void goToMyReservations(ActionEvent event) {
        System.out.println("My Reservations - to be implemented");
        showInfo("My Reservations", "This will show all reservations\nmade by the current user.");
    }

    @FXML
    void handleLogout(ActionEvent event) {
        System.out.println("Logging out...");
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Logout");
        alert.setHeaderText("Are you sure you want to logout?");
        alert.setContentText("You will be returned to the login screen.");
        
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                System.out.println("User logged out");
                showInfo("Logged Out", "You have been successfully logged out!");
            }
        });
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
