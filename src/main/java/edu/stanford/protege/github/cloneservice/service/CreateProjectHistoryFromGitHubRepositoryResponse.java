package edu.stanford.protege.github.cloneservice.service;

import static edu.stanford.protege.github.cloneservice.service.CreateProjectHistoryFromGitHubRepositoryRequest.CHANNEL;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import edu.stanford.protege.commitnavigator.model.RepositoryCoordinates;
import edu.stanford.protege.webprotege.common.ProjectId;
import edu.stanford.protege.webprotege.common.Response;

@JsonTypeName(CHANNEL)
public record CreateProjectHistoryFromGitHubRepositoryResponse(
        @JsonProperty("projectId") ProjectId projectId,
        @JsonProperty("operationId") CreateProjectHistoryFromGitHubRepositoryOperationId operationId,
        @JsonProperty("repositoryCoordinates") RepositoryCoordinates repositoryCoordinates)
        implements Response {}
