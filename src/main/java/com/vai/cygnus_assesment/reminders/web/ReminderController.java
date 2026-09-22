package com.vai.cygnus_assesment.reminders.web;


import com.vai.cygnus_assesment.reminders.domain.Reminder;
import com.vai.cygnus_assesment.reminders.service.ReminderService;
import com.vai.cygnus_assesment.reminders.web.dto.CreateReminderRequest;
import com.vai.cygnus_assesment.reminders.web.dto.EditReminderRequest;
import com.vai.cygnus_assesment.reminders.web.dto.ReminderResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/reminders")
public class ReminderController {

    private final ReminderService reminderService;

    public ReminderController(ReminderService reminderService) {
        this.reminderService = reminderService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReminderResponse create(@Valid @RequestBody CreateReminderRequest req) {
        Reminder r = reminderService.create(req.content(), req.localScheduleTime(), req.timeZoneId());
        return ReminderResponse.from(r);
    }

    @PutMapping("/{id}")
    public ReminderResponse edit(@PathVariable UUID id, @Valid @RequestBody EditReminderRequest req) {
        Reminder r = reminderService.edit(id, req.content(), req.localScheduleTime(), req.timeZoneId());
        return ReminderResponse.from(r);
    }

    @DeleteMapping("/{id}")
    public ReminderResponse cancel(@PathVariable UUID id) {
        Reminder r = reminderService.cancel(id);
        return ReminderResponse.from(r);
    }

    @GetMapping("/{id}")
    public ReminderResponse get(@PathVariable UUID id) {
        return ReminderResponse.from(reminderService.get(id));
    }

    @GetMapping
    public List<ReminderResponse> getAll() {
        return reminderService.getAll().stream().map(ReminderResponse::from).toList();
    }
}
