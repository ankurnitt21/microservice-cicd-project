package com.example.config_server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;

/**
 * Spring Cloud Config Server.
 *
 * What it does:
 *   Acts as a centralised configuration store. All microservices (product-service,
 *   order-service, api-gateway) connect to this server at startup and pull their
 *   configuration properties from here instead of (or in addition to) their local
 *   application.properties files.
 *
 * Why centralise config?
 *   Problem: With 10 microservices, changing a shared property (e.g. log level,
 *   DB connection pool size, feature flag) requires editing 10 files and redeploying
 *   all 10 services.
 *
 *   Solution: One config-server. Change the property once. Services pick it up on
 *   next restart (or instantly with Spring Cloud Bus + /actuator/refresh).
 *
 * Backend (where config files live):
 *   Git (default): config files in a Git repo → versioned, audited, rollback possible
 *   Native:        config files on the classpath or filesystem → simpler, used here
 *
 * @EnableConfigServer: turns this Spring Boot app into a Config Server.
 *   Adds the /config/{application}/{profile} endpoint that clients call.
 */
@SpringBootApplication
@EnableConfigServer
public class ConfigServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConfigServerApplication.class, args);
    }
}
