package edu.stanford.protege.github.cloneservice.event;

import edu.stanford.protege.github.cloneservice.service.CreateProjectHistoryFromGitHubRepoOperationId;
import edu.stanford.protege.webprotege.common.BlobLocation;
import edu.stanford.protege.webprotege.common.EventId;
import edu.stanford.protege.webprotege.common.ProjectId;

/**
 * Event emitted when a GitHub repository clone operation completes successfully.
 *
 * @param projectId The project for which the repository clone failed
 * @param operationId The correlated operation ID for tracking the operation
 * @param eventId The correlation event ID for tracking the operation
 * @param documentLocation The location where the project history is stored
 */
public record CreateProjectHistoryFromGitHubRepoSucceededEvent(
        ProjectId projectId,
        EventId eventId,
        CreateProjectHistoryFromGitHubRepoOperationId operationId,
        BlobLocation documentLocation)
        implements CreateProjectHistoryFromGitHubRepoCompletionEvent {

    private static final String CHANNEL = "webprotege.events.projects.CreateProjectHistoryFromGitHubRepoSucceededEvent";

    @Override
    public String getChannel() {
        return CHANNEL;
    }
}
