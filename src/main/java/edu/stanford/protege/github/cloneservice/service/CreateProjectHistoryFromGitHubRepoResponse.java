package edu.stanford.protege.github.cloneservice.service;

import static edu.stanford.protege.github.cloneservice.service.CreateProjectHistoryFromGitHubRepoRequest.CHANNEL;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import edu.stanford.protege.commitnavigator.model.RepositoryCoordinates;
import edu.stanford.protege.webprotege.common.EventId;
import edu.stanford.protege.webprotege.common.ProjectId;
import edu.stanford.protege.webprotege.common.RequestId;
import edu.stanford.protege.webprotege.common.Response;

@JsonTypeName(CHANNEL)
public record CreateProjectHistoryFromGitHubRepoResponse(
        @JsonProperty("projectId") ProjectId projectId,
        @JsonProperty("requestId") RequestId requestId,
        @JsonProperty("eventId") EventId eventId,
        @JsonProperty("repositoryCoordinates") RepositoryCoordinates repositoryCoordinates)
        implements Response {}
