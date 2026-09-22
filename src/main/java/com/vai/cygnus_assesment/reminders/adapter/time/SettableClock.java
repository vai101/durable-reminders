package com.vai.cygnus_assesment.reminders.adapter.time;

import org.springframework.stereotype.Component;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class SettableClock extends Clock {

    private final AtomicReference<Instant> currentInstant;
    private final ZoneId zone;

    public SettableClock() {
        this.currentInstant = new AtomicReference<>(Instant.now());
        this.zone = ZoneId.of("UTC");
    }

    public SettableClock(Instant initialInstant, ZoneId zone) {
        this.currentInstant = new AtomicReference<>(initialInstant);
        this.zone = zone;
    }

    @Override
    public ZoneId getZone() {
        return this.zone;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return new SettableClock(this.currentInstant.get(), zone);
    }

    @Override
    public Instant instant() {
        return this.currentInstant.get();
    }

    public void setInstant(Instant instant) {
        this.currentInstant.set(instant);
    }

    public void advanceBy(Duration duration) {
        this.currentInstant.updateAndGet(prev -> prev.plus(duration));
    }
}
