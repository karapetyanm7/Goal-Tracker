package com.example.goaltracker;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.graphics.Color;
import android.widget.ArrayAdapter;
import android.widget.CalendarView;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CalendarActivity extends AppCompatActivity {
    private static final String TAG = "CalendarActivity";
    private CalendarView calendarView;
    private TextView selectedDateTextView;
    private ListView completedHabitsListView;
    private ArrayAdapter<String> habitsAdapter;
    private List<String> completedHabits;
    private SharedPreferences sharedPreferences;
    private SimpleDateFormat dateFormat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        ThemeManager.applyTheme(this);
        
        if (!GoalTrackerApp.checkAuthenticationStatus(this)) {
            return;
        }
        
        setContentView(R.layout.activity_calendar);

        try {
            sharedPreferences = getSharedPreferences("GoalTrackerPrefs", MODE_PRIVATE);
            dateFormat = new SimpleDateFormat("MMM dd, yyyy");
            completedHabits = new ArrayList<>();

            calendarView = findViewById(R.id.calendarView);
            selectedDateTextView = findViewById(R.id.selectedDateTextView);
            completedHabitsListView = findViewById(R.id.completedHabitsListView);
            ImageButton backButton = findViewById(R.id.backButton);
            
            ThemeManager.applyNavigationButtonStyle(backButton);
            
            applyThemeColors();
            
            backButton.setOnClickListener(v -> finish());

            habitsAdapter = new ArrayAdapter<>(this, 
                R.layout.calendar_list_item, android.R.id.text1, completedHabits);
            completedHabitsListView.setAdapter(habitsAdapter);

            Set<String> habits = sharedPreferences.getStringSet("habits", new HashSet<>());
            if (habits.isEmpty()) {
                Log.d(TAG, "No habits found, creating test habit");
                habits.add("Test Habit");
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putStringSet("habits", habits);
                editor.putInt("Test Habit_points", 0);
                editor.putInt("Test Habit_streak", 0);
                editor.putLong("Test Habit_last_marked", 0);
                editor.putLong("Test Habit_created", System.currentTimeMillis());
                editor.apply();
            }


            updateSelectedDate(Calendar.getInstance().getTimeInMillis());

            calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
                Calendar selectedCalendar = Calendar.getInstance();
                selectedCalendar.set(year, month, dayOfMonth);
                updateSelectedDate(selectedCalendar.getTimeInMillis());
            });

            backButton.setOnClickListener(v -> finish());

        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate", e);
            Toast.makeText(this, "Error initializing calendar", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateSelectedDate(long dateInMillis) {
        try {
            Date selectedDate = new Date(dateInMillis);
            String formattedDate = dateFormat.format(selectedDate);
            selectedDateTextView.setText("Selected Date: " + formattedDate);

            completedHabits.clear();
            Set<String> habits = sharedPreferences.getStringSet("habits", new HashSet<>());

            Calendar selectedCal = Calendar.getInstance();
            selectedCal.setTimeInMillis(dateInMillis);
            selectedCal.set(Calendar.HOUR_OF_DAY, 0);
            selectedCal.set(Calendar.MINUTE, 0);
            selectedCal.set(Calendar.SECOND, 0);
            selectedCal.set(Calendar.MILLISECOND, 0);
            long selectedDayStart = selectedCal.getTimeInMillis();
            
            selectedCal.add(Calendar.DAY_OF_MONTH, 1);
            long selectedDayEnd = selectedCal.getTimeInMillis();

            for (String habit : habits) {
                long lastMarkedTime = sharedPreferences.getLong(habit + "_last_marked", 0);
                if (lastMarkedTime >= selectedDayStart && lastMarkedTime < selectedDayEnd) {
                    completedHabits.add(habit);
                }
            }

            habitsAdapter.notifyDataSetChanged();

            if (completedHabits.isEmpty()) {
                completedHabits.add("No habits completed on this date");
                habitsAdapter.notifyDataSetChanged();
            }

        } catch (Exception e) {
            Log.e(TAG, "Error updating selected date", e);
            Toast.makeText(this, "Error updating date", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isSameDay(Calendar cal1, Calendar cal2) {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
               cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH) &&
               cal1.get(Calendar.DAY_OF_MONTH) == cal2.get(Calendar.DAY_OF_MONTH);
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        GoalTrackerApp.checkAuthenticationStatus(this);
    }


    private void applyThemeColors() {

        int primaryColor = ThemeManager.getPrimaryColor(this);
        

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
        

        androidx.cardview.widget.CardView calendarCardView = findViewById(R.id.calendarCardView);
        androidx.cardview.widget.CardView listCardView = findViewById(R.id.listCardView);
        
        if (calendarCardView != null) {
            calendarCardView.setCardBackgroundColor(primaryColor);
        }
        
        if (listCardView != null) {
            listCardView.setCardBackgroundColor(primaryColor);
        }
        

        if (calendarView != null) {

            try {

                int accentColor = darkenColor(primaryColor, 0.3f);
                

                calendarView.setSelectedDateVerticalBar(new android.graphics.drawable.ColorDrawable(accentColor));
                

                try {
                    Class<?> calendarViewClass = Class.forName("android.widget.CalendarView");
                    java.lang.reflect.Field selectedWeekBackgroundPaintField = calendarViewClass.getDeclaredField("mSelectedDateVerticalBar");
                    selectedWeekBackgroundPaintField.setAccessible(true);
                    selectedWeekBackgroundPaintField.set(calendarView, new android.graphics.drawable.ColorDrawable(accentColor));
                } catch (Exception e) {
                    Log.d(TAG, "Could not set calendar highlight color using reflection: " + e.getMessage());
                }
            } catch (Exception e) {
                Log.e(TAG, "Error customizing calendar: " + e.getMessage());
            }
        }
    }
    

    private int lightenColor(int color, float factor) {
        int red = (int) ((Color.red(color) * (1 - factor) + 255 * factor));
        int green = (int) ((Color.green(color) * (1 - factor) + 255 * factor));
        int blue = (int) ((Color.blue(color) * (1 - factor) + 255 * factor));
        return Color.rgb(red, green, blue);
    }
    

    private int darkenColor(int color, float factor) {
        int red = (int) (Color.red(color) * (1 - factor));
        int green = (int) (Color.green(color) * (1 - factor));
        int blue = (int) (Color.blue(color) * (1 - factor));
        return Color.rgb(red, green, blue);
    }
}