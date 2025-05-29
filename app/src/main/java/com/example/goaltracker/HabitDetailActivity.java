package com.example.goaltracker;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.ImageButton;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.widget.CheckBox;
import android.widget.TimePicker;
import android.graphics.Color;
import android.view.inputmethod.InputMethodManager;
import android.text.TextWatcher;
import android.text.Editable;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;

public class HabitDetailActivity extends AppCompatActivity {

    private TextView habitNameTextView;
    private TextView habitPointsTextView;
    private TextView habitStreakTextView;
    private TextView habitMaxStreakTextView;
    private ImageView habitTreeImageView;
    private TextView motivationalTextView;
    private ImageView currentStreakIconImageView;
    private ImageView maxStreakIconImageView;
    private Button markCompleteButton;
    private ImageButton editButton;
    private ImageButton deleteButton;
    private ImageButton backButton;
    private ImageButton calendarButton;
    private ImageButton statsButton;
    private ImageButton reminderButton;
    private ImageButton frequencyButton;
    private TextView habitFrequencyTextView;
    private LinearLayout frequencyCounterLayout;
    private Button incrementButton;
    private Button decrementButton;
    private int currentCompletionCount = 0;
    private SharedPreferences sharedPreferences;
    private String habitName;
    private int habitPoints;
    private int currentStreak;
    private int maxStreak;
    private int completedCount;
    private String frequency = Habit.FREQUENCY_DAILY;
    private boolean isMarked = false;
    private AlarmManager alarmManager;
    private PendingIntent pendingIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ThemeManager.applyTheme(this);
        
        if (!GoalTrackerApp.checkAuthenticationStatus(this)) {
            return;
        }
        
        setContentView(R.layout.activity_habit_detail);

        sharedPreferences = getSharedPreferences("GoalTrackerPrefs", Context.MODE_PRIVATE);

        habitName = getIntent().getStringExtra("habit_name");
        if (habitName == null) {
            Toast.makeText(this, "Error: Habit name not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        habitPoints = getIntent().getIntExtra("habit_points", 100);
        currentStreak = getIntent().getIntExtra("habit_streak", 0);
        maxStreak = Math.max(currentStreak, sharedPreferences.getInt(habitName + "_max_streak", 0));
        completedCount = sharedPreferences.getInt(habitName + "_completed_count", 0);

        Calendar calendar = Calendar.getInstance();
        long currentTime = calendar.getTimeInMillis();

        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        long todayStart = calendar.getTimeInMillis();

        calendar.add(Calendar.DAY_OF_YEAR, -1);
        long yesterdayStart = calendar.getTimeInMillis();

        calendar.add(Calendar.DAY_OF_YEAR, -1);
        long dayBeforeYesterdayStart = calendar.getTimeInMillis();

        long lastMarkedTime = sharedPreferences.getLong(habitName + "_last_marked", 0);


        if (lastMarkedTime < yesterdayStart && lastMarkedTime >= dayBeforeYesterdayStart && currentStreak > 0) {

            int previousPoints = habitPoints;
            int pointsDeduction = 50;
            currentStreak = 0;
            habitPoints = Math.max(0, habitPoints - pointsDeduction);

            sharedPreferences.edit()
                .putInt(habitName + "_streak", currentStreak)
                .putInt(habitName + "_points", habitPoints)
                .apply();
                

            setPointsLostFlag();
                
            Toast.makeText(this, "Streak lost!", Toast.LENGTH_SHORT).show();
        } else if (lastMarkedTime < dayBeforeYesterdayStart && currentStreak > 0) {

            currentStreak = 0;
            sharedPreferences.edit()
                .putInt(habitName + "_streak", currentStreak)
                .apply();
        }

        isMarked = lastMarkedTime >= todayStart;

        if (maxStreak > sharedPreferences.getInt(habitName + "_max_streak", 0)) {
            sharedPreferences.edit()
                .putInt(habitName + "_max_streak", maxStreak)
                .apply();
        }

        habitNameTextView = findViewById(R.id.habitNameTextView);
        habitPointsTextView = findViewById(R.id.habitPointsTextView);
        habitStreakTextView = findViewById(R.id.habitStreakTextView);
        habitMaxStreakTextView = findViewById(R.id.habitMaxStreakTextView);
        habitTreeImageView = findViewById(R.id.habitTreeImageView);
        motivationalTextView = findViewById(R.id.motivationalTextView);
        currentStreakIconImageView = findViewById(R.id.currentStreakIconImageView);
        maxStreakIconImageView = findViewById(R.id.maxStreakIconImageView);
        markCompleteButton = findViewById(R.id.markCompleteButton);
        editButton = findViewById(R.id.editButton);
        deleteButton = findViewById(R.id.deleteButton);
        backButton = findViewById(R.id.backButton);
        calendarButton = findViewById(R.id.calendarButton);
        statsButton = findViewById(R.id.statsButton);
        reminderButton = findViewById(R.id.reminderButton);
        frequencyButton = findViewById(R.id.frequencyButton);
        habitFrequencyTextView = findViewById(R.id.habitFrequencyTextView);
        

        frequencyCounterLayout = findViewById(R.id.frequency_counter_layout);
        incrementButton = findViewById(R.id.incrementButton);
        decrementButton = findViewById(R.id.decrementButton);

        if (habitNameTextView == null || habitPointsTextView == null || habitStreakTextView == null ||
            habitMaxStreakTextView == null || habitTreeImageView == null || motivationalTextView == null ||
            currentStreakIconImageView == null || maxStreakIconImageView == null || markCompleteButton == null ||
            editButton == null || deleteButton == null || backButton == null || calendarButton == null ||
            statsButton == null || reminderButton == null || frequencyButton == null ||
            habitFrequencyTextView == null || frequencyCounterLayout == null ||
            incrementButton == null || decrementButton == null) {
            Toast.makeText(this, "Error initializing views", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }


        habitNameTextView.setEnabled(false);
        habitNameTextView.setCursorVisible(false);
        habitNameTextView.setFocusable(false);
        habitNameTextView.setBackgroundResource(0);

        habitNameTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (habitName != null) {
                    showEditHabitNameDialog();
                } else {
                    Toast.makeText(HabitDetailActivity.this, "Error: Habit name is null", Toast.LENGTH_SHORT).show();
                }
            }
        });

