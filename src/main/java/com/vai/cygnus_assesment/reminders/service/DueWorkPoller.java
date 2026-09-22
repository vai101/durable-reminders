package com.vai.cygnus_assesment.reminders.service;

import com.vai.cygnus_assesment.reminders.adapter.time.SettableClock;
import com.vai.cygnus_assesment.reminders.domain.Reminder;
import com.vai.cygnus_assesment.reminders.port.NotificationClient;
import com.vai.cygnus_assesment.reminders.port.ReminderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
@EnableScheduling
public class DueWorkPoller {

    private final ReminderRepository repository;
    private final NotificationClient notificationClient;
    private final SettableClock clock;
    private final int maxRetries;
    private final int leaseSeconds;
    private final int baseBackoffSeconds;

    // Self-inject to route internal calls through the Spring proxy, enforcing @Transactional
    @Autowired
    @Lazy
    private DueWorkPoller self;

    public DueWorkPoller(ReminderRepository repository,
                         NotificationClient notificationClient,
                         SettableClock clock,
                         @Value("${app.scheduler.max-retries:3}") int maxRetries,
                         @Value("${app.scheduler.claim-lease-seconds:60}") int leaseSeconds,
                         @Value("${app.scheduler.base-retry-backoff-seconds:5}") int baseBackoffSeconds) {
        this.repository = repository;
        this.notificationClient = notificationClient;
        this.clock = clock;
        this.maxRetries = maxRetries;
        this.leaseSeconds = leaseSeconds;
        this.baseBackoffSeconds = baseBackoffSeconds;
    }

    @Scheduled(fixedDelayString = "${app.scheduler.polling-rate-ms:500}")
    public void pollAndProcess() {
        Instant now = clock.instant();
        List<Reminder> dueItems = repository.findDueWork(now);
        for (Reminder item : dueItems) {
            // Crucial: Call through the 'self' proxy so the transaction is activated
            self.processItemSafely(item.getId());
        }
    }

    @Transactional
    public void processItemSafely(UUID reminderId) {
        Instant now = clock.instant();
        Reminder reminder = repository.findById(reminderId).orElse(null);
        if (reminder == null || reminder.getState().isTerminal()) {
            return;
        }

        Instant leaseExpiresAt = now.plus(Duration.ofSeconds(leaseSeconds));
        try {
            reminder.claim(leaseExpiresAt);
            // Re-assign the merged entity to ensure we track the newly incremented @Version
            reminder = repository.saveAndFlush(reminder);
        } catch (Exception e) {
            return; // Claim failed (e.g. concurrent race condition)
        }

        Long claimedVersion = reminder.getVersion();

        try {
            notificationClient.deliver(reminder.getDeliveryKey(), reminder.getId(), reminder.getContent());

            Reminder fresh = repository.findById(reminderId).orElse(null);
            if (fresh != null && fresh.getVersion().equals(claimedVersion)) {
                fresh.recordSuccess(now);
                repository.save(fresh);
            }
        } catch (Exception ex) {
            Reminder fresh = repository.findById(reminderId).orElse(null);
            if (fresh != null && fresh.getVersion().equals(claimedVersion)) {
                boolean exhausted = fresh.getRetryCount() + 1 >= maxRetries;
                long delay = (long) (baseBackoffSeconds * Math.pow(2, fresh.getRetryCount()));
                Instant nextRetry = now.plus(Duration.ofSeconds(delay));
                fresh.recordFailure(ex.getMessage(), now, nextRetry, exhausted);
                repository.save(fresh);
            }
        }
    }
}