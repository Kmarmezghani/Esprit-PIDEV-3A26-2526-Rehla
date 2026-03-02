package services;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class CurrencyService {

    private static final String API_KEY = "919f7dbd64ff9f04947646e2";

    public double convert(double amount, String from, String to) {
        try {
            String urlStr = "https://v6.exchangerate-api.com/v6/"
                    + API_KEY + "/latest/" + from;

            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(conn.getInputStream())
            );

            StringBuilder response = new StringBuilder();
            String line;

            while ((line = in.readLine()) != null) {
                response.append(line);
            }
            in.close();

            JSONObject json = new JSONObject(response.toString());
            JSONObject rates = json.getJSONObject("conversion_rates");

            double rate = rates.getDouble(to);

            return amount * rate;

        } catch (Exception e) {
            e.printStackTrace();
            return amount;
        }
    }
}