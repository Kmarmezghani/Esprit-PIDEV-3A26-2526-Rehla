package Controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import models.Attraction;
import models.Pays;
import models.Ville;
import services.AttractionService;
import services.PaysService;
import services.VilleService;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class ClientDashboardController implements Initializable {

    @FXML
    private TextField searchField;

    @FXML
    private ComboBox<Pays> filterPaysComboBox;

    @FXML
    private FlowPane paysFlowPane;

    @FXML
    private FlowPane villesFlowPane;

    @FXML
    private FlowPane attractionsFlowPane;

    @FXML
    private Label villesLabel;

    @FXML
    private Label attractionsLabel;

    private final PaysService paysService = new PaysService();
    private final VilleService villeService = new VilleService();
    private final AttractionService attractionService = new AttractionService();

    private List<Pays> allPays;
    private List<Ville> allVilles;
    private List<Attraction> allAttractions;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        loadAllData();
        setupFilterComboBox();
        displayAllContent();
    }

    private void loadAllData() {
        allPays = paysService.getAll();
        allVilles = villeService.getAll();
        allAttractions = attractionService.getAll();
    }

    private void setupFilterComboBox() {
        ObservableList<Pays> paysList = FXCollections.observableArrayList(allPays);
        filterPaysComboBox.setItems(paysList);

        // Display country name in ComboBox
        filterPaysComboBox.setCellFactory(param -> new ListCell<Pays>() {
            @Override
            protected void updateItem(Pays pays, boolean empty) {
                super.updateItem(pays, empty);
                setText(empty || pays == null ? null : pays.getNom());
            }
        });

        filterPaysComboBox.setButtonCell(new ListCell<Pays>() {
            @Override
            protected void updateItem(Pays pays, boolean empty) {
                super.updateItem(pays, empty);
                setText(empty || pays == null ? "All Countries" : pays.getNom());
            }
        });
    }

    private void displayAllContent() {
        displayPays(allPays);
        displayVilles(allVilles);
        displayAttractions(allAttractions);
    }

    private void displayPays(List<Pays> paysList) {
        paysFlowPane.getChildren().clear();

        for (Pays pays : paysList) {
            VBox card = createPaysCard(pays);
            paysFlowPane.getChildren().add(card);
        }
    }

    private VBox createPaysCard(Pays pays) {
        VBox card = new VBox(10);
        card.setPrefSize(250, 150);
        card.setStyle("-fx-background-color: white; " +
                "-fx-border-color: #ddd; " +
                "-fx-border-width: 1; " +
                "-fx-border-radius: 10; " +
                "-fx-background-radius: 10; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 5, 0, 0, 2);");
        card.setAlignment(Pos.TOP_LEFT);
        card.setPadding(new Insets(15));

        Label nameLabel = new Label(pays.getNom());
        nameLabel.setFont(Font.font("System", FontWeight.BOLD, 18));
        nameLabel.setTextFill(Color.web("#223f91"));

        Label continentLabel = new Label("📍 " + pays.getContinent());
        continentLabel.setFont(Font.font("System", 13));
        continentLabel.setTextFill(Color.GRAY);

        Label descLabel = new Label(pays.getDescription());
        descLabel.setWrapText(true);
        descLabel.setFont(Font.font("System", 12));
        descLabel.setMaxWidth(220);

        Button viewBtn = new Button("View Cities");
        viewBtn.setStyle("-fx-background-color: #223f91; -fx-text-fill: white; -fx-cursor: hand;");
        viewBtn.setOnAction(e -> filterByPays(pays));

        card.getChildren().addAll(nameLabel, continentLabel, descLabel, viewBtn);

        // Hover effect
        card.setOnMouseEntered(e -> card.setStyle(card.getStyle() + "-fx-background-color: #f0f8ff;"));
        card.setOnMouseExited(e -> card.setStyle(card.getStyle().replace("-fx-background-color: #f0f8ff;", "-fx-background-color: white;")));

        return card;
    }

    private void displayVilles(List<Ville> villesList) {
        villesFlowPane.getChildren().clear();

        for (Ville ville : villesList) {
            VBox card = createVilleCard(ville);
            villesFlowPane.getChildren().add(card);
        }
    }

    private VBox createVilleCard(Ville ville) {
        VBox card = new VBox(8);
        card.setPrefSize(200, 180);
        card.setStyle("-fx-background-color: white; " +
                "-fx-border-color: #ddd; " +
                "-fx-border-width: 1; " +
                "-fx-border-radius: 10; " +
                "-fx-background-radius: 10; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 5, 0, 0, 2);");
        card.setAlignment(Pos.TOP_LEFT);
        card.setPadding(new Insets(15));

        Label nameLabel = new Label(ville.getNom());
        nameLabel.setFont(Font.font("System", FontWeight.BOLD, 16));
        nameLabel.setTextFill(Color.web("#223f91"));

        // Get country name
        String paysNom = allPays.stream()
                .filter(p -> p.getId() == ville.getPaysId())
                .map(Pays::getNom)
                .findFirst()
                .orElse("Unknown");

        Label paysLabel = new Label("📍 " + paysNom);
        paysLabel.setFont(Font.font("System", 12));
        paysLabel.setTextFill(Color.GRAY);

        Label regionLabel = new Label("Region: " + ville.getRegion());
        regionLabel.setFont(Font.font("System", 11));

        Label typeLabel = new Label("🏖️ " + ville.getTypeTourisme());
        typeLabel.setFont(Font.font("System", 11));

        Label popularityLabel = new Label("⭐ Popularity: " + ville.getPopularite() + "/100");
        popularityLabel.setFont(Font.font("System", FontWeight.BOLD, 11));
        popularityLabel.setTextFill(Color.web("#3A5BC7"));

        Button viewBtn = new Button("View Attractions");
        viewBtn.setStyle("-fx-background-color: #3A5BC7; -fx-text-fill: white; -fx-cursor: hand; -fx-font-size: 10;");
        viewBtn.setOnAction(e -> filterByVille(ville));

        card.getChildren().addAll(nameLabel, paysLabel, regionLabel, typeLabel, popularityLabel, viewBtn);

        // Hover effect
        card.setOnMouseEntered(e -> card.setStyle(card.getStyle() + "-fx-background-color: #fffacd;"));
        card.setOnMouseExited(e -> card.setStyle(card.getStyle().replace("-fx-background-color: #fffacd;", "-fx-background-color: white;")));

        return card;
    }

    private void displayAttractions(List<Attraction> attractionsList) {
        attractionsFlowPane.getChildren().clear();

        for (Attraction attraction : attractionsList) {
            VBox card = createAttractionCard(attraction);
            attractionsFlowPane.getChildren().add(card);
        }
    }

    private VBox createAttractionCard(Attraction attraction) {
        VBox card = new VBox(8);
        card.setPrefSize(220, 200);
        card.setStyle("-fx-background-color: white; " +
                "-fx-border-color: #ddd; " +
                "-fx-border-width: 1; " +
                "-fx-border-radius: 10; " +
                "-fx-background-radius: 10; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 5, 0, 0, 2);");
        card.setAlignment(Pos.TOP_LEFT);
        card.setPadding(new Insets(15));

        Label nameLabel = new Label(attraction.getNom());
        nameLabel.setFont(Font.font("System", FontWeight.BOLD, 15));
        nameLabel.setTextFill(Color.web("#223f91"));
        nameLabel.setWrapText(true);

        // Get ville name
        String villeNom = allVilles.stream()
                .filter(v -> v.getId() == attraction.getVilleId())
                .map(Ville::getNom)
                .findFirst()
                .orElse("Unknown");

        Label villeLabel = new Label("📍 " + villeNom);
        villeLabel.setFont(Font.font("System", 12));
        villeLabel.setTextFill(Color.GRAY);

        Label typeLabel = new Label("Type: " + attraction.getType());
        typeLabel.setFont(Font.font("System", 11));

        Label descLabel = new Label(attraction.getDescription());
        descLabel.setWrapText(true);
        descLabel.setFont(Font.font("System", 10));
        descLabel.setMaxWidth(190);
        descLabel.setMaxHeight(40);

        Label priceLabel = new Label("💰 " + attraction.getPrix() + " TND");
        priceLabel.setFont(Font.font("System", FontWeight.BOLD, 12));
        priceLabel.setTextFill(Color.web("#1976D2"));

        Label hoursLabel = new Label("🕒 " + attraction.getHoraires());
        hoursLabel.setFont(Font.font("System", 10));

        card.getChildren().addAll(nameLabel, villeLabel, typeLabel, descLabel, priceLabel, hoursLabel);

        // Hover effect
        card.setOnMouseEntered(e -> card.setStyle(card.getStyle() + "-fx-background-color: #e8f5e9;"));
        card.setOnMouseExited(e -> card.setStyle(card.getStyle().replace("-fx-background-color: #e8f5e9;", "-fx-background-color: white;")));

        return card;
    }

    private void filterByPays(Pays pays) {
        List<Ville> filteredVilles = allVilles.stream()
                .filter(v -> v.getPaysId() == pays.getId())
                .collect(Collectors.toList());

        villesLabel.setText("Cities in " + pays.getNom());
        displayVilles(filteredVilles);

        // Show attractions from cities in this country
        List<Integer> villeIds = filteredVilles.stream()
                .map(Ville::getId)
                .collect(Collectors.toList());

        List<Attraction> filteredAttractions = allAttractions.stream()
                .filter(a -> villeIds.contains(a.getVilleId()))
                .collect(Collectors.toList());

        attractionsLabel.setText("Attractions in " + pays.getNom());
        displayAttractions(filteredAttractions);
    }

    private void filterByVille(Ville ville) {
        List<Attraction> filteredAttractions = allAttractions.stream()
                .filter(a -> a.getVilleId() == ville.getId())
                .collect(Collectors.toList());

        attractionsLabel.setText("Attractions in " + ville.getNom());
        displayAttractions(filteredAttractions);

        // Scroll to attractions section
        attractionsFlowPane.requestFocus();
    }

    @FXML
    void handleSearch(ActionEvent event) {
        String searchText = searchField.getText().toLowerCase().trim();
        Pays selectedPays = filterPaysComboBox.getValue();

        List<Ville> filteredVilles = allVilles.stream()
                .filter(v -> searchText.isEmpty() || v.getNom().toLowerCase().contains(searchText) ||
                        v.getRegion().toLowerCase().contains(searchText) ||
                        v.getTypeTourisme().toLowerCase().contains(searchText))
                .filter(v -> selectedPays == null || v.getPaysId() == selectedPays.getId())
                .collect(Collectors.toList());

        List<Attraction> filteredAttractions = allAttractions.stream()
                .filter(a -> searchText.isEmpty() || a.getNom().toLowerCase().contains(searchText) ||
                        a.getType().toLowerCase().contains(searchText) ||
                        a.getDescription().toLowerCase().contains(searchText))
                .collect(Collectors.toList());

        // If country filter is active, only show attractions in that country
        if (selectedPays != null) {
            List<Integer> villeIds = filteredVilles.stream()
                    .map(Ville::getId)
                    .collect(Collectors.toList());

            filteredAttractions = filteredAttractions.stream()
                    .filter(a -> villeIds.contains(a.getVilleId()))
                    .collect(Collectors.toList());

            villesLabel.setText("Cities in " + selectedPays.getNom() + 
                    (searchText.isEmpty() ? "" : " matching \"" + searchText + "\""));
            attractionsLabel.setText("Attractions in " + selectedPays.getNom() + 
                    (searchText.isEmpty() ? "" : " matching \"" + searchText + "\""));
        } else {
            villesLabel.setText("Cities" + (searchText.isEmpty() ? "" : " matching \"" + searchText + "\""));
            attractionsLabel.setText("Attractions" + (searchText.isEmpty() ? "" : " matching \"" + searchText + "\""));
        }

        displayVilles(filteredVilles);
        displayAttractions(filteredAttractions);
    }

    @FXML
    void handleReset(ActionEvent event) {
        searchField.clear();
        filterPaysComboBox.setValue(null);
        villesLabel.setText("Popular Cities");
        attractionsLabel.setText("Featured Attractions");
        displayAllContent();
    }
}
