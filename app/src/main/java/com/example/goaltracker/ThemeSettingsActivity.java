package com.example.goaltracker;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

public class ThemeSettingsActivity extends AppCompatActivity {

    private SharedPreferences sharedPreferences;
    
    // Theme UI elements
    private Button showColorsButton;
    private ImageButton backButton;
    private View colorDialog;
    private ConstraintLayout mainLayout;
    
    // Color buttons
    private View greenColorBtn;
    private View blueColorBtn;
    private View purpleColorBtn;
    private View orangeColorBtn;
    private View redColorBtn;
    private View tealColorBtn;
    private View pinkColorBtn;
    private View yellowColorBtn;
    private View grayColorBtn;
    
    // Theme settings
    private boolean themeChanged = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Get current theme settings before setting the content view
        sharedPreferences = getSharedPreferences(ThemeManager.PREF_NAME, Context.MODE_PRIVATE);
        
        setContentView(R.layout.activity_theme_settings);
        
        // Initialize UI components
        mainLayout = findViewById(R.id.theme_settings_layout);
        showColorsButton = findViewById(R.id.show_colors_button);
        backButton = findViewById(R.id.theme_settings_back_button);
        colorDialog = findViewById(R.id.color_picker_dialog);
        
        // Initialize color buttons
        greenColorBtn = findViewById(R.id.green_color);
        blueColorBtn = findViewById(R.id.blue_color);
        purpleColorBtn = findViewById(R.id.purple_color);
        orangeColorBtn = findViewById(R.id.orange_color);
        redColorBtn = findViewById(R.id.red_color);
        tealColorBtn = findViewById(R.id.teal_color);
        pinkColorBtn = findViewById(R.id.pink_color);
        yellowColorBtn = findViewById(R.id.yellow_color);
        grayColorBtn = findViewById(R.id.gray_color);

        // Set up UI initial state
        colorDialog.setVisibility(View.GONE);
        
        // Apply color to the "Choose Theme Color" button and background
        applyThemeColors();

        // Set up show colors button
        showColorsButton.setOnClickListener(v -> {
            colorDialog.setVisibility(View.VISIBLE);
        });
        
        // Set up color selection handlers
        setupColorButtonListeners();

        // Set up back button
        backButton.setOnClickListener(v -> {
            if (themeChanged) {
                // Return to calling activity and refresh
                setResult(RESULT_OK);
                finish();
            } else {
                // Just go back
                finish();
            }
        });
    }
    
    private void setupColorButtonListeners() {
        View.OnClickListener colorClickListener = v -> {
            String colorName;
            int colorValue;
            
            if (v == greenColorBtn) {
                colorName = ThemeManager.THEME_GREEN;
                colorValue = getResources().getColor(R.color.primary);
            } else if (v == blueColorBtn) {
                colorName = ThemeManager.THEME_BLUE;
                colorValue = getResources().getColor(R.color.blue_500);
            } else if (v == purpleColorBtn) {
                colorName = ThemeManager.THEME_PURPLE;
                colorValue = getResources().getColor(R.color.purple_500);
            } else if (v == orangeColorBtn) {
                colorName = ThemeManager.THEME_ORANGE;
                colorValue = getResources().getColor(R.color.orange_primary);
            } else if (v == redColorBtn) {
                colorName = ThemeManager.THEME_RED;
                colorValue = getResources().getColor(R.color.red_primary);
            } else if (v == tealColorBtn) {
                colorName = ThemeManager.THEME_TEAL;
                colorValue = getResources().getColor(R.color.teal_primary);
            } else if (v == pinkColorBtn) {
                colorName = ThemeManager.THEME_PINK;
                colorValue = getResources().getColor(R.color.pink_primary);
            } else if (v == yellowColorBtn) {
                colorName = ThemeManager.THEME_YELLOW;
                colorValue = getResources().getColor(R.color.yellow_primary);
            } else if (v == grayColorBtn) {
                colorName = ThemeManager.THEME_GRAY;
                colorValue = getResources().getColor(R.color.gray_primary);
            } else {
                return;
            }
            
            // Save the selected color theme
            ThemeManager.setColorTheme(this, colorName);
            ThemeManager.setPrimaryColor(this, colorValue);
            themeChanged = true;
            
            // Apply color change immediately to some UI elements
            applyColorToUI(colorValue);
            
            // Hide color picker
            colorDialog.setVisibility(View.GONE);
            
            Toast.makeText(this, "Theme color updated to " + colorName, Toast.LENGTH_SHORT).show();
        };
        
        // Attach the click listener to all color buttons
        greenColorBtn.setOnClickListener(colorClickListener);
        blueColorBtn.setOnClickListener(colorClickListener);
        purpleColorBtn.setOnClickListener(colorClickListener);
        orangeColorBtn.setOnClickListener(colorClickListener);
        redColorBtn.setOnClickListener(colorClickListener);
        tealColorBtn.setOnClickListener(colorClickListener);
        pinkColorBtn.setOnClickListener(colorClickListener);
        yellowColorBtn.setOnClickListener(colorClickListener);
        grayColorBtn.setOnClickListener(colorClickListener);
    }
    
    private void applyThemeColors() {
        int primaryColor = ThemeManager.getPrimaryColor(this);
        
        // Set the status bar color to match the primary theme color
        // Clear any existing flags first to ensure proper coloring
        getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().setStatusBarColor(primaryColor);
        
        // Apply color to the Choose Theme Color button
        showColorsButton.setBackgroundColor(primaryColor);
        
        // Apply a light version of the primary color to the background
        if (mainLayout != null) {
            int lightPrimaryColor = lightenColor(primaryColor, 0.8f);
            mainLayout.setBackgroundColor(lightPrimaryColor);
        }
        
        // Apply consistent styling to navigation buttons
        ThemeManager.applyNavigationButtonStyle(backButton);
    }

    private void applyColorToUI(int colorValue) {
        // Apply the color change to buttons and other UI elements directly
        showColorsButton.setBackgroundColor(colorValue);
        
        // Set the status bar color to match the new theme color
        getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().setStatusBarColor(colorValue);
        
        // Apply a light version of the color to the background
        int lightColor = lightenColor(colorValue, 0.8f);
        if (mainLayout != null) {
            mainLayout.setBackgroundColor(lightColor);
        }
    }

    // Helper method to lighten a color for backgrounds
    private int lightenColor(int color, float factor) {
        int alpha = android.graphics.Color.alpha(color);
        int red = (int) (android.graphics.Color.red(color) + (255 - android.graphics.Color.red(color)) * factor);
        int green = (int) (android.graphics.Color.green(color) + (255 - android.graphics.Color.green(color)) * factor);
        int blue = (int) (android.graphics.Color.blue(color) + (255 - android.graphics.Color.blue(color)) * factor);
        return android.graphics.Color.argb(alpha, Math.min(red, 255), Math.min(green, 255), Math.min(blue, 255));
    }
} 