package com.example.goaltracker;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private ArrayList<Habit> habits;
    private ListView habitListView;
    private ArrayAdapter<Habit> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        habits = new ArrayList<>();
        habits.add(new Habit("Exercise"));
        habits.add(new Habit("Read"));
        habits.add(new Habit("Meditate"));

        habitListView = findViewById(R.id.habitListView);
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, habits);
        habitListView.setAdapter(adapter);

        habitListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Habit habit = habits.get(position);
                Intent intent = new Intent(MainActivity.this, HabitDetailActivity.class);
                intent.putExtra("habitName", habit.getName());
                intent.putExtra("habitPoints", habit.getPoints());
                intent.putExtra("habitCompletion", habit.getCompletionCount());
                intent.putExtra("habitMaxStreak", habit.getMaxStreak());
                intent.putExtra("habitCurrentStreak", habit.getCurrentStreak());
                startActivity(intent);
            }
        });

        // Кнопка для добавления новой привычки
        Button addHabitButton = findViewById(R.id.addHabitButton);
        addHabitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddHabitDialog();
            }
        });
    }

    private void showAddHabitDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add New Habit");

        final EditText input = new EditText(this);
        builder.setView(input);

        builder.setPositiveButton("Add", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String habitName = input.getText().toString();
                if (!habitName.isEmpty()) {
                    Habit newHabit = new Habit(habitName);
                    habits.add(newHabit);
                    adapter.notifyDataSetChanged();
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
}
