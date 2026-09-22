package com.vai.cygnus_assesment.reminders.service;

import com.vai.cygnus_assesment.reminders.adapter.time.SettableClock;
import com.vai.cygnus_assesment.reminders.domain.Reminder;
import com.vai.cygnus_assesment.reminders.port.ReminderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class ReminderService {

    private final ReminderRepository repository;
    private final TimeService timeService;
    private final SettableClock clock;

    public ReminderService(ReminderRepository repository, TimeService timeService, SettableClock clock) {
        this.repository = repository;
        this.timeService = timeService;
        this.clock = clock;
    }

    @Transactional
    public Reminder create(String content, LocalDateTime localScheduleTime, String timeZoneId) {
        Instant instant = timeService.resolveInstant(localScheduleTime, timeZoneId);
        Reminder reminder = new Reminder(UUID.randomUUID(), content, timeZoneId, localScheduleTime, instant);
        return repository.save(reminder);
    }

    @Transactional
    public Reminder edit(UUID id, String newContent, LocalDateTime newLocalTime, String newTimeZoneId) {
        Reminder reminder = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Reminder not found: " + id));
        Instant newInstant = timeService.resolveInstant(newLocalTime, newTimeZoneId);
        reminder.edit(newContent, newLocalTime, newTimeZoneId, newInstant);
        return repository.save(reminder);
    }

    @Transactional
    public Reminder cancel(UUID id) {
        Reminder reminder = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Reminder not found: " + id));
        reminder.cancel();
        return repository.save(reminder);
    }

    @Transactional(readOnly = true)
    public Reminder get(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Reminder not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<Reminder> getAll() {
        return repository.findAll();
    }
}
