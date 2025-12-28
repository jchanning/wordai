package com.fistraltech.server.dto;

/**
 * DTO describing a dictionary option that the server can load.
 *
 * <p><strong>Endpoint</strong>: {@code GET /api/wordai/dictionaries}
 *
 * <p><strong>Example</strong>
 * <pre>{@code
 * {
 *   "id": "5",
 *   "name": "5 Letters",
 *   "wordLength": 5,
 *   "description": "Official 5-letter word list",
 *   "available": true,
 *   "resolvedPath": "..."
 * }
 * }</pre>
 *
 * @author Fistral Technologies
 */
public class DictionaryOption {
    private String id;
    private String name;
    private String path;
    private String fallbackPath;
    private String resolvedPath;  // The actual resolved path (either primary or fallback)
    private int wordLength;
    private String description;
    private boolean available;
    
    public DictionaryOption() {
    }
    
    public DictionaryOption(String id, String name, String path, String fallbackPath, int wordLength, String description) {
        this.id = id;
        this.name = name;
        this.path = path;
        this.fallbackPath = fallbackPath;
        this.wordLength = wordLength;
        this.description = description;
        this.available = false;
    }
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getPath() {
        return path;
    }
    
    public void setPath(String path) {
        this.path = path;
    }
    
    public String getFallbackPath() {
        return fallbackPath;
    }
    
    public void setFallbackPath(String fallbackPath) {
        this.fallbackPath = fallbackPath;
    }
    
    public int getWordLength() {
        return wordLength;
    }
    
    public void setWordLength(int wordLength) {
        this.wordLength = wordLength;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public boolean isAvailable() {
        return available;
    }
    
    public void setAvailable(boolean available) {
        this.available = available;
    }
    
    public String getResolvedPath() {
        return resolvedPath;
    }
    
    public void setResolvedPath(String resolvedPath) {
        this.resolvedPath = resolvedPath;
    }
}
