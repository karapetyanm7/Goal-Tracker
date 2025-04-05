package com.example.goaltracker;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ProfileActivity extends AppCompatActivity {

    private TextView profileName;
    private TextView profileEmail;
    private Button logoutButton;
    private ImageButton backButton;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        profileName = findViewById(R.id.profile_name);
        profileEmail = findViewById(R.id.profile_email);
        logoutButton = findViewById(R.id.profile_logout_button);
        backButton = findViewById(R.id.profile_back_button);

        if (currentUser != null) {
            String displayName = currentUser.getDisplayName();
            String email = currentUser.getEmail();
            

            if (displayName != null && !displayName.isEmpty()) {
                profileName.setText(displayName);
            } else {
                profileName.setText(email != null ? email.split("@")[0] : "User");
            }
            

            profileEmail.setText(email != null ? email : "");
        }


        logoutButton.setOnClickListener(v -> {
            mAuth.signOut();
            Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        backButton.setOnClickListener(v -> {
            finish();
        });
    }
} 