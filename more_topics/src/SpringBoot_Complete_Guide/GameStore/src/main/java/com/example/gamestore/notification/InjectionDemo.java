package com.example.gamestore.notification;

import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;

@Component
public class InjectionDemo {
    
    @Autowired
    private NotificationService autowiredNotificationService;

    @Resource(name = "smsNotificationService")
    private NotificationService resourceNotificationService;

    @PostConstruct
    public void showInjectedBeans() {
        System.out.println("Autowired Notification Service: " + autowiredNotificationService.getClass().getName());
        System.out.println("Resource Notification Service: " + resourceNotificationService.getClass().getName());
    }
}
