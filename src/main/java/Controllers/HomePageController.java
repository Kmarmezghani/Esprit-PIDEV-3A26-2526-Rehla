package Controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.stage.Stage;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * HOMEPAGE CONTROLLER - FRONTOFFICE TEMPLATE
 *
 * This controller manages the main landing page of the application.
 *
 * IMPORTANT FOR TEAMMATES:
 * - Destination module code is COMMENTED OUT below
 * - If you have the Destination module (Ville, Pays models), UNCOMMENT those sections
 * - If you DON'T have Destination module, add your own content in initialize() method
 * - The FlowPane containers (countriesFlowPane, popularCitiesFlowPane) are available for any module to use
 */
public class HomePageController implements Initializable {


    @FXML
    private FlowPane countriesFlowPane;  // Empty for now - Destination team will populate

    @FXML
    private FlowPane popularCitiesFlowPane;

    /* =====================================================
       DESTINATION MODULE IMPORTS - COMMENTED OUT

       Uncomment if you have Destination module:
       import javafx.fxml.FXMLLoader;
       import javafx.geometry.Insets;
       import javafx.geometry.Pos;
       import javafx.scene.Parent;
       import javafx.scene.Scene;
       import javafx.scene.control.Button;
       import javafx.scene.control.Label;
       import javafx.scene.layout.VBox;
       import javafx.scene.paint.Color;
       import javafx.scene.text.Font;
       import javafx.scene.text.FontWeight;
       import javafx.stage.Modality;
       import models.Pays;
       import models.Ville;
       import services.PaysService;
       import services.VilleService;
       import java.io.IOException;
       import java.util.Comparator;
       import java.util.List;
       import java.util.stream.Collectors;
       ===================================================== */

    /* =====================================================
       DESTINATION MODULE SERVICES - COMMENTED OUT

       Uncomment if you have Destination module:
       private final PaysService paysService = new PaysService();
       private final VilleService villeService = new VilleService();
       ===================================================== */

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        /* =====================================================
           DESTINATION MODULE: Uncomment to load countries and cities

           loadCountries();
           loadPopularCities();
           ===================================================== */

        // TODO: Add your own module content here
        // Example for Reservation module:
        // loadRecentReservations();

