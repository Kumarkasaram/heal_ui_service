package com.heal.dashboard.service.beans;

public enum SignalType {
    PROBLEM("P"),
    EARLY_WARNING("W"),
    INFO("I"),
    BATCH_JOB("BP");

    private String displayName;

    SignalType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName(){
        return this.displayName;
    }
}