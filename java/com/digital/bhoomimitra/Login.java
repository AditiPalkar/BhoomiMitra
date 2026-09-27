package com.digital.bhoomimitra;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

public class Login extends AppCompatActivity {

    EditText emailLogin, passwordLogin;
    Button signInButton;
    TextView createAccountText;
    FirebaseAuth auth;
    CheckBox checkboxRemember;
    SharedPreferences preferences;
    SharedPreferences.Editor editor;

    @Override
    protected void onStart() {
        super.onStart();

        // If user already logged in
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            SharedPreferences onboardingPrefs = getSharedPreferences("onboardingPrefs", MODE_PRIVATE);
            boolean onboardingShown = onboardingPrefs.getBoolean("onboardingShown", false);

            if (!onboardingShown) {
                // Show onboarding
                startActivity(new Intent(Login.this, Onboarding1.class));
            } else {
                // Go directly to dashboard
                startActivity(new Intent(Login.this, Dashboard.class));
            }

            finish(); // Close login screen
        }
    }

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences themePrefs = getSharedPreferences("themePrefs", MODE_PRIVATE);
        boolean isDarkMode = themePrefs.getBoolean("dark_mode", false);
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        auth = FirebaseAuth.getInstance();
        emailLogin = findViewById(R.id.emailLogin);
        passwordLogin = findViewById(R.id.passwordLogin);
        signInButton = findViewById(R.id.signInButton);
        createAccountText = findViewById(R.id.createAccountText);
        checkboxRemember = findViewById(R.id.checkbox_remember);

        // SharedPreferences for Remember Me
        preferences = getSharedPreferences("loginPrefs", MODE_PRIVATE);
        editor = preferences.edit();
        loadSavedCredentials();

        signInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = emailLogin.getText().toString().trim();
                String pass = passwordLogin.getText().toString().trim();

                if (!email.isEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    if (!pass.isEmpty()) {
                        // Save login if checked
                        if (checkboxRemember.isChecked()) {
                            editor.putBoolean("remember", true);
                            editor.putString("email", email);
                            editor.putString("password", pass);
                            editor.apply();
                        } else {
                            editor.clear().apply();
                        }

                        // Firebase login
                        auth.signInWithEmailAndPassword(email, pass)
                                .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                                    @Override
                                    public void onSuccess(AuthResult authResult) {
                                        Toast.makeText(Login.this, "Login Successful", Toast.LENGTH_SHORT).show();

                                        String currentEmail = emailLogin.getText().toString().trim();
                                        String sanitizedEmail = currentEmail.replace(".", ",");

                                        SharedPreferences sp = getSharedPreferences("bhoomimitra_user", MODE_PRIVATE);
                                        String savedName = sp.getString("user_" + sanitizedEmail + "_name", null);

                                        if (savedName != null && !savedName.isEmpty()) {
                                            sp.edit()
                                                    .putString("farmer_name", savedName)
                                                    .putString("user_email", currentEmail)
                                                    .apply();
                                        } else {
                                            sp.edit()
                                                    .putString("farmer_name", "Farmer")
                                                    .putString("user_email", currentEmail)
                                                    .apply();
                                        }

                                        SharedPreferences prefs = getSharedPreferences("onboardingPrefs", MODE_PRIVATE);
                                        boolean onboardingShown = prefs.getBoolean("onboardingShown", false);

                                        Intent intent;
                                        if (!onboardingShown) {
                                            prefs.edit().putBoolean("onboardingShown", true).apply();
                                            intent = new Intent(Login.this, Onboarding1.class);
                                        } else {
                                            intent = new Intent(Login.this, Dashboard.class);
                                        }
                                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                        startActivity(intent);
                                        finish();
                                    }

                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Toast.makeText(Login.this, "Login Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                    } else {
                        passwordLogin.setError("Password cannot be empty");
                    }
                } else if (email.isEmpty()) {
                    emailLogin.setError("Email cannot be empty");
                } else {
                    emailLogin.setError("Enter a valid email");
                }
            }
        });

        createAccountText.setOnClickListener(v ->
                startActivity(new Intent(Login.this, Register.class)));
    }

    private void loadSavedCredentials() {
        boolean remember = preferences.getBoolean("remember", false);
        if (remember) {
            String savedEmail = preferences.getString("email", "");
            String savedPassword = preferences.getString("password", "");
            emailLogin.setText(savedEmail);
            passwordLogin.setText(savedPassword);
            checkboxRemember.setChecked(true);
        }
    }
}
