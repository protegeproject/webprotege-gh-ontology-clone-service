package edu.stanford.protege.github.cloneservice.utils;

import edu.stanford.protege.commitnavigator.GitHubRepository;
import edu.stanford.protege.github.cloneservice.exception.OntologyComparisonException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link OntologyHistoryAnalyzer}
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OntologyHistoryAnalyzer Tests")
class OntologyHistoryAnalyzerTest {

    private OntologyHistoryAnalyzer historyAnalyzer;

    @Mock
    private OntologyLoader ontologyLoader;

    @Mock
    private OntologyDifferenceCalculator differenceCalculator;

    @Mock
    private GitHubRepository gitHubRepository;

    @BeforeEach
    void setUp() {
        historyAnalyzer = new OntologyHistoryAnalyzer(ontologyLoader, differenceCalculator);
    }

    @Test
    @DisplayName("Should throw NullPointerException when ontologyLoader is null")
    void throwExceptionWhenOntologyLoaderNull() {
        var exception = assertThrows(NullPointerException.class, () ->
            new OntologyHistoryAnalyzer(null, differenceCalculator)
        );

        assertEquals("OntologyLoader cannot be null", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw NullPointerException when differenceCalculator is null")
    void throwExceptionWhenDifferenceCalculatorNull() {
        var exception = assertThrows(NullPointerException.class, () ->
            new OntologyHistoryAnalyzer(ontologyLoader, null)
        );

        assertEquals("OntologyDifferenceCalculator cannot be null", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw NullPointerException when ontologyFilePath is null")
    void throwExceptionWhenOntologyFilePathNull() {
        var exception = assertThrows(NullPointerException.class, () ->
            historyAnalyzer.getCommitHistory(null, gitHubRepository)
        );

        assertEquals("ontologyFilePath cannot be null", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw NullPointerException when gitHubRepository is null")
    void throwExceptionWhenGitHubRepositoryNull() {
        var exception = assertThrows(NullPointerException.class, () ->
            historyAnalyzer.getCommitHistory("ontology.owl", null)
        );

        assertEquals("gitHubRepository cannot be null", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw OntologyComparisonException when repository operations fail")
    void throwOntologyComparisonExceptionWhenRepositoryOperationsFail() throws Exception {
        var ontologyFileName = "test.owl";

        // Simulate the repository throwing an exception during navigation
        when(gitHubRepository.getCommitNavigator()).thenThrow(new RuntimeException("Repository error"));

        var exception = assertThrows(OntologyComparisonException.class, () ->
            historyAnalyzer.getCommitHistory(ontologyFileName, gitHubRepository)
        );

        assertNotNull(exception.getMessage());
        assertTrue(exception.getMessage().contains("Failed to analyze ontology commit history"));
        assertNotNull(exception.getCause());
        assertInstanceOf(RuntimeException.class, exception.getCause());
        assertEquals("Repository error", exception.getCause().getMessage());
    }

    @Test
    @DisplayName("Should validate constructor parameters using Objects.requireNonNull")
    void validateConstructorParametersUsingObjectsRequireNonNull() {
        // Test first parameter validation
        var exception1 = assertThrows(NullPointerException.class, () ->
            new OntologyHistoryAnalyzer(null, differenceCalculator)
        );
        assertEquals("OntologyLoader cannot be null", exception1.getMessage());

        // Test second parameter validation  
        var exception2 = assertThrows(NullPointerException.class, () ->
            new OntologyHistoryAnalyzer(ontologyLoader, null)
        );
        assertEquals("OntologyDifferenceCalculator cannot be null", exception2.getMessage());
    }

    @Test
    @DisplayName("Should validate getCommitHistory parameters using Objects.requireNonNull")
    void validateGetCommitHistoryParametersUsingObjectsRequireNonNull() {
        // Test ontologyFilePath validation
        var exception1 = assertThrows(NullPointerException.class, () ->
            historyAnalyzer.getCommitHistory(null, gitHubRepository)
        );
        assertEquals("ontologyFilePath cannot be null", exception1.getMessage());

        // Test gitHubRepository validation
        var exception2 = assertThrows(NullPointerException.class, () ->
            historyAnalyzer.getCommitHistory("test.owl", null)
        );
        assertEquals("gitHubRepository cannot be null", exception2.getMessage());
    }
}