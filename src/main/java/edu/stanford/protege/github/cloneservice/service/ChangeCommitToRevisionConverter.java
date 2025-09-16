package edu.stanford.protege.github.cloneservice.service;

import com.google.common.collect.ImmutableList;
import edu.stanford.protege.commitnavigator.model.CommitMetadata;
import edu.stanford.protege.github.cloneservice.model.AxiomChange;
import edu.stanford.protege.github.cloneservice.model.OntologyCommitChange;
import edu.stanford.protege.webprotege.change.AddAxiomChange;
import edu.stanford.protege.webprotege.change.OntologyChange;
import edu.stanford.protege.webprotege.change.RemoveAxiomChange;
import edu.stanford.protege.webprotege.common.UserId;
import edu.stanford.protege.webprotege.revision.Revision;
import edu.stanford.protege.webprotege.revision.RevisionNumber;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Component;

@Component
public class ChangeCommitToRevisionConverter {

  private final AtomicLong orderNumber = new AtomicLong(1);

  /**
   * Converts an ontology commit change from a GitHub repository into a WebProtege revision.
   *
   * <p>This method transforms git commit information including metadata and axiom changes into a
   * format suitable for WebProtege's revision system. Each commit is assigned an incrementing
   * revision number to maintain chronological ordering.
   *
   * @param ontologyCommitChange the commit change containing git metadata and axiom modifications
   * @return a {@link Revision} representing the commit as a WebProtege revision with user ID,
   *     revision number, ontology changes, timestamp, and commit message
   */
  public Revision convert(OntologyCommitChange ontologyCommitChange) {
    var commitMetadata = ontologyCommitChange.commitMetadata();
    var repositoryUrl = ontologyCommitChange.repositoryUrl();
    var userId = UserId.valueOf(commitMetadata.committerUsername());
    var revisionNumber = RevisionNumber.getRevisionNumber(orderNumber.getAndIncrement());
    var ontologyChanges =
        ontologyCommitChange.axiomChanges().stream()
            .map(this::convertToOntologyChange)
            .collect(ImmutableList.toImmutableList());
    var commitTimestamp = commitMetadata.commitDate().toEpochMilli();
    var commitMessage = generateCommitMessage(commitMetadata, repositoryUrl);
    return new Revision(userId, revisionNumber, ontologyChanges, commitTimestamp, commitMessage);
  }

  private OntologyChange convertToOntologyChange(AxiomChange axiomChange) {
    var operation = axiomChange.operationType();
    return switch (operation) {
      case ADD -> new AddAxiomChange(axiomChange.ontologyID(), axiomChange.axiom());
      case REMOVE -> new RemoveAxiomChange(axiomChange.ontologyID(), axiomChange.axiom());
    };
  }

  private String generateCommitMessage(CommitMetadata commitMetadata, String repositoryUrl) {
    var message = """
            **Commit** [%s](%s):
            %s
            """;
    return message.formatted(
        commitMetadata.commitHash(),
        repositoryUrl + "/commit/" + commitMetadata.commitHash(),
        commitMetadata.commitMessage());
  }
}
