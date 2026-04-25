package com.fistraltech.server.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fistraltech.core.Dictionary;
import com.fistraltech.core.WordEntropy;
import com.fistraltech.server.DictionaryService;
import com.fistraltech.util.DictionaryOption;

/**
 * REST controller for dictionary catalog and dictionary-detail resources.
 *
 * <p><strong>Base path</strong>: {@code /api/wordai/dictionaries}
 *
 * <p><strong>Primary resources</strong>
 * <ul>
 *   <li><strong>Dictionary catalog</strong>: list available configured dictionaries</li>
 *   <li><strong>Dictionary detail</strong>: fetch the full word list and precomputed entropy data</li>
 * </ul>
 *
 * <p><strong>Typical flow</strong>
 * <ol>
 *   <li>List dictionaries: {@code GET /api/wordai/dictionaries}</li>
 *   <li>Read one dictionary: {@code GET /api/wordai/dictionaries/{dictionaryId}}</li>
 * </ol>
 */
@RestController
@RequestMapping({ApiRoutes.LEGACY_ROOT + "/dictionaries", ApiRoutes.V1_ROOT + "/dictionaries"})
public class DictionaryController {

    private static final Logger logger = Logger.getLogger(DictionaryController.class.getName());

    private final DictionaryService dictionaryService;

    public DictionaryController(DictionaryService dictionaryService) {
        this.dictionaryService = dictionaryService;
    }

    /**
     * Get available dictionaries.
     * GET /api/wordai/dictionaries
     */
    @GetMapping
    public ResponseEntity<List<DictionaryOption>> getDictionaries() {
        try {
            List<DictionaryOption> dictionaries = dictionaryService.getAvailableDictionaries();
            return ResponseEntity.ok(dictionaries);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error getting dictionaries: {0}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get dictionary words with entropy values.
     * GET /api/wordai/dictionaries/{dictionaryId}
     */
    @GetMapping("/{dictionaryId}")
    public ResponseEntity<Map<String, Object>> getDictionary(@PathVariable String dictionaryId) {
        try {
            Dictionary dictionary = dictionaryService.getMasterDictionary(dictionaryId);
            if (dictionary == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            WordEntropy wordEntropy = dictionaryService.getWordEntropy(dictionaryId);

                logger.info(() -> "Dictionary ID: " + dictionaryId + ", Word Length: " + dictionary.getWordLength()
                    + ", WordEntropy found: " + (wordEntropy != null));

            Map<String, Object> response = new HashMap<>();
            response.put("id", dictionaryId);
            response.put("wordLength", dictionary.getWordLength());
            response.put("wordCount", dictionary.getWordCount());
            response.put("words", new ArrayList<>(dictionary.getMasterSetOfWords()));

            if (wordEntropy != null) {
                Map<String, Float> entropyMap = new HashMap<>();
                for (String word : dictionary.getMasterSetOfWords()) {
                    entropyMap.put(word, wordEntropy.getEntropy(word));
                }
                logger.info(() -> "Added entropy values for " + entropyMap.size() + " words");
                response.put("entropy", entropyMap);
            } else {
                logger.warning(() -> "WordEntropy is null for dictionary: " + dictionaryId);
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error loading dictionary: {0}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }
}