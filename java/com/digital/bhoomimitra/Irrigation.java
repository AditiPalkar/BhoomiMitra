package com.digital.bhoomimitra;

import android.app.TimePickerDialog;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class Irrigation extends Fragment {

    // UI Elements
    private AutoCompleteTextView ddSoilType, ddCropType;
    private TextView tvTimeInput, tvMoisturePercent, tvPumpDuration, tvFlowRate, tvNextScheduleTime;
    private TextView tvControlModeDesc, btnTurnOff, btnTurnOn, tvWaterTrend; // New UI Elements
    private ProgressBar progressMoisture;
    private LineChart chartWaterUsage;
    private SwitchMaterial switchAiMode;
    private RelativeLayout layoutPumpRunning, layoutPumpStopped;

    private com.google.android.material.card.MaterialCardView cardNextSchedule;

    // Firebase
    private DatabaseReference irrigationRef, historyRef;

    // JSON
    private JSONObject soilJson;

    // Pump & Timer Logic
    private static final int FLOW_RATE = 10; // L/min
    private boolean isPumpRunning = false;
    private long localStartTime = 0;
    private final Handler timerHandler = new Handler(Looper.getMainLooper());

    private final Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            if (isPumpRunning) {
                long elapsedMillis = System.currentTimeMillis() - localStartTime;
                int secondsElapsed = (int) (elapsedMillis / 1000);
                updateTimerUI(secondsElapsed);
                timerHandler.postDelayed(this, 1000);
            }
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_irrigation, container, false);

        bindViews(view);
        setupFirebase();
        loadJson();
        setupSoilDropdown();
        setupTimePicker(view);

        listenMoisture();
        listenPumpStatus();
        listenToHistoryForGraphAndTrend(); // Consolidated listener

        setupControlModeToggle();
        setupPumpManualControl();
        listenToConfig();

        return view;
    }

    // ---------------- UI BINDING ----------------
    private void bindViews(View v) {
        ddSoilType = v.findViewById(R.id.ddSoilType);
        ddCropType = v.findViewById(R.id.ddCropType);
        tvTimeInput = v.findViewById(R.id.tvTimeInput);
        tvMoisturePercent = v.findViewById(R.id.tvMoisturePercent);
        progressMoisture = v.findViewById(R.id.progressMoisture);
        tvPumpDuration = v.findViewById(R.id.tvPumpDuration);
        tvFlowRate = v.findViewById(R.id.tvFlowRate);
        tvNextScheduleTime = v.findViewById(R.id.tvNextScheduleTime);
        chartWaterUsage = v.findViewById(R.id.chartWaterUsage);
        tvWaterTrend = v.findViewById(R.id.tvWaterTrend);
        cardNextSchedule = v.findViewById(R.id.cardNextSchedule);

        switchAiMode = v.findViewById(R.id.switchAiMode);
        tvControlModeDesc = v.findViewById(R.id.tvControlModeDesc);
        layoutPumpRunning = v.findViewById(R.id.layoutPumpRunning);
        layoutPumpStopped = v.findViewById(R.id.layoutPumpStopped);
        btnTurnOff = v.findViewById(R.id.btnTurnOff);
        btnTurnOn = v.findViewById(R.id.btnTurnOn);

        tvFlowRate.setText(FLOW_RATE + " L/min");
    }

    // ---------------- FIREBASE SETUP ----------------
    private void setupFirebase() {
        irrigationRef = FirebaseDatabase.getInstance().getReference("irrigation");
        historyRef = FirebaseDatabase.getInstance().getReference("irrigation/history");
    }

    // ---------------- NEW: RESTORE SAVED INPUTS ----------------
    private void listenToConfig() {
        // 1. Restore Soil Type
        irrigationRef.child("soil_type").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String val = snapshot.getValue(String.class);
                if (val != null) {
                    // 'false' prevents the dropdown list from popping up automatically
                    ddSoilType.setText(val, false);
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });

        // 2. Restore Crop Type
        irrigationRef.child("crop_type").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String val = snapshot.getValue(String.class);
                if (val != null) {
                    ddCropType.setText(val, false);
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });

        // 3. Restore Start Time & Schedule Text
        irrigationRef.child("start_time").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String val = snapshot.getValue(String.class);

                // ONLY show the card if we actually have a saved time
                if (val != null && !val.isEmpty()) {
                    tvTimeInput.setText(val);
                    tvNextScheduleTime.setText("Tomorrow " + val);

                    // Make the card visible now that we have data
                    if (cardNextSchedule != null) {
                        cardNextSchedule.setVisibility(View.VISIBLE);
                    }
                } else {
                    // Hide it if no time is set
                    if (cardNextSchedule != null) {
                        cardNextSchedule.setVisibility(View.GONE);
                    }
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    // ---------------- PUMP MANUAL CONTROL ----------------
    private void setupPumpManualControl() {
        btnTurnOn.setOnClickListener(v -> {
            layoutPumpStopped.setVisibility(View.GONE);
            layoutPumpRunning.setVisibility(View.VISIBLE);
            startLocalTimer();
            irrigationRef.child("start_timestamp").setValue(System.currentTimeMillis());
            irrigationRef.child("manual_pump").setValue(true);
            irrigationRef.child("pump_status").setValue("ON");
        });

        btnTurnOff.setOnClickListener(v -> {
            layoutPumpRunning.setVisibility(View.GONE);
            layoutPumpStopped.setVisibility(View.VISIBLE);
            stopLocalTimer();
            irrigationRef.child("manual_pump").setValue(false);
            irrigationRef.child("pump_status").setValue("OFF");
        });
    }

    private void listenPumpStatus() {
        irrigationRef.child("pump_status").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snap) {
                String status = snap.getValue(String.class);
                if ("ON".equals(status)) {
                    if (layoutPumpStopped.getVisibility() == View.VISIBLE) {
                        layoutPumpStopped.setVisibility(View.GONE);
                        layoutPumpRunning.setVisibility(View.VISIBLE);
                        startLocalTimer();
                    }
                } else if ("OFF".equals(status)) {
                    if (layoutPumpRunning.getVisibility() == View.VISIBLE) {
                        layoutPumpRunning.setVisibility(View.GONE);
                        layoutPumpStopped.setVisibility(View.VISIBLE);
                        stopLocalTimer();
                    }
                    checkAndSaveHistory();
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void checkAndSaveHistory() {
        irrigationRef.child("start_timestamp").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Long startMillis = snapshot.getValue(Long.class);
                if (startMillis != null && startMillis > 0) {
                    long durationMillis = System.currentTimeMillis() - startMillis;
                    float minutes = Math.max(0.1f, durationMillis / 60000f);
                    float waterLiters = minutes * FLOW_RATE;

                    SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                    SimpleDateFormat sdfTime = new SimpleDateFormat("HH:mm", Locale.US);
                    Date now = new Date();

                    Map<String, Object> historyMap = new HashMap<>();
                    historyMap.put("date", sdfDate.format(now));
                    historyMap.put("time", sdfTime.format(now));
                    historyMap.put("liters", waterLiters);
                    historyMap.put("timestamp", ServerValue.TIMESTAMP);

                    historyRef.push().setValue(historyMap);
                    irrigationRef.child("start_timestamp").removeValue();

                    Toast.makeText(getContext(), "Saved: " + String.format("%.1f", waterLiters) + "L", Toast.LENGTH_SHORT).show();
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    // ---------------- HISTORY GRAPH & TREND (REAL-TIME) ----------------
    private void listenToHistoryForGraphAndTrend() {
        // Fetch last 30 entries (enough to cover 2 weeks of activity)
        historyRef.limitToLast(50).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snap) {
                if (!snap.exists()) return;

                // 1. Prepare Data Buckets
                // Map of "Days Ago" -> Total Liters
                Map<Integer, Float> dailyUsage = new HashMap<>();
                for (int i = 0; i < 15; i++) dailyUsage.put(i, 0f);

                float currentWeekSum = 0;
                float prevWeekSum = 0;

                long now = System.currentTimeMillis();
                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd", Locale.US);
                List<String> xLabels = new ArrayList<>();

                for (DataSnapshot d : snap.getChildren()) {
                    Long timestamp = d.child("timestamp").getValue(Long.class);
                    Float liters = d.child("liters").getValue(Float.class);

                    if (timestamp != null && liters != null) {
                        long diffMillis = now - timestamp;
                        int daysAgo = (int) TimeUnit.MILLISECONDS.toDays(diffMillis);

                        // Only care about last 14 days
                        if (daysAgo < 14) {
                            // Bucket for Graph
                            dailyUsage.put(daysAgo, dailyUsage.get(daysAgo) + liters);

                            // Bucket for Trend
                            if (daysAgo < 7) {
                                currentWeekSum += liters;
                            } else {
                                prevWeekSum += liters;
                            }
                        }
                    }
                }

                // 2. Update Trend Text UI
                updateTrendUI(currentWeekSum, prevWeekSum);

                // 3. Update Graph UI (Plot Last 7 Days: 6 days ago -> Today)
                List<Entry> entries = new ArrayList<>();
                for (int i = 6; i >= 0; i--) {
                    // X index 0 is 6 days ago, X index 6 is Today
                    entries.add(new Entry(6 - i, dailyUsage.get(i)));

                    // Create Label (e.g., "Dec 12")
                    Calendar cal = Calendar.getInstance();
                    cal.add(Calendar.DAY_OF_YEAR, -i);
                    xLabels.add(sdf.format(cal.getTime()));
                }

                updateGraph(entries, xLabels);
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void updateTrendUI(float current, float previous) {
        if (tvWaterTrend == null) return;

        float percentChange = 0;
        if (previous > 0) {
            percentChange = ((current - previous) / previous) * 100;
        } else if (current > 0) {
            percentChange = 100; // 100% increase if previous was 0
        }

        String symbol = percentChange > 0 ? "+" : "";
        String text = String.format(Locale.US, "%s%.0f%%", symbol, percentChange);

        tvWaterTrend.setText(text);

        if (percentChange <= 0) {
            // Negative change (Saved water) -> Green
            tvWaterTrend.setTextColor(Color.parseColor("#388E3C")); // Green
            tvWaterTrend.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_arrow_down_green, 0); // Optional: add an arrow icon if you have one
        } else {
            // Positive change (Used more water) -> Red/Orange
            tvWaterTrend.setTextColor(Color.parseColor("#D32F2F")); // Red
        }
    }

    private void updateGraph(List<Entry> entries, List<String> labels) {
        LineDataSet ds = new LineDataSet(entries, "");
        ds.setColor(Color.parseColor("#43A047"));
        ds.setLineWidth(2.5f);
        ds.setCircleColor(Color.parseColor("#2E7D32"));
        ds.setCircleRadius(4f);
        ds.setDrawValues(false);
        ds.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        ds.setDrawFilled(true);
        ds.setFillColor(Color.parseColor("#A5D6A7"));

        LineData lineData = new LineData(ds);
        chartWaterUsage.setData(lineData);

        XAxis xAxis = chartWaterUsage.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setDrawGridLines(false);

        chartWaterUsage.getAxisRight().setEnabled(false);
        chartWaterUsage.getLegend().setEnabled(false);
        chartWaterUsage.getDescription().setEnabled(false);
        chartWaterUsage.animateY(1000);
        chartWaterUsage.invalidate();
    }

    // ---------------- UI HELPERS ----------------
    private void startLocalTimer() {
        if (!isPumpRunning) {
            localStartTime = System.currentTimeMillis();
            isPumpRunning = true;
            timerHandler.post(timerRunnable);
        }
    }

    private void stopLocalTimer() {
        isPumpRunning = false;
        timerHandler.removeCallbacks(timerRunnable);
        tvPumpDuration.setText("00:00");
    }

    private void updateTimerUI(int totalSeconds) {
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        tvPumpDuration.setText(String.format(Locale.US, "%02d:%02d min", minutes, seconds));
    }

    // ---------------- OTHER SETUP ----------------
    private void setupControlModeToggle() {
        irrigationRef.child("control_mode").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot s) {
                boolean isAi = "AI".equals(s.getValue(String.class));
                switchAiMode.setChecked(isAi);
                updateToggleVisuals(isAi);
            }
            @Override public void onCancelled(@NonNull DatabaseError e) {}
        });

        switchAiMode.setOnCheckedChangeListener((v, isChecked) -> {
            updateToggleVisuals(isChecked);
            irrigationRef.child("control_mode").setValue(isChecked ? "AI" : "MANUAL");
        });
    }

    private void updateToggleVisuals(boolean isAi) {
        if (isAi) {
            tvControlModeDesc.setText("AI Scheduling Active");
            tvControlModeDesc.setTextColor(Color.parseColor("#2E7D32"));
            switchAiMode.setTrackTintList(ColorStateList.valueOf(Color.parseColor("#4CAF50")));
        } else {
            tvControlModeDesc.setText("Manual control active");
            tvControlModeDesc.setTextColor(Color.parseColor("#757575"));
            switchAiMode.setTrackTintList(ColorStateList.valueOf(Color.parseColor("#B0BEC5")));
        }
    }

    private void setupSoilDropdown() {
        if (soilJson == null) return;
        try {
            JSONObject soils = soilJson.getJSONObject("soils");
            List<String> list = new ArrayList<>();
            Iterator<String> it = soils.keys();
            while (it.hasNext()) list.add(it.next());

            ddSoilType.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, list));
            ddSoilType.setOnItemClickListener((a, v, p, id) -> {
                String soil = ddSoilType.getText().toString();
                loadCropsForSoil(soil);
                irrigationRef.child("soil_type").setValue(soil);
            });
        } catch (Exception e) {}
    }

    private void loadCropsForSoil(String soil) {
        try {
            JSONObject crops = soilJson.getJSONObject("soils").getJSONObject(soil).getJSONObject("crops");
            List<String> list = new ArrayList<>();
            Iterator<String> it = crops.keys();
            while (it.hasNext()) list.add(it.next());

            ddCropType.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, list));
            ddCropType.setOnItemClickListener((a, v, p, id) -> {
                String crop = ddCropType.getText().toString();
                try {
                    int adj = soilJson.getJSONObject("soils").getJSONObject(soil).getInt("adjustment");
                    int target = crops.getInt(crop) + adj;
                    irrigationRef.child("crop_type").setValue(crop);
                    irrigationRef.child("target_moisture").setValue(target);
                } catch (Exception e) {}
            });
        } catch (Exception e) {}
    }

    private void loadJson() {
        try {
            InputStream is = requireContext().getAssets().open("soil_crop_moisture.json");
            byte[] buf = new byte[is.available()];
            is.read(buf);
            is.close();
            soilJson = new JSONObject(new String(buf, StandardCharsets.UTF_8));
        } catch (Exception e) {}
    }

    private void setupTimePicker(View root) {
        root.findViewById(R.id.btnTimePicker).setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            new TimePickerDialog(requireContext(), (tp, h, m) -> {
                String time = String.format(Locale.US, "%02d:%02d", h, m);
                tvTimeInput.setText(time);
                tvNextScheduleTime.setText("Today " + time);
                irrigationRef.child("start_time").setValue(time);
            }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true).show();
        });
    }

    private void listenMoisture() {
        irrigationRef.child("soil_moisture").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snap) {
                Integer m = snap.getValue(Integer.class);
                if (m != null) {
                    tvMoisturePercent.setText(m + "%");
                    progressMoisture.setProgress(m);
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
}