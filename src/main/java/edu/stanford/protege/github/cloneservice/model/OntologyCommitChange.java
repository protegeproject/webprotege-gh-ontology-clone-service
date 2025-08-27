package edu.stanford.protege.github.cloneservice.model;

import com.google.common.collect.ImmutableList;
import edu.stanford.protege.commitnavigator.model.CommitMetadata;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nonnull;

/**
 * Represents a difference between two ontology versions containing a set of axiom changes and
 * commit metadata
 */
public record OntologyCommitChange(
    @Nonnull List<AxiomChange> axiomChanges, @Nonnull CommitMetadata commitMetadata) {

  public OntologyCommitChange {
    Objects.requireNonNull(axiomChanges, "axiomChanges cannot be null");
    Objects.requireNonNull(commitMetadata, "commitMetadata cannot be null");
    // Create defensive copy to prevent external mutation
    axiomChanges = ImmutableList.copyOf(axiomChanges);
  }
}
