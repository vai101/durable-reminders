package com.vai.cygnus_assesment.reminders.domain;

public enum ReminderState {
    SCHEDULED,
    RUNNING,
    DELIVERED,
    CANCELLED,
    FAILED;

    public boolean isTerminal() {
        return this == DELIVERED || this == CANCELLED || this == FAILED;
    }
}