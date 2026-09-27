package com.digital.bhoomimitra;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class Onboarding3 extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding_3);

        Button btnBack = findViewById(R.id.btnBack);
        Button btnNext = findViewById(R.id.btnNext);

        btnBack.setOnClickListener(v -> {
            Intent intent = new Intent(this, Onboarding2.class);
            startActivity(intent);
            finish();
        });

        btnNext.setOnClickListener(v -> {
            Intent intent = new Intent(this, Onboarding4.class);
            startActivity(intent);
            finish();
        });
    }
}