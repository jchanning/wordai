package com.fistraltech;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
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
    @SuppressWarnings("unused")
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
                .resideInAPackage("com.fistraltech.server..")
                .allowEmptyShould(true);
        rule.check(APPLICATION_CLASSES);
    }

    // =========================================================================
    // PARTIALLY ENFORCED — core/util layering rules now pass after moving
    // DictionaryOption into util. Remaining disabled rules document the next
    // architectural cleanup targets.
    // =========================================================================

    @Test
    @DisplayName("core layer must not import from server layer")
    void core_mustNotDependOn_server() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("com.fistraltech.core..")
                .should().dependOnClassesThat()
                .resideInAPackage("com.fistraltech.server..");
        rule.check(APPLICATION_CLASSES);
    }

    @Test
    @DisplayName("util layer must not import from server layer")
    void util_mustNotDependOn_server() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("com.fistraltech.util..")
                .should().dependOnClassesThat()
                .resideInAPackage("com.fistraltech.server..");
        rule.check(APPLICATION_CLASSES);
    }

    @Test
    @DisplayName("analysis layer must not import from server layer")
    void analysis_mustNotDependOn_server() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("com.fistraltech.analysis..")
                .should().dependOnClassesThat()
                .resideInAPackage("com.fistraltech.server..");
        rule.check(APPLICATION_CLASSES);
    }

    @Test
    @DisplayName("core layer must not import from bot layer")
    void core_mustNotDependOn_bot() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("com.fistraltech.core..")
                .should().dependOnClassesThat()
                .resideInAPackage("com.fistraltech.bot..");
        rule.check(APPLICATION_CLASSES);
    }

    @Test
    @DisplayName("server runtime config access must be centralised in DictionaryService")
    void server_runtimeConfigAccess_mustBeCentralisedIn_dictionaryService() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("com.fistraltech.server..")
                .and().doNotHaveSimpleName("DictionaryService")
                .should().dependOnClassesThat()
                .haveFullyQualifiedName("com.fistraltech.util.ConfigManager");
        rule.check(APPLICATION_CLASSES);
    }

    @Test
    @DisplayName("No cyclic dependencies between com.fistraltech packages")
    void noCyclicPackageDependencies() {
        slices()
                .matching("com.fistraltech.(*)..")
                .should().beFreeOfCycles()
                .check(APPLICATION_CLASSES);
    }
}
