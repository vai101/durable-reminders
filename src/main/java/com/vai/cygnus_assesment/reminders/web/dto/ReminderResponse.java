package com.vai.cygnus_assesment.reminders.web.dto;


import com.vai.cygnus_assesment.reminders.domain.ExecutionAttempt;
import com.vai.cygnus_assesment.reminders.domain.Reminder;
import com.vai.cygnus_assesment.reminders.domain.ReminderState;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ReminderResponse(
        UUID id,
        String content,
        String timeZoneId,
        LocalDateTime localScheduleTime,
        Instant scheduledInstant,
        ReminderState state,
        Long version,
        String deliveryKey,
        int retryCount,
        Instant nextAttemptInstant,
        List<ExecutionAttempt> attempts
) {
    public static ReminderResponse from(Reminder r) {
        return new ReminderResponse(
                r.getId(),
                r.getContent(),
                r.getTargetTimeZone(),
                r.getLocalScheduleTime(),
                r.getScheduledInstant(),
                r.getState(),
                r.getVersion(),
                r.getDeliveryKey(),
                r.getRetryCount(),
                r.getNextAttemptInstant(),
                r.getAttempts()
        );
    }
}