        // Example for Posts module:
        // loadRecentPosts();
    }

    /* =====================================================
       DESTINATION MODULE METHODS - ALL COMMENTED OUT

       These methods load and display countries and cities.
       Uncomment if you have the Destination module.
       ===================================================== */

    /*
    private void loadCountries() {
        List<Pays> paysList = paysService.getAll();

        for (Pays pays : paysList) {
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

        Label flagLabel = new Label(getCountryEmoji(pays.getNom()));
        flagLabel.setFont(Font.font("System", 60));

        Label nameLabel = new Label(pays.getNom());
        nameLabel.setFont(Font.font("System", FontWeight.BOLD, 24));
        nameLabel.setTextFill(Color.web("#223f91"));

        Label continentLabel = new Label(pays.getContinent());
        continentLabel.setFont(Font.font("System", 14));
        continentLabel.setTextFill(Color.GRAY);

        long cityCount = villeService.getAll().stream()
                .filter(v -> v.getPaysId() == pays.getId())
                .count();

        Label cityCountLabel = new Label(cityCount + " cities to explore");
        cityCountLabel.setFont(Font.font("System", FontWeight.BOLD, 12));
        cityCountLabel.setTextFill(Color.web("#3A5BC7"));

        card.getChildren().addAll(flagLabel, nameLabel, continentLabel, cityCountLabel);

        card.setOnMouseEntered(e -> {
            card.setStyle(card.getStyle() + "-fx-background-color: #f0f8ff; -fx-cursor: hand;");
            card.setScaleX(1.05);
            card.setScaleY(1.05);
        });

        card.setOnMouseExited(e -> {
            card.setStyle(card.getStyle().replace("-fx-background-color: #f0f8ff;", "-fx-background-color: white;"));
            card.setScaleX(1.0);
            card.setScaleY(1.0);
        });

        card.setOnMouseClicked(e -> navigateToCountry(pays));

        return card;
    }

    private void loadPopularCities() {
        List<Ville> villes = villeService.getAll();

        List<Ville> topCities = villes.stream()
                .sorted(Comparator.comparingInt(Ville::getPopularite).reversed())
                .limit(6)
                .collect(Collectors.toList());

        for (Ville ville : topCities) {
            VBox cityCard = createCityCard(ville);
            popularCitiesFlowPane.getChildren().add(cityCard);
        }
    }

    private VBox createCityCard(Ville ville) {
        VBox card = new VBox(10);
        card.setPrefSize(220, 180);
        card.setStyle("-fx-background-color: white; " +
                "-fx-border-radius: 12; " +
                "-fx-background-radius: 12; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 8, 0, 0, 2);");
        card.setAlignment(Pos.TOP_LEFT);
        card.setPadding(new Insets(20));

        Label nameLabel = new Label(ville.getNom());
        nameLabel.setFont(Font.font("System", FontWeight.BOLD, 20));
        nameLabel.setTextFill(Color.web("#223f91"));

        String paysNom = paysService.getAll().stream()
                .filter(p -> p.getId() == ville.getPaysId())
                .map(Pays::getNom)
                .findFirst()
                .orElse("Unknown");

        Label countryLabel = new Label("📍 " + paysNom);
        countryLabel.setFont(Font.font("System", 13));
        countryLabel.setTextFill(Color.GRAY);

        Label typeLabel = new Label(ville.getTypeTourisme());
        typeLabel.setFont(Font.font("System", 12));
        typeLabel.setStyle("-fx-background-color: #e3f2fd; -fx-padding: 5 10; -fx-background-radius: 10;");

        Label popularityLabel = new Label("⭐ " + ville.getPopularite() + "/100");
        popularityLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        popularityLabel.setTextFill(Color.web("#3A5BC7"));

        Button bookButton = new Button("Book Now");
        bookButton.setStyle("-fx-background-color: #3A5BC7; " +
                "-fx-text-fill: white; " +
                "-fx-font-weight: bold; " +
                "-fx-font-size: 13; " +
                "-fx-padding: 10 25; " +
                "-fx-background-radius: 8; " +
                "-fx-cursor: hand; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(58,91,199,0.3), 8, 0, 0, 2);");

        bookButton.setOnMouseEntered(e -> {
            bookButton.setStyle(bookButton.getStyle() + "-fx-background-color: #2d4a9e;");
        });

        bookButton.setOnMouseExited(e -> {
            bookButton.setStyle(bookButton.getStyle().replace("-fx-background-color: #2d4a9e;", "-fx-background-color: #3A5BC7;"));
        });

        bookButton.setOnAction(e -> {
            e.consume();
            handleBookCity(ville);
        });

        card.getChildren().addAll(nameLabel, countryLabel, typeLabel, popularityLabel, bookButton);

        card.setOnMouseEntered(e -> {
            card.setStyle(card.getStyle() + "-fx-background-color: #fffacd; -fx-cursor: hand;");
        });

        card.setOnMouseExited(e -> {
            card.setStyle(card.getStyle().replace("-fx-background-color: #fffacd;", "-fx-background-color: white;"));
        });

        card.setOnMouseClicked(e -> {
            if (e.getTarget() != bookButton) {
                navigateToCity(ville);
            }
        });

        return card;
    }

    private String getCountryEmoji(String countryName) {
        switch (countryName.toLowerCase()) {
            case "france": return "🇫🇷";
            case "tunisia": case "tunisie": return "🇹🇳";
            case "japan": case "japon": return "🇯🇵";
            case "italy": case "italie": return "🇮🇹";
            case "spain": case "espagne": return "🇪🇸";
            case "usa": case "united states": case "états-unis": return "🇺🇸";
            case "uk": case "united kingdom": case "royaume-uni": return "🇬🇧";
            case "germany": case "allemagne": return "🇩🇪";
            case "morocco": case "maroc": return "🇲🇦";
            case "egypt": case "égypte": return "🇪🇬";
            default: return "🌍";
        }
    }

    private void navigateToCountry(Pays pays) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/CountryDetailPage.fxml"));
            Parent root = loader.load();

            CountryDetailController controller = loader.getController();
            controller.setPays(pays);

            Stage stage = (Stage) countriesFlowPane.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void navigateToCity(Ville ville) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/CityDetailPage.fxml"));
            Parent root = loader.load();

            CityDetailController controller = loader.getController();
            controller.setVille(ville);

            Stage stage = (Stage) popularCitiesFlowPane.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleBookCity(Ville ville) {
        // RESERVATION TEAM: Uncomment this code when ReservationForm.fxml is ready
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ReservationForm.fxml"));
            Parent root = loader.load();

            ReservationController controller = loader.getController();
            controller.setVille(ville);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Book " + ville.getNom());
            stage.setScene(new Scene(root));
            stage.showAndWait();

        } catch (Exception e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Coming Soon");
            alert.setHeaderText("Reservation Feature");
            alert.setContentText("The reservation team will implement this soon!\n\n" +
                                "City: " + ville.getNom());
            alert.showAndWait();
        }
    }
    */

    /* =====================================================
       END OF DESTINATION MODULE CODE
       ===================================================== */

