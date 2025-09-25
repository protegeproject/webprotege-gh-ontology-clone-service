package edu.stanford.protege.github.cloneservice.event;

import edu.stanford.protege.commitnavigator.model.RepositoryCoordinates;
import edu.stanford.protege.webprotege.common.EventId;
import edu.stanford.protege.webprotege.common.ProjectEvent;
import edu.stanford.protege.webprotege.common.ProjectId;
import edu.stanford.protege.webprotege.common.RequestId;

/**
 * Event dispatched when GitHub clone service fails due to other triggering errors.
 *
 * @param projectId The project for which the repository clone failed
 * @param requestId The correlated request ID for tracking the operation
 * @param eventId The correlation event ID for tracking the operation
 * @param repositoryCoordinates The coordinates of the repository that failed to clone
 * @param errorMessage The error message describing the failure
 */
public record GitHubCloneRequestFailedEvent(
        ProjectId projectId,
        RequestId requestId,
        EventId eventId,
        RepositoryCoordinates repositoryCoordinates,
        String errorMessage)
        implements ProjectEvent {

    private static final String CHANNEL = "webprotege.events.projects.GitHubCloneRequestFailedEvent";

    @Override
    public String getChannel() {
        return CHANNEL;
    }
}
