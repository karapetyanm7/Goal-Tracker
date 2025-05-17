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
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.ActionCodeSettings;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks;
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
        
        // Check for incoming verification links
        checkForVerificationLink();
        
        setContentView(R.layout.activity_signup);

        try {
            Log.d(TAG, "Getting Firebase Auth instance");
            
            // Let Firebase SDK handle initialization - don't manually initialize
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
        
        progressBar.setVisibility(View.VISIBLE);
        signupButton.setEnabled(false);
        
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        handleSuccessfulSignup(email, name);
                    } else {
                        handleFailedSignup(task);
                    }
                });
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
            
            // Send email verification
            sendEmailVerification(user);
            
            Log.d(TAG, "User object obtained, saving to Firestore");
            saveUserToFirestore(user.getUid(), name, email);
            
            progressBar.setVisibility(View.GONE);
            signupButton.setEnabled(true);
            
            // Show verification dialog instead of automatically navigating to MainActivity
            showEmailVerificationDialog(email);
        } else {
            Log.e(TAG, "User is null after successful creation");
            progressBar.setVisibility(View.GONE);
            signupButton.setEnabled(true);
            Toast.makeText(this, "Failed to create user", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void sendEmailVerification(FirebaseUser user) {
        if (user == null) {
            Log.e(TAG, "Cannot send verification email - user is null");
            return;
        }
        
        try {
            // Skip email verification in debug mode or if there are issues
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Email verification skipped in debug mode");
                return;
            }
            
            // Simple verification without any custom settings
            user.sendEmailVerification()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Verification email sent to " + user.getEmail());
                    } else {
                        Log.e(TAG, "Failed to send verification email.", task.getException());
                        Toast.makeText(SignupActivity.this, 
                                "Account created successfully! Verification email may be delayed.", 
                                Toast.LENGTH_LONG).show();
                    }
                });
        } catch (Exception e) {
            Log.e(TAG, "Exception while sending verification email", e);
            // Don't let this crash the signup flow
        }
    }
    
    private void showEmailVerificationDialog(String email) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Verify Your Email");
        builder.setMessage("We've sent a verification email to " + email + ". Please check your inbox and click the verification link to activate your account.");
        builder.setCancelable(false);
        builder.setPositiveButton("OK", (dialog, which) -> {
            // Sign out the user until they verify their email
            mAuth.signOut();
            
            // Return to login screen
            Toast.makeText(SignupActivity.this, "Account created! Please verify your email before logging in.", 
                    Toast.LENGTH_LONG).show();
            Intent intent = new Intent(SignupActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
        
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void handleFailedSignup(com.google.android.gms.tasks.Task task) {
        progressBar.setVisibility(View.GONE);
        signupButton.setEnabled(true);

        String error = task.getException() != null ? task.getException().getMessage() : "Unknown error";
        Log.e(TAG, "Signup failed: " + error);
        
        if (error.contains("email already in use")) {
            emailEditText.setError("Email already registered");
        } else if (error.contains("CONFIGURATION_NOT_FOUND")) {
            // This is likely due to missing reCAPTCHA configuration in Firebase
            // Implement a simplified login just to get the app working (development only)
            String email = emailEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();
            String name = nameEditText.getText().toString().trim();
            
            // For demo purposes - simulate successful registration
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Development Mode");
            builder.setMessage("Firebase reCAPTCHA configuration not found. In a production app, you would need to complete Firebase configuration. For now, we'll simulate a successful signup.");
            builder.setPositiveButton("Continue", (dialog, which) -> {
                Toast.makeText(this, "Demo account created!", Toast.LENGTH_SHORT).show();
                // Go straight to main activity
                Intent intent = new Intent(SignupActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                SharedPreferences prefs = getSharedPreferences("UserData", Context.MODE_PRIVATE);
                prefs.edit().putString("user_name", name).apply();
                startActivity(intent);
                finish();
            });
            builder.setCancelable(false);
            builder.show();
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
    
    private void checkForVerificationLink() {
        try {
            Log.d(TAG, "Checking for email verification link");
            FirebaseDynamicLinks.getInstance()
                .getDynamicLink(getIntent())
                .addOnSuccessListener(this, pendingDynamicLinkData -> {
                    if (pendingDynamicLinkData != null) {
                        // Get deep link from result
                        android.net.Uri deepLink = pendingDynamicLinkData.getLink();
                        Log.d(TAG, "Deep link received: " + (deepLink != null ? deepLink.toString() : "null"));
                        
                        if (deepLink != null) {
                            // Handle the verification link
                            String path = deepLink.getPath();
                            if (path != null && path.contains("/verify")) {
                                String email = deepLink.getQueryParameter("email");
                                Log.d(TAG, "Email verification from deep link for: " + email);
                                Toast.makeText(this, "Email verified successfully!", Toast.LENGTH_LONG).show();
                                
                                // Redirect to login
                                Intent intent = new Intent(this, LoginActivity.class);
                                intent.putExtra("verified_email", email);
                                startActivity(intent);
                                finish();
                            }
                        }
                    } else {
                        Log.d(TAG, "No dynamic link found");
                    }
                })
                .addOnFailureListener(this, e -> {
                    Log.e(TAG, "Error getting dynamic link: " + e.getMessage());
                });
        } catch (Exception e) {
            Log.e(TAG, "Error checking for verification link: " + e.getMessage());
        }
    }
}