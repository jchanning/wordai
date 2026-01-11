package com.fistraltech.util;

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
    
    public String getProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }
    
    public void setProperty(String key, String value) {
        properties.setProperty(key, value);
    }
    
    /**
     * Creates a Config object populated with values from the configuration file.
     * This is the primary method for obtaining application configuration.
     */
    public Config createGameConfig() {
        Config config = new Config();
        
        // Read and set basic game configuration
        config.setWordLength(Integer.parseInt(properties.getProperty("game.word.length", "5")));
        config.setMaxAttempts(Integer.parseInt(properties.getProperty("game.max.attempts", "6")));
        config.setSimulationIterations(Integer.parseInt(properties.getProperty("game.simulation.iterations", "1")));
        config.setMinWordLength(Integer.parseInt(properties.getProperty("game.min.word.length", "4")));
        config.setMaxWordLength(Integer.parseInt(properties.getProperty("game.max.word.length", "7")));
        
        // Set dictionary paths (using legacy property for backward compatibility)
        String dictPath = resolveLegacyDictionaryPath();
        config.setPathToDictionaryOfAllWords(dictPath);
        config.setPathToDictionaryOfGameWords(dictPath);
        
        // Set output configuration
        config.setOutputBasePath(properties.getProperty("output.base.path", System.getProperty("user.dir")));
        
        // Build and set available dictionaries
        config.setAvailableDictionaries(buildAvailableDictionaries());
        
        return config;
    }
    
    /**
     * Validates that required configuration is present and accessible
     */
    public boolean validateConfiguration() {
        boolean valid = true;
        
        // Check dictionary path
        String dictPath = resolveLegacyDictionaryPath();
        if (dictPath == null || !Files.exists(Paths.get(dictPath))) {
            final String finalDictPath = dictPath;
            logger.severe(() -> "Dictionary file not found: " + finalDictPath);
            valid = false;
        }
        
        // Check output directory
        String outputPath = properties.getProperty("output.base.path", System.getProperty("user.dir"));
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
     * Resolves legacy dictionary.full.path property for backward compatibility
     * @throws IllegalArgumentException if dictionary path is not configured or not found
     */
    private String resolveLegacyDictionaryPath() {
        String primaryPath = properties.getProperty("dictionary.full.path");
        
        if (primaryPath == null || primaryPath.isEmpty()) {
            throw new IllegalArgumentException("dictionary.full.path is not configured");
        }
        
        // Check if primary path exists
        Path path = Paths.get(primaryPath);
        if (!path.isAbsolute()) {
            path = Paths.get(System.getProperty("user.dir"), primaryPath);
        }
        
        final Path finalPath = path;
        if (Files.exists(finalPath)) {
            logger.info(() -> "Using dictionary path: " + finalPath);
            return finalPath.toString();
        }
        
        throw new IllegalArgumentException("Dictionary file not found: " + primaryPath + " (resolved to: " + finalPath + ")");
    }
    
    /**
     * Build list of available dictionary options from configuration
     */
    private List<DictionaryOption> buildAvailableDictionaries() {
        List<DictionaryOption> dictionaries = new ArrayList<>();
        
        // Find all dictionary configurations
        for (String key : properties.stringPropertyNames()) {
            if (key.startsWith("dictionaries.") && key.endsWith(".name")) {
                String id = key.substring("dictionaries.".length(), key.length() - ".name".length());
                DictionaryOption option = buildDictionaryOption(id);
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
     * Build a DictionaryOption from configuration properties
     * @throws IllegalArgumentException if dictionary path cannot be resolved
     */
    private DictionaryOption buildDictionaryOption(String id) {
        String name = properties.getProperty("dictionaries." + id + ".name");
        String path = properties.getProperty("dictionaries." + id + ".path");
        String wordLengthStr = properties.getProperty("dictionaries." + id + ".wordLength");
        String description = properties.getProperty("dictionaries." + id + ".description", "");
        
        if (name == null || wordLengthStr == null) {
            return null;
        }
        
        int wordLength = Integer.parseInt(wordLengthStr);
        
        DictionaryOption option = new DictionaryOption(id, name, path, null, wordLength, description);
        
        // Resolve dictionary path - will throw exception if invalid
        String resolvedPath = resolveDictionaryPath(path);
        boolean isAvailable = false;
        
        if (resolvedPath.startsWith("classpath:")) {
            // For classpath resources, check if the resource exists
            String resourcePath = resolvedPath.substring("classpath:".length());
            isAvailable = getClass().getClassLoader().getResource(resourcePath) != null;
        } else {
            // For filesystem paths, check if file exists
            isAvailable = Files.exists(Paths.get(resolvedPath));
        }
        
        option.setResolvedPath(resolvedPath);
        option.setAvailable(isAvailable);
        
        return option;
    }
    
    /**
     * Resolve dictionary path - no fallback, fails explicitly if path is invalid
     * @throws IllegalArgumentException if the dictionary path cannot be resolved
     */
    private String resolveDictionaryPath(String dictionaryPath) {
        if (dictionaryPath == null || dictionaryPath.isEmpty()) {
            throw new IllegalArgumentException("Dictionary path is null or empty");
        }
        
        // First check if it's a classpath resource
        InputStream resourceStream = getClass().getClassLoader().getResourceAsStream(dictionaryPath);
        if (resourceStream != null) {
            try {
                resourceStream.close();
                logger.info(() -> "Using classpath resource: " + dictionaryPath);
                return "classpath:" + dictionaryPath;
            } catch (IOException e) {
                logger.warning(() -> "Error checking classpath resource: " + dictionaryPath);
            }
        }
        
        // Then check as filesystem path
        Path path = Paths.get(dictionaryPath);
        if (!path.isAbsolute()) {
            path = Paths.get(System.getProperty("user.dir"), dictionaryPath);
        }
        
        final Path finalPath = path;
        if (Files.exists(finalPath)) {
            logger.info(() -> "Using filesystem path: " + finalPath);
            return finalPath.toString();
        }
        
        // Fail explicitly if dictionary not found
        throw new IllegalArgumentException("Dictionary file not found: " + dictionaryPath + " (resolved to: " + finalPath + ")");
    }
}
