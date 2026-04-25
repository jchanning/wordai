package com.fistraltech.server.controller;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fistraltech.analysis.AnalysisResponse;
import com.fistraltech.server.WordGameService;
import com.fistraltech.server.dto.AnalysisRequest;
import com.fistraltech.web.ApiErrors;

import jakarta.validation.Valid;

/**
 * REST controller for analysis resources.
 *
 * <p><strong>Base path</strong>: {@code /api/wordai/analysis}
 *
 * <p><strong>Primary resources</strong>
 * <ul>
 *   <li><strong>Dictionary analysis</strong>: run server-side algorithm analysis over a dictionary</li>
 * </ul>
 *
 * <p><strong>Typical flow</strong>
 * <ol>
 *   <li>Submit an analysis request: {@code POST /api/wordai/analysis}</li>
 *   <li>Read the aggregated result set from the response body</li>
 * </ol>
 */
@RestController
@RequestMapping({ApiRoutes.LEGACY_ROOT + "/analysis", ApiRoutes.V1_ROOT + "/analysis"})
public class AnalysisController {

    private static final Logger logger = Logger.getLogger(AnalysisController.class.getName());

    private final WordGameService wordGameService;

    public AnalysisController(WordGameService wordGameService) {
        this.wordGameService = wordGameService;
    }

    /**
     * Run full dictionary analysis with the specified algorithm.
     * POST /api/wordai/analysis
     */
    @PostMapping
    public ResponseEntity<?> runAnalysis(@Valid @RequestBody AnalysisRequest request) {
        try {
            logger.info("Starting analysis with algorithm: " + request.getAlgorithm()
                    + ", dictionary: " + request.getDictionaryId());

            AnalysisResponse response = wordGameService.runAnalysis(
                    request.getAlgorithm(), request.getDictionaryId(), request.getMaxGames());

            logger.info("Analysis completed: " + response.getTotalGames() + " games, "
                    + response.getWinRate() + "% win rate");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error running analysis", e);
            return ApiErrors.response(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR,
                    "Analysis failed", e.getMessage());
        }
    }
}