package edu.stanford.protege.github.cloneservice.event;

import edu.stanford.protege.commitnavigator.GitHubRepository;
import edu.stanford.protege.commitnavigator.model.RepositoryCoordinates;
import edu.stanford.protege.github.cloneservice.service.CreateProjectHistoryFromGitHubRepositoryOperationId;
import edu.stanford.protege.webprotege.common.EventId;
import edu.stanford.protege.webprotege.common.ProjectEvent;
import edu.stanford.protege.webprotege.common.ProjectId;

/**
 * Event dispatched when a GitHub repository is successfully cloned.
 *
 * @param projectId The project for which the repository was cloned
 * @param operationId The correlated operation ID for tracking the operation
 * @param eventId The correlation event ID for tracking the operation
 * @param repositoryCoordinates The coordinates of the successfully cloned repository
 * @param repository The cloned GitHub repository
 */
public record GitHubCloneRepositorySucceededEvent(
        ProjectId projectId,
        CreateProjectHistoryFromGitHubRepositoryOperationId operationId,
        EventId eventId,
        RepositoryCoordinates repositoryCoordinates,
        GitHubRepository repository)
        implements ProjectEvent {

    private static final String CHANNEL = "webprotege.events.projects.GitHubCloneRepositorySucceeded";

    @Override
    public String getChannel() {
        return CHANNEL;
    }
}
