package edu.stanford.protege.github.cloneservice.message;

import static edu.stanford.protege.github.cloneservice.message.CreateProjectHistoryFromGitHubRepositoryRequest.CHANNEL;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import edu.stanford.protege.commitnavigator.model.BranchCoordinates;
import edu.stanford.protege.github.cloneservice.model.RelativeFilePath;
import edu.stanford.protege.webprotege.common.ProjectId;
import edu.stanford.protege.webprotege.common.Request;

@JsonTypeName(CHANNEL)
public record CreateProjectHistoryFromGitHubRepositoryRequest(
        @JsonProperty("projectId") ProjectId projectId,
        @JsonProperty("branchCoordinates") BranchCoordinates branchCoordinates,
        @JsonProperty("rootOntologyPath") RelativeFilePath rootOntologyPath)
        implements Request<CreateProjectHistoryFromGitHubRepositoryResponse> {

    public static final String CHANNEL = "webprotege.github.CreateProjectHistory";

    @Override
    public String getChannel() {
        return CHANNEL;
    }
}
