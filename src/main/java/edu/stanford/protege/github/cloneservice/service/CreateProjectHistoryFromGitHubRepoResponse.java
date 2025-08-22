package edu.stanford.protege.github.cloneservice.service;

import static edu.stanford.protege.github.cloneservice.service.CreateProjectHistoryFromGitHubRepoRequest.CHANNEL;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import edu.stanford.protege.commitnavigator.model.RepositoryCoordinate;
import edu.stanford.protege.webprotege.common.BlobLocation;
import edu.stanford.protege.webprotege.common.ProjectId;
import edu.stanford.protege.webprotege.common.Response;

@JsonTypeName(CHANNEL)
public record CreateProjectHistoryFromGitHubRepoResponse(
    @JsonProperty("projectId") ProjectId projectId,
    @JsonProperty("repositoryCoordinate") RepositoryCoordinate repositoryCoordinate,
    @JsonProperty("documentLocation") BlobLocation projectHistoryLocation)
    implements Response {}
