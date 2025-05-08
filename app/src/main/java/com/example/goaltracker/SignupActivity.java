package com.example.goaltracker;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
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
    private TextView loginLinkTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Apply theme before setting content view
        ThemeManager.applyTheme(this);
        
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

            // Initialize views
            emailEditText = findViewById(R.id.emailEditText);
            passwordEditText = findViewById(R.id.passwordEditText);
            nameEditText = findViewById(R.id.nameEditText);
            signupButton = findViewById(R.id.signupButton);
            progressBar = findViewById(R.id.progressBar);
            loginLinkTextView = findViewById(R.id.loginLinkTextView);

            if (emailEditText == null || passwordEditText == null || nameEditText == null || 
                signupButton == null || progressBar == null) {
                Log.e(TAG, "One or more views are null");
                Toast.makeText(this, "Error initializing views", Toast.LENGTH_SHORT).show();
                return;
            }

            // Setup signup button
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

            // Setup login link
            if (loginLinkTextView != null) {
                loginLinkTextView.setOnClickListener(v -> {
                    startActivity(new Intent(SignupActivity.this, LoginActivity.class));
                    finish();
                });
            }
            
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
        
        // Try to check if Firebase configuration is available
        try {
            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            handleSuccessfulSignup(email, name);
                        } else {
                            handleFailedSignup(task);
                        }
                    });
        } catch (Exception e) {
            Log.e(TAG, "Firebase exception: " + e.getMessage());
            // Fallback to local authentication
            createLocalUser(email, password, name);
        }
    }

    private void createLocalUser(String email, String password, String name) {
        Log.d(TAG, "Using local authentication as fallback");
        // Store credentials in SharedPreferences (not secure, just for development)
        SharedPreferences prefs = getSharedPreferences("LocalAuth", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("user_email", email);
        editor.putString("user_password", password);
        editor.putString("user_name", name);
        editor.putBoolean("is_logged_in", true);
        editor.apply();
        
        Toast.makeText(this, "Local account created (Firebase unavailable)", Toast.LENGTH_LONG).show();
        
        // Navigate to MainActivity
        Intent intent = new Intent(SignupActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void handleSuccessfulSignup(String email, String name) {
        Log.d(TAG, "User created successfully");
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            // Set display name
            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                    .setDisplayName(name)
                    .build();
            
            user.updateProfile(profileUpdates)
                    .addOnCompleteListener(profileTask -> {
                        if (profileTask.isSuccessful()) {
                            Log.d(TAG, "User profile updated with display name");
                        } else {
                            Log.w(TAG, "Error updating user profile", profileTask.getException());
                        }
                    });
            
            Log.d(TAG, "User object obtained, saving to Firestore");
            saveUserToFirestore(user.getUid(), name, email);
            
            Toast.makeText(SignupActivity.this, "Account created successfully!", Toast.LENGTH_SHORT).show();
            progressBar.setVisibility(View.GONE);
            signupButton.setEnabled(true);
            
            // Navigate to MainActivity
            Intent intent = new Intent(SignupActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        } else {
            Log.e(TAG, "User is null after successful creation");
            progressBar.setVisibility(View.GONE);
            signupButton.setEnabled(true);
            Toast.makeText(this, "Failed to create user", Toast.LENGTH_SHORT).show();
        }
    }

    private void handleFailedSignup(com.google.android.gms.tasks.Task task) {
        progressBar.setVisibility(View.GONE);
        signupButton.setEnabled(true);

        String error = task.getException() != null ? task.getException().getMessage() : "Unknown error";
        Log.e(TAG, "Signup failed: " + error);
        
        if (error.contains("email already in use")) {
            emailEditText.setError("Email already registered");
        } else if (error.contains("CONFIGURATION_NOT_FOUND")) {
            // Fallback to local authentication
            String email = emailEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();
            String name = nameEditText.getText().toString().trim();
            createLocalUser(email, password, name);
        } else if (error.contains("network error") || error.contains("timeout")) {
            Toast.makeText(this, "Network error. Please check your internet connection and try again.", 
                Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "Signup failed: " + error,
                    Toast.LENGTH_LONG).show();
        }
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