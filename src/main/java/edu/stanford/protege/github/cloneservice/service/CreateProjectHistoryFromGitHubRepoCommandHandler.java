package edu.stanford.protege.github.cloneservice.service;

import edu.stanford.protege.webprotege.ipc.CommandHandler;
import edu.stanford.protege.webprotege.ipc.ExecutionContext;
import edu.stanford.protege.webprotege.ipc.WebProtegeHandler;
import org.jetbrains.annotations.NotNull;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;

@WebProtegeHandler
public class CreateProjectHistoryFromGitHubRepoCommandHandler
        implements CommandHandler<CreateProjectHistoryFromGitHubRepoRequest,
        CreateProjectHistoryFromGitHubRepoResponse> {

    private final ProjectHistoryGenerator projectHistoryGenerator;

    public CreateProjectHistoryFromGitHubRepoCommandHandler(@Nonnull ProjectHistoryGenerator projectHistoryGenerator) {
        this.projectHistoryGenerator = projectHistoryGenerator;
    }

    @NotNull
    @Override
    public String getChannelName() {
        return CreateProjectHistoryFromGitHubRepoRequest.CHANNEL;
    }

    @Override
    public Class<CreateProjectHistoryFromGitHubRepoRequest> getRequestClass() {
        return CreateProjectHistoryFromGitHubRepoRequest.class;
    }

    @Override
    public Mono<CreateProjectHistoryFromGitHubRepoResponse> handleRequest(CreateProjectHistoryFromGitHubRepoRequest request,
                                                                          ExecutionContext executionContext) {
        try {
            var projectHistoryDocumentLocation = projectHistoryGenerator
                    .writeProjectHistoryFromGitHubRepo(executionContext.userId(),
                            request.projectId(),
                            request.repositoryCoordinate(),
                            request.targetOntologyFile());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return null;
    }
}
