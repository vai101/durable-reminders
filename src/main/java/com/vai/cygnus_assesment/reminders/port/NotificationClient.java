package com.vai.cygnus_assesment.reminders.port;


import java.util.UUID;

public interface NotificationClient {
    boolean deliver(String deliveryKey, UUID reminderId, String content);
}
