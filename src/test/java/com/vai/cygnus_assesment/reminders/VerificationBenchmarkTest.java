package com.vai.cygnus_assesment.reminders;

import com.vai.cygnus_assesment.reminders.adapter.notification.FakeNotificationClient;
import com.vai.cygnus_assesment.reminders.adapter.time.SettableClock;
import com.vai.cygnus_assesment.reminders.domain.Reminder;
import com.vai.cygnus_assesment.reminders.domain.ReminderState;
import com.vai.cygnus_assesment.reminders.port.ReminderRepository;
import com.vai.cygnus_assesment.reminders.service.DueWorkPoller;
import com.vai.cygnus_assesment.reminders.service.ReminderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class VerificationBenchmarkTest {

    @Autowired private ReminderService reminderService;
    @Autowired private ReminderRepository repository;
    @Autowired private DueWorkPoller poller;
    @Autowired private SettableClock clock;
    @Autowired private FakeNotificationClient notificationClient;

    @Test
    void runVerificationBenchmark() {
        notificationClient.reset();
        repository.deleteAll();

        Instant t0 = Instant.parse("2026-10-01T08:00:00Z");
        clock.setInstant(t0);

        List<Reminder> standardItems = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            standardItems.add(reminderService.create("Kolkata Standard " + i, LocalDateTime.of(2026, 10, 1, 14, 0), "Asia/Kolkata"));
        }
        for (int i = 0; i < 4; i++) {
            standardItems.add(reminderService.create("NY Standard " + i, LocalDateTime.of(2026, 10, 1, 4, 30), "America/New_York"));
        }

        List<Reminder> editedItems = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            Reminder r = reminderService.create("To Edit " + i, LocalDateTime.of(2026, 10, 1, 14, 0), "Asia/Kolkata");
            editedItems.add(reminderService.edit(r.getId(), "Edited Content " + i, LocalDateTime.of(2026, 10, 1, 16, 0), "Asia/Kolkata"));
        }

        List<Reminder> cancelledItems = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            Reminder r = reminderService.create("To Cancel " + i, LocalDateTime.of(2026, 10, 1, 14, 0), "Asia/Kolkata");
            cancelledItems.add(reminderService.cancel(r.getId()));
        }

        List<Reminder> tempFailureItems = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            Reminder r = reminderService.create("Temp Fail " + i, LocalDateTime.of(2026, 10, 1, 14, 0), "Asia/Kolkata");
            notificationClient.simulateTemporaryFailures(r.getId(), 1);
            tempFailureItems.add(r);
        }

        List<Reminder> permFailureItems = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            Reminder r = reminderService.create("Perm Fail " + i, LocalDateTime.of(2026, 10, 1, 14, 0), "Asia/Kolkata");
            notificationClient.simulatePermanentFailure(r.getId());
            permFailureItems.add(r);
        }

        assertThat(repository.count()).isEqualTo(20);

        clock.advanceBy(Duration.ofHours(2));

        poller.pollAndProcess();

        poller.processItemSafely(standardItems.get(0).getId());
        poller.processItemSafely(standardItems.get(1).getId());

        for (int step = 0; step < 10; step++) {
            clock.advanceBy(Duration.ofMinutes(15));
            poller.pollAndProcess();
        }

        List<Reminder> finalStates = repository.findAll();
        Map<ReminderState, Long> counts = finalStates.stream()
                .collect(Collectors.groupingBy(Reminder::getState, Collectors.counting()));

        long deliveredCount = counts.getOrDefault(ReminderState.DELIVERED, 0L);
        long cancelledCount = counts.getOrDefault(ReminderState.CANCELLED, 0L);
        long failedCount = counts.getOrDefault(ReminderState.FAILED, 0L);

        System.out.println("=== CAYGNUS BENCHMARK OBSERVED RESULTS ===");
        System.out.println("Total Items Created: " + finalStates.size());
        System.out.println("State DELIVERED: " + deliveredCount);
        System.out.println("State CANCELLED: " + cancelledCount);
        System.out.println("State FAILED:    " + failedCount);
        System.out.println("Unique Messages Delivered: " + notificationClient.getDeliveredMessages().size());
        System.out.println("Unique Keys Stored:        " + notificationClient.getProcessedDeliveryKeys().size());
        System.out.println("==========================================");

        assertThat(finalStates).hasSize(20);
        assertThat(deliveredCount).isEqualTo(14);
        assertThat(cancelledCount).isEqualTo(3);
        assertThat(failedCount).isEqualTo(3);
        assertThat(notificationClient.getDeliveredMessages()).hasSize(14);
    }
}