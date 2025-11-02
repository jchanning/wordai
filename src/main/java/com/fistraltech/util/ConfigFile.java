package com.fistraltech.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * A utility class to handle JSON configuration file operations.
 * This class provides methods to read and write configuration data to/from JSON files.
 */
public final class ConfigFile {
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();
    
    private ConfigFile() {
        // Private constructor to prevent instantiation
        throw new AssertionError("Utility class should not be instantiated");
    }

    /**
     * Reads configuration from a JSON file and converts it to a Config object.
     *
     * @param configFilePath the path to the configuration file
     * @return a populated Config object
     * @throws IOException if there's an error reading the file
     * @throws IllegalArgumentException if the configFilePath is null or empty
     */
    public static Config getConfig(String configFilePath) throws IOException {
        if (configFilePath == null || configFilePath.trim().isEmpty()) {
            throw new IllegalArgumentException("Config file path cannot be null or empty");
        }

        try (FileReader reader = new FileReader(configFilePath)) {
            return GSON.fromJson(reader, Config.class);
        }
    }

    /**
     * Saves the configuration object to a JSON file.
     *
     * @param config the configuration object to save
     * @param configFilePath the path where to save the configuration file
     * @throws IOException if there's an error writing the file
     * @throws IllegalArgumentException if the config is null or configFilePath is null or empty
     */
    public static void saveConfig(Config config, String configFilePath) throws IOException {
        if (config == null) {
            throw new IllegalArgumentException("Config object cannot be null");
        }
        if (configFilePath == null || configFilePath.trim().isEmpty()) {
            throw new IllegalArgumentException("Config file path cannot be null or empty");
        }

        try (FileWriter writer = new FileWriter(configFilePath)) {
            GSON.toJson(config, writer);
        }
    }

    /**
     * Creates a backup of the current configuration file.
     *
     * @param configFilePath the path to the configuration file to backup
     * @return the path to the backup file
     * @throws IOException if there's an error creating the backup
     */
    public static String backupConfig(String configFilePath) throws IOException {
        if (configFilePath == null || configFilePath.trim().isEmpty()) {
            throw new IllegalArgumentException("Config file path cannot be null or empty");
        }

        Path path = Paths.get(configFilePath);
        String backupPath = configFilePath + ".backup";
        Config currentConfig = getConfig(configFilePath);
        saveConfig(currentConfig, backupPath);
        return backupPath;
    }
}