package com.vai.cygnus_assesment.reminders.port;



import com.vai.cygnus_assesment.reminders.domain.Reminder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface ReminderRepository extends JpaRepository<Reminder, UUID> {

    @Query("SELECT r FROM Reminder r WHERE " +
            "(r.state = 'SCHEDULED' AND r.nextAttemptInstant <= :now) OR " +
            "(r.state = 'RUNNING' AND r.claimLeaseExpiresAt IS NOT NULL AND r.claimLeaseExpiresAt <= :now) " +
            "ORDER BY r.nextAttemptInstant ASC")
    List<Reminder> findDueWork(@Param("now") Instant now);
}
