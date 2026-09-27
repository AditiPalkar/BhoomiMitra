package com.digital.bhoomimitra;

import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class SchemesFragment extends Fragment {

    private TextView tvAvailableCount;
    private EditText etSearchScheme;
    private RadioGroup rgFilterType;
    private LinearLayout layoutSchemes, eligibilitycard;

    private final List<Scheme> allSchemes = new ArrayList<>();
    private final List<Scheme> filteredSchemes = new ArrayList<>();

    public SchemesFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_schemes, container, false);

        tvAvailableCount = view.findViewById(R.id.tvAvailableCount);
        etSearchScheme = view.findViewById(R.id.etSearchScheme);
        rgFilterType = view.findViewById(R.id.rgFilterType);
        layoutSchemes = view.findViewById(R.id.layoutSchemes);

        // Load schemes from assets/schemes.json
        loadSchemesFromJson(requireContext());

        // Initial display
        applyFilters();

        // Search listener
        etSearchScheme.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyFilters();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        // Filter listener (All / Central / State)
        rgFilterType.setOnCheckedChangeListener((group, checkedId) -> applyFilters());

        return view;
    }

    private void loadSchemesFromJson(Context context) {
        allSchemes.clear();
        try {
            String jsonStr = loadJSONFromAsset(context, "schemes.json");
            if (jsonStr == null) return;

            JSONArray arr = new JSONArray(jsonStr);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);

                Scheme s = new Scheme();
                s.id = obj.optInt("id", i + 1);
                s.name = obj.optString("name", "");
                s.shortDescription = obj.optString("shortDescription", "");
                s.description = obj.optString("description", "");
                s.amount = obj.optString("amount", "");
                s.benefits = obj.optString("benefits", "");
                s.eligibility = obj.optString("eligibility", "");
                s.documents = obj.optString("documents", "");
                s.ministry = obj.optString("ministry", "");
                s.type = obj.optString("type", "Central");
                s.applyLink = obj.optString("applyLink", "");

                allSchemes.add(s);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void applyFilters() {
        filteredSchemes.clear();

        String query = etSearchScheme.getText().toString().trim().toLowerCase();
        int checkedId = rgFilterType.getCheckedRadioButtonId();

        String typeFilter = null;
        if (checkedId == R.id.rbCentral) {
            typeFilter = "central";
        } else if (checkedId == R.id.rbState) {
            typeFilter = "state";
        }

        for (Scheme s : allSchemes) {
            if (typeFilter != null) {
                if (s.type == null || !s.type.toLowerCase().equals(typeFilter)) {
                    continue;
                }
            }

            if (!TextUtils.isEmpty(query)) {
                String haystack = (s.name + " " + s.shortDescription + " " + s.description).toLowerCase();
                if (!haystack.contains(query)) {
                    continue;
                }
            }

            filteredSchemes.add(s);
        }

        tvAvailableCount.setText(String.valueOf(filteredSchemes.size()));
        renderSchemes();
    }

    private void renderSchemes() {
        layoutSchemes.removeAllViews();

        LayoutInflater inflater = LayoutInflater.from(requireContext());

        for (Scheme scheme : filteredSchemes) {
            View cardView = inflater.inflate(R.layout.item_scheme, layoutSchemes, false);

            TextView tvName = cardView.findViewById(R.id.tvName);
            TextView tvMinistry = cardView.findViewById(R.id.tvMinistry);
            TextView tvShortDescription = cardView.findViewById(R.id.tvShortDescription);
            TextView tvBenefits = cardView.findViewById(R.id.tvBenefits);
            TextView tvEligibility = cardView.findViewById(R.id.tvEligibility);
            RelativeLayout layoutHeader = cardView.findViewById(R.id.layoutHeader);
            eligibilitycard = cardView.findViewById(R.id.eligibilitycard);

            Button btnApply = cardView.findViewById(R.id.btnApply);
            Button btnDetails = cardView.findViewById(R.id.btnDetails);

            tvName.setText(scheme.name);
            tvMinistry.setText(scheme.ministry);
            tvShortDescription.setText(scheme.shortDescription);

            if (!TextUtils.isEmpty(scheme.benefits)) {
                tvBenefits.setText(scheme.benefits);
            } else {
                tvBenefits.setText("Contact department for details.");
            }

            if (!TextUtils.isEmpty(scheme.eligibility)) {
                tvEligibility.setText(scheme.eligibility);
            } else {
                tvEligibility.setText("Check official guidelines.");
            }

            if (!"State".equalsIgnoreCase(scheme.type)) {
                layoutHeader.setBackgroundResource(R.drawable.bg_dashboard_disease);
                eligibilitycard.setBackgroundResource(R.drawable.bg_eligibility_box_1);
                btnApply.setBackgroundResource(R.drawable.bg_button_yellow);
            }


            btnApply.setOnClickListener(v -> {
                if (TextUtils.isEmpty(scheme.applyLink)) {
                    Toast.makeText(requireContext(), "Link not available", Toast.LENGTH_SHORT).show();
                    return;
                }

                String docMessage = "Before proceeding to the application page, please keep the following documents ready:\n\n";

                if (!TextUtils.isEmpty(scheme.documents)) {
                    docMessage += scheme.documents;
                } else {
                    docMessage += "• Aadhaar Card\n• Bank Passbook\n• Land Records (7/12)\n• Identity Proof";
                }

                new AlertDialog.Builder(requireContext())
                        .setTitle("Required Documents")
                        .setMessage(docMessage)
                        .setCancelable(false)
                        .setPositiveButton("OK, Proceed", (dialog, which) -> {
                            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(scheme.applyLink));
                            startActivity(intent);
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            });

            btnDetails.setOnClickListener(v -> showDetailsDialog(scheme));

            layoutSchemes.addView(cardView);
        }
    }

    private void showDetailsDialog(Scheme s) {
        StringBuilder sb = new StringBuilder();
        if (!TextUtils.isEmpty(s.description)) {
            sb.append("Description:\n").append(s.description).append("\n\n");
        }
        if (!TextUtils.isEmpty(s.benefits)) {
            sb.append("Benefits:\n").append(s.benefits).append("\n\n");
        }
        if (!TextUtils.isEmpty(s.amount)) {
            sb.append("Amount:\n").append(s.amount).append("\n\n");
        }
        if (!TextUtils.isEmpty(s.eligibility)) {
            sb.append("Eligibility:\n").append(s.eligibility).append("\n\n");
        }
        if (!TextUtils.isEmpty(s.documents)) {
            sb.append("Documents Required:\n").append(s.documents).append("\n\n");
        }
        if (!TextUtils.isEmpty(s.ministry)) {
            sb.append("Ministry:\n").append(s.ministry).append("\n\n");
        }

        // Note: Apply link is usually clicked via the button, but you can keep it here for reference
        if (!TextUtils.isEmpty(s.applyLink)) {
            sb.append("Apply Link:\n").append(s.applyLink);
        }

        new AlertDialog.Builder(requireContext())
                .setTitle(s.name)
                .setMessage(sb.toString())
                .setPositiveButton("Close", null)
                .show();
    }

    private String loadJSONFromAsset(Context context, String filename) {
        try {
            AssetManager am = context.getAssets();
            InputStream is = am.open(filename);
            byte[] buffer = new byte[is.available()];
            int read = is.read(buffer);
            is.close();
            if (read <= 0) return null;
            return new String(buffer, StandardCharsets.UTF_8);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static class Scheme {
        int id;
        String name;
        String shortDescription;
        String description;
        String amount;
        String benefits;
        String eligibility;
        String documents;
        String ministry;
        String type;
        String applyLink;
    }
}