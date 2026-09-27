package com.digital.bhoomimitra;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class Register extends AppCompatActivity {

    private FirebaseAuth auth;
    private TextInputEditText emailRegister, passwordRegister, confirmPassword;
    private MaterialButton signUpButton;
    private android.widget.TextView alreadyHaveAccount;
    private TextInputEditText nameRegister;
    private AutoCompleteTextView autoCompleteLanguage;
    private String selectedLanguageCode = "en";



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        auth = FirebaseAuth.getInstance();

        emailRegister = findViewById(R.id.emailRegister);
        passwordRegister = findViewById(R.id.passwordRegister);
        confirmPassword = findViewById(R.id.confirmPassword);
        signUpButton = findViewById(R.id.signUpButton);
        alreadyHaveAccount = findViewById(R.id.alreadyHaveAccount);
        nameRegister = findViewById(R.id.nameRegister);
        autoCompleteLanguage = findViewById(R.id.autoCompleteLanguage);

        setupLanguageDropdown();

        signUpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // read values
                String name = nameRegister.getText() != null ? nameRegister.getText().toString().trim() : "";
                String email = emailRegister.getText() != null ? emailRegister.getText().toString().trim() : "";
                String password = passwordRegister.getText() != null ? passwordRegister.getText().toString().trim() : "";
                String cPassword = confirmPassword.getText() != null ? confirmPassword.getText().toString().trim() : "";


                if (TextUtils.isEmpty(name)) {
                    nameRegister.setError("Name cannot be empty");
                    nameRegister.requestFocus();
                    return;
                }

                if (TextUtils.isEmpty(email)) {
                    emailRegister.setError("Email cannot be empty");
                    emailRegister.requestFocus();
                    return;
                }

                if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    emailRegister.setError("Enter a valid email");
                    emailRegister.requestFocus();
                    return;
                }

                if (TextUtils.isEmpty(password)) {
                    passwordRegister.setError("Password cannot be empty");
                    passwordRegister.requestFocus();
                    return;
                }

                if (password.length() < 6) {
                    passwordRegister.setError("Password must be at least 6 characters");
                    passwordRegister.requestFocus();
                    return;
                }

                if (TextUtils.isEmpty(cPassword)) {
                    confirmPassword.setError("Confirm password cannot be empty");
                    confirmPassword.requestFocus();
                    return;
                }

                if (!password.equals(cPassword)) {
                    confirmPassword.setError("Passwords do not match");
                    confirmPassword.requestFocus();
                    return;
                }

                signUpButton.setEnabled(false);
                signUpButton.setText("Registering...");

                // create user with email and password
                auth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener(Register.this, new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                signUpButton.setEnabled(true);
                                signUpButton.setText("Sign up");

                                if (task.isSuccessful()) {
                                    String uid = task.getResult().getUser().getUid();
                                    DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("users");

                                    dbRef.child(uid).child("name").setValue(name);
                                    dbRef.child(uid).child("email").setValue(email);
                                    dbRef.child(uid).child("language").setValue(selectedLanguageCode);

                                    getSharedPreferences("bhoomimitra_user", MODE_PRIVATE)
                                            .edit()
                                            .putString("farmer_name", name)
                                            .apply();

                                    LocaleHelper.setLocale(Register.this, selectedLanguageCode);
                                    Toast.makeText(Register.this, "SignUp Successful", Toast.LENGTH_SHORT).show();

                                    Intent intent = new Intent(Register.this, Dashboard.class);
                                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                    startActivity(intent);
                                    finish();

                                } else {
                                    String err = "SignUp Failed";
                                    if (task.getException() != null && task.getException().getMessage() != null) {
                                        err = err + ": " + task.getException().getMessage();
                                    }
                                    Toast.makeText(Register.this, err, Toast.LENGTH_LONG).show();
                                }
                            }
                        });
            }
        });

        alreadyHaveAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Register.this, Login.class));
                finish();
            }
        });
    }

    private void setupLanguageDropdown() {
        // Must match the arrays in your Profile.java
        String[] languages = {"English", "हिन्दी (Hindi)", "मराठी (Marathi)","தமிழ் (Tamil)","বাংলা (Bengali)","ಕನ್ನಡ (Kannada)","ગુજરાતી (Gujarati)","മലയാളം (Malayalam)","ਪੰਜਾਬੀ (Punjabi)","ଓଡ଼ିଆ (Odia)"};
        String[] codes = {"en", "hi", "mr","ta","bn","kn","gu","ml","pa","or"};

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, languages);
        autoCompleteLanguage.setAdapter(adapter);

        autoCompleteLanguage.setText(languages[0], false);

        autoCompleteLanguage.setOnItemClickListener((parent, view, position, id) -> {
            selectedLanguageCode = codes[position];
        });
    }
}
