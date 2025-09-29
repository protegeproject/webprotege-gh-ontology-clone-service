package edu.stanford.protege.github.cloneservice.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import edu.stanford.protege.github.cloneservice.service.CreateProjectHistoryFromGitHubRepoOperationId;
import edu.stanford.protege.webprotege.common.BlobLocation;
import edu.stanford.protege.webprotege.common.EventId;
import edu.stanford.protege.webprotege.common.ProjectId;

/**
 * Event emitted when a GitHub repository clone operation completes successfully.
 *
 * @param eventId The correlation event ID for tracking the operation
 * @param operationId The correlated operation ID for tracking the operation
 * @param projectId The project for which the repository clone failed
 * @param documentLocation The location where the project history is stored
 */
public record CreateProjectHistoryFromGitHubRepoSucceededEvent(
        @JsonProperty("eventId") EventId eventId,
        @JsonProperty("operationId") CreateProjectHistoryFromGitHubRepoOperationId operationId,
        @JsonProperty("projectId") ProjectId projectId,
        @JsonProperty("documentLocation") BlobLocation documentLocation)
        implements CreateProjectHistoryFromGitHubRepoCompletionEvent {

    private static final String CHANNEL = "webprotege.events.projects.CreateProjectHistoryFromGitHubRepoSucceeded";

    @Override
    public String getChannel() {
        return CHANNEL;
    }
}
