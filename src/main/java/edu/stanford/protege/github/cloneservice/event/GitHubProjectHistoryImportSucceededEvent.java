package edu.stanford.protege.github.cloneservice.event;

import edu.stanford.protege.commitnavigator.model.RepositoryCoordinates;
import edu.stanford.protege.webprotege.common.EventId;
import edu.stanford.protege.webprotege.common.ProjectEvent;
import edu.stanford.protege.webprotege.common.ProjectId;
import edu.stanford.protege.webprotege.common.RequestId;

/**
 * Event dispatched when project history is successfully imported from a GitHub repository.
 *
 * @param projectId The project for which the history was imported
 * @param requestId The correlated request ID for tracking the operation
 * @param eventId The correlation event ID for tracking the operation
 * @param repositoryCoordinates The coordinates of the repository from which history was imported
 */
public record GitHubProjectHistoryImportSucceededEvent(
    ProjectId projectId,
    RequestId requestId,
    EventId eventId,
    RepositoryCoordinates repositoryCoordinates)
    implements ProjectEvent {

  private static final String CHANNEL =
      "webprotege.events.projects.GitHubProjectHistoryImportSucceededEvent";

  @Override
  public String getChannel() {
    return CHANNEL;
  }

  @Override
  public EventId eventId() {
    return null;
  }
}