        applyNavigationButtonStyling();

        updateUI();

        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        incrementButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String frequency = sharedPreferences.getString(habitName + HabitFrequencyManager.PREF_FREQUENCY_PREFIX, Habit.FREQUENCY_DAILY);
                int timesPerPeriod = sharedPreferences.getInt(habitName + HabitFrequencyManager.PREF_TIMES_PER_PERIOD_PREFIX, 1);
                
                if (currentCompletionCount < timesPerPeriod) {
                    int totalPoints = 70;
                    int pointsPerCompletion = totalPoints / timesPerPeriod;
                    int extraPoints = totalPoints % timesPerPeriod;
                    
                    int pointsToAward = pointsPerCompletion;
                    if (currentCompletionCount < extraPoints) {
                        pointsToAward += 1;
                    }
                    
                    habitPoints += pointsToAward;
                    currentCompletionCount++;
                    
                    sharedPreferences.edit()
                        .putInt(habitName + "_points", habitPoints)
                        .apply();
                    
                    HabitFrequencyManager.updateCompletionCount(HabitDetailActivity.this, habitName, currentCompletionCount);
                    resetPointsLostFlag();
                    
                    String message = "+" + pointsToAward + " points (" + currentCompletionCount + "/" + timesPerPeriod + " times)";
                    Toast.makeText(HabitDetailActivity.this, message, Toast.LENGTH_SHORT).show();
                    
                    updateUI();
                } else {
                    Toast.makeText(HabitDetailActivity.this, "Maximum completions reached for this period", Toast.LENGTH_SHORT).show();
                }
            }
        });
        
        decrementButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentCompletionCount > 0) {
                    int timesPerPeriod = sharedPreferences.getInt(habitName + HabitFrequencyManager.PREF_TIMES_PER_PERIOD_PREFIX, 1);
                    int totalPoints = 70;
                    int pointsPerCompletion = totalPoints / timesPerPeriod;
                    int extraPoints = totalPoints % timesPerPeriod;
                    
                    int pointsToRemove = pointsPerCompletion;
                    if (currentCompletionCount <= extraPoints) {
                        pointsToRemove += 1;
                    }
                    
                    habitPoints = Math.max(0, habitPoints - pointsToRemove);
                    currentCompletionCount--;
                    
                    sharedPreferences.edit()
                        .putInt(habitName + "_points", habitPoints)
                        .apply();
                    
                    HabitFrequencyManager.updateCompletionCount(HabitDetailActivity.this, habitName, currentCompletionCount);
                    setPointsLostFlag();
                    
                    String message = "-" + pointsToRemove + " points (" + currentCompletionCount + "/" + timesPerPeriod + " times)";
                    Toast.makeText(HabitDetailActivity.this, message, Toast.LENGTH_SHORT).show();
                    
                    updateUI();
                } else {
                    Toast.makeText(HabitDetailActivity.this, "Completion count cannot be negative", Toast.LENGTH_SHORT).show();
                }
            }
        });

        markCompleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isMarked && canMarkComplete()) {

                    sharedPreferences.edit()
                        .putInt(habitName + "_previous_points", habitPoints)
                        .putInt(habitName + "_previous_streak", currentStreak)
                        .putInt(habitName + "_previous_completed", completedCount)
                        .putInt(habitName + "_previous_max_streak", maxStreak)
                        .apply();


                    int basePoints = 10;
                    int bonusPoints = 0;
                    int totalPointsAwarded = 0;


                    if (currentStreak >= 1) {
                        if (currentStreak >= 10) {
                            bonusPoints = basePoints;
                        } else if (currentStreak >= 9) {
                            bonusPoints = (int) (basePoints * 0.9);
                        } else if (currentStreak >= 8) {
                            bonusPoints = (int)(basePoints * 0.8);
                        } else if (currentStreak >= 7) {
                            bonusPoints = (int)(basePoints * 0.7);
                        } else if (currentStreak >= 6) {
                            bonusPoints = (int)(basePoints * 0.6);
                        } else if (currentStreak >= 5) {
                            bonusPoints = (int)(basePoints * 0.5);
                        } else if (currentStreak >= 4) {
                            bonusPoints = (int)(basePoints * 0.4);
                        } else if (currentStreak >= 3) {
                            bonusPoints = (int)(basePoints * 0.3);
                        } else if (currentStreak >= 2) {
                            bonusPoints = (int)(basePoints * 0.2);
                        } else if (currentStreak == 1) {
                            bonusPoints = (int)(basePoints * 0.1);
                        }
                    }
                    
                    totalPointsAwarded = basePoints + bonusPoints;
                    
                    habitPoints += totalPointsAwarded;
                    completedCount++;
                    

                    currentStreak++;

                    if (currentStreak > maxStreak) {
                        maxStreak = currentStreak;
                    }

                    long currentTime = Calendar.getInstance().getTimeInMillis();
                    sharedPreferences.edit()
                        .putLong(habitName + "_last_marked", currentTime)
                        .putInt(habitName + "_points", habitPoints)
                        .putInt(habitName + "_max_streak", maxStreak)
                        .putInt(habitName + "_streak", currentStreak)
                        .putInt(habitName + "_completed_count", completedCount)
                        .putInt(habitName + "_last_points_awarded", totalPointsAwarded)
                        .apply();

                    resetPointsLostFlag();

                    String message = "+" + basePoints + " points";
                    if (bonusPoints > 0) {
                        message += " (+" + bonusPoints + " streak bonus)";
                    }
                    Toast.makeText(HabitDetailActivity.this, message, Toast.LENGTH_SHORT).show();

                    isMarked = true;

                    String frequency = sharedPreferences.getString(habitName + HabitFrequencyManager.PREF_FREQUENCY_PREFIX, Habit.FREQUENCY_DAILY);
                    

                    if (Habit.FREQUENCY_CUSTOM.equals(frequency)) {
                        currentCompletionCount++;
                        updateButtonStates();
                    }
                    
                    updateUI();
                } else if (isMarked) {
                    int previousPoints = sharedPreferences.getInt(habitName + "_previous_points", habitPoints);
                    int previousStreak = sharedPreferences.getInt(habitName + "_previous_streak", currentStreak);
                    int previousCompleted = sharedPreferences.getInt(habitName + "_previous_completed", completedCount);
                    int previousMaxStreak = sharedPreferences.getInt(habitName + "_previous_max_streak", maxStreak);

                    int pointsLost = habitPoints - previousPoints;

                    habitPoints = previousPoints;
                    currentStreak = previousStreak;
                    completedCount = previousCompleted;
                    maxStreak = previousMaxStreak;
                    
                    sharedPreferences.edit()
                        .remove(habitName + "_last_marked")
                        .remove(habitName + "_last_points_awarded")
                        .remove(habitName + "_previous_points")
                        .remove(habitName + "_previous_streak")
                        .remove(habitName + "_previous_completed")
                        .remove(habitName + "_previous_max_streak")
                        .putInt(habitName + "_points", habitPoints)
                        .putInt(habitName + "_streak", currentStreak)
                        .putInt(habitName + "_completed_count", completedCount)
                        .putInt(habitName + "_max_streak", maxStreak)
                        .apply();


                    Toast.makeText(HabitDetailActivity.this, "Habit unmarked: -" + pointsLost + " points", Toast.LENGTH_SHORT).show();
                    
                    isMarked = false;
                    updateUI();
                }
            }
        });

        editButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (habitName != null) {
                    showEditHabitNameDialog();
                } else {
                    Toast.makeText(HabitDetailActivity.this, "Error: Habit name is null", Toast.LENGTH_SHORT).show();
                }
            }
        });

        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDeleteConfirmationDialog();
            }
        });

        calendarButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, HabitCalendarActivity.class);
            intent.putExtra("habit_name", habitName);
            startActivity(intent);
        });

        statsButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, HabitStatsActivity.class);
            intent.putExtra("habitName", habitName);
            startActivity(intent);
        });

        reminderButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, ReminderActivity.class);
            startActivity(intent);
        });


        alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, NotificationReceiver.class);
        pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        NotificationHelper.createNotificationChannel(this);
    }


    private void applyNavigationButtonStyling() {

        ThemeManager.applyNavigationButtonStyle(backButton);
        ThemeManager.applyNavigationButtonStyle(editButton);
        ThemeManager.applyNavigationButtonStyle(deleteButton);
        ThemeManager.applyNavigationButtonStyle(calendarButton);
        ThemeManager.applyNavigationButtonStyle(statsButton);
        ThemeManager.applyNavigationButtonStyle(reminderButton);
        ThemeManager.applyNavigationButtonStyle(frequencyButton);

        int primaryColor = ThemeManager.getPrimaryColor(this);
        markCompleteButton.setBackgroundColor(primaryColor);
        

        getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().setStatusBarColor(primaryColor);
        

        int textColor = getResources().getColor(android.R.color.black);
        

        View rootLayout = findViewById(R.id.habit_detail_root_layout);
        if (rootLayout != null) {

            int lightPrimaryColor = lightenColor(primaryColor, 0.8f);
            rootLayout.setBackgroundColor(lightPrimaryColor);
        } else {

            View contentRootView = findViewById(android.R.id.content);
            if (contentRootView != null) {
                View mainLayout = ((ViewGroup) contentRootView).getChildAt(0);
                if (mainLayout != null) {
                    int lightPrimaryColor = lightenColor(primaryColor, 0.8f);
                    mainLayout.setBackgroundColor(lightPrimaryColor);
                }
            }
        }
        View statsContainer = findViewById(R.id.stats_container);
        if (statsContainer != null) {

            android.graphics.drawable.GradientDrawable shape = new android.graphics.drawable.GradientDrawable();
            shape.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
            shape.setCornerRadius(8 * getResources().getDisplayMetrics().density);
            shape.setStroke(2, primaryColor);
            shape.setColor(Color.WHITE);
            statsContainer.setBackground(shape);
        }
        

        if (habitNameTextView != null) {
            habitNameTextView.setTextColor(textColor);
        }
        if (habitPointsTextView != null) {
            habitPointsTextView.setTextColor(textColor);
        }
        if (habitStreakTextView != null) {
            habitStreakTextView.setTextColor(textColor);
        }
        if (habitMaxStreakTextView != null) {
            habitMaxStreakTextView.setTextColor(textColor);
        }
        if (motivationalTextView != null) {
            motivationalTextView.setTextColor(textColor);
        }
    }


    private int lightenColor(int color, float factor) {
        int alpha = android.graphics.Color.alpha(color);
        int red = (int) (android.graphics.Color.red(color) + (255 - android.graphics.Color.red(color)) * factor);
        int green = (int) (android.graphics.Color.green(color) + (255 - android.graphics.Color.green(color)) * factor);
        int blue = (int) (android.graphics.Color.blue(color) + (255 - android.graphics.Color.blue(color)) * factor);
        return android.graphics.Color.argb(alpha, Math.min(red, 255), Math.min(green, 255), Math.min(blue, 255));
    }
    
    @Override
    protected void onResume() {
        super.onResume();

        GoalTrackerApp.checkAuthenticationStatus(this);
    }

    private boolean canMarkComplete() {
        long lastMarkedTime = sharedPreferences.getLong(habitName + "_last_marked", 0);
        long currentTime = Calendar.getInstance().getTimeInMillis();
        
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        long startOfDay = calendar.getTimeInMillis();
        

        if (lastMarkedTime >= startOfDay) {
            return false;
        }
        

        return HabitFrequencyManager.canMarkCompleteToday(this, habitName);
    }


    private void updateButtonStates() {
        int timesPerPeriod = sharedPreferences.getInt(habitName + HabitFrequencyManager.PREF_TIMES_PER_PERIOD_PREFIX, 1);
        

        incrementButton.setEnabled(currentCompletionCount < timesPerPeriod);
        decrementButton.setEnabled(currentCompletionCount > 0);
    }

    private void updateUI() {
        try {

            habitNameTextView.setText(habitName != null ? habitName : "");
            
            if (!habitNameTextView.isEnabled()) {
                int primaryColor = ThemeManager.getPrimaryColor(this);
                habitNameTextView.setTextColor(primaryColor);
                habitNameTextView.setTextSize(24);
                habitNameTextView.setPadding(0, 0, 0, 0);
            }
        
            habitPointsTextView.setText("Points: " + habitPoints);
            

            String streakUnit = "days";
            String frequency = sharedPreferences.getString(habitName + HabitFrequencyManager.PREF_FREQUENCY_PREFIX, Habit.FREQUENCY_DAILY);
            if (Habit.FREQUENCY_CUSTOM.equals(frequency)) {
                int periodType = sharedPreferences.getInt(habitName + HabitFrequencyManager.PREF_PERIOD_TYPE_PREFIX, 0);
                streakUnit = periodType == 0 ? "weeks" : "months";
            }
            
            habitStreakTextView.setText("Current Streak: " + currentStreak);
            habitMaxStreakTextView.setText("Max Streak: " + maxStreak);


            String frequencyDesc = habitName != null ? 
                HabitFrequencyManager.getFrequencyDescription(this, habitName) : 
                "Not scheduled";
            habitFrequencyTextView.setText(frequencyDesc);
            

            if (Habit.FREQUENCY_CUSTOM.equals(frequency)) {

                currentCompletionCount = sharedPreferences.getInt(habitName + HabitFrequencyManager.PREF_COMPLETIONS_THIS_PERIOD_PREFIX, 0);
                frequencyCounterLayout.setVisibility(View.VISIBLE);
                updateButtonStates();
            } else {
                frequencyCounterLayout.setVisibility(View.GONE);
            }
        

        boolean canMark = canMarkComplete();
        boolean scheduledForToday = HabitFrequencyManager.canMarkCompleteToday(this, habitName);
        

        if (isMarked) {
            markCompleteButton.setText("Unmark Complete");
            markCompleteButton.setEnabled(true);
        } else {

            if (Habit.FREQUENCY_CUSTOM.equals(frequency)) {
                int timesPerPeriod = sharedPreferences.getInt(habitName + HabitFrequencyManager.PREF_TIMES_PER_PERIOD_PREFIX, 1);
                markCompleteButton.setText("Marked " + currentCompletionCount + "/" + timesPerPeriod + " times");
            } else {
                markCompleteButton.setText("Mark Complete");
            }
            markCompleteButton.setEnabled(scheduledForToday);
        }

        currentStreakIconImageView.setVisibility(currentStreak > 0 ? View.VISIBLE : View.GONE);
        maxStreakIconImageView.setVisibility(maxStreak > 0 ? View.VISIBLE : View.GONE);
 
        updateTreeDisplay();
        

            updateMotivationalText();
        } catch (Exception e) {

            Toast.makeText(this, "Error updating UI: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void showDeleteConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete Habit");
        builder.setMessage("Are you sure you want to delete this habit?");

        builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent resultIntent = new Intent();
                resultIntent.putExtra("habit_name", habitName);
                resultIntent.putExtra("action", "delete");
                setResult(RESULT_OK, resultIntent);
                finish();
            }
        });

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cancelAllReminders();
    }

    @Override
    public void onBackPressed() {
        Intent resultIntent = new Intent();
        resultIntent.putExtra("habit_name", habitName);
        resultIntent.putExtra("habit_points", habitPoints);
        resultIntent.putExtra("action", "update");
        setResult(RESULT_OK, resultIntent);
        super.onBackPressed();
    }
    

    private void showFrequencyDialog() {

        final String oldFrequency = sharedPreferences.getString(habitName + HabitFrequencyManager.PREF_FREQUENCY_PREFIX, Habit.FREQUENCY_DAILY);
        
        HabitFrequencyManager.showFrequencyDialog(this, habitName, new HabitFrequencyManager.FrequencyDialogCallback() {
            @Override
            public void onFrequencySet(String newFrequency, Set<Integer> selectedDays, int timesPerPeriod, int periodType) {

                currentCompletionCount = 0;
                

                boolean oldIsCustom = Habit.FREQUENCY_CUSTOM.equals(oldFrequency);
                boolean newIsCustom = Habit.FREQUENCY_CUSTOM.equals(newFrequency);
                
                if (oldIsCustom != newIsCustom) {

                    if (oldIsCustom && !newIsCustom) {

                        int factor = 7;
                        int oldPeriodType = sharedPreferences.getInt(habitName + HabitFrequencyManager.PREF_PERIOD_TYPE_PREFIX, 0);
                        if (oldPeriodType == 1) {
                            factor = 30;
                        }
                        

                        currentStreak = currentStreak * factor;
                        maxStreak = maxStreak * factor;

                        sharedPreferences.edit()
                            .putInt(habitName + "_streak", currentStreak)
                            .putInt(habitName + "_max_streak", maxStreak)
                            .apply();
                    } 
                    else if (!oldIsCustom && newIsCustom) {

                        int factor = 7;
                        if (periodType == 1) {
                            factor = 30;
                        }

                        currentStreak = Math.max(1, currentStreak / factor);
                        maxStreak = Math.max(1, maxStreak / factor);
                        
                        sharedPreferences.edit()
                            .putInt(habitName + "_streak", currentStreak)
                            .putInt(habitName + "_max_streak", maxStreak)
                            .apply();
                    }
                }
                
                updateUI();
                
                Toast.makeText(HabitDetailActivity.this, "Habit schedule updated", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void cancelAllReminders() {
        Intent intent = new Intent(this, NotificationReceiver.class);
        for (int i = 0; i < 7; i++) {
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                i,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            alarmManager.cancel(pendingIntent);
        }
    }



    private void setPointsLostFlag() {
        if (sharedPreferences != null && habitName != null) {
            sharedPreferences.edit()
                .putBoolean(habitName + "_has_lost_points", true)
                .apply();
        }
    }

    private void resetPointsLostFlag() {
        if (sharedPreferences != null && habitName != null) {
            sharedPreferences.edit()
                .putBoolean(habitName + "_has_lost_points", false)
                .apply();
        }
    }
    private void updateTreeDisplay() {

        boolean hasLostPoints = sharedPreferences.getBoolean(habitName + "_has_lost_points", false);
        

        if (habitPoints >= 800) {

            habitTreeImageView.setImageResource(R.drawable.tree_final);
        } else if (habitPoints >= 500) {

            habitTreeImageView.setImageResource(R.drawable.tree_stage2);
        } else if (habitPoints >= 200) {
            habitTreeImageView.setImageResource(R.drawable.tree_normal);
        } else if (hasLostPoints) {

            habitTreeImageView.setImageResource(R.drawable.tree_unnormal);
        } else {

            habitTreeImageView.setImageResource(R.drawable.tree_normal);
        }
    }
    
    private void updateMotivationalText() {
        String motivationalText;
        
        Calendar cal = Calendar.getInstance();
        int year = cal.get(Calendar.YEAR);
        int dayOfYear = cal.get(Calendar.DAY_OF_YEAR);

        int habitNameHashCode = habitName != null ? Math.abs(habitName.hashCode()) : 0;
        int dailySeed = (year * 1000 + dayOfYear) ^ habitNameHashCode;
        

        if (currentStreak == 0) {

            String[] beginningQuotes = {

                "The journey of a thousand miles begins with a single step.",
                "Today is your opportunity to build the tomorrow you want.",
                "Discipline is choosing between what you want now and what you want most.",
                "Start where you are. Use what you have. Do what you can.",
                "The best time to plant a tree was 20 years ago. The second best time is now.",
                "Every achievement begins with the decision to try.",
                "Don't wait for opportunity. Create it.",
                "The secret of getting ahead is getting started.",
                "You don't have to be great to start, but you have to start to be great.",
                "The beginning is the most important part of the work.",
                "New beginnings are often disguised as painful endings.",
                "Take the first step in faith. You don't have to see the whole staircase, just take the first step.",
                "The hardest part of any journey is taking the first step.",
                "Every moment is a fresh beginning.",
                "The distance between dreams and reality is called action.",
                "All great achievements require time and a single first step.",
                "The way to get started is to quit talking and begin doing.",
                "Dream big, start small, but most of all, start.",
                "The future depends on what you do today.",
                "Action is the foundational key to all success.",

                "Begin anywhere. Just begin.",
                "The first step toward success is taken when you refuse to be a captive of the environment in which you first find yourself.",
                "A journey of a thousand miles must begin with a single step.",
                "The beginning is always now.",
                "Start today, not tomorrow. If anything, you should have started yesterday.",
                "The only impossible journey is the one you never begin.",
                "Small beginnings are the launching pad to great endings.",
                "Everything you've ever wanted is on the other side of fear.",
                "The most effective way to do it, is to do it.",
                "The only limit to our realization of tomorrow will be our doubts of today.",
                "Do not wait until the conditions are perfect to begin. Beginning makes the conditions perfect.",
                "The only way to discover the limits of the possible is to go beyond them into the impossible.",
                "Your present circumstances don't determine where you can go; they merely determine where you start.",
                "Believe you can and you're halfway there.",
                "You will never win if you never begin.",
                "The scariest moment is always just before you start.",
                "Do not wait to strike till the iron is hot; but make it hot by striking.",
                "Start by doing what's necessary; then do what's possible; and suddenly you are doing the impossible.",
                "The journey of a lifetime starts with the turning of a page.",
                "The starting point of all achievement is desire."
            };

            motivationalText = beginningQuotes[Math.abs(dailySeed) % beginningQuotes.length];
        } else if (currentStreak >= 1 && currentStreak <= 3) {

            String[] earlyQuotes = {
                "Success is the sum of small efforts repeated day in and day out.",
                "Small daily improvements lead to extraordinary long-term results.",
                "The difference between ordinary and extraordinary is that little 'extra'.",
                "You don't have to be great to start, but you have to start to be great.",
                "Progress is not achieved by luck or accident, but by working on yourself daily.",
                "Every step forward is a step toward achievement.",
                "Small progress is still progress.",
                "The expert in anything was once a beginner.",
                "Little by little, a little becomes a lot.",
                "Progress is the ultimate motivation.",
                "Don't compare your beginning to someone else's middle.",
                "Each day brings its own progress and growth.",
                "The only limit to your impact is your imagination and commitment.",
                "A little progress each day adds up to big results.",
                "Your daily decisions determine your destiny.",
                "Look how far you've come, not how far you have to go.",
                "Even the smallest steps forward can create momentum.",
                "The progress of yesterday becomes the baseline for today.",
                "Focus on progress, not perfection.",
                "Every step counts on the path to success.",

                "Inch by inch, it's a cinch. Yard by yard, it's hard.",
                "Fall in love with the process and the results will come.",
                "The day you plant the seed is not the day you eat the fruit.",
                "Great things are done by a series of small things brought together.",
                "Slow progress is better than no progress.",
                "Success is the progressive realization of a worthy goal.",
                "The harder you work for something, the greater you'll feel when you achieve it.",
                "Don't count the days, make the days count.",
                "Rome wasn't built in a day, but they were laying bricks every hour.",
                "The ladder of success is best climbed by stepping on the rungs of opportunity.",
                "Progress always involves risks. You can't steal second base and keep your foot on first.",
                "Every noble work is at first impossible.",
                "Courage doesn't always roar. Sometimes courage is the quiet voice at the end of the day saying, 'I will try again tomorrow.'",
                "If it doesn't challenge you, it won't change you.",
                "Perseverance is not a long race; it is many short races one after the other.",
                "A goal without a plan is just a wish.",
                "Dreams don't work unless you do.",
                "Motivation is what gets you started. Habit is what keeps you going.",
                "To climb steep hills requires slow pace at first.",
                "The journey of a thousand miles begins with a single step."
            };
            motivationalText = earlyQuotes[Math.abs(dailySeed) % earlyQuotes.length];
        } else if (currentStreak >= 4 && currentStreak <= 7) {

            String[] momentumQuotes = {

                "Discipline is the bridge between goals and accomplishment.",
                "Your habits will determine your future.",
                "Motivation is what gets you started. Habit is what keeps you going.",
                "Through discipline comes freedom.",
                "What you do every day matters more than what you do once in a while.",
                "Success is the result of small efforts repeated day in and day out.",
                "Momentum builds when small efforts are repeated consistently.",
                "The rhythm of daily discipline creates the melody of success.",
                "Discipline is doing what needs to be done, even when you don't want to do it.",
                "You will never always be motivated. You have to learn to be disciplined.",
                "The distance between dreams and reality is called discipline.",
                "Habits form the foundation of mastery.",
                "Consistency is the mother of mastery.",
                "Good habits are as addictive as bad ones – but much more rewarding.",
                "The chains of habit are too light to be felt until they are too heavy to be broken.",
                "A river cuts through rock not because of its power but its persistence.",
                "Motivation is temporary. Discipline is permanent.",
                "Self-discipline is the magic power that makes you virtually unstoppable.",
                "Discipline is choosing between what you want now and what you want most.",
                "The price of discipline is always less than the pain of regret.",
                

                "Discipline is the refining fire by which talent becomes ability.",
                "Continuous effort — not strength or intelligence — is the key to unlocking our potential.",
                "We are what we repeatedly do. Excellence, then, is not an act, but a habit.",
                "It's not about having time, it's about making time.",
                "Successful people do what unsuccessful people are not willing to do.",
                "The only discipline that lasts is self-discipline.",
                "The habit of persistence is the habit of victory.",
                "Small daily improvements over time lead to stunning results.",
                "Your life does not get better by chance, it gets better by change.",
                "The difference between who you are and who you want to be is what you do.",
                "Willpower is like a muscle: the more you train it, the stronger it gets.",
                "Habits are first cobwebs, then cables.",
                "The greatest amount of power is that which we exercise over ourselves.",
                "Mastering others is strength. Mastering yourself is true power.",
                "Excellence is a habit cultivated daily.",
                "First we make our habits, then our habits make us.",
                "Discipline is the foundation upon which all success is built.",
                "The pain of discipline weighs ounces; the pain of regret weighs tons.",
                "Success doesn't come from what you do occasionally, it comes from what you do consistently.",
                "Don't let yesterday take up too much of today."
            };
            motivationalText = momentumQuotes[Math.abs(dailySeed) % momentumQuotes.length];
        } else if (currentStreak >= 8 && currentStreak <= 14) {

            String[] consistencyQuotes = {

                "Consistency is the key to achieving and maintaining momentum.",
                "It's not what we do once in a while that shapes our lives, but what we do consistently.",
                "We are what we repeatedly do. Excellence, then, is not an act, but a habit.",
                "Long-term consistency trumps short-term intensity.",
                "Success isn't always about greatness. It's about consistency.",
                "Small disciplines repeated with consistency lead to great achievements gained slowly over time.",
                "The secret of your success is found in your daily routine.",
                "Consistency is what transforms average into excellence.",
                "Daily improvement is unstoppable.",
                "Showing up consistently is half the battle.",
                "Consistency is harder when nobody's watching, but also more powerful.",
                "Success is neither magical nor mysterious. Success is the natural consequence of consistently applying the basic fundamentals.",
                "Consistency builds trust with yourself.",
                "The hallmark of excellence is consistency.",
                "It's not what we do once that shapes our lives, but what we do consistently.",
                "Slow progress is still progress. Be consistent.",
                "Your consistency says more about you than your intensity ever could.",
                "Trust the process. Your time is coming. Just do the work and the results will handle themselves.",
                "You don't have to be extreme, just consistent.",
                "Consistent effort is a consistent challenge, but a consistent reward.",
                

                "Consistency breeds familiarity, familiarity breeds confidence.",
                "The secret to getting ahead is getting started. The secret to getting started is breaking your complex overwhelming tasks into small manageable tasks, and then starting on the first one.",
                "Nothing in this world can take the place of persistence.",
                "Dripping water hollows out stone, not through force but through persistence.",
                "Your true power comes not from bursts of energy, but from the consistency of your daily rituals.",
                "In the realm of excellence, consistency is king and persistence is queen.",
                "What you do every day is more important than what you do every once in a while.",
                "Consistent action creates consistent results.",
                "The greatest gap in life is the one between knowing and doing.",
                "Habits determine 95% of a person's behavior.",
                "Champions keep playing until they get it right.",
                "You will never change your life until you change something you do daily.",
                "Don't be afraid of moving slowly. Be afraid of standing still.",
                "Persistence guarantees that results are inevitable.",
                "Consistency is the true foundation of trust.",
                "When nothing seems to help, I go and look at a stonecutter hammering away at his rock, perhaps a hundred times without as much as a crack showing in it. Yet at the hundred and first blow it will split in two, and I know it was not that last blow that did it, but all that had gone before.",
                "A year from now you may wish you had started today.",
                "Energy and persistence conquer all things.",
                "Persistence is to the character of man as carbon is to steel.",
                "Many of life's failures are people who did not realize how close they were to success when they gave up."
            };
            motivationalText = consistencyQuotes[Math.abs(dailySeed) % consistencyQuotes.length];
        } else {

            String[] masteryQuotes = {
                "The more you practice, the luckier you get.",
                "Champions keep playing until they get it right.",
                "Mastery is not a function of genius or talent. It is a function of time and intense focus.",
                "The secret of your future is hidden in your daily routine.",
                "Success is no accident. It is hard work, perseverance, learning, studying, sacrifice, and most of all, love of what you are doing.",
                "Mastery comes from consistent repetition of the fundamentals.",
                "Those who reach mastery are not those who learn more than others, but those who practice more than others.",
                "Excellence is not a destination; it is a continuous journey that never ends.",
                "There are no shortcuts to mastery. It is a path taken one step at a time.",
                "Mastery is the beautiful moment when what you think perfectly aligns with what you do.",
                "Discipline is the refining fire by which talent becomes ability.",
                "What you habitually think often becomes what you ultimately are.",
                "The greatest achievement is not in never falling, but in rising after each fall.",
                "Dedication and mastery are fueled by passion.",
                "Perfection is achieved not when there is nothing more to add, but when there is nothing left to take away.",
                "The master has failed more times than the beginner has even tried.",
                "Mastery is in the reaching, not the arriving. It's in constantly stretching beyond your current self.",
                "Great things are not done by impulse, but by a series of small things brought together.",
                "Success is walking from failure to failure with no loss of enthusiasm.",
                "Discipline is the bridge between goals and accomplishment.",

                "Mastery requires endurance. Mastery, a word we don't use often, is not the equivalent of what we might consider its cognate—perfectionism—an inhuman aim motivated by a concern with how others view us.",
                "A true master is a student first.",
                "Mastery lies in commitment to the fundamentals.",
                "It's what you practice in private that you will be rewarded for in public.",
                "Skill is only developed by hours and hours of work.",
                "To master a craft, master your mindset first.",
                "The path to mastery is through deliberate, daily practice.",
                "You can't rush the harvest, no matter how badly you need food.",
                "Mastery is the combination of precision, presence, and time.",
                "Only by going too far can you find out how far you can go.",
                "Don't practice until you get it right. Practice until you can't get it wrong.",
                "Your level of success is determined by your level of discipline and perseverance.",
                "The expert in anything was once a beginner.",
                "The road to mastery is built upon failure after failure.",
                "To know, is to know that you know nothing. That is the meaning of true knowledge.",
                "Mastery is not found in complexity; it's found in the perfection of simplicity.",
                "What separates the good from the great is the attention to detail.",
                "Every single man I know who is successful at what he does is successful because he loves it.",
                "True mastery transcends technique.",
                "The advanced practitioner seeks mastery not of technique, but of self."
            };
            motivationalText = masteryQuotes[Math.abs(dailySeed) % masteryQuotes.length];
        }
        

        if (habitPoints >= 500) {
            String[] achievementQuotes = {
                "Your dedication has transformed this habit into something beautiful.",
                "Look how far you've come! Your consistency has created amazing results.",
                "Great achievements are born from persistence and dedication.",
                "The height of your accomplishments will equal the depth of your convictions.",
                "Success is not the destination, but the journey you've embraced every single day.",
                "What you've accomplished proves that great things are possible with dedication.",
                "Your commitment has blossomed into remarkable achievement.",
                "Achievement is the crown of effort, the diadem of thought.",
                "Behind every achievement is a story of determination, dedication, and perseverance.",
                "The reward for work well done is the opportunity to do more.",
                "Your persistence has created a masterpiece of habit and character.",
                "The fruit of your labor is sweet because of the effort you've invested.",
                "Achievement is not about reaching a destination, but becoming the person who can get there.",
                "What you've built through daily dedication stands as a testament to your character.",
                "The more difficult the victory, the greater the happiness in winning.",
                "The view from the top is worth every step of the climb.",
                "Achievement is the knowledge that you have studied, worked hard, and done the best that is in you.",
                "Effort and courage are not enough without purpose and direction.",
                "Your achievement is proof that obstacles can be overcome by those who believe in themselves.",
                "The greatest glory in living lies not in never falling, but in rising every time we fall.",

                "Your daily choices have compounded into extraordinary achievement.",
                "Success is the sum of small efforts, repeated day in and day out.",
                "What you have accomplished is just the beginning of what you will achieve.",
                "Excellence is not a skill. It's an attitude that becomes a habit.",
                "Don't limit your challenges, challenge your limits.",
                "Today's accomplishments were yesterday's impossibilities.",
                "Your habits have become the architecture of your achievement.",
                "The most rewarding things you do in life are often the ones that look like they cannot be done.",
                "The difference between who you are and who you want to be is what you do.",
                "The expert in anything was once a beginner. Look at how far you've come.",
                "Your persistence and dedication have blossomed into mastery.",
                "Achievement is the result of perfection, hard work, learning from failure, loyalty, and persistence.",
                "Never mistake a single defeat for a final defeat. Your journey proves this.",
                "Discipline is the bridge between goals and accomplishment. You've built that bridge beautifully.",
                "In the end, we only regret the chances we didn't take. You took yours.",
                "The world makes way for the person who knows where they're going. You've shown your direction.",
                "Success isn't overnight. It's when every day you get a little better than the day before. It all adds up.",
                "Life's real failure is when you do not realize how close you were to success when you gave up. You never gave up.",
                "There are no shortcuts to any place worth going. You've walked the full path with determination.",
                "Your achievement isn't just what you've done, but who you've become in the process."
            };
            motivationalText = achievementQuotes[Math.abs(dailySeed) % achievementQuotes.length];
        }
        
        motivationalTextView.setText(motivationalText);
    }

    private void showEditHabitNameDialog() {
        if (habitName == null) {
            Toast.makeText(this, "Error: Cannot edit null habit name", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit Habit Name");

        int primaryColor = ThemeManager.getPrimaryColor(this);

        final EditText input = new EditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
        input.setText(habitName); // habitName won't be null here due to check above
        input.setPadding(50, 30, 50, 30);
        input.selectAll();
        

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(50, 30, 50, 30);
        container.addView(input);
        
        builder.setView(container);
        

        builder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                if (input == null || input.getText() == null) {
                    Toast.makeText(HabitDetailActivity.this, "Error: Could not get input text", Toast.LENGTH_SHORT).show();
                    return;
                }
                

                String newName = input.getText().toString().trim();
                

                if (newName.isEmpty()) {
                    Toast.makeText(HabitDetailActivity.this, "Habit name cannot be empty", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                if (newName.length() > 15) {
                    Toast.makeText(HabitDetailActivity.this, "Habit name cannot exceed 15 characters", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                if (habitName != null && newName.equals(habitName)) {
                    return;
                }
                
                if (habitAlreadyExists(newName)) {
                    Toast.makeText(HabitDetailActivity.this, "A habit with this name already exists", Toast.LENGTH_SHORT).show();
                    return;
                }
                

                Toast.makeText(HabitDetailActivity.this, "Saving habit name...", Toast.LENGTH_SHORT).show();
                
                String oldName = habitName;
                migrateHabitData(oldName, newName);
                habitName = newName;
                updateUI();
                Intent resultIntent = new Intent();
                resultIntent.putExtra("action", "edit");
                resultIntent.putExtra("old_habit_name", oldName);
                resultIntent.putExtra("new_habit_name", newName);
                setResult(RESULT_OK, resultIntent);
                
                Toast.makeText(HabitDetailActivity.this, "Habit renamed successfully", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        

        AlertDialog dialog = builder.create();
        

        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(primaryColor);
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(primaryColor);
            }
        });
        
        dialog.show();
        input.requestFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
    }

    private void migrateHabitData(String oldName, String newName) {

        if (oldName == null || newName == null) {
            Toast.makeText(this, "Error: Cannot migrate habit data with null values", Toast.LENGTH_SHORT).show();
            return;
        }
        
        SharedPreferences.Editor editor = sharedPreferences.edit();
        
        copyPreference(editor, oldName + "_points", newName + "_points", habitPoints);
        copyPreference(editor, oldName + "_streak", newName + "_streak", currentStreak);
        copyPreference(editor, oldName + "_max_streak", newName + "_max_streak", maxStreak);
        copyPreference(editor, oldName + "_completed_count", newName + "_completed_count", completedCount);
        copyPreference(editor, oldName + "_last_marked", newName + "_last_marked", 
                       sharedPreferences.getLong(oldName + "_last_marked", 0));
        copyPreference(editor, oldName + "_created", newName + "_created", 
                       sharedPreferences.getLong(oldName + "_created", System.currentTimeMillis()));
        

        copyPreference(editor, oldName + "_previous_points", newName + "_previous_points", 
                       sharedPreferences.getInt(oldName + "_previous_points", 0));
        copyPreference(editor, oldName + "_previous_streak", newName + "_previous_streak", 
                       sharedPreferences.getInt(oldName + "_previous_streak", 0));
        copyPreference(editor, oldName + "_previous_max_streak", newName + "_previous_max_streak", 
                       sharedPreferences.getInt(oldName + "_previous_max_streak", 0));
        copyPreference(editor, oldName + "_previous_completed", newName + "_previous_completed", 
                       sharedPreferences.getInt(oldName + "_previous_completed", 0));
        

        Set<String> habitSet = new HashSet<>(sharedPreferences.getStringSet("habits", new HashSet<>()));
        habitSet.remove(oldName);
        habitSet.add(newName);
        editor.putStringSet("habits", habitSet);

        editor.remove(oldName + "_points");
        editor.remove(oldName + "_streak");
        editor.remove(oldName + "_max_streak");
        editor.remove(oldName + "_completed_count");
        editor.remove(oldName + "_last_marked");
        editor.remove(oldName + "_created");
        editor.remove(oldName + "_previous_points");
        editor.remove(oldName + "_previous_streak");
        editor.remove(oldName + "_previous_max_streak");
        editor.remove(oldName + "_previous_completed");

        editor.apply();
    }
    

    private void copyPreference(SharedPreferences.Editor editor, String oldKey, String newKey, int defaultValue) {
        editor.putInt(newKey, sharedPreferences.getInt(oldKey, defaultValue));
    }

    private void copyPreference(SharedPreferences.Editor editor, String oldKey, String newKey, long defaultValue) {
        SharedPreferences prefs = getSharedPreferences("GoalTrackerPrefs", MODE_PRIVATE);
        editor.putLong(newKey, prefs.getLong(oldKey, defaultValue));
    }
    private boolean habitAlreadyExists(String habitName) {
        if (habitName == null || habitName.isEmpty()) {
            return false;
        }
        
        SharedPreferences prefs = getSharedPreferences("GoalTrackerPrefs", MODE_PRIVATE);
        Set<String> habitNames = prefs.getStringSet("habit_names", new HashSet<String>());
        
        if (habitNames.isEmpty()) {
            Map<String, ?> allPrefs = prefs.getAll();
            for (String key : allPrefs.keySet()) {
                if (key.endsWith("_points") && !key.equals(this.habitName + "_points")) {
                    String potentialHabitName = key.substring(0, key.length() - 7);
                    if (potentialHabitName.equalsIgnoreCase(habitName)) {
                        return true;
                    }
                }
            }
            return false;
        }
        

        for (String name : habitNames) {
            if (name.equalsIgnoreCase(habitName) && !name.equals(this.habitName)) {
                return true;
            }
        }
        
        return false;
    }
}