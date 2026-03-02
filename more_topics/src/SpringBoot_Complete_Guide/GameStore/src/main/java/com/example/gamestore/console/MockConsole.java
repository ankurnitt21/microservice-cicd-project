package com.example.gamestore.console;

import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Profile;

@Component
@Profile("test")
public class MockConsole implements Console {
    @Override
    public void startGame(){
        System.out.println("Starting Mock Game");
    }

    @Override
    public void stopGame(){
        System.out.println("Stopping Mock Game");
    }
}
