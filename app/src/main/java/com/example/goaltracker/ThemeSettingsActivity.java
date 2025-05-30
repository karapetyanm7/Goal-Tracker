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
    

    private Button showColorsButton;
    private ImageButton backButton;
    private View colorDialog;
    private ConstraintLayout mainLayout;
    

    private View greenColorBtn;
    private View blueColorBtn;
    private View purpleColorBtn;
    private View orangeColorBtn;
    private View redColorBtn;
    private View tealColorBtn;
    private View pinkColorBtn;
    private View yellowColorBtn;
    private View grayColorBtn;
    private View darkBlueColorBtn;
    private View brownColorBtn;
    private View beigeColorBtn;

    private boolean themeChanged = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        sharedPreferences = getSharedPreferences(ThemeManager.PREF_NAME, Context.MODE_PRIVATE);
        
        setContentView(R.layout.activity_theme_settings);
        
        mainLayout = findViewById(R.id.theme_settings_layout);
        showColorsButton = findViewById(R.id.show_colors_button);
        backButton = findViewById(R.id.theme_settings_back_button);
        colorDialog = findViewById(R.id.color_picker_dialog);
        
        greenColorBtn = findViewById(R.id.green_color);
        blueColorBtn = findViewById(R.id.blue_color);
        purpleColorBtn = findViewById(R.id.purple_color);
        orangeColorBtn = findViewById(R.id.orange_color);
        redColorBtn = findViewById(R.id.red_color);
        tealColorBtn = findViewById(R.id.teal_color);
        pinkColorBtn = findViewById(R.id.pink_color);
        yellowColorBtn = findViewById(R.id.yellow_color);
        grayColorBtn = findViewById(R.id.gray_color);
        darkBlueColorBtn = findViewById(R.id.dark_blue_color);
        brownColorBtn = findViewById(R.id.brown_color);
        beigeColorBtn = findViewById(R.id.beige_color);

        colorDialog.setVisibility(View.GONE);
        
        applyThemeColors();

        showColorsButton.setOnClickListener(v -> {
            colorDialog.setVisibility(View.VISIBLE);
        });
        
        setupColorButtonListeners();

        backButton.setOnClickListener(v -> {
            if (themeChanged) {
                setResult(RESULT_OK);
                finish();
            } else {
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
            } else if (v == darkBlueColorBtn) {
                colorName = ThemeManager.THEME_DARK_BLUE;
                colorValue = getResources().getColor(R.color.dark_blue_primary);
            } else if (v == brownColorBtn) {
                colorName = ThemeManager.THEME_BROWN;
                colorValue = getResources().getColor(R.color.brown_primary);
            } else if (v == beigeColorBtn) {
                colorName = ThemeManager.THEME_BEIGE;
                colorValue = getResources().getColor(R.color.beige_primary);
            } else {
                return;
            }
            

            ThemeManager.setColorTheme(this, colorName);
            ThemeManager.setPrimaryColor(this, colorValue);
            themeChanged = true;

            applyColorToUI(colorValue);
            

            colorDialog.setVisibility(View.GONE);
            
            Toast.makeText(this, "Theme color updated to " + colorName, Toast.LENGTH_SHORT).show();
        };
        

        greenColorBtn.setOnClickListener(colorClickListener);
        blueColorBtn.setOnClickListener(colorClickListener);
        purpleColorBtn.setOnClickListener(colorClickListener);
        orangeColorBtn.setOnClickListener(colorClickListener);
        redColorBtn.setOnClickListener(colorClickListener);
        tealColorBtn.setOnClickListener(colorClickListener);
        pinkColorBtn.setOnClickListener(colorClickListener);
        yellowColorBtn.setOnClickListener(colorClickListener);
        grayColorBtn.setOnClickListener(colorClickListener);
        darkBlueColorBtn.setOnClickListener(colorClickListener);
        brownColorBtn.setOnClickListener(colorClickListener);
        beigeColorBtn.setOnClickListener(colorClickListener);
    }
    
    private void applyThemeColors() {
        int primaryColor = ThemeManager.getPrimaryColor(this);
        

        getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().setStatusBarColor(primaryColor);
        

        showColorsButton.setBackgroundColor(primaryColor);

        if (mainLayout != null) {
            int lightPrimaryColor = lightenColor(primaryColor, 0.8f);
            mainLayout.setBackgroundColor(lightPrimaryColor);
        }

        ThemeManager.applyNavigationButtonStyle(backButton);
    }

    private void applyColorToUI(int colorValue) {
        showColorsButton.setBackgroundColor(colorValue);
        getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().setStatusBarColor(colorValue);
        int lightColor = lightenColor(colorValue, 0.8f);
        if (mainLayout != null) {
            mainLayout.setBackgroundColor(lightColor);
        }
    }


    private int lightenColor(int color, float factor) {
        int alpha = android.graphics.Color.alpha(color);
        int red = (int) (android.graphics.Color.red(color) + (255 - android.graphics.Color.red(color)) * factor);
        int green = (int) (android.graphics.Color.green(color) + (255 - android.graphics.Color.green(color)) * factor);
        int blue = (int) (android.graphics.Color.blue(color) + (255 - android.graphics.Color.blue(color)) * factor);
        return android.graphics.Color.argb(alpha, Math.min(red, 255), Math.min(green, 255), Math.min(blue, 255));
    }
} 