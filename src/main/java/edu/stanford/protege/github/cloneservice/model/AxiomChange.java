package edu.stanford.protege.github.cloneservice.model;

import org.semanticweb.owlapi.model.OWLAxiom;

import javax.annotation.Nonnull;
import java.util.Objects;

/**
 * Represents a single axiom change containing an operation type and the OWL axiom involved
 */
public record AxiomChange(
        @Nonnull OperationType operationType,
        @Nonnull OWLAxiom axiom
) {

    public AxiomChange {
        Objects.requireNonNull(operationType, "operationType cannot be null");
        Objects.requireNonNull(axiom, "axiom cannot be null");
    }

    public static AxiomChange addAxiom(@Nonnull OWLAxiom axiom) {
        return new AxiomChange(OperationType.ADD, axiom);
    }

    public static AxiomChange removeAxiom(@Nonnull OWLAxiom axiom) {
        return new AxiomChange(OperationType.REMOVE, axiom);
    }

    /**
     * Type of operation performed on the axiom
     */
    public enum OperationType {
        ADD, REMOVE
    }
}
