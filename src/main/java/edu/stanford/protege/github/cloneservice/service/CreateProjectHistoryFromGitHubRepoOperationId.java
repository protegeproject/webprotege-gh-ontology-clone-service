package edu.stanford.protege.github.cloneservice.service;

import java.util.Objects;
import java.util.UUID;

public record CreateProjectHistoryFromGitHubRepoOperationId(String operationId) {

    public CreateProjectHistoryFromGitHubRepoOperationId {
        Objects.requireNonNull(operationId, "Operation ID should not be null");
    }

    public static CreateProjectHistoryFromGitHubRepoOperationId generate() {
        return new CreateProjectHistoryFromGitHubRepoOperationId(
                UUID.randomUUID().toString());
    }
}
