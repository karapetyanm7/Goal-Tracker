package com.example.goaltracker;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import androidx.core.content.ContextCompat;

public class ThemeUtils {

    public static final String PREF_NAME = "GoalTracker";

    public static final String KEY_COLOR_THEME = "color_theme";

    public static final String THEME_GREEN = "green";
    public static final String THEME_BLUE = "blue";
    public static final String THEME_PURPLE = "purple";
    public static final String THEME_ORANGE = "orange";
    public static final String THEME_RED = "red";
    public static final String THEME_TEAL = "teal";
    public static final String THEME_PINK = "pink";
    public static final String THEME_YELLOW = "yellow";
    public static final String THEME_GRAY = "gray";

    public static void applyTheme(Activity activity) {
    }



    public static String getColorTheme(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_COLOR_THEME, THEME_GREEN);
    }

    public static int getPrimaryColor(Context context) {
        String theme = getColorTheme(context);
        
        switch (theme) {
            case THEME_BLUE:
                return ContextCompat.getColor(context, R.color.blue_primary);
            case THEME_PURPLE:
                return ContextCompat.getColor(context, R.color.purple_primary);
            case THEME_ORANGE:
                return ContextCompat.getColor(context, R.color.orange_primary);
            case THEME_RED:
                return ContextCompat.getColor(context, R.color.red_primary);
            case THEME_TEAL:
                return ContextCompat.getColor(context, R.color.teal_primary);
            case THEME_PINK:
                return ContextCompat.getColor(context, R.color.pink_primary);
            case THEME_YELLOW:
                return ContextCompat.getColor(context, R.color.yellow_primary);
            case THEME_GRAY:
                return ContextCompat.getColor(context, R.color.gray_primary);
            case THEME_GREEN:
            default:
                return ContextCompat.getColor(context, R.color.colorPrimary);
        }
    }
} 