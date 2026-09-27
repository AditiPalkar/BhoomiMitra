package com.digital.bhoomimitra;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import org.json.JSONArray;
import org.json.JSONObject;

public class WeatherWorker extends Worker {

    public WeatherWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        String apiKey = BuildConfig.WEATHER_API_KEY;
        double lat = 19.12;
        double lon = 72.88;

        if (apiKey != null && !apiKey.isEmpty()) {
            fetchForecast(apiKey, lat, lon);
        }
        return Result.success();
    }

    private void fetchForecast(String apiKey, double lat, double lon) {
        String url = "https://api.weatherapi.com/v1/forecast.json?key=" + apiKey + "&q=" + lat + "," + lon + "&days=1&hour=24";

        JsonObjectRequest req = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        JSONObject forecast = response.getJSONObject("forecast");
                        JSONArray forecastday = forecast.getJSONArray("forecastday");
                        JSONObject today = forecastday.getJSONObject(0);
                        JSONArray hours = today.getJSONArray("hour");

                        long currentTime = System.currentTimeMillis() / 1000;

                        // Check the next 4 hours
                        for (int i = 0; i < hours.length(); i++) {
                            JSONObject hourObj = hours.getJSONObject(i);
                            long hourEpoch = hourObj.getLong("time_epoch");

                            if (hourEpoch > currentTime && hourEpoch < (currentTime + 14400)) {
                                int willRain = hourObj.optInt("will_it_rain", 0);
                                int chanceRain = hourObj.optInt("chance_of_rain", 0);
                                double windKph = hourObj.optDouble("wind_kph", 0);

                                if (willRain == 1 || chanceRain > 70) {
                                    sendNotification("Rain Alert 🌧️", "Rain is expected shortly (" + chanceRain + "% chance).");
                                    break;
                                }
                                if (windKph > 25) {
                                    sendNotification("High Wind Alert 💨", "Wind speed is " + windKph + " km/h. Avoid spraying.");
                                    break;
                                }
                            }
                        }
                    } catch (Exception e) {
                        Log.e("WeatherWorker", "Error parsing forecast", e);
                    }
                },
                error -> Log.e("WeatherWorker", "Network error", error)
        );

        Volley.newRequestQueue(getApplicationContext()).add(req);
    }

    private void sendNotification(String title, String message) {
        NotificationManager manager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        String channelId = "agri_weather_alerts";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, "Weather Alerts", NotificationManager.IMPORTANCE_HIGH);
            manager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), channelId)
                .setSmallIcon(R.drawable.ic_cloud)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        manager.notify(1, builder.build());
    }
}