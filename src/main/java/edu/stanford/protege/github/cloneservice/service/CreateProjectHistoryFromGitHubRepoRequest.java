package edu.stanford.protege.github.cloneservice.service;

import static edu.stanford.protege.github.cloneservice.service.CreateProjectHistoryFromGitHubRepoRequest.CHANNEL;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import edu.stanford.protege.commitnavigator.model.RepositoryCoordinates;
import edu.stanford.protege.github.cloneservice.model.RelativeFilePath;
import edu.stanford.protege.webprotege.common.ProjectId;
import edu.stanford.protege.webprotege.common.Request;
import edu.stanford.protege.webprotege.common.RequestId;

@JsonTypeName(CHANNEL)
public record CreateProjectHistoryFromGitHubRepoRequest(
    @JsonProperty("requestId") RequestId requestId,
    @JsonProperty("projectId") ProjectId projectId,
    @JsonProperty("repositoryCoordinates") RepositoryCoordinates repositoryCoordinates,
    @JsonProperty("targetOntologyFile") RelativeFilePath targetOntologyFile)
    implements Request<CreateProjectHistoryFromGitHubRepoResponse> {

  public static final String CHANNEL = "webprotege.github.CreateProjectHistoryFromGitHubRepo";

  @Override
  public String getChannel() {
    return CHANNEL;
  }
}
