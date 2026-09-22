package com.vai.cygnus_assesment.reminders.web.dto;

import jakarta.validation.constraints.Min;

public record AdvanceTimeRequest(
        @Min(1) long amount,
        String unit
) {}
