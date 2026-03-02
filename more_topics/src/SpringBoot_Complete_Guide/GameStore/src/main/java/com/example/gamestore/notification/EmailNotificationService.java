package com.example.gamestore.notification;

import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Primary;

@Component
@Primary
public class EmailNotificationService implements NotificationService {
    @Override
    public void send(String message){
        System.out.println("Sending email notification: " + message);
    }
}
