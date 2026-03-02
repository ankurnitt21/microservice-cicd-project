package com.example.gamestore.session;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

@Service
public class GameSessionManager {

    // Direct injection of prototype bean
    // WRONG pattern: prototype injected once into a singleton
    private final GameSession injectedSession;

    // Correct way: provider to obtain new prototype instances
    private final ObjectProvider<GameSession> sessionProvider;

    public GameSessionManager(GameSession injectedSession,
                              ObjectProvider<GameSession> sessionProvider) {
        this.injectedSession = injectedSession;
        this.sessionProvider = sessionProvider;
    }

    public void demoSessions() {

        System.out.println("\n--- Prototype Injection Demo ---");

        // Directly injected prototype behaves like a singleton inside this class
        System.out.println("Injected session ID (1): " + injectedSession.getId());
        System.out.println("Injected session ID (2): " + injectedSession.getId());

        // Using the provider returns a fresh prototype each time
        GameSession session1 = sessionProvider.getObject();
        GameSession session2 = sessionProvider.getObject();

        System.out.println("Provider session ID (1): " + session1.getId());
        System.out.println("Provider session ID (2): " + session2.getId());
    }
}