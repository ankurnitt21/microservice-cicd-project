package com.example.gamestore.console;

import org.springframework.stereotype.Component;

@Component
public class Ps4Console implements Console {

    @Override
    public void startGame(){
        System.out.println("Starting PS4 Game");
    }

    @Override
    public void stopGame(){
        System.out.println("Stopping PS4 Game");
    }
}
