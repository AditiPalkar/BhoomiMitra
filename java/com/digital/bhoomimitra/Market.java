package com.digital.bhoomimitra;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.gson.Gson;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class Market extends Fragment {

    // --- Configuration ---
    private static final String API_KEY = "579b464db66ec23bdd000001dd90cd941e9445b9664465726100f728";
    // Base URL without filters
    private static final String BASE_URL = "https://api.data.gov.in/resource/9ef84268-d588-465a-a308-a864a43d0070?api-key=" + API_KEY + "&format=json";

    // --- Views ---
    private TextView tvLastUpdated, tvTrendTitle, tvMin, tvCurrent, tvMax;
    private TextView tvMarketInsight, tvBestSelling;
    private LineChart priceTrendChart;
    private RecyclerView rvMandiRates, rvNearbyMandis;
    private ProgressBar progressBar;
    private Spinner spinnerStates;
    private EditText etSearchMandi;
    private Button btnViewMore;
    private FusedLocationProviderClient fusedLocationClient;
    private MandiRatesAdapter ratesAdapter;
    private NearbyMandisAdapter mandisAdapter;
    private List<MarketModel.Record> fullDataList = new ArrayList<>();
    private List<MarketModel.Record> displayList = new ArrayList<>();
    private List<MarketModel.Record> nearbyList = new ArrayList<>();

    private boolean isExpanded = false;
    private String searchText = "";
    private final String[] INDIAN_STATES = {
            "Maharashtra", "Andhra Pradesh", "Bihar", "Goa", "Gujarat", "Haryana", "Himachal Pradesh",
            "Karnataka", "Kerala", "Madhya Pradesh", "Meghalaya","Odisha", "Punjab",
            "Rajasthan", "Tamil Nadu", "Telangana", "Tripura", "Uttar Pradesh",
            "Uttarakhand", "West Bengal", "Chandigarh", "Jammu and Kashmir"
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_market, container, false);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        initViews(view);
        setupRecyclerViews();
        setupStateSpinner();

        return view;
    }

    private void initViews(View view) {
        tvLastUpdated = view.findViewById(R.id.tvLastUpdated);
        tvTrendTitle = view.findViewById(R.id.tvTrendTitle);
        tvMin = view.findViewById(R.id.tvMin);
        tvCurrent = view.findViewById(R.id.tvCurrent);
        tvMax = view.findViewById(R.id.tvMax);
        tvMarketInsight = view.findViewById(R.id.tvMarketInsight);
        tvBestSelling = view.findViewById(R.id.tvBestSelling);
        priceTrendChart = view.findViewById(R.id.priceTrendChart);
        rvMandiRates = view.findViewById(R.id.rvMandiRates);
        rvNearbyMandis = view.findViewById(R.id.rvNearbyMandis);

        progressBar = view.findViewById(R.id.progressBar);

        spinnerStates = view.findViewById(R.id.spinnerStates);
        etSearchMandi = view.findViewById(R.id.etSearchMandi);
        btnViewMore = view.findViewById(R.id.btnViewMore);

        btnViewMore.setOnClickListener(v -> {
            isExpanded = true;
            updateLists();
            btnViewMore.setVisibility(View.GONE);
        });

        // Search Listener
        etSearchMandi.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchText = s.toString();
                isExpanded = true;
                updateLists();
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void setupRecyclerViews() {
        ratesAdapter = new MandiRatesAdapter(displayList);
        rvMandiRates.setLayoutManager(new LinearLayoutManager(getContext()));
        rvMandiRates.setAdapter(ratesAdapter);
        rvMandiRates.setNestedScrollingEnabled(false);

        mandisAdapter = new NearbyMandisAdapter(nearbyList);
        rvNearbyMandis.setLayoutManager(new LinearLayoutManager(getContext()));
        rvNearbyMandis.setAdapter(mandisAdapter);
        rvNearbyMandis.setNestedScrollingEnabled(false);
    }

    private void setupStateSpinner() {
        Arrays.sort(INDIAN_STATES);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, INDIAN_STATES);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerStates.setAdapter(adapter);

        String defaultState = "Maharashtra";

        int defaultPosition = adapter.getPosition(defaultState);
        if (defaultPosition >= 0) {
            spinnerStates.setSelection(defaultPosition);
        }

        spinnerStates.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selected = parent.getItemAtPosition(position).toString();
                fetchMarketData(selected);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void fetchMarketData(String state) {
        if (!isNetworkAvailable()) {
            showNoInternetDialog(state);
            return;
        }
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);

        String url = BASE_URL + "&limit=2000&filters[state.keyword]=" + state;

        RequestQueue queue = Volley.newRequestQueue(requireContext());
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                response -> {
                    if (!isAdded()) return;
                    if (progressBar != null) progressBar.setVisibility(View.GONE);

                    try {
                        Gson gson = new Gson();
                        MarketModel data = gson.fromJson(response, MarketModel.class);

                        if (data != null && data.records != null) {

                            for(MarketModel.Record r : data.records) {
                                // PASS BOTH DISTRICT AND STATE HERE
                                double[] coords = getCoordinates(r.district, r.state);

                                // Assign coords
                                r.latitude = coords[0];
                                r.longitude = coords[1];

                                r.latitude += (Math.random() * 0.05) - 0.025;
                                r.longitude += (Math.random() * 0.05) - 0.025;
                            }

                            fullDataList.clear();
                            fullDataList.addAll(data.records);

                            // Reset View State
                            isExpanded = false;
                            btnViewMore.setVisibility(fullDataList.size() > 7 ? View.VISIBLE : View.GONE);

                            updateUIComponents();
                        } else {
                            Toast.makeText(getContext(), "No data found for " + state, Toast.LENGTH_SHORT).show();
                            fullDataList.clear();
                            updateLists();
                        }
                    } catch (Exception e) {
                        Log.e("MarketFragment", "Parsing Error", e);
                    }
                },
                error -> {
                    if (isAdded()) {
                        if (progressBar != null) progressBar.setVisibility(View.GONE);
                        Toast.makeText(getContext(), "Network Error", Toast.LENGTH_SHORT).show();
                    }
                });

        queue.add(stringRequest);
    }

    private void updateUIComponents() {
        if (!displayList.isEmpty()) {
            MarketModel.Record firstItem = displayList.get(0);

            MarketModel.Record aggregatedStats = calculateCommodityStats(firstItem.commodity);
            if (aggregatedStats != null) {
                updateHeaderAndGraph(aggregatedStats);
            }
        }
        updateLists();
        updateNearbyList();
    }
    private void updateLists() {
        displayList.clear();
        List<MarketModel.Record> searchFiltered = new ArrayList<>();
        if (searchText.isEmpty()) {
            searchFiltered.addAll(fullDataList);
        } else {
            for (MarketModel.Record item : fullDataList) {
                if (item.commodity.toLowerCase().contains(searchText.toLowerCase()) ||
                        item.market.toLowerCase().contains(searchText.toLowerCase())) {
                    searchFiltered.add(item);
                }
            }
        }

        if (!searchFiltered.isEmpty()) {
            MarketModel.Record topResult = searchFiltered.get(0);
            MarketModel.Record stats = calculateCommodityStats(topResult.commodity);
            if (stats != null) updateHeaderAndGraph(stats);
        }

        if (isExpanded || !searchText.isEmpty()) {
            int safeLimit = Math.min(searchFiltered.size(), 100);
            for(int i=0; i<safeLimit; i++) displayList.add(searchFiltered.get(i));
        } else {
            int limit = Math.min(searchFiltered.size(), 7);
            for(int i=0; i<limit; i++) displayList.add(searchFiltered.get(i));
        }
        ratesAdapter.notifyDataSetChanged();
    }

    private void updateHeaderAndGraph(MarketModel.Record highlight) {

        String displayDate;
        if (highlight.arrival_date != null && !highlight.arrival_date.isEmpty()) {
            displayDate = highlight.arrival_date;
        } else {
            displayDate = new SimpleDateFormat("dd MMM, yyyy", Locale.getDefault()).format(new Date());
        }
        tvLastUpdated.setText("Updated: " + displayDate);

        tvTrendTitle.setText(highlight.commodity + " Price Trend");
        tvMin.setText("₹" + highlight.min_price);
        tvCurrent.setText("₹" + highlight.modal_price);
        tvMax.setText("₹" + highlight.max_price);

        float min = parseFloatSafe(highlight.min_price);
        float modal = parseFloatSafe(highlight.modal_price);
        float max = parseFloatSafe(highlight.max_price);

        setupChart(min, modal, max);

        tvMarketInsight.setText("Prices for " + highlight.commodity + " range from ₹" + highlight.min_price + " to ₹" + highlight.max_price + " across markets.");
        tvBestSelling.setText("Next 2 Days");
    }

    private MarketModel.Record calculateCommodityStats(String commodityName) {
        float globalMin = Float.MAX_VALUE;
        float globalMax = Float.MIN_VALUE;
        float totalModal = 0;
        int count = 0;
        String sampleMarket = "";
        String sampleDate = "";

        for (MarketModel.Record r : fullDataList) {
            // Only check records matching the target commodity (case-insensitive)
            if (r.commodity.equalsIgnoreCase(commodityName)) {
                float min = parseFloatSafe(r.min_price);
                float max = parseFloatSafe(r.max_price);
                float modal = parseFloatSafe(r.modal_price);

                if (min < globalMin) globalMin = min;
                if (max > globalMax) globalMax = max;
                totalModal += modal;
                count++;

                // Keep track of latest details
                sampleMarket = r.market;
                sampleDate = r.arrival_date;
            }
        }

        if (count == 0) return null;

        MarketModel.Record summary = new MarketModel.Record();
        summary.commodity = commodityName;
        summary.market = "All Markets (" + count + ")";
        summary.min_price = String.valueOf((int)globalMin);
        summary.max_price = String.valueOf((int)globalMax);
        summary.modal_price = String.valueOf((int)(totalModal / count));
        summary.arrival_date = sampleDate;

        return summary;
    }
    private float parseFloatSafe(String value) {
        try {
            return Float.parseFloat(value);
        } catch (Exception e) {
            return 0f;
        }
    }


    private void updateNearbyList() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            nearbyList.clear();
            int limit = Math.min(fullDataList.size(), 5);
            for(int i=0; i<limit; i++) nearbyList.add(fullDataList.get(i));
            mandisAdapter.notifyDataSetChanged();
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                // Calculate distances for the CURRENT loaded state data
                for (MarketModel.Record item : fullDataList) {
                    float[] results = new float[1];
                    Location.distanceBetween(location.getLatitude(), location.getLongitude(), item.latitude, item.longitude, results);
                    item.distance = results[0] / 1000;
                }

                List<MarketModel.Record> sorted = new ArrayList<>(fullDataList);
                Collections.sort(sorted, new Comparator<MarketModel.Record>() {
                    @Override
                    public int compare(MarketModel.Record o1, MarketModel.Record o2) {
                        return Double.compare(o1.distance, o2.distance);
                    }
                });

                nearbyList.clear();
                int limit = Math.min(sorted.size(), 5);
                for(int i=0; i<limit; i++) nearbyList.add(sorted.get(i));
                mandisAdapter.notifyDataSetChanged();
            }
        });
    }

    private double[] getCoordinates(String district, String state) {
        String d = (district != null) ? district.toLowerCase().trim() : "";
        String s = (state != null) ? state.toLowerCase().trim() : "";

        switch (d) {
            // --- MAHARASHTRA ---
            case "pune": return new double[]{18.5204, 73.8567};
            case "mumbai": return new double[]{19.0760, 72.8777};
            case "nashik": return new double[]{19.9975, 73.7898};
            case "nagpur": return new double[]{21.1458, 79.0882};
            case "aurangabad": return new double[]{19.8762, 75.3433};
            case "thane": return new double[]{19.2183, 72.9781};
            case "solapur": return new double[]{17.6599, 75.9064};
            case "ahmednagar": return new double[]{19.0952, 74.7496};
            case "kolhapur": return new double[]{16.7050, 74.2433};
            case "amravati": return new double[]{20.9320, 77.7523};
            case "jalgaon": return new double[]{21.0077, 75.5626};
            case "latur": return new double[]{18.4088, 76.5604};
            case "satara": return new double[]{17.6805, 74.0183};
            case "sangli": return new double[]{16.8524, 74.5815};
            case "akola": return new double[]{20.7002, 77.0082};

            // --- PUNJAB ---
            case "ludhiana": return new double[]{30.9010, 75.8573};
            case "amritsar": return new double[]{31.6340, 74.8723};
            case "jalandhar": return new double[]{31.3260, 75.5762};
            case "patiala": return new double[]{30.3398, 76.3869};
            case "bathinda": return new double[]{30.2110, 74.9455};
            case "mohali": return new double[]{30.7046, 76.7179};
            case "firozpur": return new double[]{30.9098, 74.6094};
            case "pathankot": return new double[]{32.2643, 75.6497};
            case "sangrur": return new double[]{30.2458, 75.8421};

            // --- HARYANA ---
            case "gurgaon": return new double[]{28.4595, 77.0266};
            case "faridabad": return new double[]{28.4089, 77.3178};
            case "panipat": return new double[]{29.3909, 76.9635};
            case "ambala": return new double[]{30.3782, 76.7767};
            case "karnal": return new double[]{29.6857, 76.9905};
            case "hisar": return new double[]{29.1492, 75.7217};
            case "rohtak": return new double[]{28.8955, 76.6066};

            // --- GUJARAT ---
            case "ahmedabad": return new double[]{23.0225, 72.5714};
            case "surat": return new double[]{21.1702, 72.8311};
            case "vadodara": return new double[]{22.3072, 73.1812};
            case "rajkot": return new double[]{22.3039, 70.8022};
            case "bhavnagar": return new double[]{21.7645, 72.1519};
            case "jamnagar": return new double[]{22.4707, 70.0577};
            case "gandhinagar": return new double[]{23.2156, 72.6369};
            case "junagadh": return new double[]{21.5222, 70.4579};

            // --- MADHYA PRADESH ---
            case "indore": return new double[]{22.7196, 75.8577};
            case "bhopal": return new double[]{23.2599, 77.4126};
            case "jabalpur": return new double[]{23.1815, 79.9864};
            case "gwalior": return new double[]{26.2183, 78.1828};
            case "ujjain": return new double[]{23.1765, 75.7885};
            case "sagar": return new double[]{23.8388, 78.7378};
            case "ratlam": return new double[]{23.3315, 75.0367};

            // --- UTTAR PRADESH ---
            case "lucknow": return new double[]{26.8467, 80.9462};
            case "kanpur": return new double[]{26.4499, 80.3319};
            case "varanasi": return new double[]{25.3176, 82.9739};
            case "agra": return new double[]{27.1767, 78.0081};
            case "meerut": return new double[]{28.9845, 77.7064};
            case "ghaziabad": return new double[]{28.6692, 77.4538};
            case "prayagraj":
            case "allahabad": return new double[]{25.4358, 81.8463};
            case "bareilly": return new double[]{28.3670, 79.4304};
            case "aligarh": return new double[]{27.8974, 78.0880};
            case "gorakhpur": return new double[]{26.7606, 83.3732};

            // --- KARNATAKA ---
            case "bangalore":
            case "bengaluru": return new double[]{12.9716, 77.5946};
            case "mysore": return new double[]{12.2958, 76.6394};
            case "hubli": return new double[]{15.3647, 75.1240};
            case "belgaum": return new double[]{15.8497, 74.4977};
            case "mangalore": return new double[]{12.9141, 74.8560};
            case "gulbarga": return new double[]{17.3297, 76.8343};

            // --- RAJASTHAN ---
            case "jaipur": return new double[]{26.9124, 75.7873};
            case "jodhpur": return new double[]{26.2389, 73.0243};
            case "udaipur": return new double[]{24.5854, 73.7125};
            case "kota": return new double[]{25.2138, 75.8648};
            case "bikaner": return new double[]{28.0229, 73.3119};
            case "ajmer": return new double[]{26.4499, 74.6399};

            // --- TAMIL NADU ---
            case "chennai": return new double[]{13.0827, 80.2707};
            case "coimbatore": return new double[]{11.0168, 76.9558};
            case "madurai": return new double[]{9.9252, 78.1198};
            case "salem": return new double[]{11.6643, 78.1460};
            case "trichy": return new double[]{10.7905, 78.7047};
            case "vellore": return new double[]{12.9165, 79.1325};

            // --- TELANGANA ---
            case "hyderabad": return new double[]{17.3850, 78.4867};
            case "warangal": return new double[]{17.9689, 79.5941};
            case "nizamabad": return new double[]{18.6725, 78.0941};
            case "khammam": return new double[]{17.2473, 80.1514};

            // --- ANDHRA PRADESH ---
            case "visakhapatnam": return new double[]{17.6868, 83.2185};
            case "vijayawada": return new double[]{16.5062, 80.6480};
            case "guntur": return new double[]{16.3067, 80.4365};
            case "tirupati": return new double[]{13.6288, 79.4192};
            case "kurnool": return new double[]{15.8281, 78.0373};

            // --- WEST BENGAL ---
            case "kolkata": return new double[]{22.5726, 88.3639};
            case "howrah": return new double[]{22.5958, 88.2636};
            case "siliguri": return new double[]{26.7271, 88.3953};
            case "durgapur": return new double[]{23.5204, 87.3119};
            case "asansol": return new double[]{23.6739, 86.9524};
            case "bardhaman": return new double[]{23.2324, 87.8615};

            // --- BIHAR ---
            case "patna": return new double[]{25.5941, 85.1376};
            case "gaya": return new double[]{24.7914, 85.0002};
            case "bhagalpur": return new double[]{25.2425, 87.0117};
            case "muzaffarpur": return new double[]{26.1197, 85.3910};

            // --- ODISHA ---
            case "bhubaneswar": return new double[]{20.2961, 85.8245};
            case "cuttack": return new double[]{20.4625, 85.8828};
            case "rourkela": return new double[]{22.2604, 84.8536};
            case "berhampur": return new double[]{19.3150, 84.7941};

            // --- KERALA ---
            case "thiruvananthapuram": return new double[]{8.5241, 76.9366};
            case "kochi": return new double[]{9.9312, 76.2673};
            case "kozhikode": return new double[]{11.2588, 75.7804};
            case "thrissur": return new double[]{10.5276, 76.2144};

            // --- OTHERS ---
            case "delhi": return new double[]{28.7041, 77.1025};
            case "chandigarh": return new double[]{30.7333, 76.7794};
            case "goa": return new double[]{15.2993, 74.1240};
            case "shillong": return new double[]{25.5788, 91.8933}; // Meghalaya
            case "agartala": return new double[]{23.8315, 91.2868}; // Tripura
            case "dehradun": return new double[]{30.3165, 78.0322}; // Uttarakhand
            case "shimla": return new double[]{31.1048, 77.1734}; // Himachal
            case "srinagar": return new double[]{34.0837, 74.7973}; // J&K
        }

        switch (s) {
            case "maharashtra": return new double[]{19.7515, 75.7139};
            case "andhra pradesh": return new double[]{15.9129, 79.7400};
            case "bihar": return new double[]{25.0961, 85.3131};
            case "goa": return new double[]{15.2993, 74.1240};
            case "gujarat": return new double[]{22.2587, 71.1924};
            case "haryana": return new double[]{29.0588, 76.0856};
            case "himachal pradesh": return new double[]{31.1048, 77.1734};
            case "karnataka": return new double[]{15.3173, 75.7139};
            case "kerala": return new double[]{10.8505, 76.2711};
            case "madhya pradesh": return new double[]{22.9734, 78.6569};
            case "meghalaya": return new double[]{25.4670, 91.3662};
            case "odisha": return new double[]{20.9517, 85.0985};
            case "punjab": return new double[]{31.1471, 75.3412};
            case "rajasthan": return new double[]{27.0238, 74.2179};
            case "tamil nadu": return new double[]{11.1271, 78.6569};
            case "telangana": return new double[]{18.1124, 79.0193};
            case "tripura": return new double[]{23.9408, 91.9882};
            case "uttar pradesh": return new double[]{26.8467, 80.9462};
            case "uttarakhand": return new double[]{30.0668, 79.0193};
            case "west bengal": return new double[]{22.9868, 87.8550};
            case "chandigarh": return new double[]{30.7333, 76.7794};
            case "jammu and kashmir": return new double[]{33.7782, 76.5762};
        }

        return new double[]{20.5937, 78.9629};
    }

    private void setupChart(float min, float modal, float max) {
        List<Entry> entries = new ArrayList<>();
        entries.add(new Entry(0, min + (modal - min) * 0.2f));
        entries.add(new Entry(1, min));
        entries.add(new Entry(2, (min + modal) / 2));
        entries.add(new Entry(3, modal));
        entries.add(new Entry(4, max));


        LineDataSet dataSet = new LineDataSet(entries, "Price Range");
        dataSet.setColor(Color.parseColor("#1B5E20"));
        dataSet.setCircleColor(Color.parseColor("#4CAF50"));
        dataSet.setLineWidth(3f);
        dataSet.setCircleRadius(5f);
        dataSet.setDrawValues(true);
        dataSet.setValueTextSize(10f);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);

        dataSet.setDrawFilled(true);
        dataSet.setFillColor(Color.parseColor("#C8E6C9"));

        LineData lineData = new LineData(dataSet);
        priceTrendChart.setData(lineData);

        // Styling
        priceTrendChart.getDescription().setEnabled(false);
        priceTrendChart.getLegend().setEnabled(false);
        priceTrendChart.getAxisRight().setEnabled(false);
        priceTrendChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        priceTrendChart.getXAxis().setDrawGridLines(false);
        priceTrendChart.getAxisLeft().setDrawGridLines(true);

        priceTrendChart.animateY(1000);
        priceTrendChart.invalidate(); // Refresh
    }

    class MandiRatesAdapter extends RecyclerView.Adapter<MandiRatesAdapter.ViewHolder> {
        List<MarketModel.Record> list;
        public MandiRatesAdapter(List<MarketModel.Record> list) {
            this.list = list;
        }
        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_mandi_rate, parent, false);
            return new ViewHolder(v);
        }
        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            MarketModel.Record item = list.get(position);
            holder.tvCommodity.setText(item.commodity);
            holder.tvMandiName.setText(item.market);
            holder.tvPrice.setText("₹ " + item.modal_price + " / Quintal");
            holder.tvChange.setText("Live");

        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvCommodity, tvMandiName, tvPrice, tvChange;
            View dotView;

            ViewHolder(View v) {
                super(v);
                tvCommodity = v.findViewById(R.id.tvCommodity);
                tvMandiName = v.findViewById(R.id.tvMandiName);
                tvPrice = v.findViewById(R.id.tvPrice);
                tvChange = v.findViewById(R.id.tvChange);
                dotView = v.findViewById(R.id.viewDot);
            }
        }
    }

    class NearbyMandisAdapter extends RecyclerView.Adapter<NearbyMandisAdapter.ViewHolder> {
        List<MarketModel.Record> list;
        public NearbyMandisAdapter(List<MarketModel.Record> list) { this.list = list; }
        @NonNull @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_nearby_mandi, parent, false);
            return new ViewHolder(v);
        }
        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            MarketModel.Record item = list.get(position);
            holder.tvMandiName.setText(item.market + " Mandi");
            holder.tvMandiOpen.setText(item.district + ", " + item.state);
            holder.tvDistance.setText(String.format("%.1f km", item.distance));
        }
        @Override
        public int getItemCount() { return list.size(); }
        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvMandiName, tvMandiOpen, tvDistance;
            ViewHolder(View v) {
                super(v);
                tvMandiName = v.findViewById(R.id.tvMandiName);
                tvMandiOpen = v.findViewById(R.id.tvMandiOpen);
                tvDistance = v.findViewById(R.id.tvDistance);
            }
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) requireContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            NetworkCapabilities capabilities = cm.getNetworkCapabilities(cm.getActiveNetwork());
            return capabilities != null && (
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
        }
        return false;
    }

    private void showNoInternetDialog(String stateToRetry) {
        new AlertDialog.Builder(requireContext())
                .setTitle("No Internet Connection")
                .setMessage("Please check your internet connection to see the latest mandi prices.")
                .setCancelable(false)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton("Retry", (dialog, which) -> {
                    fetchMarketData(stateToRetry);
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    dialog.dismiss();
                })
                .show();
    }
}