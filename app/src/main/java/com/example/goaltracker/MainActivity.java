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

import java.util.ArrayList;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {

    private ArrayList<String> habits;
    private HashMap<String, Integer> streaks;
    private ListView habitListView;
    private HabitAdapter habitAdapter;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sharedPreferences = getSharedPreferences("GoalTrackerPrefs", Context.MODE_PRIVATE);

        habits = loadHabits();
        streaks = loadStreaks();

        habitListView = findViewById(R.id.habitListView);
        habitAdapter = new HabitAdapter(this, habits);
        habitListView.setAdapter(habitAdapter);

        habitListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String habit = habits.get(position);
                Intent intent = new Intent(MainActivity.this, HabitDetailActivity.class);
                intent.putExtra("habit", habit);
                startActivity(intent);
            }
        });

        Button addButton = findViewById(R.id.addButton);
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddDialog();
            }
        });

    }

    private void showAddDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add Habit");

        final EditText input = new EditText(this);
        builder.setView(input);

        builder.setPositiveButton("Add", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String habitName = input.getText().toString();
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
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("habits_size", habits.size());
        for (int i = 0; i < habits.size(); i++) {
            editor.putString("habit_" + i, habits.get(i));
        }
        editor.apply();
    }

    private HashMap<String, Integer> loadStreaks() {
        HashMap<String, Integer> streakMap = new HashMap<>();
        for (String habit : habits) {
            streakMap.put(habit, sharedPreferences.getInt(habit + " streak", 0));
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

            Button editButton = convertView.findViewById(R.id.editButton);
            Button deleteButton = convertView.findViewById(R.id.deleteButton);

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

            markCompleteButton.setOnClickListener(v -> {
                streaks.put(habit, streak + 1);
                saveStreaks();
                habitAdapter.notifyDataSetChanged();
            });

            deleteButton.setOnClickListener(v -> {
                habits.remove(position);
                streaks.remove(habit); 
                saveHabits();
                saveStreaks();
                updateHabitList();
            });

            editButton.setOnClickListener(v -> showEditDialog(position));

            return convertView;
        }
    }
}

