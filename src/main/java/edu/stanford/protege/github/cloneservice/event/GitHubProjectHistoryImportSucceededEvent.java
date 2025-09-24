package edu.stanford.protege.github.cloneservice.event;

import edu.stanford.protege.commitnavigator.model.RepositoryCoordinates;
import edu.stanford.protege.webprotege.common.EventId;
import edu.stanford.protege.webprotege.common.ProjectEvent;
import edu.stanford.protege.webprotege.common.ProjectId;

/**
 * Event dispatched when project history is successfully imported from a GitHub repository.
 *
 * @param eventId The correlation event ID for tracking the operation
 * @param projectId The project for which the history was imported
 * @param repositoryCoordinates The coordinates of the repository from which history was imported
 */
public record GitHubProjectHistoryImportSucceededEvent(
    EventId eventId, ProjectId projectId, RepositoryCoordinates repositoryCoordinates)
    implements ProjectEvent {

  private static final String CHANNEL =
      "webprotege.events.projects.GitHubProjectHistoryImportSucceededEvent";

  @Override
  public String getChannel() {
    return CHANNEL;
  }
}
