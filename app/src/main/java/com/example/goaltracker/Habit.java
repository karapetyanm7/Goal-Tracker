package com.example.goaltracker;

import java.util.HashSet;
import java.util.Set;

public class Habit {

    // Frequency constants
    public static final String FREQUENCY_DAILY = "daily";
    public static final String FREQUENCY_WEEKLY = "weekly";
    public static final String FREQUENCY_MONTHLY = "monthly";
    public static final String FREQUENCY_CUSTOM = "custom";

    private String name;
    private int points;
    private int completionCount;
    private int maxStreak;
    private int currentStreak;
    
    // Frequency tracking fields
    private String frequency = FREQUENCY_DAILY; // Default is daily
    private Set<Integer> selectedDays = new HashSet<>(); // For weekly and custom frequencies (1=Monday, 7=Sunday)
    private int timesPerPeriod = 1; // For custom frequency - how many times per week/month
    
    public Habit(String name) {
        this.name = name;
        this.points = 100;
        this.completionCount = 0; 
        this.maxStreak = 0;
        this.currentStreak = 0;
    }
    
    public Habit(String name, String frequency) {
        this(name);
        this.frequency = frequency;
    }

    public String getName() {
        return name;
    }

    public int getPoints() {
        return points;
    }

    public void setPoints(int points) {
        this.points = points;
    }

    public int getCompletionCount() {
        return completionCount;
    }

    public void incrementCompletionCount() {
        this.completionCount++;
    }

    public int getMaxStreak() {
        return maxStreak;
    }

    public void setMaxStreak(int maxStreak) {
        this.maxStreak = maxStreak;
    }

    public int getCurrentStreak() {
        return currentStreak;
    }

    public void setCurrentStreak(int currentStreak) {
        this.currentStreak = currentStreak;
    }
    
    // Frequency getters and setters
    public String getFrequency() {
        return frequency;
    }
    
    public void setFrequency(String frequency) {
        this.frequency = frequency;
    }
    
    public Set<Integer> getSelectedDays() {
        return selectedDays;
    }
    
    public void setSelectedDays(Set<Integer> selectedDays) {
        this.selectedDays = selectedDays;
    }
    
    public void addSelectedDay(int day) {
        selectedDays.add(day);
    }
    
    public void removeSelectedDay(int day) {
        selectedDays.remove(day);
    }
    
    public int getTimesPerPeriod() {
        return timesPerPeriod;
    }
    
    public void setTimesPerPeriod(int timesPerPeriod) {
        this.timesPerPeriod = timesPerPeriod;
    }
    
    /**
     * Check if habit is scheduled for today
     * @return true if habit is scheduled for current day
     */
    public boolean isScheduledForToday() {
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        int dayOfWeek = calendar.get(java.util.Calendar.DAY_OF_WEEK);
        int dayOfMonth = calendar.get(java.util.Calendar.DAY_OF_MONTH);
        
        // Convert Calendar.DAY_OF_WEEK to our 1-7 format (where 1=Monday, 7=Sunday)
        int adjustedDay = dayOfWeek == java.util.Calendar.SUNDAY ? 7 : dayOfWeek - 1;
        
        switch (frequency) {
            case FREQUENCY_DAILY:
                return true;
            case FREQUENCY_WEEKLY:
                return selectedDays.contains(adjustedDay);
            case FREQUENCY_MONTHLY:
                // If set for specific days of the month
                return selectedDays.contains(dayOfMonth);
            case FREQUENCY_CUSTOM:
                // For custom frequency, check if day is selected
                return selectedDays.contains(adjustedDay);
            default:
                return true;
        }
    }

    @Override
    public String toString() {
        return "Habit: " + name;
    }
}
