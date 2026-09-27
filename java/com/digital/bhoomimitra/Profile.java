package com.digital.bhoomimitra;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.io.File;


public class Profile extends Fragment {

    private TextView tvName, tvEmail;
    private CardView cardResetData, cardAbout, cardChangeLanguage;
    private MaterialButton btnLogout;

    private FirebaseAuth auth;
    private SharedPreferences prefs;

    // List of SharedPreferences names used across the app (add any other names you used)
    private final String[] PREF_NAMES_TO_CLEAR = new String[]{
            "bhoomimitra_user",
            "themePrefs",
            "app_settings",
            "onboardingPrefs",
    };

    private final String[] languages = {"English", "हिन्दी (Hindi)", "मराठी (Marathi)","தமிழ் (Tamil)","বাংলা (Bengali)","ಕನ್ನಡ (Kannada)","ગુજરાતી (Gujarati)","മലയാളം (Malayalam)","ਪੰਜਾਬੀ (Punjabi)","ଓଡ଼ିଆ (Odia)"};
    private final String[] langCodes = {"en", "hi", "mr","ta","bn","kn","gu","ml","pa","or"};

    // layout resource name - replace if your layout file has a different name
    private static final int LAYOUT_RES = R.layout.fragment_profile;

    public Profile() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        // inflate the layout
        View view = inflater.inflate(LAYOUT_RES, container, false);

        // init views (IDs based on your XML)
        tvName = view.findViewById(R.id.tvName);
        tvEmail = view.findViewById(R.id.tvEmail);
        cardResetData = view.findViewById(R.id.card_reset_data);
        cardAbout = view.findViewById(R.id.card_about);
        btnLogout = view.findViewById(R.id.button_logout);
        cardChangeLanguage = view.findViewById(R.id.card_change_language);

        // Firebase and prefs
        auth = FirebaseAuth.getInstance();
        prefs = requireActivity().getSharedPreferences("bhoomimitra_user", Context.MODE_PRIVATE);

        // load profile data
        loadProfile();

        // listeners
        cardChangeLanguage.setOnClickListener(v -> showLanguageChangeDialog());
        cardAbout.setOnClickListener(v -> showAboutDialog());

        cardResetData.setOnClickListener(v -> confirmAndResetAllData());

        btnLogout.setOnClickListener(v -> confirmAndLogout());

        return view;
    }

    private void loadProfile() {
        // Priority: SharedPreferences -> FirebaseUser -> defaults
        String name = prefs.getString("farmer_name", "");
        String email = prefs.getString("farmer_email", "");

        FirebaseUser user = auth.getCurrentUser();
        if ((name == null || name.isEmpty()) && user != null && user.getDisplayName() != null) {
            name = user.getDisplayName();
        }
        if ((email == null || email.isEmpty()) && user != null && user.getEmail() != null) {
            email = user.getEmail();
        }

        if (name == null || name.isEmpty()) name = "Farmer";
        if (email == null || email.isEmpty()) email = "—";

        tvName.setText(name);
        tvEmail.setText(email);
    }

    private void showLanguageChangeDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(requireContext());
        builder.setTitle("Select Language / भाषा चुनें");

        builder.setItems(languages, (dialog, which) -> {
            String selectedCode = langCodes[which];

            // 1. Save language
            LocaleHelper.setLocale(requireContext(), selectedCode);

            // 2. Restart App to apply changes
            android.content.Intent intent = new android.content.Intent(requireActivity(), Dashboard.class);
            intent.setFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK | android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        builder.show();
    }

    private void showAboutDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("About Bhoomi Mitra")
                .setMessage("Bhoomi Mitra\n\nSmart Agriculture assistant app.\nVersion 1.0\n\nDeveloped by your team.")
                .setPositiveButton("OK", null)
                .show();
    }

    private void confirmAndLogout() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    auth.signOut();

                    // Optionally clear only auth-related prefs (not whole app)
                    requireActivity().getSharedPreferences("bhoomimitra_user", Context.MODE_PRIVATE)
                            .edit().remove("farmer_name").remove("farmer_email").apply();

                    // Go to Login activity
                    Intent i = new Intent(requireActivity(), Login.class);
                    i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(i);
                    requireActivity().finish();
                    Toast.makeText(requireContext(), "Logged out", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("No", null)
                .show();
    }

    private void confirmAndResetAllData() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Reset App Data")
                .setMessage("This will clear all local app data (preferences, cache, files) and sign you out. This action cannot be undone. Continue?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    performFullReset();
                })
                .setNegativeButton("No", null)
                .show();
    }

    private void performFullReset() {
        Context ctx = requireContext();

        // 1) Sign out Firebase
        try {
            auth.signOut();
        } catch (Exception ignored) { }

        // 2) Clear known SharedPreferences
        for (String prefName : PREF_NAMES_TO_CLEAR) {
            try {
                ctx.getSharedPreferences(prefName, Context.MODE_PRIVATE).edit().clear().apply();
            } catch (Exception ignored) { }
        }

        // 3) Clear app cache and files (best-effort)
        try {
            // delete cache dir contents
            File cacheDir = ctx.getCacheDir();
            if (cacheDir != null && cacheDir.exists()) {
                deleteRecursive(cacheDir);
            }

            // delete files dir contents
            File filesDir = ctx.getFilesDir();
            if (filesDir != null && filesDir.exists()) {
                deleteRecursive(filesDir);
            }

            // delete databases (best-effort)
            String[] databases = ctx.databaseList();
            if (databases != null) {
                for (String dbName : databases) {
                    try {
                        ctx.deleteDatabase(dbName);
                    } catch (Exception ignored) { }
                }
            }
        } catch (Exception ex) {
            // not critical; continue
            ex.printStackTrace();
        }

        // 4) Restart app at Login (clear task)
        Intent i = new Intent(ctx, Login.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        requireActivity().finish();

        Toast.makeText(ctx, "App data reset. Restarting...", Toast.LENGTH_SHORT).show();
    }

    // recursive delete helper
    private void deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory == null || !fileOrDirectory.exists()) return;
        if (fileOrDirectory.isDirectory()) {
            File[] children = fileOrDirectory.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursive(child);
                }
            }
        }
        // try delete; ignore failure
        try {
            fileOrDirectory.delete();
        } catch (Exception ignored) { }
    }
}
