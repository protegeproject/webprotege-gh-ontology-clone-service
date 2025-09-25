package edu.stanford.protege.github.cloneservice.event;

import edu.stanford.protege.commitnavigator.model.RepositoryCoordinates;
import edu.stanford.protege.webprotege.common.*;

/**
 * Event dispatched when project history is successfully imported from a GitHub repository.
 *
 * @param projectId The project for which the history was imported
 * @param requestId The correlated request ID for tracking the operation
 * @param eventId The correlation event ID for tracking the operation
 * @param repositoryCoordinates The coordinates of the repository from which history was imported
 * @param projectHistoryLocation The location where the project history is stored
 */
public record GitHubProjectHistoryStoreSucceededEvent(
    ProjectId projectId,
    RequestId requestId,
    EventId eventId,
    RepositoryCoordinates repositoryCoordinates,
    BlobLocation projectHistoryLocation)
    implements ProjectEvent {

  private static final String CHANNEL =
      "webprotege.events.projects.GitHubProjectHistoryStoreSucceededEvent";

  @Override
  public String getChannel() {
    return CHANNEL;
  }
}
