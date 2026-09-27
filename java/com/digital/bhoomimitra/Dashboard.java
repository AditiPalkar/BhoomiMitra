package com.digital.bhoomimitra;

import static android.content.ContentValues.TAG;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;

import android.os.Build;
import android.os.Bundle;
import android.os.Handler; // For the timer
import android.os.Looper;  // For the timer
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import org.json.JSONArray;
import org.json.JSONObject;

import android.Manifest;
import android.content.pm.PackageManager;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

// WorkManager Imports
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

public class Dashboard extends AppCompatActivity {

    private TextView userNameText;
    private ImageButton btnProfile;
    private View dashboardContent;
    private View fragmentContainer;
    private BottomNavigationView bottomNavigationView;
    private LinearLayout cropdiary, community, schemes;
    private CardView diseasecard, irrigationcard, cardOfflineLibrary;

    // Weather UI Variables
    private TextView weatherStatusTxt, temperatureTxt, feelsLikeTxt, humidityTxt, windSpeedTxt, lastUpdatedTxt;
    private TextView tvForecastSummary;
    private View weatherMainCard;
    private TextView tvSoilMoistureValue, tvIrrigationStatus;
    private DatabaseReference irrigationRef;

    private SwipeRefreshLayout swipeRefresh;

    private static final String WEATHER_PREFS = "weather_prefs";
    private static final String LOCATION_PREFS = "farm_location_prefs";

    // Default Fallback
    private static final double DEFAULT_LAT = 19.12;
    private static final double DEFAULT_LON = 72.88;

    private static final int REQ_LOCATION = 1001;
    private FusedLocationProviderClient fusedLocationClient;

    private double currentLat = DEFAULT_LAT;
    private double currentLon = DEFAULT_LON;

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        // --- UI Init ---
        swipeRefresh = findViewById(R.id.swipeRefresh);
        userNameText = findViewById(R.id.userName);
        btnProfile = findViewById(R.id.profile_btn);
        dashboardContent = findViewById(R.id.dashboard_content);
        fragmentContainer = findViewById(R.id.fragment_container);
        bottomNavigationView = findViewById(R.id.bottom_nav);
        diseasecard = findViewById(R.id.disease_card);
        irrigationcard = findViewById(R.id.irrigation_card);
        cardOfflineLibrary = findViewById(R.id.card_offline_library);
        cropdiary = findViewById(R.id.cropdiary);
        community = findViewById(R.id.community);
        schemes = findViewById(R.id.schemes);

        // --- Weather UI ---
        weatherStatusTxt = findViewById(R.id.weatherStatus);
        temperatureTxt = findViewById(R.id.temperature);
        feelsLikeTxt = findViewById(R.id.feelsLike);
        humidityTxt = findViewById(R.id.humidity);
        windSpeedTxt = findViewById(R.id.windSpeed);
        lastUpdatedTxt = findViewById(R.id.tvLastUpdated);
        tvForecastSummary = findViewById(R.id.tvForecastSummary);
        tvSoilMoistureValue = findViewById(R.id.tvSoilMoistureValue);
        tvIrrigationStatus = findViewById(R.id.tvIrrigationStatus);

