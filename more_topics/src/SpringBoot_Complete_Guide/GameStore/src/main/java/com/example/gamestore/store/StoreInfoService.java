package com.example.gamestore.store;

import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

@Component
public class StoreInfoService {

    @Value("${app.name}")
    private String appName;

    @Value("${app.max-players}")
    private int maxPlayers;

    public void displayStoreInfo() {
        System.out.println("Store Info: " + appName + " - " + maxPlayers);
    }
    
}
