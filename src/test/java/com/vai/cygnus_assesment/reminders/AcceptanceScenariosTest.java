package com.vai.cygnus_assesment.reminders;


import com.vai.cygnus_assesment.reminders.adapter.notification.FakeNotificationClient;
import com.vai.cygnus_assesment.reminders.adapter.time.SettableClock;
import com.vai.cygnus_assesment.reminders.domain.Reminder;
import com.vai.cygnus_assesment.reminders.domain.ReminderState;
import com.vai.cygnus_assesment.reminders.port.ReminderRepository;
import com.vai.cygnus_assesment.reminders.service.DueWorkPoller;
import com.vai.cygnus_assesment.reminders.service.ReminderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class AcceptanceScenariosTest {

    @Autowired private ReminderService reminderService;
    @Autowired private ReminderRepository repository;
    @Autowired private DueWorkPoller poller;
    @Autowired private SettableClock clock;
    @Autowired private FakeNotificationClient notificationClient;

    private final Instant baseTime = Instant.parse("2026-03-01T10:00:00Z");

    @BeforeEach
    void setUp() {
        notificationClient.reset();
        repository.deleteAll();
        clock.setInstant(baseTime);
    }

    @Test
    void testAC1_ScheduledDelivery() {
        Reminder r = reminderService.create("AC1 Test", LocalDateTime.of(2026, 3, 1, 15, 30), "Asia/Kolkata");
        assertThat(r.getState()).isEqualTo(ReminderState.SCHEDULED);

        clock.advanceBy(Duration.ofHours(1));
        poller.pollAndProcess();

        Reminder delivered = repository.findById(r.getId()).orElseThrow();
        assertThat(delivered.getState()).isEqualTo(ReminderState.DELIVERED);
        assertThat(delivered.getAttempts()).hasSize(1);
        assertThat(notificationClient.getDeliveredMessages()).hasSize(1);
    }

    @Test
    void testAC2_RestartRecovery() {
        Reminder r = reminderService.create("AC2 Test", LocalDateTime.of(2026, 3, 1, 15, 30), "Asia/Kolkata");
        clock.advanceBy(Duration.ofHours(5));

        poller.pollAndProcess();

        Reminder recovered = repository.findById(r.getId()).orElseThrow();
        assertThat(recovered.getState()).isEqualTo(ReminderState.DELIVERED);
    }

    @Test
    void testAC3_TemporaryFailureAndRetry() {
        Reminder r = reminderService.create("AC3 Test", LocalDateTime.of(2026, 3, 1, 15, 30), "Asia/Kolkata");
        notificationClient.simulateTemporaryFailures(r.getId(), 1);

        clock.setInstant(r.getScheduledInstant());
        poller.pollAndProcess();

        Reminder failedFirst = repository.findById(r.getId()).orElseThrow();
        assertThat(failedFirst.getState()).isEqualTo(ReminderState.SCHEDULED);
        assertThat(failedFirst.getRetryCount()).isEqualTo(1);

        clock.setInstant(failedFirst.getNextAttemptInstant());
        poller.pollAndProcess();

        Reminder deliveredSecond = repository.findById(r.getId()).orElseThrow();
        assertThat(deliveredSecond.getState()).isEqualTo(ReminderState.DELIVERED);
        assertThat(deliveredSecond.getAttempts()).hasSize(2);
    }

    @Test
    void testAC4_DuplicateExecutionIdempotency() {
        Reminder r = reminderService.create("AC4 Test", LocalDateTime.of(2026, 3, 1, 15, 30), "Asia/Kolkata");
        clock.setInstant(r.getScheduledInstant());

        poller.processItemSafely(r.getId());
        poller.processItemSafely(r.getId());

        assertThat(notificationClient.getDeliveredMessages()).hasSize(1);
    }

    @Test
    void testAC5_EditBeforeExecution() {
        Reminder r = reminderService.create("Original", LocalDateTime.of(2026, 3, 1, 15, 30), "Asia/Kolkata");
        Reminder edited = reminderService.edit(r.getId(), "Updated", LocalDateTime.of(2026, 3, 1, 18, 30), "Asia/Kolkata");

        assertThat(edited.getVersion()).isGreaterThan(r.getVersion());
        assertThat(edited.getDeliveryKey()).isNotEqualTo(r.getDeliveryKey());

        clock.setInstant(r.getScheduledInstant());
        poller.pollAndProcess();

        Reminder notYet = repository.findById(r.getId()).orElseThrow();
        assertThat(notYet.getState()).isEqualTo(ReminderState.SCHEDULED);

        clock.setInstant(edited.getScheduledInstant());
        poller.pollAndProcess();

        Reminder finished = repository.findById(r.getId()).orElseThrow();
        assertThat(finished.getState()).isEqualTo(ReminderState.DELIVERED);
        assertThat(finished.getContent()).isEqualTo("Updated");
    }

    @Test
    void testAC6_Cancellation() {
        Reminder r = reminderService.create("To Cancel", LocalDateTime.of(2026, 3, 1, 15, 30), "Asia/Kolkata");
        reminderService.cancel(r.getId());

        clock.setInstant(r.getScheduledInstant());
        poller.pollAndProcess();

        Reminder cancelled = repository.findById(r.getId()).orElseThrow();
        assertThat(cancelled.getState()).isEqualTo(ReminderState.CANCELLED);
        assertThat(notificationClient.getDeliveredMessages()).isEmpty();
    }

    @Test
    void testAC7_TimeZoneBoundaryAndDst() {
        Reminder est = reminderService.create("EST Reminder", LocalDateTime.of(2026, 3, 8, 1, 30), "America/New_York");
        Reminder edt = reminderService.create("EDT Reminder", LocalDateTime.of(2026, 3, 8, 3, 30), "America/New_York");

        Duration diff = Duration.between(est.getScheduledInstant(), edt.getScheduledInstant());
        assertThat(diff).isEqualTo(Duration.ofHours(1));
    }
}