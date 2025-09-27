package edu.stanford.protege.github.cloneservice.event;

/**
 * Sealed interface representing the completion of a GitHub repository clone operation.
 *
 * <p>This sealed interface extends {@link CreateProjectHistoryFromGitHubRepoEvent} and restricts
 * the possible implementations to only {@link CreateProjectHistoryFromGitHubRepoSucceededEvent}
 * and {@link CreateProjectHistoryFromGitHubRepoFailedEvent}, providing type safety and ensuring
 * that completion events can only represent success or failure outcomes.</p>
 *
 * @see CreateProjectHistoryFromGitHubRepoSucceededEvent
 * @see CreateProjectHistoryFromGitHubRepoFailedEvent
 */
sealed interface CreateProjectHistoryFromGitHubRepoCompletionEvent extends CreateProjectHistoryFromGitHubRepoEvent
        permits CreateProjectHistoryFromGitHubRepoSucceededEvent, CreateProjectHistoryFromGitHubRepoFailedEvent {}
