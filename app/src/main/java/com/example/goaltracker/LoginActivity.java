package com.example.goaltracker;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.ActionCodeSettings;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    private EditText emailEditText, passwordEditText;
    private Button loginButton;
    private ProgressBar progressBar;
    private FirebaseAuth mAuth;
    private SharedPreferences localPrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Apply theme before setting content view
        ThemeManager.applyTheme(this);
        
        // Check for incoming verification links
        checkForVerificationLink();
        
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        localPrefs = getSharedPreferences("LocalAuth", Context.MODE_PRIVATE);

        // Check if user is already signed in (Firebase or local)
        if (isUserLoggedIn()) {
            // User is already signed in, go to MainActivity
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
            finish();
            return;
        }

        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        loginButton = findViewById(R.id.loginButton);
        progressBar = findViewById(R.id.progressBar);

        loginButton.setOnClickListener(v -> handleLogin());
    }
    
    private boolean isUserLoggedIn() {
        // Check only Firebase authentication
        FirebaseUser currentUser = mAuth.getCurrentUser();
        return currentUser != null;
    }

    private void handleLogin() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        // Validate inputs
        if (TextUtils.isEmpty(email)) {
            emailEditText.setError("Email is required");
            return;
        }

        if (TextUtils.isEmpty(password)) {
            passwordEditText.setError("Password is required");
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        loginButton.setEnabled(false);

        // Sign in with Firebase
        mAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this, task -> {
                if (task.isSuccessful()) {
                    // Sign in success
                    handleSuccessfulLogin();
                } else {
                    // Authentication failed
                    handleFailedLogin(task.getException() != null ? 
                        task.getException().getMessage() : 
                        "Authentication failed");
                }
            });
    }
    
    private void handleSuccessfulLogin() {
        // Sign in success
        Log.d(TAG, "signInWithEmail:success");
        FirebaseUser user = mAuth.getCurrentUser();
        progressBar.setVisibility(View.GONE);
        loginButton.setEnabled(true);
        
        // Check if email is verified
        if (user != null && user.isEmailVerified()) {
            // User's email is verified, proceed to main activity
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
            finish();
        } else if (user != null) {
            // User's email is not verified, show dialog and sign them out
            showEmailVerificationRequiredDialog(user.getEmail());
        } else {
            // This should not happen, but just in case
            Toast.makeText(LoginActivity.this, "Login error: User is null", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void showEmailVerificationRequiredDialog(String email) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Email Verification Required");
        builder.setMessage("Please verify your email address before logging in. Check your inbox for a verification link sent to " + email);
        builder.setCancelable(false);
        
        builder.setPositiveButton("OK", (dialog, which) -> {
            mAuth.signOut(); // Sign out until email is verified
        });
        
        builder.setNeutralButton("Resend Email", (dialog, which) -> {
            // Resend verification email
            FirebaseUser user = mAuth.getCurrentUser();
            if (user != null) {
                sendVerificationEmail(user);
            }
            mAuth.signOut(); // Sign out until email is verified
        });
        
        AlertDialog dialog = builder.create();
        dialog.show();
    }
    
    private void sendVerificationEmail(FirebaseUser user) {
        if (user == null) {
            Log.e(TAG, "Cannot send verification email - user is null");
            Toast.makeText(this, "Could not send verification email. Please try again later.", Toast.LENGTH_SHORT).show();
            return;
        }
        
        try {
            // Skip email verification in debug mode to avoid potential issues
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Email verification skipped in debug mode");
                Toast.makeText(LoginActivity.this, 
                              "DEBUG MODE: Email verification skipped", 
                              Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Simple verification with no custom settings
            user.sendEmailVerification()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(LoginActivity.this, 
                                "Verification email sent to " + user.getEmail(), 
                                Toast.LENGTH_LONG).show();
                    } else {
                        Log.e(TAG, "Failed to send verification email", task.getException());
                        Toast.makeText(LoginActivity.this, 
                                "Failed to send verification email. Please try again later.", 
                                Toast.LENGTH_LONG).show();
                    }
                });
        } catch (Exception e) {
            Log.e(TAG, "Exception while sending verification email", e);
            Toast.makeText(this, "Error sending verification email", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void handleFailedLogin(String errorMessage) {
        progressBar.setVisibility(View.GONE);
        loginButton.setEnabled(true);
        
        // Log the failure
        Log.w(TAG, "signInWithEmail:failure: " + errorMessage);
        
        // Check if it's a Firebase configuration error
        if (errorMessage.contains("CONFIGURATION_NOT_FOUND")) {
            // This is likely due to missing reCAPTCHA configuration in Firebase
            String email = emailEditText.getText().toString().trim();
            
            // For demo purposes - check if there's a stored name for this user
            SharedPreferences prefs = getSharedPreferences("UserData", Context.MODE_PRIVATE);
            String userName = prefs.getString("user_name", "");
            
            if (!userName.isEmpty()) {
                // Development mode login
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Development Mode");
                builder.setMessage("Firebase configuration issue detected. Using development mode login to continue.");
                builder.setPositiveButton("Continue", (dialog, which) -> {
                    Toast.makeText(this, "Welcome " + userName + "!", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(LoginActivity.this, MainActivity.class));
                    finish();
                });
                builder.setCancelable(false);
                builder.show();
            } else {
                // No stored user, prompt to sign up
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Account Not Found");
                builder.setMessage("No account found with these credentials. Would you like to sign up?");
                builder.setPositiveButton("Sign Up", (dialog, which) -> {
                    startActivity(new Intent(LoginActivity.this, SignupActivity.class));
                });
                builder.setNegativeButton("Cancel", null);
                builder.show();
            }
        } else {
            // Regular authentication failure
            Toast.makeText(LoginActivity.this, "Login failed: " + errorMessage,
                    Toast.LENGTH_LONG).show();
        }
    }

    public void goToSignup(View view) {
        startActivity(new Intent(this, SignupActivity.class));
    }
    
    private void checkForVerificationLink() {
        FirebaseDynamicLinks.getInstance()
                .getDynamicLink(getIntent())
                .addOnSuccessListener(this, pendingDynamicLinkData -> {
                    if (pendingDynamicLinkData != null) {
                        // Handle the verification link
                        handleVerificationLink(pendingDynamicLinkData.getLink());
                    }
                })
                .addOnFailureListener(this, e -> {
                    Log.e(TAG, "getDynamicLink:onFailure", e);
                });
    }
    
    private void handleVerificationLink(android.net.Uri deepLink) {
        if (deepLink != null) {
            String link = deepLink.toString();
            Log.d(TAG, "Verification link received: " + link);
            
            // Extract the email and verification code if needed
            String actionCode = deepLink.getQueryParameter("oobCode");
            
            if (actionCode != null) {
                // Verify the email with Firebase
                verifyEmailWithCode(actionCode);
            }
        }
    }
    
    private void verifyEmailWithCode(String actionCode) {
        mAuth.applyActionCode(actionCode)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Email successfully verified
                        Toast.makeText(LoginActivity.this, "Email verified successfully!", 
                                Toast.LENGTH_LONG).show();
                                
                        // If user is signed in, refresh the token
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            user.reload().addOnCompleteListener(reloadTask -> {
                                if (reloadTask.isSuccessful()) {
                                    // Enable login button after verification
                                    loginButton.setEnabled(true);
                                }
                            });
                        }
                    } else {
                        // Handle verification error
                        Log.e(TAG, "Error verifying email", task.getException());
                        Toast.makeText(LoginActivity.this, 
                                "Error verifying email: " + (task.getException() != null ? 
                                task.getException().getMessage() : "Unknown error"), 
                                Toast.LENGTH_LONG).show();
                    }
                });
    }
}