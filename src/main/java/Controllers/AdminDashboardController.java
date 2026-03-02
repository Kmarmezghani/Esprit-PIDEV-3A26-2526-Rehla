package Controllers;

import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.control.Label;
import services.AnalyticsReservationService;

import java.util.Map;

public class AdminDashboardController {

    @FXML private Label lblTotalRevenue;
    @FXML private Label lblMostDestination;
    @FXML private Label lblConversionRate;
    @FXML private BarChart<String, Number> revenueChart;

    private AnalyticsReservationService analytics = new AnalyticsReservationService();

    @FXML
    public void initialize() {

        // 1️⃣ Total Revenue
        double total = analytics.getTotalRevenue();
        lblTotalRevenue.setText("💰 Total Revenue: " + total + " TND");

        // 2️⃣ Most Reserved Destination
        lblMostDestination.setText("🌍 Most Reserved: "
                + analytics.getMostReservedDestination());

        // 3️⃣ Conversion Rate
        double conversion = analytics.getConversionRate();
        lblConversionRate.setText("🔄 Conversion Rate: "
                + String.format("%.2f", conversion) + "%");

        // 4️⃣ Revenue Per Month Chart
        loadChart();
    }

    private void loadChart() {

        Map<Integer, Double> data = analytics.getRevenuePerMonth();

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Monthly Revenue");

        for (Map.Entry<Integer, Double> entry : data.entrySet()) {
            series.getData().add(
                    new XYChart.Data<>(
                            "Month " + entry.getKey(),
                            entry.getValue()
                    )
            );
        }

        revenueChart.getData().add(series);
    }
}