package edu.stanford.protege.github.cloneservice;

import edu.stanford.protege.commitnavigator.GitHubRepository;
import edu.stanford.protege.commitnavigator.GitHubRepositoryBuilderFactory;
import edu.stanford.protege.commitnavigator.exceptions.GitHubNavigatorException;
import edu.stanford.protege.commitnavigator.model.BranchCoordinates;
import edu.stanford.protege.github.cloneservice.exception.OntologyComparisonException;
import edu.stanford.protege.github.cloneservice.message.*;
import edu.stanford.protege.github.cloneservice.model.OntologyCommitChange;
import edu.stanford.protege.github.cloneservice.model.RelativeFilePath;
import edu.stanford.protege.github.cloneservice.service.ProjectHistoryStorer;
import edu.stanford.protege.github.cloneservice.utils.OntologyHistoryAnalyzer;
import edu.stanford.protege.webprotege.common.*;
import edu.stanford.protege.webprotege.ipc.CommandHandler;
import edu.stanford.protege.webprotege.ipc.EventDispatcher;
import edu.stanford.protege.webprotege.ipc.ExecutionContext;
import edu.stanford.protege.webprotege.ipc.WebProtegeHandler;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import javax.annotation.Nonnull;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import reactor.core.publisher.Mono;

@WebProtegeHandler
public class CreateProjectHistoryFromGitHubRepositoryCommandHandler
        implements CommandHandler<
                CreateProjectHistoryFromGitHubRepositoryRequest, CreateProjectHistoryFromGitHubRepositoryResponse> {

    private static final Logger logger =
            LoggerFactory.getLogger(CreateProjectHistoryFromGitHubRepositoryCommandHandler.class);

    private final OntologyHistoryAnalyzer ontologyHistoryAnalyzer;
    private final ProjectHistoryStorer projectHistoryStorer;
    private final EventDispatcher eventDispatcher;
    private final Executor projectHistoryImportExecutor;

    public CreateProjectHistoryFromGitHubRepositoryCommandHandler(
            @Nonnull OntologyHistoryAnalyzer ontologyHistoryAnalyzer,
            @Nonnull ProjectHistoryStorer projectHistoryStorer,
            @Nonnull EventDispatcher eventDispatcher,
            @Qualifier("projectHistoryImportExecutor") @Nonnull Executor projectHistoryImportExecutor) {
        this.ontologyHistoryAnalyzer = ontologyHistoryAnalyzer;
        this.projectHistoryStorer = projectHistoryStorer;
        this.eventDispatcher = eventDispatcher;
        this.projectHistoryImportExecutor = projectHistoryImportExecutor;
    }

    @NotNull @Override
    public String getChannelName() {
        return CreateProjectHistoryFromGitHubRepositoryRequest.CHANNEL;
    }

    @Override
    public Class<CreateProjectHistoryFromGitHubRepositoryRequest> getRequestClass() {
        return CreateProjectHistoryFromGitHubRepositoryRequest.class;
    }

    @Override
    public Mono<CreateProjectHistoryFromGitHubRepositoryResponse> handleRequest(
            CreateProjectHistoryFromGitHubRepositoryRequest request, ExecutionContext executionContext) {

        var operationId = CreateProjectHistoryFromGitHubRepositoryOperationId.generate();

        var userId = executionContext.userId();

        var projectId = request.projectId();
        var branchCoordinates = request.branchCoordinates();
        var rootOntologyPath = request.rootOntologyPath();

        startAsyncProcessing(userId, projectId, operationId, branchCoordinates, rootOntologyPath);

        return Mono.just(
                new CreateProjectHistoryFromGitHubRepositoryResponse(projectId, operationId, branchCoordinates));
    }

    private void startAsyncProcessing(
            UserId userId,
            ProjectId projectId,
            CreateProjectHistoryFromGitHubRepositoryOperationId operationId,
            BranchCoordinates branchCoordinates,
            RelativeFilePath rootOntologyPath) {

        cloneRepositoryAsync(userId, projectId, operationId, branchCoordinates)
                .whenComplete((repository, t) -> {
                    if (t == null) {
                        var eventId = EventId.generate();
                        logger.info(
                                "{} {} {} Successfully cloned repository {}",
                                projectId,
                                operationId,
                                eventId,
                                branchCoordinates.repositoryUrl());
                        fireCloneSucceeded(projectId, operationId, eventId, branchCoordinates, repository);
                    }
                })
                .thenComposeAsync(repository -> extractOntologyChanges(projectId, operationId, rootOntologyPath, branchCoordinates, repository))
                .whenComplete((projectHistory, t) -> {
                    if (t == null)  {
                        logger.info(
                                "{} {} Successfully extracted ontology changes from file {}",
                                projectId,
                                operationId,
                                rootOntologyPath);
                        fireImportSucceeded(projectId, operationId, branchCoordinates);
                    }
                })
                .thenComposeAsync(projectHistory -> storeProjectHistory(projectId, operationId, branchCoordinates, projectHistory))
                .whenComplete((documentLocation, t) -> {
                    if (t == null) {
                        logger.info(
                                "{} {} Successfully stored project history at location {}",
                                projectId,
                                operationId,
                                documentLocation);
                        fireStoreSucceeded(projectId, operationId, branchCoordinates);
                        fireCreateProjectHistoryFromGitHubRepoSucceeded(operationId, projectId, documentLocation);
                    } else {
                        // Terminal overarching error
                        fireCreateProjectHistoryFromGitHubRepoFailed(operationId, projectId, t);
                    }
                });
    }

    private CompletableFuture<GitHubRepository> cloneRepositoryAsync(
            UserId userId,
            ProjectId projectId,
            CreateProjectHistoryFromGitHubRepositoryOperationId operationId,
            BranchCoordinates branchCoordinates) {

        return CompletableFuture.supplyAsync(
                () -> {
                    try {
                        logger.info(
                                "{} {} Starting repository clone {}",
                                projectId,
                                operationId,
                                branchCoordinates.repositoryUrl());
                        var workingDirectory = getLocalWorkingDirectory(userId, projectId);
                        return cloneGitHubRepository(branchCoordinates, workingDirectory);
                    } catch (GitHubNavigatorException e) {
                        logger.error(
                                "{} {} Failed to clone GitHub repository {}",
                                projectId,
                                operationId,
                                branchCoordinates.repositoryUrl(),
                                e);
                        fireCloneFailed(projectId, operationId, EventId.generate(), branchCoordinates, e);
                        throw new RuntimeException("Failed to clone repository", e);
                    }
                },
                projectHistoryImportExecutor);
    }

    private CompletableFuture<List<OntologyCommitChange>> extractOntologyChanges(
            ProjectId projectId,
            CreateProjectHistoryFromGitHubRepositoryOperationId operationId,
            RelativeFilePath rootOntologyPath,
            BranchCoordinates branchCoordinates,
            GitHubRepository repository) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                logger.info(
                        "{} {} Starting ontology change extraction from file {}", projectId, operationId, rootOntologyPath);
                return ontologyHistoryAnalyzer.getCommitHistory(rootOntologyPath, repository);
            } catch (OntologyComparisonException e) {
                logger.error(
                        "{} {} Failed to extract ontology changes from file {}",
                        projectId,
                        operationId,
                        rootOntologyPath,
                        e);
                fireImportFailed(projectId, operationId, EventId.generate(), branchCoordinates, e);
                throw new RuntimeException("Failed to extract ontology change", e);
            }
        }, projectHistoryImportExecutor);
    }

    private CompletableFuture<BlobLocation> storeProjectHistory(
            ProjectId projectId,
            CreateProjectHistoryFromGitHubRepositoryOperationId operationId,
            BranchCoordinates branchCoordinates,
            List<OntologyCommitChange> projectHistory) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                logger.info("{} {} Starting project history store", projectId, operationId);
                return projectHistoryStorer.storeProjectHistory(projectId, projectHistory);
            } catch (Exception e) {
                logger.error(
                        "{} {} Failed to store project history",
                        projectId,
                        operationId, e);
                fireStoreFailed(projectId, operationId, branchCoordinates, e);
                throw new RuntimeException(e);
            }
        }, projectHistoryImportExecutor);
    }

    private Path getLocalWorkingDirectory(UserId userId, ProjectId projectId) {
        var tempDir = Paths.get(System.getProperty("java.io.tmpdir"));
        return tempDir.resolve("github-repos" + File.separator + userId.value() + File.separator + projectId.value());
    }

    private GitHubRepository cloneGitHubRepository(BranchCoordinates branchCoordinates, Path workingDirectory)
            throws GitHubNavigatorException {
        var repository = GitHubRepositoryBuilderFactory.create(branchCoordinates)
                .localWorkingDirectory(workingDirectory)
                .build();
        repository.initialize();
        return repository;
    }

    private void fireCloneFailed(
            ProjectId projectId,
            CreateProjectHistoryFromGitHubRepositoryOperationId operationId,
            EventId eventId,
            BranchCoordinates branchCoordinates,
            Throwable t) {
        eventDispatcher.dispatchEvent(new GitHubCloneRepositoryFailedEvent(
                projectId, operationId, eventId, branchCoordinates, t.getMessage()));
    }

    private void fireCloneSucceeded(
            ProjectId projectId,
            CreateProjectHistoryFromGitHubRepositoryOperationId operationId,
            EventId eventId,
            BranchCoordinates branchCoordinates,
            GitHubRepository repository) {
        eventDispatcher.dispatchEvent(new GitHubCloneRepositorySucceededEvent(
                projectId, operationId, eventId, branchCoordinates, repository));
    }

    private void fireImportFailed(
            ProjectId projectId,
            CreateProjectHistoryFromGitHubRepositoryOperationId operationId,
            EventId eventId,
            BranchCoordinates branchCoordinates,
            Throwable t) {
        eventDispatcher.dispatchEvent(new GitHubProjectHistoryImportFailedEvent(
                projectId, operationId, eventId, branchCoordinates, t.getMessage()));
    }

    private void fireImportSucceeded(
            ProjectId projectId,
            CreateProjectHistoryFromGitHubRepositoryOperationId operationId,
            BranchCoordinates branchCoordinates) {
        eventDispatcher.dispatchEvent(
                new GitHubProjectHistoryImportSucceededEvent(projectId, operationId, EventId.generate(), branchCoordinates));
    }

    private void fireStoreFailed(
            ProjectId projectId,
            CreateProjectHistoryFromGitHubRepositoryOperationId operationId,
            BranchCoordinates branchCoordinates,
            Throwable t) {
        eventDispatcher.dispatchEvent(new GitHubProjectHistoryStoreFailedEvent(
                projectId, operationId, EventId.generate(), branchCoordinates, t.getMessage()));
    }

    private void fireStoreSucceeded(
            ProjectId projectId,
            CreateProjectHistoryFromGitHubRepositoryOperationId operationId,
            BranchCoordinates branchCoordinates) {
        eventDispatcher.dispatchEvent(
                new GitHubProjectHistoryStoreSucceededEvent(projectId, operationId, EventId.generate(), branchCoordinates));
    }

    private void fireCreateProjectHistoryFromGitHubRepoFailed(
            CreateProjectHistoryFromGitHubRepositoryOperationId operationId, ProjectId projectId, Throwable t) {
        eventDispatcher.dispatchEvent(new CreateProjectHistoryFromGitHubRepositoryFailedEvent(
                EventId.generate(), operationId, projectId, t.getMessage()));
    }

    private void fireCreateProjectHistoryFromGitHubRepoSucceeded(
            CreateProjectHistoryFromGitHubRepositoryOperationId operationId,
            ProjectId projectId,
            BlobLocation documentLocation) {
        eventDispatcher.dispatchEvent(new CreateProjectHistoryFromGitHubRepositorySucceededEvent(
                EventId.generate(), operationId, projectId, documentLocation));
    }
}
