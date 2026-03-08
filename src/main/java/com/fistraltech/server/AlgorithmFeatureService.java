package com.fistraltech.server;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import com.fistraltech.server.algo.AlgorithmDescriptor;
import com.fistraltech.server.algo.AlgorithmRegistry;

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

    private final AlgorithmRegistry algorithmRegistry;
    private final Environment environment;

    public AlgorithmFeatureService(AlgorithmRegistry algorithmRegistry, Environment environment) {
        this.algorithmRegistry = algorithmRegistry;
        this.environment = environment;
    }
    
    @PostConstruct
    public void logAlgorithmStatus() {
        logger.info("=== Algorithm Feature Toggles ===");
        for (AlgorithmDescriptor descriptor : algorithmRegistry.getDescriptors()) {
            logger.info(() -> descriptor.getId() + ": "
                    + (isAlgorithmEnabled(descriptor.getId()) ? "ENABLED" : "DISABLED"));
        }
        logger.info("=================================");
    }
    
    /**
     * Checks if a specific algorithm is enabled.
     * 
     * @param algorithmId the algorithm identifier (e.g., "RANDOM", "ENTROPY")
     * @return true if the algorithm is enabled, false otherwise
     */
    public boolean isAlgorithmEnabled(String algorithmId) {
        return algorithmRegistry.getDescriptor(algorithmId)
                .map(this::isEnabled)
                .orElse(false);
    }
    
    /**
     * Gets all algorithms with their enabled/disabled status.
     * 
     * @return map of algorithm ID to enabled status
     */
    public Map<String, AlgorithmInfo> getAllAlgorithms() {
        Map<String, AlgorithmInfo> algorithms = new LinkedHashMap<>();
        for (AlgorithmDescriptor descriptor : algorithmRegistry.getDescriptors()) {
            algorithms.put(descriptor.getId(), new AlgorithmInfo(
                    descriptor.getId(),
                    descriptor.getDisplayName(),
                    descriptor.getDescription(),
                    descriptor.isStateful(),
                    getFeatureProperty(descriptor),
                    isEnabled(descriptor)));
        }
        return algorithms;
    }

    private boolean isEnabled(AlgorithmDescriptor descriptor) {
        return environment.getProperty(
                getFeatureProperty(descriptor),
                Boolean.class,
                descriptor.isEnabledByDefault());
    }

    private String getFeatureProperty(AlgorithmDescriptor descriptor) {
        return "algorithm.features." + descriptor.getFeatureToggleKey() + ".enabled";
    }
    
    /**
     * Data class representing algorithm metadata.
     */
    public static class AlgorithmInfo {
        private final String id;
        private final String displayName;
        private final String description;
        private final boolean stateful;
        private final String featureProperty;
        private final boolean enabled;
        
        public AlgorithmInfo(String id, String displayName, String description,
                             boolean stateful, String featureProperty, boolean enabled) {
            this.id = id;
            this.displayName = displayName;
            this.description = description;
            this.stateful = stateful;
            this.featureProperty = featureProperty;
            this.enabled = enabled;
        }
        
        public String getId() {
            return id;
        }
        
        public String getDisplayName() {
            return displayName;
        }

        public String getDescription() {
            return description;
        }

        public boolean isStateful() {
            return stateful;
        }

        public String getFeatureProperty() {
            return featureProperty;
        }
        
        public boolean isEnabled() {
            return enabled;
        }
    }
}
