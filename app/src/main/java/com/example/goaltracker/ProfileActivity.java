package com.example.goaltracker;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.graphics.Color;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class ProfileActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    
    // Profile UI elements
    private TextView nameTextView;
    private TextView emailTextView;
    private TextView creationDateTextView;
    private View nameDisplayLayout;
    private View nameEditLayout;
    private EditText editNameText;
    private ImageButton editNameButton;
    private Button saveNameButton;
    private Button logoutButton;
    private ImageButton backButton;
    private ImageView profileImageView;
    
    // Badge views
    private ImageView badge50DaysIcon;
    private ImageView badge100DaysIcon;
    private ImageView badge365DaysIcon;
    
    // User data
    private String userName;
    private String userEmail;
    private String profileImagePath;
    private int currentStreak; // Add this line to track the current streak
    
    // Preferences
    private SharedPreferences sharedPreferences;
    
    // Image selection
    private static final String PROFILE_IMAGE_PATH = "profile_image_path";
    private static final String PROFILE_IMAGE_FILENAME = "profile_image.jpg";
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private ActivityResultLauncher<String> requestPermissionLauncher;
    private static final int PERMISSION_REQUEST_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        sharedPreferences = getSharedPreferences(ThemeManager.PREF_NAME, Context.MODE_PRIVATE);
        
        ThemeManager.applyTheme(this);
        
        setContentView(R.layout.activity_profile);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUser = mAuth.getCurrentUser();


        nameTextView = findViewById(R.id.profile_name);
        

        badge50DaysIcon = findViewById(R.id.badge_50_days_icon);
        badge100DaysIcon = findViewById(R.id.badge_100_days_icon);
        badge365DaysIcon = findViewById(R.id.badge_365_days_icon);

        updateBadgeVisibility();
        emailTextView = findViewById(R.id.profile_email);
        creationDateTextView = findViewById(R.id.profile_creation_date);
        nameDisplayLayout = findViewById(R.id.name_display_layout);
        nameEditLayout = findViewById(R.id.name_edit_layout);
        editNameText = findViewById(R.id.edit_name_text);
        editNameButton = findViewById(R.id.edit_name_button);
        saveNameButton = findViewById(R.id.save_name_button);
        logoutButton = findViewById(R.id.profile_logout_button);
        backButton = findViewById(R.id.profile_back_button);
        profileImageView = findViewById(R.id.profile_image);

        registerImagePickerLauncher();
        registerPermissionLauncher();

        loadUserData();
        

        setupClickListeners();
    }
    
    @Override
    public void onBackPressed() {
        if (nameEditLayout.getVisibility() == View.VISIBLE) {

            toggleEditMode(false);
        } else {
            super.onBackPressed();
        }
    }
    
    private void setupClickListeners() {

        backButton.setOnClickListener(v -> onBackPressed());
        

        editNameButton.setOnClickListener(v -> toggleEditMode(true));

        saveNameButton.setOnClickListener(v -> saveUserName());

        logoutButton.setOnClickListener(v -> logout());

        profileImageView.setOnClickListener(v -> openImagePicker());

        applyThemeColors();
    }
    

    private void applyThemeColors() {

        int primaryColor = ThemeManager.getPrimaryColor(this);
        

        saveNameButton.setBackgroundColor(primaryColor);
        logoutButton.setBackgroundColor(primaryColor);

        View rootView = findViewById(android.R.id.content);
        if (rootView != null) {
            View mainLayout = ((ViewGroup) rootView).getChildAt(0);
            if (mainLayout != null) {

                int lightPrimaryColor = lightenColor(primaryColor, 0.8f);
                mainLayout.setBackgroundColor(lightPrimaryColor);
            }
        }
        

        getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().setStatusBarColor(primaryColor);

        ThemeManager.applyNavigationButtonStyle(backButton);

        if (editNameButton != null) {
            editNameButton.setImageResource(R.drawable.edit_icon);
            editNameButton.setBackgroundColor(Color.TRANSPARENT);
            editNameButton.setScaleType(android.widget.ImageView.ScaleType.FIT_CENTER);
            editNameButton.setPadding(8, 8, 8, 8);
        }
    }

    private int lightenColor(int color, float factor) {
        int red = (int) ((android.graphics.Color.red(color) * (1 - factor) + 255 * factor));
        int green = (int) ((android.graphics.Color.green(color) * (1 - factor) + 255 * factor));
        int blue = (int) ((android.graphics.Color.blue(color) * (1 - factor) + 255 * factor));
        return android.graphics.Color.rgb(red, green, blue);
    }
    
    private void toggleEditMode(boolean editing) {
        if (editing) {

            nameDisplayLayout.setVisibility(View.GONE);
            nameEditLayout.setVisibility(View.VISIBLE);
            editNameText.setText(userName);
            editNameText.requestFocus();
        } else {

            nameDisplayLayout.setVisibility(View.VISIBLE);
            nameEditLayout.setVisibility(View.GONE);
        }
    }
    
    private void updateBadgeVisibility() {

        SharedPreferences prefs = getSharedPreferences("HabitPrefs", MODE_PRIVATE);
        currentStreak = prefs.getInt("current_streak", 0);
        

        if (currentStreak >= 50) {
            badge50DaysIcon.setVisibility(View.VISIBLE);
        }
        if (currentStreak >= 100) {
            badge100DaysIcon.setVisibility(View.VISIBLE);
        }
        if (currentStreak >= 365) {
            badge365DaysIcon.setVisibility(View.VISIBLE);
        }
    }
    
    private void loadUserData() {
        if (currentUser != null) {
            userEmail = currentUser.getEmail();
            emailTextView.setText(userEmail);
            

            java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("MMMM dd, yyyy", java.util.Locale.ENGLISH);
            
            if (currentUser.getMetadata() != null) {
                long creationTimestamp = currentUser.getMetadata().getCreationTimestamp();
                if (creationTimestamp > 0) {

                    String formattedDate = dateFormat.format(new java.util.Date(creationTimestamp));
                    creationDateTextView.setText("Account created: " + formattedDate);
                } else {

                    String todayDate = dateFormat.format(new java.util.Date());
                    creationDateTextView.setText("Account created: " + todayDate);
                }
            } else {

                String todayDate = dateFormat.format(new java.util.Date());
                creationDateTextView.setText("Account created: " + todayDate);
            }
            

            String savedName = sharedPreferences.getString("user_name", "");
            if (!savedName.isEmpty()) {
                userName = savedName;
                nameTextView.setText(userName);
            }
            

            loadProfileImage();

            String uid = currentUser.getUid();
            db.collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        userName = documentSnapshot.getString("name");
                        if (userName != null && !userName.isEmpty()) {
                            nameTextView.setText(userName);

                            sharedPreferences.edit().putString("user_name", userName).apply();
                        } else {

                            try {
                                nameTextView.setText(R.string.default_username);
                                userName = getString(R.string.default_username);
                            } catch (Exception e) {

                                nameTextView.setText("User");
                                userName = "User";
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(ProfileActivity.this, "Error loading user data", Toast.LENGTH_SHORT).show();

                    try {
                        nameTextView.setText(R.string.default_username);
                        userName = getString(R.string.default_username);
                    } catch (Exception ex) {

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
        

        Toast.makeText(ProfileActivity.this, "Saving name...", Toast.LENGTH_SHORT).show();
        Log.d("ProfileActivity", "Attempting to save name: " + newName);
        

        userName = newName;
        nameTextView.setText(userName);
        toggleEditMode(false);

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("user_name", newName);
        editor.apply();
        

        if (currentUser != null) {
            String uid = currentUser.getUid();
            Log.d("ProfileActivity", "Updating Firestore document for UID: " + uid);
            

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

            db.collection("users").document(uid)
                .update("name", newName)
                .addOnSuccessListener(aVoid -> {
                    Log.d("ProfileActivity", "Name updated successfully in Firestore");
                    Toast.makeText(ProfileActivity.this, "Name updated successfully", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e("ProfileActivity", "Failed to update name in Firestore", e);

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
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Logout");
        builder.setMessage("Do you want to logout?");
        builder.setPositiveButton("Yes", (dialog, which) -> {
            mAuth.signOut();
                    
            Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
        
        builder.setNegativeButton("No", (dialog, which) -> {
            dialog.dismiss();
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }
    
    @Override
    public void finish() {
        super.finish();
    }
    

    private void registerImagePickerLauncher() {
        imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri selectedImageUri = result.getData().getData();
                    if (selectedImageUri != null) {
                        try {

                            saveProfileImage(selectedImageUri);
                        } catch (Exception e) {
                            Log.e("ProfileActivity", "Error processing selected image", e);
                            Toast.makeText(this, "Failed to process image", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            }
        );
    }

    private void openImagePicker() {
        if (checkAndRequestPermissions()) {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.setType("image/*");
            imagePickerLauncher.launch(intent);
        }
    }
    

    private void registerPermissionLauncher() {
        requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) {

                    Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    intent.setType("image/*");
                    imagePickerLauncher.launch(intent);
                } else {

                    Toast.makeText(this, "Permission is required to select a profile image", Toast.LENGTH_LONG).show();
                }
            }
        );
    }

    private boolean checkAndRequestPermissions() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_MEDIA_IMAGES)) {

                    showPermissionExplanationDialog(Manifest.permission.READ_MEDIA_IMAGES);
                    return false;
                } else {

                    requestPermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES);
                    return false;
                }
            }
        } 

        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {

                    showPermissionExplanationDialog(Manifest.permission.READ_EXTERNAL_STORAGE);
                    return false;
                } else {

                    requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
                    return false;
                }
            }
        }
        

        return true;
    }
    

    private void showPermissionExplanationDialog(String permission) {
        new AlertDialog.Builder(this)
            .setTitle("Permission Required")
            .setMessage("This permission is needed to select images from your gallery for your profile picture.")
            .setPositiveButton("OK", (dialog, which) -> {

                requestPermissionLauncher.launch(permission);
            })
            .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
            .create()
            .show();
    }
    

    private void saveProfileImage(Uri imageUri) {
        try {

            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            if (inputStream != null) {
                inputStream.close();
            }
            

            FileOutputStream fos = openFileOutput(PROFILE_IMAGE_FILENAME, Context.MODE_PRIVATE);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
            fos.close();
            

            profileImageView.setImageBitmap(bitmap);
            

            profileImagePath = getFilesDir().getAbsolutePath() + "/" + PROFILE_IMAGE_FILENAME;
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString(PROFILE_IMAGE_PATH, profileImagePath);
            editor.apply();
            
            Toast.makeText(this, "Profile image updated", Toast.LENGTH_SHORT).show();
            

            if (currentUser != null) {

                Log.d("ProfileActivity", "Profile image saved locally at: " + profileImagePath);
            }
            
        } catch (IOException e) {
            Log.e("ProfileActivity", "Error saving profile image", e);
            Toast.makeText(this, "Failed to save profile image", Toast.LENGTH_SHORT).show();
        }
    }
    

    private void loadProfileImage() {
        profileImagePath = sharedPreferences.getString(PROFILE_IMAGE_PATH, null);
        
        if (profileImagePath != null) {
            try {
                FileInputStream fis = openFileInput(PROFILE_IMAGE_FILENAME);
                Bitmap bitmap = BitmapFactory.decodeStream(fis);
                fis.close();
                
                if (bitmap != null) {
                    profileImageView.setImageBitmap(bitmap);
                }
            } catch (IOException e) {
                Log.e("ProfileActivity", "Error loading profile image", e);
                profileImageView.setImageResource(R.drawable.user_icon);
            }
        }
    }
} 