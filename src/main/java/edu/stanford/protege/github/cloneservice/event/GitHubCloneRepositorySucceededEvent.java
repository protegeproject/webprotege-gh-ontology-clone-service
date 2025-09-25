package edu.stanford.protege.github.cloneservice.event;

import edu.stanford.protege.commitnavigator.GitHubRepository;
import edu.stanford.protege.commitnavigator.model.RepositoryCoordinates;
import edu.stanford.protege.webprotege.common.EventId;
import edu.stanford.protege.webprotege.common.ProjectEvent;
import edu.stanford.protege.webprotege.common.ProjectId;
import edu.stanford.protege.webprotege.common.RequestId;

/**
 * Event dispatched when a GitHub repository is successfully cloned.
 *
 * @param projectId The project for which the repository was cloned
 * @param requestId The correlated request ID for tracking the operation
 * @param eventId The correlation event ID for tracking the operation
 * @param repositoryCoordinates The coordinates of the successfully cloned repository
 * @param repository The cloned GitHub repository
 */
public record GitHubCloneRepositorySucceededEvent(
    ProjectId projectId,
    RequestId requestId,
    EventId eventId,
    RepositoryCoordinates repositoryCoordinates,
    GitHubRepository repository)
    implements ProjectEvent {

  private static final String CHANNEL =
      "webprotege.events.projects.GitHubCloneRepositorySucceededEvent";

  @Override
  public String getChannel() {
    return CHANNEL;
  }
}
