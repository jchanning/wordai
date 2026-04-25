package com.fistraltech.server;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import com.fistraltech.server.algo.AlgorithmDescriptor;
import com.fistraltech.server.algo.AlgorithmRegistry;
import com.fistraltech.server.model.AlgorithmPolicyEntity;
import com.fistraltech.server.repository.AlgorithmPolicyRepository;

import jakarta.annotation.PostConstruct;

/**
 * Service for managing algorithm runtime policy.
 * 
 * <p>Persisted runtime policy rows take precedence over static defaults so administrators
 * can change algorithm exposure without a redeploy. When no runtime row exists, the
 * service falls back to descriptor defaults or an explicit environment override.
 * 
 * @author Fistral Technologies
 */
@Service
public class AlgorithmFeatureService {
    
    private static final Logger logger = Logger.getLogger(AlgorithmFeatureService.class.getName());

    private final AlgorithmRegistry algorithmRegistry;
    private final Environment environment;
    private final AlgorithmPolicyRepository algorithmPolicyRepository;
    private final Clock clock;

    @Autowired
    public AlgorithmFeatureService(AlgorithmRegistry algorithmRegistry,
                                   Environment environment,
                                   AlgorithmPolicyRepository algorithmPolicyRepository) {
        this(algorithmRegistry, environment, algorithmPolicyRepository, Clock.systemDefaultZone());
    }

    AlgorithmFeatureService(AlgorithmRegistry algorithmRegistry,
                            Environment environment,
                            AlgorithmPolicyRepository algorithmPolicyRepository,
                            Clock clock) {
        this.algorithmRegistry = algorithmRegistry;
        this.environment = environment;
        this.algorithmPolicyRepository = algorithmPolicyRepository;
        this.clock = clock;
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
            algorithms.put(descriptor.getId(), toAlgorithmInfo(descriptor));
        }
        return algorithms;
    }

        /**
         * Persists the runtime enabled state for a registered algorithm.
         *
         * @param algorithmId algorithm identifier or supported alias
         * @param enabled desired runtime state
         * @return the updated algorithm metadata snapshot
         */
        public AlgorithmInfo updateAlgorithmEnabled(String algorithmId, boolean enabled) {
        AlgorithmDescriptor descriptor = algorithmRegistry.getDescriptor(algorithmId)
            .orElseThrow(() -> new IllegalArgumentException("Unknown algorithm: " + algorithmId));

        AlgorithmPolicyEntity policy = algorithmPolicyRepository.findById(descriptor.getId())
            .orElseGet(AlgorithmPolicyEntity::new);
        policy.setAlgorithmId(descriptor.getId());
        policy.setEnabled(enabled);
        policy.setUpdatedAt(LocalDateTime.now(clock));
        algorithmPolicyRepository.save(policy);

        logger.info(() -> "Runtime policy for algorithm " + descriptor.getId() + " set to "
            + (enabled ? "ENABLED" : "DISABLED"));
        return new AlgorithmInfo(
            descriptor.getId(),
            descriptor.getDisplayName(),
            descriptor.getDescription(),
            descriptor.isStateful(),
            getFeatureProperty(descriptor),
            enabled);
        }

    private boolean isEnabled(AlgorithmDescriptor descriptor) {
        return algorithmPolicyRepository.findById(descriptor.getId())
            .map(AlgorithmPolicyEntity::isEnabled)
            .orElseGet(() -> environment.getProperty(
                getFeatureProperty(descriptor),
                Boolean.class,
                descriptor.isEnabledByDefault()));
    }

        private AlgorithmInfo toAlgorithmInfo(AlgorithmDescriptor descriptor) {
        return new AlgorithmInfo(
            descriptor.getId(),
            descriptor.getDisplayName(),
            descriptor.getDescription(),
            descriptor.isStateful(),
            getFeatureProperty(descriptor),
            isEnabled(descriptor));
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
