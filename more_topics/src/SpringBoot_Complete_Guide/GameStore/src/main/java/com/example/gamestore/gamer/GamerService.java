package com.example.gamestore.gamer;

import org.springframework.stereotype.Service;

import com.example.gamestore.console.Console;
import com.example.gamestore.notification.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import com.example.gamestore.gamer.HelperService;


@Service
public class GamerService {

    private Console console;
    private NotificationService notificationService;

    @Autowired  // Field Injection
    private HelperService helperService;

    // Constructor Injection
    public GamerService(Console console){
        this.console = console;
    }


    // Setter Injection
    @Autowired
    public void setNotificationService(@Qualifier("smsNotificationService") NotificationService notificationService){
        this.notificationService = notificationService;
    }

    public void play() {
        console.startGame();
        console.stopGame();
        helperService.help();
        notificationService.send("Game started");
    }

    
    
}
