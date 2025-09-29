package edu.stanford.protege.github.cloneservice.service;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Objects;
import java.util.UUID;

public record CreateProjectHistoryFromGitHubRepoOperationId(@JsonValue String operationId) {

    public CreateProjectHistoryFromGitHubRepoOperationId {
        Objects.requireNonNull(operationId, "Operation ID should not be null");
    }

    public static CreateProjectHistoryFromGitHubRepoOperationId generate() {
        return new CreateProjectHistoryFromGitHubRepoOperationId(
                UUID.randomUUID().toString());
    }

    @JsonCreator
    public static CreateProjectHistoryFromGitHubRepoOperationId valueOf(String operationId) {
        return new CreateProjectHistoryFromGitHubRepoOperationId(operationId);
    }
}
