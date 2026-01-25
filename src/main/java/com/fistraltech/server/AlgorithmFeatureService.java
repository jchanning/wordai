package com.fistraltech.server;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

/**
 * Service for managing algorithm feature toggles.
 * 
 * <p>Provides configuration-based control over which selection algorithms are available.
 * This allows administrators to disable resource-intensive algorithms when running on
 * lightweight hardware or cloud services.
 * 
 * <p><strong>Configuration:</strong><br>
 * Feature toggles are controlled via {@code application.properties}:
 * <pre>
 * algorithm.features.random.enabled=true
 * algorithm.features.entropy.enabled=true
 * algorithm.features.bellman-full-dictionary.enabled=true
 * </pre>
 * 
 * <p><strong>Minimal Configuration:</strong><br>
 * For lightweight environments, disable all except RANDOM:
 * <pre>
 * algorithm.features.random.enabled=true
 * algorithm.features.entropy.enabled=false
 * algorithm.features.bellman-full-dictionary.enabled=false
 * </pre>
 * 
 * @author Fistral Technologies
 */
@Service
public class AlgorithmFeatureService {
    
    private static final Logger logger = Logger.getLogger(AlgorithmFeatureService.class.getName());
    
    @Value("${algorithm.features.random.enabled:true}")
    private boolean randomEnabled;
    
    @Value("${algorithm.features.entropy.enabled:true}")
    private boolean entropyEnabled;
    
    @Value("${algorithm.features.bellman-full-dictionary.enabled:true}")
    private boolean bellmanFullDictionaryEnabled;
    
    @PostConstruct
    public void logAlgorithmStatus() {
        logger.info("=== Algorithm Feature Toggles ===");
        logger.info("RANDOM: " + (randomEnabled ? "ENABLED" : "DISABLED"));
        logger.info("ENTROPY: " + (entropyEnabled ? "ENABLED" : "DISABLED"));
        logger.info("BELLMAN_FULL_DICTIONARY: " + (bellmanFullDictionaryEnabled ? "ENABLED" : "DISABLED"));
        logger.info("=================================");
    }
    
    /**
     * Checks if a specific algorithm is enabled.
     * 
     * @param algorithmId the algorithm identifier (e.g., "RANDOM", "ENTROPY")
     * @return true if the algorithm is enabled, false otherwise
     */
    public boolean isAlgorithmEnabled(String algorithmId) {
        if (algorithmId == null) {
            return false;
        }
        
        switch (algorithmId.toUpperCase()) {
            case "RANDOM":
                return randomEnabled;
            case "ENTROPY":
            case "MAXIMUM_ENTROPY":
                return entropyEnabled;
            case "BELLMAN_FULL_DICTIONARY":
                return bellmanFullDictionaryEnabled;
            default:
                return false;
        }
    }
    
    /**
     * Gets all algorithms with their enabled/disabled status.
     * 
     * @return map of algorithm ID to enabled status
     */
    public Map<String, AlgorithmInfo> getAllAlgorithms() {
        Map<String, AlgorithmInfo> algorithms = new HashMap<>();
        
        algorithms.put("RANDOM", new AlgorithmInfo("RANDOM", "Random Selection", randomEnabled));
        algorithms.put("ENTROPY", new AlgorithmInfo("ENTROPY", "Maximum Entropy", entropyEnabled));
        algorithms.put("BELLMAN_FULL_DICTIONARY", new AlgorithmInfo("BELLMAN_FULL_DICTIONARY", "Bellman Full Dictionary", bellmanFullDictionaryEnabled));
        
        return algorithms;
    }
    
    /**
     * Data class representing algorithm metadata.
     */
    public static class AlgorithmInfo {
        private final String id;
        private final String displayName;
        private final boolean enabled;
        
        public AlgorithmInfo(String id, String displayName, boolean enabled) {
            this.id = id;
            this.displayName = displayName;
            this.enabled = enabled;
        }
        
        public String getId() {
            return id;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        public boolean isEnabled() {
            return enabled;
        }
    }
}
