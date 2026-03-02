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

    private static final String API_KEY = "e454c81c0c8a48b0a90222020262202"; // 🔑 Replace with your key
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
            // ✅ Clean city input
            city = city.trim().replace(" ", "%20");

            String urlStr = BASE_URL +
                    "?key=" + API_KEY +
                    "&q=" + city +
                    "&days=14";

            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            int status = conn.getResponseCode();

            BufferedReader reader;

            // ✅ Handle both success & error responses
            if (status >= 200 && status < 300) {
                reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream())
                );
            } else {
                reader = new BufferedReader(
                        new InputStreamReader(conn.getErrorStream())
                );
            }

            StringBuilder response = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                response.append(line);
            }

            reader.close();

            JSONObject json = new JSONObject(response.toString());

            // 🔥 If API returned error, print it
            if (status != 200) {
                System.out.println("Weather API ERROR:");
                System.out.println(json.toString(2));
                return null;
            }

            JSONArray forecastDays =
                    json.getJSONObject("forecast")
                            .getJSONArray("forecastday");

            for (int i = 0; i < forecastDays.length(); i++) {

                JSONObject day = forecastDays.getJSONObject(i);
                LocalDate forecastDate =
                        LocalDate.parse(day.getString("date"));

                if (forecastDate.equals(targetDate)) {
                    return day.getJSONObject("day");
                }
            }

            return null;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}