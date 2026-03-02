package com.example.gamestore.analytics;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
@Lazy
public class AnalyticsService {

    public AnalyticsService() {
        System.out.println("AnalyticsService created!");
    }

    public void analyze() {
        System.out.println("Analyzing game data...");
    }
}