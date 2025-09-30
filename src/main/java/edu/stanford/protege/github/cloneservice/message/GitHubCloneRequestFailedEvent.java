package edu.stanford.protege.github.cloneservice.message;

import edu.stanford.protege.commitnavigator.model.BranchCoordinates;
import edu.stanford.protege.github.cloneservice.service.CreateProjectHistoryFromGitHubRepositoryOperationId;
import edu.stanford.protege.webprotege.common.EventId;
import edu.stanford.protege.webprotege.common.ProjectEvent;
import edu.stanford.protege.webprotege.common.ProjectId;

/**
 * Event dispatched when GitHub clone service fails due to other triggering errors.
 *
 * @param projectId The project for which the repository clone failed
 * @param operationId The correlated operation ID for tracking the operation
 * @param eventId The correlation event ID for tracking the operation
 * @param branchCoordinates The coordinates of the repository branch that failed to clone
 * @param errorMessage The error message describing the failure
 */
public record GitHubCloneRequestFailedEvent(
        ProjectId projectId,
        CreateProjectHistoryFromGitHubRepositoryOperationId operationId,
        EventId eventId,
        BranchCoordinates branchCoordinates,
        String errorMessage)
        implements ProjectEvent {

    private static final String CHANNEL = "webprotege.events.projects.GitHubCloneRequestFailed";

    @Override
    public String getChannel() {
        return CHANNEL;
    }
}
