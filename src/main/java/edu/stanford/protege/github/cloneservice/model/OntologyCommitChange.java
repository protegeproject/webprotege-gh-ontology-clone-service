package edu.stanford.protege.github.cloneservice.model;

import edu.stanford.protege.commitnavigator.model.CommitMetadata;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Objects;

/**
 * Represents a difference between two ontology versions containing a set of axiom changes
 * and commit metadata
 */
public record OntologyCommitChange(
        @Nonnull List<AxiomChange> axiomChanges,
        @Nonnull CommitMetadata commitMetadata
) {

    public OntologyCommitChange {
        Objects.requireNonNull(axiomChanges, "axiomChanges cannot be null");
        Objects.requireNonNull(commitMetadata, "commitMetadata cannot be null");
    }
}