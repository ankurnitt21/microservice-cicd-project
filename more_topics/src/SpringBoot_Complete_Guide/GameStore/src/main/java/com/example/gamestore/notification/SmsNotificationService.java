package com.example.gamestore.notification;

import org.springframework.stereotype.Component;

@Component
public class SmsNotificationService implements NotificationService {
    @Override
    public void send(String message){
        System.out.println("Sending SMS notification: " + message);
    }
}
