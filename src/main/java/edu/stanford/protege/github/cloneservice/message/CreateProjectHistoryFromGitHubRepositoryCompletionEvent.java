package edu.stanford.protege.github.cloneservice.message;

/**
 * Sealed interface representing the completion of a GitHub repository clone operation.
 *
 * <p>This sealed interface extends {@link CreateProjectHistoryFromGitHubRepositoryEvent} and restricts
 * the possible implementations to only {@link CreateProjectHistoryFromGitHubRepositorySucceededEvent}
 * and {@link CreateProjectHistoryFromGitHubRepositoryFailedEvent}, providing type safety and ensuring
 * that completion events can only represent success or failure outcomes.</p>
 */
sealed interface CreateProjectHistoryFromGitHubRepositoryCompletionEvent
        extends CreateProjectHistoryFromGitHubRepositoryEvent
        permits CreateProjectHistoryFromGitHubRepositorySucceededEvent,
                CreateProjectHistoryFromGitHubRepositoryFailedEvent {}
