package com.digital.bhoomimitra;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class Onboarding4 extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding_4);

        Button btnBack = findViewById(R.id.btnBack);
        Button btnFinish = findViewById(R.id.btnFinish);

        // Click listener for the Back button
        btnBack.setOnClickListener(v -> {
            Intent intent = new Intent(this, Onboarding3.class);
            startActivity(intent);
            finish();
        });

        // Click listener for the Finish button
        btnFinish.setOnClickListener(v -> {
            // Save state to indicate onboarding is complete
            getSharedPreferences("onboardingPrefs", MODE_PRIVATE)
                    .edit()
                    .putBoolean("onboardingShown", true)
                    .apply();

            Intent intent = new Intent(this, Dashboard.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });
    }
}