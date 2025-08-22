package edu.stanford.protege.github.cloneservice.model;

import edu.stanford.protege.commitnavigator.model.CommitMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link OntologyCommitChange} record
 */
@DisplayName("OntologyCommitChange Tests")
class OntologyCommitChangeTest {

    private List<AxiomChange> mockAxiomChanges;
    private CommitMetadata mockCommitMetadata;

    @BeforeEach
    void setUp() {
        mockAxiomChanges = List.of(mock(AxiomChange.class), mock(AxiomChange.class));
        mockCommitMetadata = mock(CommitMetadata.class);
    }

    @Test
    @DisplayName("Should create OntologyCommitChange with all required parameters")
    void createOntologyCommitChangeWhenAllParametersProvided() {
        var ontologyCommitChange = new OntologyCommitChange(
            mockAxiomChanges,
            mockCommitMetadata
        );

        assertEquals(mockAxiomChanges, ontologyCommitChange.axiomChanges());
        assertEquals(mockCommitMetadata, ontologyCommitChange.commitMetadata());
    }

    @Test
    @DisplayName("Should throw NullPointerException when axiomChanges is null")
    void throwExceptionWhenAxiomChangesNull() {
        var exception = assertThrows(NullPointerException.class, () ->
            new OntologyCommitChange(null, mockCommitMetadata)
        );

        assertEquals("axiomChanges cannot be null", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw NullPointerException when commitMetadata is null")
    void throwExceptionWhenCommitMetadataNull() {
        var exception = assertThrows(NullPointerException.class, () ->
            new OntologyCommitChange(mockAxiomChanges, null)
        );

        assertEquals("commitMetadata cannot be null", exception.getMessage());
    }

    @Test
    @DisplayName("Should accept empty axiom changes list")
    void acceptEmptyAxiomChangesList() {
        var emptyAxiomChanges = List.<AxiomChange>of();
        
        var ontologyCommitChange = new OntologyCommitChange(
            emptyAxiomChanges,
            mockCommitMetadata
        );

        assertEquals(emptyAxiomChanges, ontologyCommitChange.axiomChanges());
        assertTrue(ontologyCommitChange.axiomChanges().isEmpty());
    }
}