//    @FXML
//    void handleSearch(ActionEvent event) {
//        String searchText = searchField.getText().trim();
//        if (!searchText.isEmpty()) {
//            System.out.println("Search: " + searchText);
//            // TODO: Implement search for your module
//            showInfo("Search", "Search feature - implement for your module");
//        }
//    }

    @FXML
    void goToHome(ActionEvent event) {
        System.out.println("Already on home");
    }

    @FXML
    void goToDestinations(ActionEvent event) {
        System.out.println("Destinations - to be implemented");
        showInfo("Destinations", "Implement navigation to your module page");
    }

    @FXML
    void goToPosts(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/blogAllPosts.fxml"));
            Parent root = loader.load();

            Scene scene = new Scene(root);
            scene.getStylesheets().add(
                    getClass().getResource("/css/blog_styles.css").toExternalForm()
            );

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(scene);
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
            showInfo("Posts", "Erreur lors du chargement de blogAllPosts.");
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



    @FXML
    private void goToProfile(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/blogProfileView.fxml"));
            Parent root = loader.load();

            Scene scene = new Scene(root);

            scene.getStylesheets().add(
                    getClass().getResource("/css/blog_styles.css").toExternalForm()
            );

            Stage stage;

            if (event.getSource() instanceof javafx.scene.control.MenuItem menuItem) {
                stage = (Stage) menuItem.getParentPopup().getOwnerWindow();
            } else {
                stage = (Stage) ((Node) event.getSource())
                        .getScene()
                        .getWindow();
            }

            stage.setScene(scene);
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }




//    @FXML
//    void goToMyReservations(ActionEvent event) {
//        try {
//            FXMLLoader loader = new FXMLLoader(getClass().getResource("/MyReservation.fxml"));
//            Parent root = loader.load();
//
//            Stage stage = (Stage) searchField.getScene().getWindow();
//            stage.setScene(new Scene(root));
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }

//    @FXML
//    void handleLogout(ActionEvent event) {
//        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
//        alert.setTitle("Logout");
//        alert.setHeaderText("Are you sure you want to logout?");
//        alert.setContentText("You will be returned to the login screen.");
//
//        alert.showAndWait().ifPresent(response -> {
//            if (response == ButtonType.OK) {
//                util.Session.clear();
//                try {
//                    Parent root = FXMLLoader.load(getClass().getResource("/loginPage.fxml"));
//                    Stage stage = (Stage) searchField.getScene().getWindow();
//                    stage.setScene(new Scene(root));
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//            }
//        });
//    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /* ========================================
       TEMPORARY: Module Testing Methods
       DELETE THESE AFTER INTEGRATION!
       ======================================== */

    @FXML
    void frontofficeReservation(ActionEvent event) {
        /* RESERVATION TEAM: Uncomment this code when ReservationForm.fxml is ready
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ReservationForm.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Ajouter Réservation");
            stage.setScene(new Scene(root));
            stage.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
        }
        */

        showInfo("Module Réservation",
                "Équipe Réservation:\n\n" +
                        "1. Créer ReservationForm.fxml\n" +
                        "2. Créer ReservationController.java\n" +
                        "3. Décommenter le code dans frontofficeReservation()\n" +
                        "4. Cliquer ce bouton pour tester!\n\n" +
                        "Plus tard: Sera déclenché par le bouton 'Book Now' sur les villes");
    }

    /*@FXML
    private void handleAddReservation(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ajout.fxml"));
            Parent root = loader.load();

            AjouterReservationController controller = loader.getController();

            // Création d'un Stage modal
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Add Reservation");
            stage.setScene(new Scene(root));
            stage.showAndWait();

            // Après fermeture du popup, naviguer vers MyReservations
            FXMLLoader myResLoader = new FXMLLoader(getClass().getResource("/MyReservation.fxml"));
            Parent myResRoot = myResLoader.load();

            Stage currentStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            currentStage.setScene(new Scene(myResRoot));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }*/

    @FXML
    void frontofficeActivite(ActionEvent event) {
        /* ACTIVITIES TEAM: Uncomment this code when ActivityForm.fxml is ready
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ActivityForm.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Ajouter Activité");
            stage.setScene(new Scene(root));
            stage.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
        }
        */

        showInfo("Module Activités",
                "Équipe Activités:\n\n" +
                        "1. Créer ActivityForm.fxml\n" +
                        "2. Créer ActivityController.java\n" +
                        "3. Décommenter le code dans frontofficeActivite()\n" +
                        "4. Cliquer ce bouton pour tester!\n\n" +
                        "Plus tard: Montrera les activités pour chaque ville");
    }

}