        weatherMainCard = findViewById(R.id.weather_card);
        if (weatherMainCard == null) weatherMainCard = weatherStatusTxt;

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }

        swipeRefresh.setOnRefreshListener(() -> {
            if (isLocationSaved()) {
                fetchWeatherAndUpdate();
            } else {
                swipeRefresh.setRefreshing(false);
                showLocationDialog();
            }
        });

        SharedPreferences prefs = getSharedPreferences("bhoomimitra_user", MODE_PRIVATE);
        String localName = prefs.getString("farmer_name", null);
        if (localName != null) userNameText.setText(localName);
        fetchUserNameFromCloud();

        dashboardContent.setVisibility(View.VISIBLE);
        fragmentContainer.setVisibility(View.GONE);
        bottomNavigationView.setVisibility(View.VISIBLE);
        bottomNavigationView.setSelectedItemId(R.id.home);

        // --- WEATHER STARTUP ---
        checkPermanentLocation();
        scheduleWeatherAlerts();

        // Long Press to Reset (Feature we added earlier)
        weatherMainCard.setOnLongClickListener(v -> {
            resetLocationSettings();
            return true;
        });

        listenToIrrigationData();
        setupBottomNavigation();
        setupCardListeners();
    }

    private void resetLocationSettings() {
        new AlertDialog.Builder(this)
                .setTitle("Reset Location?")
                .setMessage("Clear saved location and re-capture?")
                .setPositiveButton("Reset", (dialog, which) -> {
                    getSharedPreferences(LOCATION_PREFS, MODE_PRIVATE).edit().clear().apply();
                    getSharedPreferences(WEATHER_PREFS, MODE_PRIVATE).edit().clear().apply();
                    resetWeatherUI();
                    checkPermanentLocation();
                    Toast.makeText(this, "Location cleared.", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void checkPermanentLocation() {
        SharedPreferences prefs = getSharedPreferences(LOCATION_PREFS, MODE_PRIVATE);
        boolean isSet = prefs.getBoolean("is_set", false);

        if (!isSet) {
            resetWeatherUI();
            weatherMainCard.setOnClickListener(v -> showLocationDialog());
        } else {
            currentLat = Double.parseDouble(prefs.getString("lat", String.valueOf(DEFAULT_LAT)));
            currentLon = Double.parseDouble(prefs.getString("lon", String.valueOf(DEFAULT_LON)));
            weatherMainCard.setOnClickListener(null);
            loadCachedWeather();
            fetchWeatherAndUpdate();
        }
    }

    private boolean isLocationSaved() {
        SharedPreferences prefs = getSharedPreferences(LOCATION_PREFS, MODE_PRIVATE);
        return prefs.getBoolean("is_set", false);
    }

    private void resetWeatherUI() {
        weatherStatusTxt.setText("Tap to activate Farm Weather");
        temperatureTxt.setText("--");
        feelsLikeTxt.setText("");
        humidityTxt.setText("");
        windSpeedTxt.setText("");
        if (tvForecastSummary != null) tvForecastSummary.setVisibility(View.GONE);
        if (lastUpdatedTxt != null) lastUpdatedTxt.setText("");
    }

    private void showLocationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Enable Farm Weather")
                .setMessage("Please allow location only when you are at your farm for it's updates.\n\nAllow access?")
                .setPositiveButton("Allow Now", (dialog, which) -> {
                    requestLocationPermission();
                })
                .setNegativeButton("Later", (dialog, which) -> {
                    dialog.dismiss();
                })
                .setCancelable(false)
                .show();
    }

    private void requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQ_LOCATION);
        } else {
            captureSmartLocation();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                captureSmartLocation();
            } else {
                Toast.makeText(this, "Permission required for weather.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void captureSmartLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        weatherStatusTxt.setText("Locating...");

        // Token to handle cancellation
        CancellationTokenSource cts = new CancellationTokenSource();

        // STEP 1: Try High Accuracy (GPS)
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.getToken())
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        saveLocationToPrefs(location.getLatitude(), location.getLongitude());
                    } else {
                        // High Accuracy returned null? Try Fast Mode immediately.
                        captureFastLocation();
                    }
                })
                .addOnFailureListener(e -> captureFastLocation());

        // STEP 2: The 5-Second Timer
        // If High Accuracy takes > 5 seconds, CANCEL it and switch to Fast Mode.
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (weatherStatusTxt.getText().toString().equals("Locating...")) {
                // Cancel the GPS search
                cts.cancel();

                // Show feedback
                Toast.makeText(Dashboard.this, "GPS slow. Switching to fast mode...", Toast.LENGTH_SHORT).show();

                // Switch to Fast Mode
                captureFastLocation();
            }
        }, 5000); // 5000ms = 5 Seconds
    }

    private void captureFastLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return;

        CancellationTokenSource cts = new CancellationTokenSource();
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cts.getToken())
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        saveLocationToPrefs(location.getLatitude(), location.getLongitude());
                    } else {
                        // Total failure: Use defaults to prevent app freeze
                        Toast.makeText(Dashboard.this, "Could not find location.", Toast.LENGTH_SHORT).show();
                        saveLocationToPrefs(currentLat, currentLon);
                    }
                });
    }

    private void saveLocationToPrefs(double lat, double lon) {
        SharedPreferences prefs = getSharedPreferences(LOCATION_PREFS, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("lat", String.valueOf(lat));
        editor.putString("lon", String.valueOf(lon));
        editor.putBoolean("is_set", true);
        editor.apply();

        currentLat = lat;
        currentLon = lon;

        Toast.makeText(this, "Location Captured!", Toast.LENGTH_SHORT).show();

        fetchWeatherAndUpdate();
        weatherMainCard.setOnClickListener(null);
    }

    private void listenToIrrigationData() {
        irrigationRef = FirebaseDatabase.getInstance().getReference("irrigation");

        irrigationRef.addValueEventListener(new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(@NonNull com.google.firebase.database.DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Integer moisture = snapshot.child("soil_moisture").getValue(Integer.class);
                    if (moisture != null) {
                        tvSoilMoistureValue.setText(moisture + "%");
                    } else {
                        tvSoilMoistureValue.setText("--%");
                    }

                    String status = snapshot.child("pump_status").getValue(String.class);
                    if (status != null) {
                        tvIrrigationStatus.setText(status);

                        if (status.equalsIgnoreCase("ON")) {
                            tvIrrigationStatus.setTextColor(android.graphics.Color.parseColor("#43A047")); // Green
                        } else {
                            tvIrrigationStatus.setTextColor(android.graphics.Color.parseColor("#111111")); // Black
                        }
                    } else {
                        tvIrrigationStatus.setText("OFF");
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull com.google.firebase.database.DatabaseError error) {
                Log.e(TAG, "Failed to read irrigation data", error.toException());
            }
        });
    }

    private void fetchWeatherAndUpdate() {
        String apiKey = BuildConfig.WEATHER_API_KEY;
        if (apiKey == null || apiKey.isEmpty()) return;

        String url = "https://api.weatherapi.com/v1/forecast.json?key=" + apiKey + "&q=" + currentLat + "," + currentLon + "&days=2";

        RequestQueue queue = Volley.newRequestQueue(this);
        JsonObjectRequest req = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        JSONObject current = response.optJSONObject("current");
                        JSONObject forecast = response.optJSONObject("forecast");

                        if (current == null) return;

                        double temp = current.optDouble("temp_c", Double.NaN);
                        double feelsLike = current.optDouble("feelslike_c", Double.NaN);
                        int humidity = current.optInt("humidity", 0);
                        double windKph = current.optDouble("wind_kph", Double.NaN);
                        String condition = "N/A";

                        if (current.has("condition")) {
                            JSONObject cond = current.getJSONObject("condition");
                            condition = cond.optString("text", condition);
                        }

                        String forecastText = "";
                        if (forecast != null) {
                            JSONArray forecastday = forecast.getJSONArray("forecastday");
                            if (forecastday.length() > 1) {
                                JSONObject tomorrow = forecastday.getJSONObject(1);
                                JSONObject day = tomorrow.getJSONObject("day");
                                String tomCond = day.getJSONObject("condition").getString("text");
                                int chanceRain = day.getInt("daily_chance_of_rain");
                                forecastText = "Tomorrow: " + tomCond + " • Rain: " + chanceRain + "%";
                            }
                        }

                        updateWeatherUI(condition, temp, feelsLike, humidity, windKph, forecastText);
                        cacheWeather(condition, temp, feelsLike, humidity, windKph, forecastText);

                        if (swipeRefresh != null) swipeRefresh.setRefreshing(false);

                    } catch (Exception e) {
                        Log.e(TAG, "Failed to parse weather JSON", e);
                    }
                },
                error -> {
                    if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
                });

        queue.add(req);
    }

    private void updateWeatherUI(String condition, double temp, double feels, int humidity, double windKph, String forecast) {
        runOnUiThread(() -> {
            weatherStatusTxt.setText(condition != null ? capitalize(condition) : "—");

            if (!Double.isNaN(temp)) temperatureTxt.setText(Math.round(temp) + "°C");
            else temperatureTxt.setText("—");

            if (!Double.isNaN(feels)) feelsLikeTxt.setText("Feels like " + Math.round(feels) + "°C");
            else feelsLikeTxt.setText("");

            if (humidity > 0) humidityTxt.setText(humidity + "%");
            else humidityTxt.setText("");

            if (!Double.isNaN(windKph)) windSpeedTxt.setText(Math.round(windKph) + " km/h");
            else windSpeedTxt.setText("");

            if (tvForecastSummary != null && !forecast.isEmpty()) {
                tvForecastSummary.setText(forecast);
                tvForecastSummary.setVisibility(View.VISIBLE);
            }
        });
    }

    private void cacheWeather(String condition, double temp, double feels, int humidity, double windKph, String forecast) {
        long now = System.currentTimeMillis();
        SharedPreferences prefs = getSharedPreferences(WEATHER_PREFS, MODE_PRIVATE);
        SharedPreferences.Editor ed = prefs.edit();
        ed.putString("condition", condition);
        ed.putFloat("temp", (float) temp);
        ed.putFloat("feels", (float) feels);
        ed.putInt("humidity", humidity);
        ed.putFloat("wind", (float) windKph);
        ed.putString("forecast", forecast);
        ed.putLong("fetchedAt", now);
        ed.apply();

        updateLastUpdated(now);
    }

    private void loadCachedWeather() {
        SharedPreferences prefs = getSharedPreferences(WEATHER_PREFS, MODE_PRIVATE);
        if (prefs.contains("fetchedAt")) {
            String condition = prefs.getString("condition", "—");
            float temp = prefs.getFloat("temp", Float.NaN);
            float feels = prefs.getFloat("feels", Float.NaN);
            int humidity = prefs.getInt("humidity", 0);
            float wind = prefs.getFloat("wind", Float.NaN);
            String forecast = prefs.getString("forecast", "");
            long fetchedAt = prefs.getLong("fetchedAt", 0L);

            updateWeatherUI(condition,
                    Double.isNaN(temp) ? Double.NaN : temp,
                    Double.isNaN(feels) ? Double.NaN : feels,
                    humidity,
                    Double.isNaN(wind) ? Double.NaN : wind,
                    forecast);

            updateLastUpdated(fetchedAt);
        }
    }

    private void updateLastUpdated(long timeMillis) {
        if (lastUpdatedTxt == null || timeMillis <= 0) return;
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        String text = "Last updated: " + sdf.format(new Date(timeMillis));
        lastUpdatedTxt.setText(text);
    }

    private String capitalize(String s) {
        if (s == null || s.length() == 0) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }

    private void setupBottomNavigation() {
        if (bottomNavigationView.getMenu() == null || bottomNavigationView.getMenu().size() == 0) return;
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.home) {
                getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                fragmentContainer.setVisibility(View.GONE);
                dashboardContent.setVisibility(View.VISIBLE);
                bottomNavigationView.setVisibility(View.VISIBLE);
                return true;
            }
            dashboardContent.setVisibility(View.GONE);
            fragmentContainer.setVisibility(View.VISIBLE);
            bottomNavigationView.setVisibility(View.VISIBLE);
            if (id == R.id.disease) { loadFragment(new Disease(), "disease"); return true; }
            if (id == R.id.voice) { loadFragment(new VoiceAssistant(), "voice"); return true; }
            if (id == R.id.irrigation) { loadFragment(new Irrigation(), "irrigation"); return true; }
            if (id == R.id.market) { loadFragment(new Market(), "market"); return true; }
            return false;
        });
    }

    private void loadFragment(androidx.fragment.app.Fragment fragment, String tag) {
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, fragment).addToBackStack(tag).commit();
    }

    private void setupCardListeners() {
        btnProfile.setOnClickListener(v -> {
            dashboardContent.setVisibility(View.GONE);
            fragmentContainer.setVisibility(View.VISIBLE);
            loadFragment(new Profile(), null);
        });
        irrigationcard.setOnClickListener(v -> {
            dashboardContent.setVisibility(View.GONE);
            fragmentContainer.setVisibility(View.VISIBLE);
            loadFragment(new Irrigation(), null);
        });
        diseasecard.setOnClickListener(v -> {
            dashboardContent.setVisibility(View.GONE);
            fragmentContainer.setVisibility(View.VISIBLE);
            loadFragment(new Disease(), null);
        });
        cropdiary.setOnClickListener(v -> {
            dashboardContent.setVisibility(View.GONE);
            fragmentContainer.setVisibility(View.VISIBLE);
            loadFragment(new CropDiary(), "crop_diary");
        });
        community.setOnClickListener(v -> {
            dashboardContent.setVisibility(View.GONE);
            fragmentContainer.setVisibility(View.VISIBLE);
            loadFragment(new CommunityChatFragment(), "community_chat");
        });
        schemes.setOnClickListener(v -> {
            dashboardContent.setVisibility(View.GONE);
            fragmentContainer.setVisibility(View.VISIBLE);
            loadFragment(new SchemesFragment(), "Schemes");
        });
        cardOfflineLibrary.setOnClickListener(v -> {
            dashboardContent.setVisibility(View.GONE);
            fragmentContainer.setVisibility(View.VISIBLE);
            loadFragment(new OfflineLibrary(), "offline_library");
        });
    }

    private void scheduleWeatherAlerts() {
        Constraints constraints = new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build();
        PeriodicWorkRequest weatherWork = new PeriodicWorkRequest.Builder(WeatherWorker.class, 2, TimeUnit.HOURS).setConstraints(constraints).build();
        WorkManager.getInstance(this).enqueueUniquePeriodicWork("WeatherAlertsWork", ExistingPeriodicWorkPolicy.KEEP, weatherWork);
    }

    @Override
    public void onBackPressed() {
        FragmentManager fm = getSupportFragmentManager();
        if (fm.getBackStackEntryCount() > 0) {
            fm.popBackStack(); dashboardContent.setVisibility(View.VISIBLE);
            fragmentContainer.setVisibility(View.GONE);
            bottomNavigationView.setVisibility(View.VISIBLE);
            bottomNavigationView.setSelectedItemId(R.id.home);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences prefs = getSharedPreferences("bhoomimitra_user", MODE_PRIVATE);
        String name = prefs.getString("farmer_name", "Farmer");
        if (userNameText != null) userNameText.setText(name);
        if (isLocationSaved()) fetchWeatherAndUpdate();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void fetchUserNameFromCloud() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String uid = user.getUid();
            DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("users").child(uid);
            dbRef.child("name").get().addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult().exists()) {
                    String cloudName = task.getResult().getValue(String.class);
                    userNameText.setText(cloudName);
                    getSharedPreferences("bhoomimitra_user", MODE_PRIVATE).edit().putString("farmer_name", cloudName).apply();
                }
            });
        }
    }
}