package com.vai.cygnus_assesment.reminders.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record CreateReminderRequest(
        @NotBlank String content,
        @NotNull LocalDateTime localScheduleTime,
        @NotBlank String timeZoneId
) {}
