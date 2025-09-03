package edu.stanford.protege.github.cloneservice.service;

import static edu.stanford.protege.github.cloneservice.service.CreateProjectHistoryFromGitHubRepoRequest.CHANNEL;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import edu.stanford.protege.commitnavigator.model.RepositoryCoordinate;
import edu.stanford.protege.webprotege.common.ProjectId;
import edu.stanford.protege.webprotege.common.Request;

@JsonTypeName(CHANNEL)
public record CreateProjectHistoryFromGitHubRepoRequest(
    @JsonProperty("projectId") ProjectId projectId,
    @JsonProperty("repositoryCoordinate") RepositoryCoordinate repositoryCoordinate,
    @JsonProperty("targetOntologyFile") String targetOntologyFile)
    implements Request<CreateProjectHistoryFromGitHubRepoResponse> {

  public static final String CHANNEL = "webprotege.github.CreateProjectHistoryFromGitHubRepo";

  @Override
  public String getChannel() {
    return CHANNEL;
  }
}
