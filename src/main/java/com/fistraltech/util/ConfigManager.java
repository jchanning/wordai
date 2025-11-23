package com.fistraltech.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

import com.fistraltech.server.dto.DictionaryOption;

/**
 * Configuration manager that loads settings from properties file and provides
 * fallback mechanisms for file paths and configuration values.
 */
public class ConfigManager {
    private static final Logger logger = Logger.getLogger(ConfigManager.class.getName());
    
    private static final String DEFAULT_CONFIG_FILE = "application.properties";
    private static final String USER_CONFIG_FILE = "wordai.properties";
    
    private Properties properties;
    private static ConfigManager instance;
    
    private ConfigManager() {
        loadConfiguration();
    }
    
    public static ConfigManager getInstance() {
        if (instance == null) {
            instance = new ConfigManager();
        }
        return instance;
    }
    
    private void loadConfiguration() {
        properties = new Properties();
        
        // Try to load user-specific config first
        if (!loadPropertiesFile(USER_CONFIG_FILE)) {
            // Fall back to default config
            loadPropertiesFile(DEFAULT_CONFIG_FILE);
        }
        
        // Expand system properties
        expandSystemProperties();
    }
    
    private boolean loadPropertiesFile(String filename) {
        try {
            // Try to load from classpath first
            InputStream resourceStream = getClass().getClassLoader().getResourceAsStream(filename);
            if (resourceStream != null) {
                properties.load(resourceStream);
                logger.info(() -> "Loaded configuration from classpath: " + filename);
                return true;
            }
            
            // Try to load from file system
            Path configPath = Paths.get(filename);
            if (Files.exists(configPath)) {
                try (FileInputStream fileStream = new FileInputStream(configPath.toFile())) {
                    properties.load(fileStream);
                    logger.info(() -> "Loaded configuration from file: " + filename);
                    return true;
                }
            }
            
        } catch (IOException e) {
            logger.warning(() -> "Failed to load configuration file: " + filename + " - " + e.getMessage());
        }
        
        return false;
    }
    
    private void expandSystemProperties() {
        for (String key : properties.stringPropertyNames()) {
            String value = properties.getProperty(key);
            if (value != null && value.contains("${")) {
                String expandedValue = expandSystemProperty(value);
                properties.setProperty(key, expandedValue);
            }
        }
    }
    
    private String expandSystemProperty(String value) {
        String result = value;
        
        // Replace ${user.home}
        result = result.replace("${user.home}", System.getProperty("user.home"));
        
        // Replace ${user.dir}
        result = result.replace("${user.dir}", System.getProperty("user.dir"));
        
        // Replace other properties within the same config
        for (String key : properties.stringPropertyNames()) {
            String placeholder = "${" + key + "}";
            if (result.contains(placeholder) && !key.equals(getKeyForValue(value))) {
                result = result.replace(placeholder, properties.getProperty(key));
            }
        }
        
        return result;
    }
    
    private String getKeyForValue(String value) {
        for (String key : properties.stringPropertyNames()) {
            if (properties.getProperty(key).equals(value)) {
                return key;
            }
        }
        return null;
    }
    
    public String getDictionaryPath() {
        String primaryPath = properties.getProperty("dictionary.full.path");
        
        // Check if primary path exists
        if (primaryPath != null && Files.exists(Paths.get(primaryPath))) {
            return primaryPath;
        }
        
        // Try fallback paths
        String fallbackPaths = properties.getProperty("dictionary.fallback.paths", "");
        for (String fallbackPath : fallbackPaths.split(",")) {
            fallbackPath = fallbackPath.trim();
            if (!fallbackPath.isEmpty()) {
                // Expand relative paths
                Path path = Paths.get(fallbackPath);
                if (!path.isAbsolute()) {
                    path = Paths.get(System.getProperty("user.dir"), fallbackPath);
                }
                
                if (Files.exists(path)) {
                    final String pathStr = path.toString();
                    logger.info(() -> "Using fallback dictionary path: " + pathStr);
                    return pathStr;
                }
            }
        }
        
        // If no valid path found, return primary path anyway (will cause error later)
        final String finalPrimaryPath = primaryPath;
        logger.warning(() -> "No valid dictionary path found. Primary path: " + finalPrimaryPath);
        return primaryPath;
    }
    
    public String getOutputBasePath() {
        return properties.getProperty("output.base.path", System.getProperty("user.dir"));
    }
    
    public String generateOutputFilePath(String prefix) {
        String basePath = getOutputBasePath();
        String extension = properties.getProperty("output.file.extension", ".csv");
        long timestamp = System.currentTimeMillis();
        
        return basePath + File.separator + prefix + timestamp + extension;
    }
    
    public String getDetailsFilePath() {
        String prefix = properties.getProperty("output.file.prefix.details", "details-");
        return generateOutputFilePath(prefix);
    }
    
    public String getSummaryFilePath() {
        String prefix = properties.getProperty("output.file.prefix.summary", "summary-");
        return generateOutputFilePath(prefix);
    }
    
    public String getColumnsFilePath() {
        String prefix = properties.getProperty("output.file.prefix.columns", "columns-");
        return generateOutputFilePath(prefix);
    }
    
    public int getWordLength() {
        return Integer.parseInt(properties.getProperty("game.word.length", "5"));
    }
    
    public int getMaxAttempts() {
        return Integer.parseInt(properties.getProperty("game.max.attempts", "6"));
    }
    
    public int getSimulationIterations() {
        return Integer.parseInt(properties.getProperty("game.simulation.iterations", "1"));
    }
    
