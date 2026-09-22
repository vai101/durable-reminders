package com.vai.cygnus_assesment.reminders.service;

import org.springframework.stereotype.Service;
import java.time.*;
import java.time.zone.ZoneRules;

@Service
public class TimeService {

    public Instant resolveInstant(LocalDateTime localDateTime, String zoneIdString) {
        ZoneId zoneId = ZoneId.of(zoneIdString);
        ZoneRules rules = zoneId.getRules();

        if (rules.isDaylightSavings(localDateTime.atZone(zoneId).toInstant())) {
            return localDateTime.atZone(zoneId).withEarlierOffsetAtOverlap().toInstant();
        }

        ZonedDateTime zdt = ZonedDateTime.of(localDateTime, zoneId);
        return zdt.toInstant();
    }
}
