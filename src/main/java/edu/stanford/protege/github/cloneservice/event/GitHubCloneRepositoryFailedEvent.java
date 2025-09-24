package edu.stanford.protege.github.cloneservice.event;

import edu.stanford.protege.commitnavigator.model.RepositoryCoordinates;
import edu.stanford.protege.webprotege.common.EventId;
import edu.stanford.protege.webprotege.common.ProjectEvent;
import edu.stanford.protege.webprotege.common.ProjectId;

/**
 * Event dispatched when cloning a GitHub repository fails.
 *
 * @param eventId The correlation event ID for tracking the operation
 * @param projectId The project for which the repository clone failed
 * @param repositoryCoordinates The coordinates of the repository that failed to clone
 * @param errorMessage The error message describing the failure
 */
public record GitHubCloneRepositoryFailedEvent(
    EventId eventId,
    ProjectId projectId,
    RepositoryCoordinates repositoryCoordinates,
    String errorMessage)
    implements ProjectEvent {

  private static final String CHANNEL =
      "webprotege.events.projects.GitHubCloneRepositoryFailedEvent";

  @Override
  public String getChannel() {
    return CHANNEL;
  }
}
