package com.example.gamestore;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import com.example.gamestore.gamer.GamerService;
import com.example.gamestore.session.GameSessionManager;
import com.example.gamestore.analytics.AnalyticsService;
import com.example.gamestore.store.StoreInfoService;

@SpringBootApplication
public class GameStoreApplication {

	public static void main(String[] args) {
		SpringApplication.run(GameStoreApplication.class, args);
	}

	@Bean
	CommandLineRunner run(GamerService gamerService, GameSessionManager gameSessionManager, AnalyticsService analyticsService, StoreInfoService storeInfoService) {
		return args -> {System.out.println("Game Store Application Started");
			gamerService.play();
			gameSessionManager.demoSessions();
			analyticsService.analyze();
			storeInfoService.displayStoreInfo();
		};
	}

}
