package Controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import models.Preference;
import models.Reservation;
import models.Ticket;
import services.PreferenceService;
import services.TicketService;

import java.time.LocalDate;
import java.util.List;

public class BookingTypeController {

    private boolean addMode;
    private Reservation reservation;
    private LocalDate selectedDate;
    private int villeId;
    private TicketService ticketService = new TicketService();
    private PreferenceService preferenceService = new PreferenceService();
    private final int loggedUserId = 1;

    public void setVilleId(int villeId) {
        this.villeId = villeId;
    }

    public void setAddMode(boolean addMode) {
        this.addMode = addMode;
    }

    public void setReservation(Reservation reservation) {
        this.reservation = reservation;
    }

    public void setSelectedDate(LocalDate date) {
        this.selectedDate = date;
    }

    private void loadPage(ActionEvent event, String path) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(path));
            Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void loadTicketPage(ActionEvent event, String type) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Frontoffice/FrontTickets.fxml"));
            Parent root = loader.load();

            FrontTicketsController controller = loader.getController();

            controller.setTicketType(type);
            controller.setDestinationId(villeId);

            // 🔥 PASS MODE + DATA
            controller.setAddMode(addMode);
            controller.setReservation(reservation);
            controller.setSelectedDate(selectedDate);

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(root);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void goToFlights(ActionEvent e) {
        loadTicketPage(e, "flight");
    }

    public void goToHotels(ActionEvent e) {
        loadTicketPage(e, "hotel");
    }

    public void goToTransport(ActionEvent e) {
        loadTicketPage(e, "transport");
    }

    public void goToActivities(ActionEvent e) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Frontoffice/ActivitiesPage.fxml"));
            Parent root = loader.load();

            ActivitiesPageController controller = loader.getController();

            // ✅ Passer le contexte pour “add to existing reservation”
            controller.setAddMode(addMode);
            controller.setReservation(reservation);
            controller.setSelectedDate(selectedDate);

            Stage stage = (Stage) ((Node) e.getSource()).getScene().getWindow();
            stage.getScene().setRoot(root);

            root.applyCss();
            root.layout();

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @FXML
    private void handleBack(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/Frontoffice/HomePage.fxml"));

            if (loader.getLocation() == null) {
                throw new RuntimeException("FXML file not found!");
            }

            Parent root = loader.load();

            Stage stage = (Stage) ((Node) event.getSource())
                    .getScene().getWindow();

            stage.getScene().setRoot(root);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @FXML
    private void handleGeneratePack(ActionEvent event) {

        Preference pref = preferenceService.getByPersonneId(loggedUserId);

        if (pref == null) {
            System.out.println("No preferences found");
            return;
        }

        List<Ticket> all = ticketService.getAvailableTickets();

        List<Ticket> flights = all.stream()
                .filter(t -> t.getType().equalsIgnoreCase("flight"))
                .toList();

        List<Ticket> hotels = all.stream()
                .filter(t -> t.getType().equalsIgnoreCase("hotel"))
                .toList();

        List<Ticket> transports = all.stream()
                .filter(t -> t.getType().equalsIgnoreCase("transport"))
                .toList();

        Ticket bestFlight = null;
        Ticket bestHotel = null;
        Ticket bestTransport = null;

        double bestTotal = 0;
        double budgetMax = pref.getBudgetMax();

        for (Ticket f : flights) {
            for (Ticket h : hotels) {
                for (Ticket tr : transports) {

                    double total = f.getPrix() + h.getPrix() + tr.getPrix();

                    if (total <= budgetMax-300 && total > bestTotal) {
                        bestTotal = total;
                        bestFlight = f;
                        bestHotel = h;
                        bestTransport = tr;
                    }
                }
            }
        }

        if (bestFlight == null) {
            System.out.println("No valid pack found within budget.");
            return;
        }

        try {

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Frontoffice/CreateReservation.fxml"));
            Parent root = loader.load();

            CreateReservationController controller = loader.getController();

            List<Ticket> selectedPack = List.of(bestFlight, bestHotel, bestTransport);
            controller.setSelectedTickets(selectedPack);

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));

            System.out.println("🎁 Smart Pack Generated! Total = " + bestTotal);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}