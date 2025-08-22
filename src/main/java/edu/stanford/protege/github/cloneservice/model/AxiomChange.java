package edu.stanford.protege.github.cloneservice.model;

import java.util.Objects;
import javax.annotation.Nonnull;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLOntologyID;

/** Represents a single axiom change containing an operation type and the OWL axiom involved */
public record AxiomChange(
    @Nonnull OperationType operationType,
    @Nonnull OWLAxiom axiom,
    @Nonnull OWLOntologyID ontologyID) {

  public AxiomChange {
    Objects.requireNonNull(operationType, "operationType cannot be null");
    Objects.requireNonNull(axiom, "axiom cannot be null");
    Objects.requireNonNull(ontologyID, "ontologyID cannot be null");
  }

  public static AxiomChange addAxiom(@Nonnull OWLAxiom axiom, @Nonnull OWLOntologyID ontologyID) {
    return new AxiomChange(OperationType.ADD, axiom, ontologyID);
  }

  public static AxiomChange removeAxiom(
      @Nonnull OWLAxiom axiom, @Nonnull OWLOntologyID ontologyID) {
    return new AxiomChange(OperationType.REMOVE, axiom, ontologyID);
  }

  /** Type of operation performed on the axiom */
  public enum OperationType {
    ADD,
    REMOVE
  }
}
