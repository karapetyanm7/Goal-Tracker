package com.example.goaltracker;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.graphics.Color;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;

public class ProfileActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    
    // Profile UI elements
    private TextView nameTextView;
    private TextView emailTextView;
    private View nameDisplayLayout;
    private View nameEditLayout;
    private EditText editNameText;
    private ImageButton editNameButton;
    private Button saveNameButton;
    private Button logoutButton;
    private ImageButton backButton;
    
    // User data
    private String userName;
    private String userEmail;
    
    // Preferences
    private SharedPreferences sharedPreferences;
    private boolean isDarkMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Get current theme settings before setting the content view
        sharedPreferences = getSharedPreferences(ThemeManager.PREF_NAME, Context.MODE_PRIVATE);
        isDarkMode = ThemeManager.isDarkMode(this);
        
        // Apply theme before setting content view
        ThemeManager.applyTheme(this);
        
        setContentView(R.layout.activity_profile);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUser = mAuth.getCurrentUser();

        // Initialize profile UI components
        nameTextView = findViewById(R.id.profile_name);
        emailTextView = findViewById(R.id.profile_email);
        nameDisplayLayout = findViewById(R.id.name_display_layout);
        nameEditLayout = findViewById(R.id.name_edit_layout);
        editNameText = findViewById(R.id.edit_name_text);
        editNameButton = findViewById(R.id.edit_name_button);
        saveNameButton = findViewById(R.id.save_name_button);
        logoutButton = findViewById(R.id.profile_logout_button);
        backButton = findViewById(R.id.profile_back_button);
        
        // Load user data
        loadUserData();
        
        // Set up click listeners
        setupClickListeners();
    }
    
    @Override
    public void onBackPressed() {
        if (nameEditLayout.getVisibility() == View.VISIBLE) {
            // If in edit mode, cancel editing
            toggleEditMode(false);
        } else {
            super.onBackPressed();
        }
    }
    
    private void setupClickListeners() {
        // Back button
        backButton.setOnClickListener(v -> onBackPressed());
        
        // Edit name button
        editNameButton.setOnClickListener(v -> toggleEditMode(true));
        
        // Save name button
        saveNameButton.setOnClickListener(v -> saveUserName());
        
        // Logout button
        logoutButton.setOnClickListener(v -> logout());
        
        // Apply theme colors to buttons
        applyThemeColors();
    }
    
    /**
     * Apply theme colors to UI elements
     */
    private void applyThemeColors() {
        // Get the primary color from ThemeManager
        int primaryColor = ThemeManager.getPrimaryColor(this);
        
        // Apply to buttons
        saveNameButton.setBackgroundColor(primaryColor);
        logoutButton.setBackgroundColor(primaryColor);
        
        // Apply background color to the main layout
        View rootView = findViewById(android.R.id.content);
        if (rootView != null) {
            View mainLayout = ((ViewGroup) rootView).getChildAt(0);
            if (mainLayout != null) {
                // Apply a tinted background that's lighter than the primary color
                int lightPrimaryColor = lightenColor(primaryColor, 0.8f);
                mainLayout.setBackgroundColor(lightPrimaryColor);
            }
        }
        
        // Set the status bar color to match the primary theme color
        // Clear any existing flags first to ensure proper coloring
        getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().setStatusBarColor(primaryColor);
        
        // Apply navigation button styling, but not to editNameButton to preserve its icon
        ThemeManager.applyNavigationButtonStyle(backButton);
        
        // Make sure edit button shows the icon properly
        if (editNameButton != null) {
            editNameButton.setImageResource(R.drawable.edit_icon);
            editNameButton.setBackgroundColor(Color.TRANSPARENT);
            editNameButton.setScaleType(android.widget.ImageView.ScaleType.FIT_CENTER);
            editNameButton.setPadding(8, 8, 8, 8);
        }
    }
    
    // Helper method to create a lighter version of a color
    private int lightenColor(int color, float factor) {
        int red = (int) ((android.graphics.Color.red(color) * (1 - factor) + 255 * factor));
        int green = (int) ((android.graphics.Color.green(color) * (1 - factor) + 255 * factor));
        int blue = (int) ((android.graphics.Color.blue(color) * (1 - factor) + 255 * factor));
        return android.graphics.Color.rgb(red, green, blue);
    }
    
    private void toggleEditMode(boolean editing) {
        if (editing) {
            // Show edit layout
            nameDisplayLayout.setVisibility(View.GONE);
            nameEditLayout.setVisibility(View.VISIBLE);
            editNameText.setText(userName);
            editNameText.requestFocus();
        } else {
            // Show display layout
            nameDisplayLayout.setVisibility(View.VISIBLE);
            nameEditLayout.setVisibility(View.GONE);
        }
    }
    
    private void loadUserData() {
        if (currentUser != null) {
            userEmail = currentUser.getEmail();
            emailTextView.setText(userEmail);
            
            // First check if we have the name stored in SharedPreferences
            String savedName = sharedPreferences.getString("user_name", "");
            if (!savedName.isEmpty()) {
                userName = savedName;
                nameTextView.setText(userName);
                return; // Use the locally saved name as the source of truth
            }
            
            // If no local name, try to get from Firebase
            String uid = currentUser.getUid();
            db.collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        userName = documentSnapshot.getString("name");
                        if (userName != null && !userName.isEmpty()) {
                            nameTextView.setText(userName);
                            // Also save to SharedPreferences for future use
                            sharedPreferences.edit().putString("user_name", userName).apply();
                        } else {
                            // Use string resource with fallback to hardcoded value
                            try {
                                nameTextView.setText(R.string.default_username);
                                userName = getString(R.string.default_username);
                            } catch (Exception e) {
                                // Fallback if resource is not found
                                nameTextView.setText("User");
                                userName = "User";
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(ProfileActivity.this, "Error loading user data", Toast.LENGTH_SHORT).show();
                    // Use string resource with fallback to hardcoded value
                    try {
                        nameTextView.setText(R.string.default_username);
                        userName = getString(R.string.default_username);
                    } catch (Exception ex) {
                        // Fallback if resource is not found
                        nameTextView.setText("User");
                        userName = "User";
                    }
                });
        }
    }
    
    private void saveUserName() {
        String newName = editNameText.getText().toString().trim();
        
        if (newName.isEmpty()) {
            editNameText.setError("Name cannot be empty");
            return;
        }
        
        // Show a progress toast to indicate something is happening
        Toast.makeText(ProfileActivity.this, "Saving name...", Toast.LENGTH_SHORT).show();
        Log.d("ProfileActivity", "Attempting to save name: " + newName);
        
        // First update the UI regardless of Firebase success
        // This ensures the button always has a visible effect for the user
        userName = newName;
        nameTextView.setText(userName);
        toggleEditMode(false);
        
        // Also save to SharedPreferences for local persistence
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("user_name", newName);
        editor.apply();
        
        // Try to update in Firebase if the user is logged in
        if (currentUser != null) {
            String uid = currentUser.getUid();
            Log.d("ProfileActivity", "Updating Firestore document for UID: " + uid);
            
            // Update display name in Firebase Auth
            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setDisplayName(newName)
                .build();
                
            currentUser.updateProfile(profileUpdates)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d("ProfileActivity", "User profile updated in Firebase Auth");
                    } else {
                        Log.e("ProfileActivity", "Failed to update profile in Firebase Auth", task.getException());
                    }
                });
                
            // Also update in Firestore
            db.collection("users").document(uid)
                .update("name", newName)
                .addOnSuccessListener(aVoid -> {
                    Log.d("ProfileActivity", "Name updated successfully in Firestore");
                    Toast.makeText(ProfileActivity.this, "Name updated successfully", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e("ProfileActivity", "Failed to update name in Firestore", e);
                    // If Firestore update fails, try to create the document instead
                    db.collection("users").document(uid)
                        .set(new java.util.HashMap<String, Object>() {{
                            put("name", newName);
                            put("email", userEmail);
                        }})
                        .addOnSuccessListener(aVoid -> {
                            Log.d("ProfileActivity", "Created new user document in Firestore");
                            Toast.makeText(ProfileActivity.this, "Profile created successfully", Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e2 -> {
                            Log.e("ProfileActivity", "Failed to create new user document", e2);
                            Toast.makeText(ProfileActivity.this, "Saved locally only", Toast.LENGTH_LONG).show();
                        });
                });
        } else {
            Log.w("ProfileActivity", "No Firebase user logged in, saving only locally");
            Toast.makeText(ProfileActivity.this, "Name saved locally", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void logout() {
        mAuth.signOut();
        Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
    
    @Override
    public void finish() {
        super.finish();
    }
} 