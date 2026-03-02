package com.example.gamestore.console;

import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Primary;

@Component
@Primary
public class Ps5Console implements Console {

    @Override
    public void startGame(){
        System.out.println("Starting PS5 Game");
    }

    @Override
    public void stopGame(){
        System.out.println("Stopping PS5 Game");
    }
}

/*Now Spring has two beans of type Console:
Ps4Console
Ps5Console

So use Primary annotation to tell Spring to use this bean as the primary bean.
*/
