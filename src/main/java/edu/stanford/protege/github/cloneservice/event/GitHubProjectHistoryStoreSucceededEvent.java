package edu.stanford.protege.github.cloneservice.event;

import edu.stanford.protege.commitnavigator.model.RepositoryCoordinates;
import edu.stanford.protege.github.cloneservice.service.CreateProjectHistoryFromGitHubRepoOperationId;
import edu.stanford.protege.webprotege.common.*;

/**
 * Event dispatched when project history is successfully imported from a GitHub repository.
 *
 * @param projectId The project for which the history was imported
 * @param operationId The correlated operation ID for tracking the operation
 * @param eventId The correlation event ID for tracking the operation
 * @param repositoryCoordinates The coordinates of the repository from which history was imported
 */
public record GitHubProjectHistoryStoreSucceededEvent(
        ProjectId projectId,
        CreateProjectHistoryFromGitHubRepoOperationId operationId,
        EventId eventId,
        RepositoryCoordinates repositoryCoordinates)
        implements ProjectEvent {

    private static final String CHANNEL = "webprotege.events.projects.GitHubProjectHistoryStoreSucceeded";

    @Override
    public String getChannel() {
        return CHANNEL;
    }
}
