package com.example.goaltracker;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;

import java.util.Random;

public class LoginActivity extends AppCompatActivity {

    private EditText emailEditText, passwordEditText, verificationCodeEditText;
    private Button loginButton, verifyButton;
    private ProgressBar progressBar;
    private FirebaseAuth mAuth;
    private String verificationCode;
    private String userEmail, userPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();

        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        verificationCodeEditText = findViewById(R.id.verificationCodeEditText);
        loginButton = findViewById(R.id.loginButton);
        verifyButton = findViewById(R.id.verifyButton);
        progressBar = findViewById(R.id.progressBar);

        verificationCodeEditText.setVisibility(View.GONE);
        verifyButton.setVisibility(View.GONE);

        loginButton.setOnClickListener(v -> handleLogin());
        verifyButton.setOnClickListener(v -> verifyCodeAndLogin());
    }

    private void handleLogin() {
        userEmail = emailEditText.getText().toString().trim();
        userPassword = passwordEditText.getText().toString().trim();

        if (TextUtils.isEmpty(userEmail) || TextUtils.isEmpty(userPassword)) {
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        verificationCode = generateVerificationCode();
        sendVerificationEmail(userEmail, verificationCode);
    }

    private void verifyCodeAndLogin() {
        String enteredCode = verificationCodeEditText.getText().toString().trim();

        if (TextUtils.isEmpty(enteredCode)) {
            return;
        }

        if (enteredCode.equals(verificationCode)) {
            loginUser(userEmail, userPassword);
        }
    }

    private String generateVerificationCode() {
        return String.valueOf(new Random().nextInt(900000) + 100000);
    }

    private void sendVerificationEmail(String email, String code) {
        verificationCodeEditText.setVisibility(View.VISIBLE);
        verifyButton.setVisibility(View.VISIBLE);
        loginButton.setVisibility(View.GONE);
        progressBar.setVisibility(View.GONE);
    }

    private void loginUser(String email, String password) {
        progressBar.setVisibility(View.VISIBLE);
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        startActivity(new Intent(LoginActivity.this, MainActivity.class));
                        finish();
                    }
                });
    }

    public void goToSignup(View view) {
        startActivity(new Intent(this, SignupActivity.class));
    }
}