package com.vai.cygnus_assesment.reminders.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "reminders")
public class Reminder {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String content;

    @Column(nullable = false)
    private String targetTimeZone;

    @Column(nullable = false)
    private LocalDateTime localScheduleTime;

    @Column(nullable = false)
    private Instant scheduledInstant;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReminderState state;

    @Version
    private Long version;

    @Column(nullable = false)
    private String deliveryKey;

    @Column(nullable = false)
    private int retryCount;

    @Column(nullable = false)
    private Instant nextAttemptInstant;

    private Instant claimLeaseExpiresAt;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "reminder_attempts", joinColumns = @JoinColumn(name = "reminder_id"))
    @OrderColumn(name = "attempt_order")
    private List<ExecutionAttempt> attempts = new ArrayList<>();

    protected Reminder() {
    }

    public Reminder(UUID id, String content, String timeZoneId, LocalDateTime localScheduleTime, Instant scheduledInstant) {
        this.id = id;
        this.content = content;
        this.targetTimeZone = timeZoneId;
        this.localScheduleTime = localScheduleTime;
        this.scheduledInstant = scheduledInstant;
        this.nextAttemptInstant = scheduledInstant;
        this.state = ReminderState.SCHEDULED;
        this.retryCount = 0;
        this.deliveryKey = generateDeliveryKey(id, 0L);
    }

    public void edit(String newContent, LocalDateTime newLocalTime, String newTimeZoneId, Instant newScheduledInstant) {
        if (this.state.isTerminal()) {
            throw new IllegalStateException("Cannot edit reminder in terminal state: " + this.state);
        }
        this.content = newContent;
        this.localScheduleTime = newLocalTime;
        this.targetTimeZone = newTimeZoneId;
        this.scheduledInstant = newScheduledInstant;
        this.nextAttemptInstant = newScheduledInstant;
        this.state = ReminderState.SCHEDULED;
        this.claimLeaseExpiresAt = null;
        this.deliveryKey = generateDeliveryKey(this.id, this.version != null ? this.version + 1 : 1L);
    }

    public void cancel() {
        if (this.state.isTerminal()) {
            throw new IllegalStateException("Cannot cancel reminder in terminal state: " + this.state);
        }
        this.state = ReminderState.CANCELLED;
        this.claimLeaseExpiresAt = null;
    }

    public void claim(Instant leaseExpiresAt) {
        if (this.state != ReminderState.SCHEDULED && this.state != ReminderState.RUNNING) {
            throw new IllegalStateException("Cannot claim reminder in state: " + this.state);
        }
        this.state = ReminderState.RUNNING;
        this.claimLeaseExpiresAt = leaseExpiresAt;
    }

    public void recordSuccess(Instant now) {
        this.state = ReminderState.DELIVERED;
        this.claimLeaseExpiresAt = null;
        this.attempts.add(new ExecutionAttempt(this.attempts.size() + 1, now, true, null, this.deliveryKey));
    }

    public void recordFailure(String reason, Instant now, Instant nextRetry, boolean maxRetriesReached) {
        this.attempts.add(new ExecutionAttempt(this.attempts.size() + 1, now, false, reason, this.deliveryKey));
        this.claimLeaseExpiresAt = null;
        if (maxRetriesReached) {
            this.state = ReminderState.FAILED;
        } else {
            this.state = ReminderState.SCHEDULED;
            this.retryCount++;
            this.nextAttemptInstant = nextRetry;
        }
    }

    public static String generateDeliveryKey(UUID id, Long ver) {
        return "reminder:" + id + ":v" + (ver == null ? 0L : ver);
    }

    public UUID getId() { return id; }
    public String getContent() { return content; }
    public String getTargetTimeZone() { return targetTimeZone; }
    public LocalDateTime getLocalScheduleTime() { return localScheduleTime; }
    public Instant getScheduledInstant() { return scheduledInstant; }
    public ReminderState getState() { return state; }
    public Long getVersion() { return version; }
    public String getDeliveryKey() { return deliveryKey; }
    public int getRetryCount() { return retryCount; }
    public Instant getNextAttemptInstant() { return nextAttemptInstant; }
    public Instant getClaimLeaseExpiresAt() { return claimLeaseExpiresAt; }
    public List<ExecutionAttempt> getAttempts() { return Collections.unmodifiableList(attempts); }
}