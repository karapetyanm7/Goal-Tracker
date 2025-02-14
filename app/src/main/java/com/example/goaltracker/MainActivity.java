package com.example.goaltracker;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private ArrayList<String> habits = new ArrayList<>();
    private ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        loadHabits();


        EditText habitEditText = findViewById(R.id.habitEditText);
        Button addButton = findViewById(R.id.addButton);
        ListView habitListView = findViewById(R.id.habitListView);


        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, habits);
        habitListView.setAdapter(adapter);


        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String habitName = habitEditText.getText().toString();
                if (!habitName.isEmpty()) {
                    habits.add(habitName);
                    saveHabits();
                    updateHabitList();  
                }
            }
        });

        habitListView.setOnItemClickListener((parent, view, position, id) -> {
            String habit = habits.get(position);
            Intent intent = new Intent(MainActivity.this, HabitDetailActivity.class);
            intent.putExtra("habitName", habit);
            startActivity(intent);
        });
    }


    private void saveHabits() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = prefs.edit();

        JSONArray jsonArray = new JSONArray();
        for (String habit : habits) {
            jsonArray.put(habit);
        }
        editor.putString("habits", jsonArray.toString());
        editor.apply();
    }

    private void loadHabits() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String jsonString = prefs.getString("habits", null);

        if (jsonString != null) {
            try {
                JSONArray jsonArray = new JSONArray(jsonString);
                for (int i = 0; i < jsonArray.length(); i++) {
                    habits.add(jsonArray.getString(i));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }


    private void updateHabitList() {
        adapter.notifyDataSetChanged();
    }
}

