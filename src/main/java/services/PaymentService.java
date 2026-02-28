package services;

import com.stripe.Stripe;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;

import java.awt.Desktop;
import java.net.URI;

public class PaymentService {

    private static final String SECRET_KEY = "sk_test_51T52MfL0u11MDUrGfmHxRTlFeQldRdY8XJfZiv7KCGdQK7ARS0gin2wkoawJqAgIom4e5EEqMcuUSxyLBm1DRefY0061UIYdXG"; // 🔥 replace

    public void payReservation(int reservationId, double amountTND) {

        try {
            Stripe.apiKey = SECRET_KEY;

            // 🔹 Use your CurrencyService
            CurrencyService currencyService = new CurrencyService();
            double amountUSD = currencyService.convert(amountTND, "TND", "USD");

            // Stripe needs cents
            long amountInCents = Math.round(amountUSD * 100);

            System.out.println("Amount TND: " + amountTND);
            System.out.println("Converted USD: " + amountUSD);
            System.out.println("Stripe cents: " + amountInCents);

            SessionCreateParams params =
                    SessionCreateParams.builder()
                            .setMode(SessionCreateParams.Mode.PAYMENT)
                            .setSuccessUrl("https://example.com/success")
                            .setCancelUrl("https://example.com/cancel")
                            .addLineItem(
                                    SessionCreateParams.LineItem.builder()
                                            .setQuantity(1L)
                                            .setPriceData(
                                                    SessionCreateParams.LineItem.PriceData.builder()
                                                            .setCurrency("usd")
                                                            .setUnitAmount(amountInCents)
                                                            .setProductData(
                                                                    SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                            .setName("Reservation #" + reservationId)
                                                                            .build()
                                                            )
                                                            .build()
                                            )
                                            .build()
                            )
                            .build();

            Session session = Session.create(params);

            Desktop.getDesktop().browse(new URI(session.getUrl()));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}