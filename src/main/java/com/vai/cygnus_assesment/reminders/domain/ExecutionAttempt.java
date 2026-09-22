package com.vai.cygnus_assesment.reminders.domain;

import jakarta.persistence.Embeddable;
import java.time.Instant;

@Embeddable
public class ExecutionAttempt {

    private int attemptNumber;
    private Instant attemptInstant;
    private boolean successful;
    private String failureReason;
    private String deliveryKeyUsed;

    protected ExecutionAttempt() {
    }

    public ExecutionAttempt(int attemptNumber, Instant attemptInstant, boolean successful, String failureReason, String deliveryKeyUsed) {
        this.attemptNumber = attemptNumber;
        this.attemptInstant = attemptInstant;
        this.successful = successful;
        this.failureReason = failureReason;
        this.deliveryKeyUsed = deliveryKeyUsed;
    }

    public int getAttemptNumber() {
        return attemptNumber;
    }

    public Instant getAttemptInstant() {
        return attemptInstant;
    }

    public boolean isSuccessful() {
        return successful;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public String getDeliveryKeyUsed() {
        return deliveryKeyUsed;
    }
}
