package com.vai.cygnus_assesment.reminders.adapter.notification;

import  com.vai.cygnus_assesment.reminders.port.NotificationClient;
import org.springframework.stereotype.Component;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class FakeNotificationClient implements NotificationClient {

    private final Set<String> processedDeliveryKeys = ConcurrentHashMap.newKeySet();
    private final List<DeliveredMessage> deliveredMessages = Collections.synchronizedList(new ArrayList<>());
    private final Map<UUID, Integer> failureCounters = new ConcurrentHashMap<>();
    private final Set<UUID> permanentFailures = ConcurrentHashMap.newKeySet();

    public record DeliveredMessage(String deliveryKey, UUID reminderId, String content) {}

    @Override
    public boolean deliver(String deliveryKey, UUID reminderId, String content) {
        if (permanentFailures.contains(reminderId)) {
            throw new RuntimeException("Simulated permanent delivery error");
        }

        Integer remainingTransientErrors = failureCounters.get(reminderId);
        if (remainingTransientErrors != null && remainingTransientErrors > 0) {
            failureCounters.put(reminderId, remainingTransientErrors - 1);
            throw new RuntimeException("Simulated temporary connection timeout");
        }

        boolean newlyAdded = processedDeliveryKeys.add(deliveryKey);
        if (newlyAdded) {
            deliveredMessages.add(new DeliveredMessage(deliveryKey, reminderId, content));
        }
        return true;
    }

    public void simulateTemporaryFailures(UUID reminderId, int failureCount) {
        failureCounters.put(reminderId, failureCount);
    }

    public void simulatePermanentFailure(UUID reminderId) {
        permanentFailures.add(reminderId);
    }

    public List<DeliveredMessage> getDeliveredMessages() {
        return Collections.unmodifiableList(deliveredMessages);
    }

    public Set<String> getProcessedDeliveryKeys() {
        return Collections.unmodifiableSet(processedDeliveryKeys);
    }

    public void reset() {
        processedDeliveryKeys.clear();
        deliveredMessages.clear();
        failureCounters.clear();
        permanentFailures.clear();
    }
}
