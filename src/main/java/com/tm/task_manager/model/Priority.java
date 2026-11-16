package com.tm.task_manager.model;

public enum Priority {
    LOW("Thấp"),
    MEDIUM("Trung bình"),
    HIGH("Cao");

    private final String displayName;

    Priority(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
