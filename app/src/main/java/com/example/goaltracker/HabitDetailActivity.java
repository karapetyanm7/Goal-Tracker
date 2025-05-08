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

import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;
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
    private SharedPreferences sharedPreferences;
    private String habitName;
    private int habitPoints;
    private int currentStreak;
    private int maxStreak;
    private int completedCount;
    private boolean isMarked = false;
    private AlarmManager alarmManager;
    private PendingIntent pendingIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ThemeManager.applyTheme(this);
        
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
            int pointsDeduction = 0;
            currentStreak = 0;
            habitPoints = Math.max(0, habitPoints - pointsDeduction);
            
            sharedPreferences.edit()
                .putInt(habitName + "_streak", currentStreak)
                .putInt(habitName + "_points", habitPoints)
                .apply();
                
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

        if (habitNameTextView == null || habitPointsTextView == null || habitStreakTextView == null ||
            habitMaxStreakTextView == null || habitTreeImageView == null || motivationalTextView == null ||
            currentStreakIconImageView == null || maxStreakIconImageView == null || markCompleteButton == null ||
            editButton == null || deleteButton == null || backButton == null || calendarButton == null ||
            statsButton == null || reminderButton == null) {
            Toast.makeText(this, "Error initializing views", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }


        if (habitNameTextView != null) {
            habitNameTextView.setEnabled(false);
            habitNameTextView.setCursorVisible(false);
            habitNameTextView.setFocusable(false);
            habitNameTextView.setBackgroundResource(0);

            habitNameTextView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showEditHabitNameDialog();
                }
            });
        }

        applyNavigationButtonStyling();

        updateUI();

        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
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

                    habitPoints += (basePoints + bonusPoints);
                    currentStreak++;
                    completedCount++;

                    int totalPointsAwarded = basePoints + bonusPoints;

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

                    String message = "+" + basePoints + " points";
                    if (bonusPoints > 0) {
                        message += " (+" + bonusPoints + " streak bonus)";
                    }
                    Toast.makeText(HabitDetailActivity.this, message, Toast.LENGTH_SHORT).show();

                    isMarked = true;
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
                showEditHabitNameDialog();
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

        int primaryColor = ThemeManager.getPrimaryColor(this);
        markCompleteButton.setBackgroundColor(primaryColor);
        

        getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().setStatusBarColor(primaryColor);
        

        boolean isDarkMode = ThemeManager.isDarkMode(this);

        int textColor = isDarkMode ? getResources().getColor(android.R.color.white) : getResources().getColor(android.R.color.black);
        

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
            shape.setColor(isDarkMode ? Color.parseColor("#222222") : Color.WHITE);
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

    private boolean canMarkComplete() {
        long lastMarkedTime = sharedPreferences.getLong(habitName + "_last_marked", 0);
        long currentTime = Calendar.getInstance().getTimeInMillis();
        
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        long startOfDay = calendar.getTimeInMillis();
        
        return lastMarkedTime < startOfDay;
    }

    private void updateUI() {
        habitNameTextView.setText(habitName);
        

        if (!habitNameTextView.isEnabled()) {
            int primaryColor = ThemeManager.getPrimaryColor(this);
            habitNameTextView.setTextColor(primaryColor);
            habitNameTextView.setTextSize(24);
            habitNameTextView.setPadding(0, 0, 0, 0);
        }
        
        habitPointsTextView.setText("Points: " + habitPoints);
        habitStreakTextView.setText("Current Streak: " + currentStreak);
        habitMaxStreakTextView.setText("Max Streak: " + maxStreak);

        markCompleteButton.setText(isMarked ? "Unmark Complete" : "Mark Complete");
        markCompleteButton.setEnabled(true);

        currentStreakIconImageView.setVisibility(currentStreak > 0 ? View.VISIBLE : View.GONE);
        maxStreakIconImageView.setVisibility(maxStreak > 0 ? View.VISIBLE : View.GONE);
 
        if (habitPoints >= 500) {
            habitTreeImageView.setImageResource(R.drawable.tree_final);
        } else if (habitPoints >= 200) {
            habitTreeImageView.setImageResource(R.drawable.tree_stage2);
        } else {
            habitTreeImageView.setImageResource(R.drawable.tree_normal);
        }
        

        updateMotivationalText();
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


    private void updateMotivationalText() {
        String motivationalText;
        

        Calendar calendar = Calendar.getInstance();
        int dayOfYear = calendar.get(Calendar.DAY_OF_YEAR);
        int year = calendar.get(Calendar.YEAR);

        int dailySeed = year * 1000 + dayOfYear;
        

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
                "Action is the foundational key to all success."
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
                "Every step counts on the path to success."
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
                "The price of discipline is always less than the pain of regret."
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
                "Consistent effort is a consistent challenge, but a consistent reward."
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
                "Discipline is the bridge between goals and accomplishment."
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
                "The greatest glory in living lies not in never falling, but in rising every time we fall."
            };
            motivationalText = achievementQuotes[Math.abs(dailySeed) % achievementQuotes.length];
        }
        
        motivationalTextView.setText(motivationalText);
    }

    private void showEditHabitNameDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit Habit Name");

        int primaryColor = ThemeManager.getPrimaryColor(this);

        final EditText input = new EditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
        input.setText(habitName);
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
                String newName = input.getText().toString().trim();
                if (newName.isEmpty()) {
                    Toast.makeText(HabitDetailActivity.this, "Habit name cannot be empty", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                if (newName.equals(habitName)) {
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
        editor.putLong(newKey, sharedPreferences.getLong(oldKey, defaultValue));
    }
}