package services;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class WeatherService {

    private static final String API_KEY = "2e4278de64b54abb812142759262102"; // 🔑 Replace with your key
    private static final String BASE_URL = "http://api.weatherapi.com/v1/forecast.json";

    /**
     * Fetch weather forecast for a city and target date (string version)
     */
    public String getWeatherForecast(String city, LocalDate targetDate) {
        try {
            JSONObject forecastJson = getFullForecastForDate(city, targetDate);
            if (forecastJson != null) {
                double avgTemp = forecastJson.getDouble("avgtemp_c");
                String condition = forecastJson.getJSONObject("condition").getString("text");
                return String.format("Forecast on %s: %.1f°C, %s", targetDate, avgTemp, condition);
            }
            return "No forecast available for this date";
        } catch (Exception e) {
            e.printStackTrace();
            return "Error fetching weather";
        }
    }

    /**
     * 🔥 New method: returns full JSON for a specific city & date
     */
    public JSONObject getFullForecastForDate(String city, LocalDate targetDate) {
        try {
            String urlStr = BASE_URL + "?key=" + API_KEY + "&q=" + city + "&days=3";
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            JSONObject json = new JSONObject(response.toString());
            JSONArray forecastDays = json.getJSONObject("forecast").getJSONArray("forecastday");

            // Find the forecast for the requested date
            for (int i = 0; i < forecastDays.length(); i++) {
                JSONObject day = forecastDays.getJSONObject(i);
                String dateStr = day.getString("date");
                LocalDate forecastDate = LocalDate.parse(dateStr, DateTimeFormatter.ISO_DATE);

                if (forecastDate.equals(targetDate)) {
                    return day.getJSONObject("day"); // return full "day" JSON
                }
            }
            return null;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}