package com.fistraltech;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

/**
 * Architectural fitness functions for the WordAI application.
 *
 * <p>These tests enforce the package layering rules documented in
 * .github/copilot-instructions.md. Violations caught here are caught on every
 * commit — before they accumulate as technical debt.
 *
 * <p><strong>Intended layer hierarchy (top → bottom):</strong>
 * <pre>
 *   server  (HTTP API, Spring controllers, DTOs)
 *     ↓
 *   analysis  (analytics, entropy, complexity)
 *     ↓
 *   bot  (players, strategies, filter)
 *     ↓
 *   core  (WordGame, Dictionary, Response — pure domain)
 *     ↓
 *   util  (Config, ConfigManager — pure utilities)
 * </pre>
 *
 * <p><strong>Currently enforced (green):</strong> rules that the codebase already satisfies.
 * <p><strong>@Disabled (red — known violations):</strong> rules that document existing
 * architectural drift. Each has a TODO explaining the fix required to enable it.
 */
@DisplayName("Architecture Fitness Tests")
class ArchitectureFitnessTest {

    private static JavaClasses APPLICATION_CLASSES;

    @BeforeAll
    static void importClasses() {
        APPLICATION_CLASSES = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.fistraltech");
    }

    // =========================================================================
    // ENFORCED — these rules pass today and must continue to pass
    // =========================================================================

    @Test
    @DisplayName("bot layer must not import from server layer")
    void bot_mustNotDependOn_server() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("com.fistraltech.bot..")
                .should().dependOnClassesThat()
                .resideInAPackage("com.fistraltech.server..");
        rule.check(APPLICATION_CLASSES);
    }

    @Test
    @DisplayName("game layer must not import from server layer")
    void game_mustNotDependOn_server() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("com.fistraltech.game..")
                .should().dependOnClassesThat()
                .resideInAPackage("com.fistraltech.server..");
        rule.check(APPLICATION_CLASSES);
    }

    // =========================================================================
    // DISABLED — known violations that document architectural drift.
    //
    // The root cause of all violations below is that DictionaryOption lives in
    // com.fistraltech.server.dto but is used by core, util, and analysis.
    // Fix: move DictionaryOption (and any other shared DTOs) to
    // com.fistraltech.core or com.fistraltech.util, then re-enable these tests.
    // =========================================================================

    @Test
    @Disabled("TODO: DictionaryManager imports server.dto.DictionaryOption — " +
              "move DictionaryOption to com.fistraltech.core to fix")
    @DisplayName("core layer must not import from server layer")
    void core_mustNotDependOn_server() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("com.fistraltech.core..")
                .should().dependOnClassesThat()
                .resideInAPackage("com.fistraltech.server..");
        rule.check(APPLICATION_CLASSES);
    }

    @Test
    @Disabled("TODO: Config and ConfigManager import server.dto.DictionaryOption — " +
              "move DictionaryOption to com.fistraltech.core or com.fistraltech.util to fix")
    @DisplayName("util layer must not import from server layer")
    void util_mustNotDependOn_server() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("com.fistraltech.util..")
                .should().dependOnClassesThat()
                .resideInAPackage("com.fistraltech.server..");
        rule.check(APPLICATION_CLASSES);
    }

    @Test
    @Disabled("TODO: PlayerAnalyser imports server.dto.AnalysisGameResult and AnalysisResponse — " +
              "move those DTOs to com.fistraltech.analysis or introduce a shared results model")
    @DisplayName("analysis layer must not import from server layer")
    void analysis_mustNotDependOn_server() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("com.fistraltech.analysis..")
                .should().dependOnClassesThat()
                .resideInAPackage("com.fistraltech.server..");
        rule.check(APPLICATION_CLASSES);
    }

    @Test
    @Disabled("TODO: Cyclic dependency exists between core and bot " +
              "(core.Dictionary → bot.filter.FilterCharacters, " +
              "core.ResponseHelper → bot.filter.Filter). " +
              "Fix: move FilterCharacters/Filter to core, or extract an interface in core " +
              "that bot implements.")
    @DisplayName("No cyclic dependencies between com.fistraltech packages")
    void noCyclicPackageDependencies() {
        slices()
                .matching("com.fistraltech.(*)..")
                .should().beFreeOfCycles()
                .check(APPLICATION_CLASSES);
    }
}
