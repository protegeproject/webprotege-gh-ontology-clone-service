package edu.stanford.protege.github.cloneservice.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLOntologyID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link AxiomChange} record
 */
@DisplayName("AxiomChange Tests")
class AxiomChangeTest {

    private OWLAxiom mockAxiom;
    private OWLOntologyID mockOntologyId;

    @BeforeEach
    void setUp() {
        mockAxiom = mock(OWLAxiom.class);
        mockOntologyId = mock(OWLOntologyID.class);
    }

    @Test
    @DisplayName("Should create AxiomChange with all required parameters")
    void createAxiomChangeWhenAllParametersProvided() {
        var axiomChange = new AxiomChange(
            AxiomChange.OperationType.ADD,
            mockAxiom,
            mockOntologyId
        );

        assertEquals(AxiomChange.OperationType.ADD, axiomChange.operationType());
        assertEquals(mockAxiom, axiomChange.axiom());
        assertEquals(mockOntologyId, axiomChange.ontologyID());
    }

    @Test
    @DisplayName("Should throw NullPointerException when operationType is null")
    void throwExceptionWhenOperationTypeNull() {
        var exception = assertThrows(NullPointerException.class, () ->
            new AxiomChange(null, mockAxiom, mockOntologyId)
        );

        assertEquals("operationType cannot be null", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw NullPointerException when axiom is null")
    void throwExceptionWhenAxiomNull() {
        var exception = assertThrows(NullPointerException.class, () ->
            new AxiomChange(AxiomChange.OperationType.ADD, null, mockOntologyId)
        );

        assertEquals("axiom cannot be null", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw NullPointerException when ontologyID is null")
    void throwExceptionWhenOntologyIdNull() {
        var exception = assertThrows(NullPointerException.class, () ->
            new AxiomChange(AxiomChange.OperationType.ADD, mockAxiom, null)
        );

        assertEquals("ontologyID cannot be null", exception.getMessage());
    }

    @Test
    @DisplayName("Should create ADD AxiomChange using addAxiom factory method")
    void createAddAxiomChangeUsingFactoryMethod() {
        var axiomChange = AxiomChange.addAxiom(mockAxiom, mockOntologyId);

        assertEquals(AxiomChange.OperationType.ADD, axiomChange.operationType());
        assertEquals(mockAxiom, axiomChange.axiom());
        assertEquals(mockOntologyId, axiomChange.ontologyID());
    }

    @Test
    @DisplayName("Should create REMOVE AxiomChange using removeAxiom factory method")
    void createRemoveAxiomChangeUsingFactoryMethod() {
        var axiomChange = AxiomChange.removeAxiom(mockAxiom, mockOntologyId);

        assertEquals(AxiomChange.OperationType.REMOVE, axiomChange.operationType());
        assertEquals(mockAxiom, axiomChange.axiom());
        assertEquals(mockOntologyId, axiomChange.ontologyID());
    }

    @Test
    @DisplayName("Should throw NullPointerException when addAxiom called with null axiom")
    void throwExceptionWhenAddAxiomFactoryMethodNullAxiom() {
        var exception = assertThrows(NullPointerException.class, () ->
            AxiomChange.addAxiom(null, mockOntologyId)
        );

        assertEquals("axiom cannot be null", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw NullPointerException when removeAxiom called with null ontologyID")
    void throwExceptionWhenRemoveAxiomFactoryMethodNullOntologyId() {
        var exception = assertThrows(NullPointerException.class, () ->
            AxiomChange.removeAxiom(mockAxiom, null)
        );

        assertEquals("ontologyID cannot be null", exception.getMessage());
    }
}