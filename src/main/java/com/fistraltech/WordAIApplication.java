package com.fistraltech;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot application entry point for the WordAI web server.
 * This enables the WordAI game to be played via REST API endpoints.
 */
@SpringBootApplication
public class WordAIApplication {

    public static void main(String[] args) {
        SpringApplication.run(WordAIApplication.class, args);
    }
}