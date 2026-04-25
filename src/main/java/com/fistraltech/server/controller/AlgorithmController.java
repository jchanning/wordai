package com.fistraltech.server.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fistraltech.server.AlgorithmFeatureService;

/**
 * REST controller for algorithm-catalog resources.
 *
 * <p><strong>Base path</strong>: {@code /api/wordai/algorithms}
 *
 * <p><strong>Primary resources</strong>
 * <ul>
 *   <li><strong>Algorithm catalog</strong>: list selection strategies exposed by the server</li>
 * </ul>
 *
 * <p><strong>Typical flow</strong>
 * <ol>
 *   <li>List algorithms: {@code GET /api/wordai/algorithms}</li>
 * </ol>
 */
@RestController
@RequestMapping({ApiRoutes.LEGACY_ROOT + "/algorithms", ApiRoutes.V1_ROOT + "/algorithms"})
public class AlgorithmController {

    private final AlgorithmFeatureService algorithmFeatureService;

    public AlgorithmController(AlgorithmFeatureService algorithmFeatureService) {
        this.algorithmFeatureService = algorithmFeatureService;
    }

    /**
     * Get available selection algorithms.
     * GET /api/wordai/algorithms
     */
    @GetMapping
    public ResponseEntity<List<Map<String, String>>> getAlgorithms() {
        List<Map<String, String>> algorithms = algorithmFeatureService.getAllAlgorithms().values().stream()
                .map(algorithm -> Map.of(
                        "id", algorithm.getId(),
                        "name", algorithm.getDisplayName(),
                        "description", algorithm.getDescription(),
                    "stateful", String.valueOf(algorithm.isStateful()),
                        "enabled", String.valueOf(algorithm.isEnabled())))
                .toList();
        return ResponseEntity.ok(algorithms);
    }
}