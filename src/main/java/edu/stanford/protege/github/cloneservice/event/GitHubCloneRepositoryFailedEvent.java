package edu.stanford.protege.github.cloneservice.event;

import edu.stanford.protege.commitnavigator.model.RepositoryCoordinates;
import edu.stanford.protege.github.cloneservice.service.CreateProjectHistoryFromGitHubRepoOperationId;
import edu.stanford.protege.webprotege.common.EventId;
import edu.stanford.protege.webprotege.common.ProjectEvent;
import edu.stanford.protege.webprotege.common.ProjectId;

/**
 * Event dispatched when cloning a GitHub repository fails.
 *
 * @param projectId The project for which the repository clone failed
 * @param operationId The correlated operation ID for tracking the operation
 * @param eventId The correlated event ID for tracking the operation
 * @param repositoryCoordinates The coordinates of the repository that failed to clone
 * @param errorMessage The error message describing the failure
 */
public record GitHubCloneRepositoryFailedEvent(
        ProjectId projectId,
        CreateProjectHistoryFromGitHubRepoOperationId operationId,
        EventId eventId,
        RepositoryCoordinates repositoryCoordinates,
        String errorMessage)
        implements ProjectEvent {

    private static final String CHANNEL = "webprotege.events.projects.GitHubCloneRepositoryFailedEvent";

    @Override
    public String getChannel() {
        return CHANNEL;
    }
}
