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
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

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
        // Check Firebase authentication
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            return true;
        }
        
        // Check local authentication
        return localPrefs.getBoolean("is_logged_in", false);
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

        // Try Firebase authentication first
        try {
            // Sign in with Firebase
            mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Sign in success
                        handleSuccessfulLogin();
                    } else {
                        // If sign in fails, try local auth as fallback
                        if (task.getException() != null && 
                            task.getException().getMessage() != null && 
                            task.getException().getMessage().contains("CONFIGURATION_NOT_FOUND")) {
                            tryLocalLogin(email, password);
                        } else {
                            // True authentication failure
                            handleFailedLogin(task.getException() != null ? 
                                task.getException().getMessage() : 
                                "Authentication failed");
                        }
                    }
                });
        } catch (Exception e) {
            // Firebase is likely not configured, try local authentication
            Log.e(TAG, "Firebase exception: " + e.getMessage());
            tryLocalLogin(email, password);
        }
    }
    
    private void tryLocalLogin(String email, String password) {
        // Check against stored local credentials
        String storedEmail = localPrefs.getString("user_email", "");
        String storedPassword = localPrefs.getString("user_password", "");
        
        if (email.equals(storedEmail) && password.equals(storedPassword)) {
            // Local login success
            Toast.makeText(this, "Logged in with local account", Toast.LENGTH_SHORT).show();
            localPrefs.edit().putBoolean("is_logged_in", true).apply();
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
            finish();
        } else {
            handleFailedLogin("Invalid email or password");
        }
    }
    
    private void handleSuccessfulLogin() {
        // Sign in success
        Log.d(TAG, "signInWithEmail:success");
        FirebaseUser user = mAuth.getCurrentUser();
        progressBar.setVisibility(View.GONE);
        loginButton.setEnabled(true);
                        startActivity(new Intent(LoginActivity.this, MainActivity.class));
                        finish();
                    }
    
    private void handleFailedLogin(String errorMessage) {
        progressBar.setVisibility(View.GONE);
        loginButton.setEnabled(true);
        
        // If sign in fails, display a message to the user.
        Log.w(TAG, "signInWithEmail:failure: " + errorMessage);
        Toast.makeText(LoginActivity.this, "Login failed: " + errorMessage,
                Toast.LENGTH_LONG).show();
    }

    public void goToSignup(View view) {
        startActivity(new Intent(this, SignupActivity.class));
    }
}