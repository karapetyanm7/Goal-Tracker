package com.example.goaltracker;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import android.widget.ImageButton;
import androidx.appcompat.app.AppCompatDelegate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private ArrayList<String> habits;
    private HashMap<String, Integer> streaks;
    private ListView habitListView;
    private HabitAdapter habitAdapter;
    private SharedPreferences sharedPreferences;
    private Button addButton;
    private ImageButton themeToggleButton;
    private ActivityResultLauncher<Intent> habitDetailLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sharedPreferences = getSharedPreferences("GoalTrackerPrefs", Context.MODE_PRIVATE);

        habits = new ArrayList<>(sharedPreferences.getStringSet("habits", new HashSet<>()));
        streaks = loadStreaks();

        habitListView = findViewById(R.id.habitListView);
        habitAdapter = new HabitAdapter(this, habits);
        habitListView.setAdapter(habitAdapter);

        addButton = findViewById(R.id.addButton);
        themeToggleButton = findViewById(R.id.themeToggleButton);

        boolean isDarkMode = sharedPreferences.getBoolean("dark_mode", false);
        updateThemeToggleButton(isDarkMode);

        themeToggleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean isDarkMode = sharedPreferences.getBoolean("dark_mode", false);
                isDarkMode = !isDarkMode;
                sharedPreferences.edit().putBoolean("dark_mode", isDarkMode).apply();
                updateThemeToggleButton(isDarkMode);
                if (isDarkMode) {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                } else {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                }
            }
        });

        habitDetailLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    String action = result.getData().getStringExtra("action");
                    String habitName = result.getData().getStringExtra("habit_name");
                    
                    if ("delete".equals(action)) {
                        habits.remove(habitName);
                        streaks.remove(habitName);
                        saveHabits();
                        saveStreaks();
                        habitAdapter.notifyDataSetChanged();
                    } else if ("edit".equals(action)) {
                        String oldName = result.getData().getStringExtra("old_habit_name");
                        String newName = result.getData().getStringExtra("new_habit_name");
                        int index = habits.indexOf(oldName);
                        if (index != -1) {
                            habits.set(index, newName);
                            
                            int streak = streaks.remove(oldName);
                            streaks.put(newName, streak);
                            
                            int points = sharedPreferences.getInt(oldName + "_points", 0);
                            long lastMarked = sharedPreferences.getLong(oldName + "_last_marked", 0);
                            
                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            editor.putInt(newName + "_points", points);
                            editor.putLong(newName + "_last_marked", lastMarked);
                            editor.remove(oldName + "_points");
                            editor.remove(oldName + "_last_marked");
                            editor.apply();
                            
                            saveHabits();
                            saveStreaks();
                            habitAdapter.notifyDataSetChanged();
                        }
                    } else if ("update".equals(action)) {
                        int points = result.getData().getIntExtra("habit_points", 0);
                        sharedPreferences.edit().putInt(habitName + "_points", points).apply();
                        habitAdapter.notifyDataSetChanged();
                    }
                }
            }
        );

        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddDialog();
            }
        });

        habitListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String habit = habits.get(position);
                Intent intent = new Intent(MainActivity.this, HabitDetailActivity.class);
                intent.putExtra("habit_name", habit);
                int points = sharedPreferences.getInt(habit + "_points", 100);
                int streak = streaks.containsKey(habit) ? streaks.get(habit) : 0;
                intent.putExtra("habit_points", points);
                intent.putExtra("habit_streak", streak);
                habitDetailLauncher.launch(intent);
            }
        });
    }

    private void updateThemeToggleButton(boolean isDarkMode) {
        if (isDarkMode) {
            themeToggleButton.setImageResource(R.drawable.light_icon);
        } else {
            themeToggleButton.setImageResource(R.drawable.dark_icon);
        }
    }

    private void showAddDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add Habit");

        final EditText input = new EditText(this);
        builder.setView(input);

        builder.setPositiveButton("Add", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String habitName = input.getText().toString().trim();
                if (!habitName.isEmpty()) {
                    habits.add(habitName);
                    streaks.put(habitName, 0);
                    saveHabits();
                    saveStreaks();
                    habitAdapter.notifyDataSetChanged();
                }
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

    private void showEditDialog(int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit Habit");

        final EditText input = new EditText(this);
        input.setText(habits.get(position));
        builder.setView(input);

        builder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String habitName = input.getText().toString();
                if (!habitName.isEmpty()) {
                    String oldHabit = habits.get(position);
                    habits.set(position, habitName);

                    int streak = streaks.remove(oldHabit);
                    streaks.put(habitName, streak);

                    saveHabits();
                    saveStreaks();
                    habitAdapter.notifyDataSetChanged();
                }
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

    private void updateHabitList() {
        habitAdapter.notifyDataSetChanged();
    }

    private ArrayList<String> loadHabits() {
        ArrayList<String> habitList = new ArrayList<>();
        int size = sharedPreferences.getInt("habits_size", 0);
        for (int i = 0; i < size; i++) {
            habitList.add(sharedPreferences.getString("habit_" + i, null));
        }
        return habitList;
    }

    private void saveHabits() {
        Set<String> habitSet = new HashSet<>(habits);
        sharedPreferences.edit().putStringSet("habits", habitSet).apply();
    }

    private HashMap<String, Integer> loadStreaks() {
        HashMap<String, Integer> streakMap = new HashMap<>();
        for (String habit : habits) {
            streakMap.put(habit, sharedPreferences.getInt(habit + "_streak", 0));
        }
        return streakMap;
    }

    private void saveStreaks() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        for (String habit : streaks.keySet()) {
            editor.putInt(habit + "_streak", streaks.get(habit));
        }
        editor.apply();
    }

    private class HabitAdapter extends ArrayAdapter<String> {
        public HabitAdapter(Context context, ArrayList<String> habits) {
            super(context, 0, habits);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.habit_item, parent, false);
            }

            TextView habitText = convertView.findViewById(R.id.habitText);
            ImageView streakIcon = convertView.findViewById(R.id.streakIcon);
            TextView streakText = convertView.findViewById(R.id.streakText);
            ImageView markCompleteButton = convertView.findViewById(R.id.markCompleteButton);

            String habit = getItem(position);
            habitText.setText(habit);

            int streak = streaks.containsKey(habit) ? streaks.get(habit) : 0;

            if (streak >= 1) {
                streakIcon.setVisibility(View.VISIBLE);
                streakText.setVisibility(View.VISIBLE);
                streakText.setText(" " + streak);
            } else {
                streakIcon.setVisibility(View.GONE);
                streakText.setVisibility(View.GONE);
            }

            long lastMarkedTime = sharedPreferences.getLong(habit + "_last_marked", 0);
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            long startOfDay = calendar.getTimeInMillis();

            markCompleteButton.setVisibility(lastMarkedTime < startOfDay ? View.VISIBLE : View.GONE);

            markCompleteButton.setOnClickListener(v -> {
                int points = sharedPreferences.getInt(habit + "_points", 100);
                points += 10;
                streaks.put(habit, streak + 1);

                long currentTime = Calendar.getInstance().getTimeInMillis();
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putLong(habit + "_last_marked", currentTime);
                editor.putInt(habit + "_points", points);
                editor.apply();
                
                saveStreaks();
                
                markCompleteButton.setVisibility(View.GONE);
                
                habitAdapter.notifyDataSetChanged();
                Toast.makeText(getContext(), "+10 points added!", Toast.LENGTH_SHORT).show();
            });

            return convertView;
        }
    }
}

