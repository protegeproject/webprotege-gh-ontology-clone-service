package edu.stanford.protege.github.cloneservice.service;

import edu.stanford.protege.commitnavigator.GitHubRepository;
import edu.stanford.protege.commitnavigator.GitHubRepositoryBuilderFactory;
import edu.stanford.protege.commitnavigator.exceptions.GitHubNavigatorException;
import edu.stanford.protege.commitnavigator.model.RepositoryCoordinates;
import edu.stanford.protege.github.cloneservice.event.*;
import edu.stanford.protege.github.cloneservice.exception.OntologyComparisonException;
import edu.stanford.protege.github.cloneservice.model.OntologyCommitChange;
import edu.stanford.protege.github.cloneservice.model.RelativeFilePath;
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
public class CreateProjectHistoryFromGitHubRepoCommandHandler
        implements CommandHandler<
                CreateProjectHistoryFromGitHubRepoRequest, CreateProjectHistoryFromGitHubRepoResponse> {

    private static final Logger logger =
            LoggerFactory.getLogger(CreateProjectHistoryFromGitHubRepoCommandHandler.class);

    private final OntologyHistoryAnalyzer ontologyHistoryAnalyzer;
    private final ProjectHistoryStorer projectHistoryStorer;
    private final EventDispatcher eventDispatcher;
    private final Executor projectHistoryImportExecutor;

    public CreateProjectHistoryFromGitHubRepoCommandHandler(
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
        return CreateProjectHistoryFromGitHubRepoRequest.CHANNEL;
    }

    @Override
    public Class<CreateProjectHistoryFromGitHubRepoRequest> getRequestClass() {
        return CreateProjectHistoryFromGitHubRepoRequest.class;
    }

    @Override
    public Mono<CreateProjectHistoryFromGitHubRepoResponse> handleRequest(
            CreateProjectHistoryFromGitHubRepoRequest request, ExecutionContext executionContext) {

        var operationId = CreateProjectHistoryFromGitHubRepoOperationId.generate();

        var eventId = EventId.generate();
        var userId = executionContext.userId();

        var projectId = request.projectId();
        var repositoryCoordinates = request.repositoryCoordinates();
        var targetOntologyFile = request.targetOntologyFile();

        startAsyncProcessing(userId, projectId, operationId, eventId, repositoryCoordinates, targetOntologyFile);

        return Mono.just(
                new CreateProjectHistoryFromGitHubRepoResponse(projectId, operationId, eventId, repositoryCoordinates));
    }

    private void startAsyncProcessing(
            UserId userId,
            ProjectId projectId,
            CreateProjectHistoryFromGitHubRepoOperationId operationId,
            EventId eventId,
            RepositoryCoordinates repositoryCoordinates,
            RelativeFilePath targetOntologyFile) {

        cloneRepositoryAsync(userId, projectId, operationId, eventId, repositoryCoordinates)
                .whenComplete((repository, t) -> {
                    if (t != null) {
                        logger.error(
                                "{} {} {} Failed to clone GitHub repository {}",
                                projectId,
                                operationId,
                                eventId,
                                repositoryCoordinates.repositoryUrl(),
                                t);
                        fireCloneFailed(projectId, operationId, eventId, repositoryCoordinates, t);
                    } else {
                        logger.info(
                                "{} {} {} Successfully cloned repository {}",
                                projectId,
                                operationId,
                                eventId,
                                repositoryCoordinates.repositoryUrl());
                        fireCloneSucceeded(projectId, operationId, eventId, repositoryCoordinates, repository);
                    }
                })
                .thenApplyAsync(
                        repository -> extractOntologyChanges(
                                projectId, operationId, eventId, repositoryCoordinates, targetOntologyFile, repository),
                        projectHistoryImportExecutor)
                .whenComplete((projectHistory, t) -> {
                    if (t != null) {
                        logger.error(
                                "{} {} {} Failed to extract ontology changes from file {}",
                                projectId,
                                operationId,
                                eventId,
                                targetOntologyFile,
                                t);
                        fireImportFailed(projectId, operationId, eventId, repositoryCoordinates, t);
                    } else {
                        logger.info(
                                "{} {} {} Successfully extracted ontology changes from file {}",
                                projectId,
                                operationId,
                                eventId,
                                targetOntologyFile);
                        fireImportSucceeded(projectId, operationId, eventId, repositoryCoordinates);
                    }
                })
                .thenApplyAsync(
                        projectHistory -> storeProjectHistory(
                                projectId, operationId, eventId, repositoryCoordinates, projectHistory),
                        projectHistoryImportExecutor)
                .whenComplete((documentLocation, t) -> {
                    if (t != null) {
                        logger.error(
                                "{} {} {} Failed to store project history at location {}",
                                projectId,
                                operationId,
                                eventId,
                                documentLocation,
                                t);
                        fireStoreFailed(projectId, operationId, eventId, repositoryCoordinates, t);
                    } else {
                        logger.info(
                                "{} {} {} Successfully stored project history at location {}",
                                projectId,
                                operationId,
                                eventId,
                                documentLocation);
                        fireStoreSucceeded(projectId, operationId, eventId, repositoryCoordinates, documentLocation);
                    }
                });
    }

    private CompletableFuture<GitHubRepository> cloneRepositoryAsync(
            UserId userId,
            ProjectId projectId,
            CreateProjectHistoryFromGitHubRepoOperationId operationId,
            EventId eventId,
            RepositoryCoordinates repositoryCoordinates) {

        return CompletableFuture.supplyAsync(
                () -> {
                    try {
                        logger.info(
                                "{} {} {} Starting repository clone {}",
                                projectId,
                                operationId,
                                eventId,
                                repositoryCoordinates.repositoryUrl());
                        var workingDirectory = getLocalWorkingDirectory(userId, projectId);
                        return cloneGitHubRepository(repositoryCoordinates, workingDirectory);
                    } catch (GitHubNavigatorException e) {
                        throw new RuntimeException("Failed to clone repository", e);
                    }
                },
                projectHistoryImportExecutor);
    }

    private List<OntologyCommitChange> extractOntologyChanges(
            ProjectId projectId,
            CreateProjectHistoryFromGitHubRepoOperationId operationId,
            EventId eventId,
            RepositoryCoordinates repositoryCoordinates,
            RelativeFilePath targetOntologyFile,
            GitHubRepository repository) {
        try {
            logger.info(
                    "{} {} {} Starting ontology change extraction from file {}",
                    projectId,
                    operationId,
                    eventId,
                    targetOntologyFile);
            return ontologyHistoryAnalyzer.getCommitHistory(targetOntologyFile, repository);
        } catch (OntologyComparisonException e) {
            throw new RuntimeException("Failed to extract ontology change", e);
        }
    }

    private BlobLocation storeProjectHistory(
            ProjectId projectId,
            CreateProjectHistoryFromGitHubRepoOperationId operationId,
            EventId eventId,
            RepositoryCoordinates repositoryCoordinates,
            List<OntologyCommitChange> projectHistory) {
        try {
            logger.info("{} {} {} Starting project history store", projectId, operationId, eventId);
            return projectHistoryStorer.storeProjectHistory(projectId, projectHistory);
        } catch (Exception e) {
            throw new RuntimeException("Failed to store project history", e);
        }
    }

    private Path getLocalWorkingDirectory(UserId userId, ProjectId projectId) {
        var tempDir = Paths.get(System.getProperty("java.io.tmpdir"));
        return tempDir.resolve("github-repos" + File.separator + userId.value() + File.separator + projectId.value());
    }

    private GitHubRepository cloneGitHubRepository(RepositoryCoordinates repositoryCoordinates, Path workingDirectory)
            throws GitHubNavigatorException {
        var repository = GitHubRepositoryBuilderFactory.create(repositoryCoordinates)
                .localWorkingDirectory(workingDirectory)
                .build();
        repository.initialize();
        return repository;
    }

    private void fireCloneFailed(
            ProjectId projectId,
            CreateProjectHistoryFromGitHubRepoOperationId operationId,
            EventId eventId,
            RepositoryCoordinates repositoryCoordinates,
            Throwable t) {
        eventDispatcher.dispatchEvent(new GitHubCloneRepositoryFailedEvent(
                projectId, operationId, eventId, repositoryCoordinates, t.getMessage()));
    }

    private void fireCloneSucceeded(
            ProjectId projectId,
            CreateProjectHistoryFromGitHubRepoOperationId operationId,
            EventId eventId,
            RepositoryCoordinates repositoryCoordinates,
            GitHubRepository repository) {
        eventDispatcher.dispatchEvent(new GitHubCloneRepositorySucceededEvent(
                projectId, operationId, eventId, repositoryCoordinates, repository));
    }

    private void fireImportFailed(
            ProjectId projectId,
            CreateProjectHistoryFromGitHubRepoOperationId operationId,
            EventId eventId,
            RepositoryCoordinates repositoryCoordinates,
            Throwable t) {
        eventDispatcher.dispatchEvent(new GitHubProjectHistoryImportFailedEvent(
                projectId, operationId, eventId, repositoryCoordinates, t.getMessage()));
    }

    private void fireImportSucceeded(
            ProjectId projectId,
            CreateProjectHistoryFromGitHubRepoOperationId operationId,
            EventId eventId,
            RepositoryCoordinates repositoryCoordinates) {
        eventDispatcher.dispatchEvent(
                new GitHubProjectHistoryImportSucceededEvent(projectId, operationId, eventId, repositoryCoordinates));
    }

    private void fireStoreFailed(
            ProjectId projectId,
            CreateProjectHistoryFromGitHubRepoOperationId operationId,
            EventId eventId,
            RepositoryCoordinates repositoryCoordinates,
            Throwable t) {
        eventDispatcher.dispatchEvent(new GitHubProjectHistoryStoreFailedEvent(
                projectId, operationId, eventId, repositoryCoordinates, t.getMessage()));
    }

    private void fireStoreSucceeded(
            ProjectId projectId,
            CreateProjectHistoryFromGitHubRepoOperationId operationId,
            EventId eventId,
            RepositoryCoordinates repositoryCoordinates,
            BlobLocation projectHistoryLocation) {
        eventDispatcher.dispatchEvent(new GitHubProjectHistoryStoreSucceededEvent(
                projectId, operationId, eventId, repositoryCoordinates, projectHistoryLocation));
    }

    private void fireRequestFailed(
            ProjectId projectId,
            CreateProjectHistoryFromGitHubRepoOperationId operationId,
            EventId eventId,
            RepositoryCoordinates repositoryCoordinates,
            Throwable t) {
        eventDispatcher.dispatchEvent(new GitHubCloneRequestFailedEvent(
                projectId, operationId, eventId, repositoryCoordinates, t.getMessage()));
    }
}
