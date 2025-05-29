package com.example.goaltracker;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatDelegate;

public class ThemeManager {
    public static final String PREF_NAME = "GoalTrackerPrefs";
    public static final String KEY_COLOR_THEME = "color_theme";
    public static final String KEY_BACKGROUND = "background_style";
    public static final String KEY_PRIMARY_COLOR = "primary_color";
    
    public static final String THEME_GREEN = "green";
    public static final String THEME_BLUE = "blue";
    public static final String THEME_PURPLE = "purple";
    public static final String THEME_ORANGE = "orange";
    public static final String THEME_RED = "red";
    public static final String THEME_TEAL = "teal";
    public static final String THEME_PINK = "pink";
    public static final String THEME_YELLOW = "yellow";
    public static final String THEME_GRAY = "gray";
    public static final String THEME_DARK_BLUE = "dark_blue";
    public static final String THEME_BROWN = "brown";
    public static final String THEME_BEIGE = "beige";

    public static final String BG_DEFAULT = "default";
    public static final String BG_PATTERN1 = "pattern1";
    public static final String BG_PATTERN2 = "pattern2";
    public static final String BG_PATTERN3 = "pattern3";
    

    public static void setPrimaryColor(Context context, int colorValue) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putInt(KEY_PRIMARY_COLOR, colorValue).apply();
    }
    
    public static int getPrimaryColor(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(KEY_PRIMARY_COLOR, context.getResources().getColor(R.color.primary));
    }
    

    
    public static String getColorTheme(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_COLOR_THEME, THEME_GREEN);
    }
    
    public static String getBackgroundStyle(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_BACKGROUND, BG_DEFAULT);
    }
    


    
    public static void setColorTheme(Context context, String colorTheme) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_COLOR_THEME, colorTheme).apply();
    }
    
    public static void setBackgroundStyle(Context context, String backgroundStyle) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_BACKGROUND, backgroundStyle).apply();
    }
    


    public static void applyTheme(Activity activity) {
        // Always apply light mode
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        activity.setTheme(R.style.AppTheme);
    }
    

    public static int getBackgroundResource(Context context) {
        String backgroundStyle = getBackgroundStyle(context);
        
        switch (backgroundStyle) {
            case BG_PATTERN1:
                return android.R.drawable.screen_background_light;
            case BG_PATTERN2:
                return android.R.drawable.screen_background_dark;
            case BG_PATTERN3:
                return R.color.lavender;
            case BG_DEFAULT:
            default:
                return R.color.app_background;
        }
    }


    public static void applyNavigationButtonStyle(ImageButton button) {
        if (button != null) {
            button.setBackgroundColor(android.graphics.Color.TRANSPARENT);
            // Always use default color for light mode
            button.clearColorFilter();
        }
    }
    
    /**
     * Get background color based on the primary color
     * @param context The context
     * @param primaryColor The primary color
     * @return The appropriate background color
     */
    public static int getBackgroundColorForTheme(Context context, int primaryColor) {
        // Always use lightened primary color
        return lightenColor(primaryColor, 0.8f);
    }
    
    /**
     * Lighten a color by a given factor
     * @param color The color to lighten
     * @param factor The factor to lighten by (0.0-1.0)
     * @return The lightened color
     */
    public static int lightenColor(int color, float factor) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[2] = hsv[2] + (1 - hsv[2]) * factor; // Adjust brightness
        return Color.HSVToColor(hsv);
    }
} 