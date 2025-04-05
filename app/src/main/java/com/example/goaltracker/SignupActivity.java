package com.example.goaltracker;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class SignupActivity extends AppCompatActivity {
    private static final String TAG = "SignupActivity";
    private FirebaseAuth mAuth;
    private EditText emailEditText, passwordEditText, nameEditText;
    private Button signupButton;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        try {
            Log.d(TAG, "Starting Firebase initialization");
            
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this);
                Log.d(TAG, "Firebase initialized: " + FirebaseApp.getInstance().getName());
            } else {
                Log.d(TAG, "Firebase already initialized");
            }

        mAuth = FirebaseAuth.getInstance();
            Log.d(TAG, "FirebaseAuth instance created");

        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        nameEditText = findViewById(R.id.nameEditText);
        signupButton = findViewById(R.id.signupButton);
        progressBar = findViewById(R.id.progressBar);

            if (emailEditText == null || passwordEditText == null || nameEditText == null || 
                signupButton == null || progressBar == null) {
                Log.e(TAG, "One or more views are null");
                Toast.makeText(this, "Error initializing views", Toast.LENGTH_SHORT).show();
                return;
            }

            signupButton.setOnClickListener(v -> {
                if (!isNetworkAvailable()) {
                    Log.e(TAG, "No network connection available");
                    Toast.makeText(this, "No internet connection. Please check your network and try again.", 
                        Toast.LENGTH_LONG).show();
                    return;
                }
                Log.d(TAG, "Network connection available");

                String email = emailEditText.getText().toString().trim();
                String password = passwordEditText.getText().toString().trim();
                String name = nameEditText.getText().toString().trim();

                Log.d(TAG, "Attempting signup with email: " + email);

                if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    emailEditText.setError("Enter valid email");
            return;
        }

                if (TextUtils.isEmpty(password) || password.length() < 6) {
                    passwordEditText.setError("Password must be ≥6 characters");
            return;
        }

                if (TextUtils.isEmpty(name)) {
                    nameEditText.setError("Enter your name");
            return;
        }

                progressBar.setVisibility(View.VISIBLE);
                signupButton.setEnabled(false);

                createUserWithEmail(email, password, name);
            });
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate: " + e.getMessage(), e);
            Toast.makeText(this, "Error initializing: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isNetworkAvailable() {
        try {
            ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            if (connectivityManager != null) {
                NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
                boolean isConnected = activeNetworkInfo != null && activeNetworkInfo.isConnected();
                Log.d(TAG, "Network available: " + isConnected);
                return isConnected;
            }
            Log.e(TAG, "ConnectivityManager is null");
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Error checking network availability: " + e.getMessage());
            return false;
        }
    }

    private void createUserWithEmail(String email, String password, String name) {
        Log.d(TAG, "Creating user with email: " + email);
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "User created successfully");
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            Log.d(TAG, "User object obtained, saving to Firestore");
                            saveUserToFirestore(user.getUid(), name, email);
                            

                            progressBar.setVisibility(View.GONE);
                            signupButton.setEnabled(true);
                            startActivity(new Intent(SignupActivity.this, MainActivity.class));
                            finish();
                        } else {
                            Log.e(TAG, "User is null after successful creation");
                            progressBar.setVisibility(View.GONE);
                            signupButton.setEnabled(true);
                            Toast.makeText(this, "Failed to create user", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        progressBar.setVisibility(View.GONE);
                        signupButton.setEnabled(true);

                        String error = task.getException() != null ? task.getException().getMessage() : "Unknown error";
                        Log.e(TAG, "Signup failed: " + error);
                        
                        if (error.contains("email already in use")) {
                            emailEditText.setError("Email already registered");
                        } else if (error.contains("network error") || error.contains("timeout") || 
                                  error.contains("CONFIGURATION_NOT_FOUND")) {
                            Toast.makeText(this, "Firebase configuration error. Please try again later.", 
                                Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(this, "Signup failed: " + error,
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    private void saveUserToFirestore(String userId, String name, String email) {
        Log.d(TAG, "Saving user data to Firestore");
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> user = new HashMap<>();
        user.put("name", name);
        user.put("email", email);
        user.put("createdAt", FieldValue.serverTimestamp());

        db.collection("users").document(userId)
                .set(user)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "User data saved to Firestore"))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error saving user data to Firestore: " + e.getMessage());
                    Toast.makeText(this, "Error saving user data", Toast.LENGTH_SHORT).show();
                });
    }
}