package com.example.gamestore.session;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Scope("prototype")
public class GameSession {

    // Prototype bean: a new instance is created each time it’s requested from the container.

    private String id;

    public GameSession() {
        this.id = UUID.randomUUID().toString();
    }

    public String getId() {
        return id;
    }
}