package com.example.goaltracker;

public class HabitItem {
    public String name;
    public int streak;
    public boolean isMarked;

    public HabitItem(String name) {
        this.name = name;
        this.streak = 0;
        this.isMarked = false;
    }

    public HabitItem(String name, int streak, boolean isMarked) {
        this.name = name;
        this.streak = streak;
        this.isMarked = isMarked;
    }
} 