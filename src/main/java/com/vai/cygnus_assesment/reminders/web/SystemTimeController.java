package com.vai.cygnus_assesment.reminders.web;

import com.vai.cygnus_assesment.reminders.adapter.time.SettableClock;
import com.vai.cygnus_assesment.reminders.web.dto.AdvanceTimeRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/system/time")
public class SystemTimeController {

    private final SettableClock clock;

    public SystemTimeController(SettableClock clock) {
        this.clock = clock;
    }

    @GetMapping
    public Map<String, Object> getTime() {
        return Map.of("currentInstant", clock.instant().toString());
    }

    @PostMapping("/advance")
    public Map<String, Object> advanceTime(@Valid @RequestBody AdvanceTimeRequest req) {
        Duration duration = switch (req.unit().toLowerCase()) {
            case "minutes", "minute" -> Duration.ofMinutes(req.amount());
            case "hours", "hour" -> Duration.ofHours(req.amount());
            case "days", "day" -> Duration.ofDays(req.amount());
            default -> Duration.ofSeconds(req.amount());
        };
        clock.advanceBy(duration);
        return Map.of("newInstant", clock.instant().toString());
    }

    @PostMapping("/set")
    public Map<String, Object> setTime(@RequestParam String instant) {
        clock.setInstant(Instant.parse(instant));
        return Map.of("newInstant", clock.instant().toString());
    }
}