    public String getProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }
    
    public void setProperty(String key, String value) {
        properties.setProperty(key, value);
    }
    
    /**
     * Creates a Config object populated with values from the configuration file
     */
    public Config createGameConfig() {
        Config config = new Config();
        config.setWordLength(getWordLength());
        config.setMaxAttempts(getMaxAttempts());
        config.setPathToDictionaryOfAllWords(getDictionaryPath());
        config.setPathToDictionaryOfGameWords(getDictionaryPath());
        
        return config;
    }
    
    /**
     * Validates that required configuration is present and accessible
     */
    public boolean validateConfiguration() {
        boolean valid = true;
        
        // Check dictionary path
        String dictPath = getDictionaryPath();
        if (dictPath == null || !Files.exists(Paths.get(dictPath))) {
            final String finalDictPath = dictPath;
            logger.severe(() -> "Dictionary file not found: " + finalDictPath);
            valid = false;
        }
        
        // Check output directory
        String outputPath = getOutputBasePath();
        try {
            Path outputDir = Paths.get(outputPath);
            if (!Files.exists(outputDir)) {
                Files.createDirectories(outputDir);
                final String finalOutputPath = outputPath;
                logger.info(() -> "Created output directory: " + finalOutputPath);
            }
        } catch (IOException e) {
            final String finalOutputPath = outputPath;
            logger.severe(() -> "Cannot create output directory: " + finalOutputPath + " - " + e.getMessage());
            valid = false;
        }
        
        return valid;
    }
    
    /**
     * Get all available dictionary options from configuration
     */
    public List<DictionaryOption> getAvailableDictionaries() {
        List<DictionaryOption> dictionaries = new ArrayList<>();
        
        // Find all dictionary configurations
        for (String key : properties.stringPropertyNames()) {
            if (key.startsWith("dictionaries.") && key.endsWith(".name")) {
                String id = key.substring("dictionaries.".length(), key.length() - ".name".length());
                DictionaryOption option = createDictionaryOption(id);
                if (option != null) {
                    dictionaries.add(option);
                }
            }
        }
        
        // Sort dictionaries by word length (4, 5, 6, 7...)
        dictionaries.sort((d1, d2) -> Integer.compare(d1.getWordLength(), d2.getWordLength()));
        
        return dictionaries;
    }
    
    /**
     * Create a DictionaryOption from configuration
     */
    private DictionaryOption createDictionaryOption(String id) {
        String name = properties.getProperty("dictionaries." + id + ".name");
        String path = properties.getProperty("dictionaries." + id + ".path");
        String fallbackPath = properties.getProperty("dictionaries." + id + ".fallbackPath");
        String wordLengthStr = properties.getProperty("dictionaries." + id + ".wordLength");
        String description = properties.getProperty("dictionaries." + id + ".description", "");
        
        if (name == null || wordLengthStr == null) {
            return null;
        }
        
        int wordLength = Integer.parseInt(wordLengthStr);
        
        DictionaryOption option = new DictionaryOption(id, name, path, fallbackPath, wordLength, description);
        
        // Check if dictionary file is available
        String resolvedPath = resolveDictionaryPath(path, fallbackPath);
        boolean isAvailable = false;
        
        if (resolvedPath != null) {
            if (resolvedPath.startsWith("classpath:")) {
                // For classpath resources, check if the resource exists
                String resourcePath = resolvedPath.substring("classpath:".length());
                isAvailable = getClass().getClassLoader().getResource(resourcePath) != null;
            } else {
                // For filesystem paths, check if file exists
                isAvailable = Files.exists(Paths.get(resolvedPath));
            }
        }
        
        option.setAvailable(isAvailable);
        
        return option;
    }
    
    /**
     * Get dictionary path by ID
     */
    public String getDictionaryPathById(String id) {
        String path = properties.getProperty("dictionaries." + id + ".path");
        String fallbackPath = properties.getProperty("dictionaries." + id + ".fallbackPath");
        
        return resolveDictionaryPath(path, fallbackPath);
    }
    
    /**
     * Resolve dictionary path with fallback support
     */
    private String resolveDictionaryPath(String primaryPath, String fallbackPath) {
        // Try primary path first
        if (primaryPath != null && !primaryPath.isEmpty()) {
            Path path = Paths.get(primaryPath);
            if (Files.exists(path)) {
                return path.toString();
            }
            logger.warning(() -> "Primary dictionary path not found: " + primaryPath + ", trying fallback");
        }
        
        // Try fallback path
        if (fallbackPath != null && !fallbackPath.isEmpty()) {
            // First check if it's a classpath resource
            InputStream resourceStream = getClass().getClassLoader().getResourceAsStream(fallbackPath);
            if (resourceStream != null) {
                try {
                    resourceStream.close();
                    // Return the classpath path prefixed with "classpath:"
                    logger.info(() -> "Using classpath resource: " + fallbackPath);
                    return "classpath:" + fallbackPath;
                } catch (IOException e) {
                    logger.warning(() -> "Error checking classpath resource: " + fallbackPath);
                }
            }
            
            // Check as filesystem path
            Path path = Paths.get(fallbackPath);
            if (!path.isAbsolute()) {
                path = Paths.get(System.getProperty("user.dir"), fallbackPath);
            }
            
            if (Files.exists(path)) {
                logger.info(() -> "Using filesystem fallback: " + path);
                return path.toString();
            }
        }
        
        // If both paths failed, return null to indicate failure
        logger.severe(() -> "Failed to resolve dictionary path. Primary: " + primaryPath + ", Fallback: " + fallbackPath);
        return null;
    }
    
    /**
     * Get word length for a specific dictionary
     */
    public int getWordLengthForDictionary(String dictionaryId) {
        String wordLengthStr = properties.getProperty("dictionaries." + dictionaryId + ".wordLength");
        if (wordLengthStr != null) {
            return Integer.parseInt(wordLengthStr);
        }
        return getWordLength(); // Default
    }
}
