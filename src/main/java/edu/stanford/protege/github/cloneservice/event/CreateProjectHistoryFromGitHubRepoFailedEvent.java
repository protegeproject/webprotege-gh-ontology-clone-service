package edu.stanford.protege.github.cloneservice.event;

import edu.stanford.protege.github.cloneservice.service.CreateProjectHistoryFromGitHubRepoOperationId;
import edu.stanford.protege.webprotege.common.EventId;
import edu.stanford.protege.webprotege.common.ProjectId;

/**
 * Event emitted when a GitHub repository clone operation fails.
 *
 * @param projectId The project for which the repository clone failed
 * @param operationId The correlated operation ID for tracking the operation
 * @param eventId The correlation event ID for tracking the operation
 * @param errorMessage The error message describing the failure
 */
public record CreateProjectHistoryFromGitHubRepoFailedEvent(
        ProjectId projectId,
        CreateProjectHistoryFromGitHubRepoOperationId operationId,
        EventId eventId,
        String errorMessage)
        implements CreateProjectHistoryFromGitHubRepoCompletionEvent {

    private static final String CHANNEL = "webprotege.events.projects.CreateProjectHistoryFromGitHubRepoFailedEvent";

    @Override
    public String getChannel() {
        return CHANNEL;
    }
}
