package com.fistraltech;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot application entry point for the WordAI web server.
 * 
 * <p>This enables the WordAI game to be played via REST API endpoints.
 * Dictionary caching and entropy pre-computation are handled by 
 * {@link com.fistraltech.server.DictionaryService} during Spring initialization.
 */
@SpringBootApplication
public class WordAIApplication {

    public static void main(String[] args) {
        SpringApplication.run(WordAIApplication.class, args);
    }
}