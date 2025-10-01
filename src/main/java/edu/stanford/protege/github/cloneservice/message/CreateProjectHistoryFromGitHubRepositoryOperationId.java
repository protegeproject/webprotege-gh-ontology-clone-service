package edu.stanford.protege.github.cloneservice.message;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Objects;
import java.util.UUID;

public record CreateProjectHistoryFromGitHubRepositoryOperationId(@JsonValue String operationId) {

    public CreateProjectHistoryFromGitHubRepositoryOperationId {
        Objects.requireNonNull(operationId, "Operation ID should not be null");
    }

    public static CreateProjectHistoryFromGitHubRepositoryOperationId generate() {
        return new CreateProjectHistoryFromGitHubRepositoryOperationId(
                UUID.randomUUID().toString());
    }

    @JsonCreator
    public static CreateProjectHistoryFromGitHubRepositoryOperationId valueOf(String operationId) {
        return new CreateProjectHistoryFromGitHubRepositoryOperationId(operationId);
    }
}
