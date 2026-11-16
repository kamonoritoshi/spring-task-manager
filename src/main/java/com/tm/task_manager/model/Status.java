package com.tm.task_manager.model;

public enum Status {
    TODO("Cần làm"),
    IN_PROGRESS("Đang thực hiện"),
    DONE("Hoàn thành");

    private final String displayName;

    Status(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
