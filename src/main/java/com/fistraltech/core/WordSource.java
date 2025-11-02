package com.fistraltech.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Utility class for loading word dictionaries from various sources.
 * 
 * <p>This class provides static methods to load word lists from both filesystem paths
 * and classpath resources, supporting flexible dictionary management for the WordAI game system.
 * 
 * <p><strong>Supported Sources:</strong>
 * <ul>
 *   <li><strong>Filesystem:</strong> Absolute or relative file paths (e.g., "/path/to/words.txt")</li>
 *   <li><strong>Classpath Resources:</strong> Prefixed with "classpath:" (e.g., "classpath:dictionaries/5_letter_words.txt")</li>
 * </ul>
 * 
 * <p><strong>File Format Requirements:</strong>
 * <ul>
 *   <li>One word per line</li>
 *   <li>UTF-8 encoding</li>
 *   <li>Words should be pre-normalized (lowercase, trimmed)</li>
 * </ul>
 * 
 * <p><strong>Usage Examples:</strong>
 * <pre>{@code
 * // Load from filesystem
 * Set<String> words1 = WordSource.getWordsFromFile("/home/user/5-letter-words.txt");
 * 
 * // Load from classpath resource
 * Set<String> words2 = WordSource.getWordsFromFile("classpath:dictionaries/5_letter_words.txt");
 * 
 * // Use with Dictionary
 * Dictionary dict = new Dictionary(5);
 * dict.addWords(words1);
 * }</pre>
 * 
 * <p><strong>Return Value:</strong> Words are returned in a {@link TreeSet}, ensuring:
 * <ul>
 *   <li>Alphabetical ordering</li>
 *   <li>No duplicate words</li>
 *   <li>Efficient lookup operations</li>
 * </ul>
 * 
 * <p><strong>Thread Safety:</strong> This class is thread-safe as it contains only static methods
 * with no shared mutable state.
 * 
 * @author Fistral Technologies
 * @see Dictionary
 * @see ConfigManager
 */
public class WordSource {

    /**
     * Reads words from a file or classpath resource.
     * 
     * <p>This method automatically detects whether the path refers to a filesystem
     * path or a classpath resource based on the "classpath:" prefix.
     * 
     * <p><strong>Filesystem Example:</strong>
     * <pre>{@code
     * Set<String> words = getWordsFromFile("C:/dictionaries/words.txt");
     * }</pre>
     * 
     * <p><strong>Classpath Example:</strong>
     * <pre>{@code
     * Set<String> words = getWordsFromFile("classpath:dictionaries/5_letter_words.txt");
     * }</pre>
     * 
     * @param fileName the path to the file, or "classpath:" prefixed path for classpath resources
     * @return a sorted set of words read from the file, with duplicates removed
     * @throws IOException if an I/O error occurs reading from the file
     * @throws IOException if a classpath resource is not found
     */
    public static Set<String> getWordsFromFile(String fileName) throws IOException {
        List<String> wordsInFile;
        
        // Check if this is a classpath resource
        if (fileName != null && fileName.startsWith("classpath:")) {
            String resourcePath = fileName.substring("classpath:".length());
            InputStream inputStream = WordSource.class.getClassLoader().getResourceAsStream(resourcePath);
            
            if (inputStream == null) {
                throw new IOException("Classpath resource not found: " + resourcePath);
            }
            
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                wordsInFile = reader.lines().collect(Collectors.toList());
            }
        } else {
            // Read from filesystem
            wordsInFile = Files.readAllLines(Paths.get(fileName), StandardCharsets.UTF_8);
        }
        
        return new TreeSet<>(wordsInFile);
    }
}
