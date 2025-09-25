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
import edu.stanford.protege.webprotege.common.BlobLocation;
import edu.stanford.protege.webprotege.common.EventId;
import edu.stanford.protege.webprotege.common.ProjectId;
import edu.stanford.protege.webprotege.common.UserId;
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

    var eventId = EventId.generate();
    var userId = executionContext.userId();
    var projectId = request.projectId();
    var repositoryCoordinates = request.repositoryCoordinates();
    var targetOntologyFile = request.targetOntologyFile();

    startAsyncProcessing(eventId, userId, projectId, repositoryCoordinates, targetOntologyFile);

    return Mono.just(
        new CreateProjectHistoryFromGitHubRepoResponse(projectId, repositoryCoordinates, eventId));
  }

  private void startAsyncProcessing(
      EventId eventId,
      UserId userId,
      ProjectId projectId,
      RepositoryCoordinates repositoryCoordinates,
      RelativeFilePath targetOntologyFile) {

    cloneRepository(eventId, userId, projectId, repositoryCoordinates)
        .thenApplyAsync(
            repository ->
                analyzeCommitHistory(
                    repository, targetOntologyFile, eventId, projectId, repositoryCoordinates),
            projectHistoryImportExecutor)
        .thenApplyAsync(
            projectHistory ->
                storeProjectHistory(projectHistory, eventId, projectId, repositoryCoordinates),
            projectHistoryImportExecutor)
        .whenComplete(
            (documentLocation, throwable) ->
                handleCompletion(
                    documentLocation, throwable, eventId, projectId, repositoryCoordinates));
  }

  private CompletableFuture<GitHubRepository> cloneRepository(
      EventId eventId,
      UserId userId,
      ProjectId projectId,
      RepositoryCoordinates repositoryCoordinates) {

    return CompletableFuture.supplyAsync(
        () -> {
          try {
            // Log starting
            logger.info(
                "Starting repository clone {} for project {}",
                repositoryCoordinates.repositoryUrl(),
                projectId);

            // Method call
            var workingDirectory = getLocalWorkingDirectory(userId, projectId);
            var repository = getGitHubRepository(repositoryCoordinates, workingDirectory);

            // Log succeed
            logger.info(
                "Successfully cloned repository {} for project {}",
                repositoryCoordinates.repositoryUrl(),
                projectId);

            // Dispatch success event
            fireCloneSucceeded(eventId, projectId, repositoryCoordinates, repository);

            return repository;

          } catch (GitHubNavigatorException e) {
            // Log error
            logger.error(
                "Failed to clone GitHub repository {} for project {}",
                repositoryCoordinates.repositoryUrl(),
                projectId,
                e);

            // Dispatch failed event
            fireCloneFailed(eventId, projectId, repositoryCoordinates, e);

            throw new RuntimeException("Failed to clone repository", e);
          }
        },
        projectHistoryImportExecutor);
  }

  private List<OntologyCommitChange> analyzeCommitHistory(
      GitHubRepository repository,
      RelativeFilePath targetOntologyFile,
      EventId eventId,
      ProjectId projectId,
      RepositoryCoordinates repositoryCoordinates) {

    try {
      // Start logging
      logger.info(
          "Starting commit history analysis for project {} from file {}",
          projectId,
          targetOntologyFile);

      // Method call
      var projectHistory = ontologyHistoryAnalyzer.getCommitHistory(targetOntologyFile, repository);

      // Log success
      logger.info(
          "Successfully analyzed commit history for project {} from file {}",
          projectId,
          targetOntologyFile);

      // Dispatch success event
      fireImportSucceeded(eventId, projectId, repositoryCoordinates);

      return projectHistory;

    } catch (OntologyComparisonException e) {
      // Log error
      logger.error(
          "Failed to analyze commit history for project {} from file {}",
          projectId,
          targetOntologyFile,
          e);

      // Dispatch failed event
      fireImportFailed(eventId, projectId, repositoryCoordinates, e);

      throw new RuntimeException("Failed to analyze commit history", e);
    }
  }

  private BlobLocation storeProjectHistory(
      List<OntologyCommitChange> projectHistory,
      EventId eventId,
      ProjectId projectId,
      RepositoryCoordinates repositoryCoordinates) {

    try {
      // Start logging
      logger.info("Starting project history storage for project {}", projectId);

      // Method call
      var projectHistoryLocation = projectHistoryStorer.storeProjectHistory(projectHistory);

      // Log success
      logger.info(
          "Successfully stored project history for project {} at location {}",
          projectId,
          projectHistoryLocation);

      // Dispatch success event
      fireStoreSucceeded(eventId, projectId, repositoryCoordinates, projectHistoryLocation);

      return projectHistoryLocation;

    } catch (Exception e) {
      // Log error
      logger.error("Failed to store project history for project {}", projectId, e);

      // Dispatch failed event
      fireStoreFailed(eventId, projectId, repositoryCoordinates, e);

      throw new RuntimeException("Failed to store project history", e);
    }
  }

  private void handleCompletion(
      BlobLocation documentLocation,
      Throwable throwable,
      EventId eventId,
      ProjectId projectId,
      RepositoryCoordinates repositoryCoordinates) {
    if (throwable != null) {
      logger.error(
          "Async processing failed for event {}: {}", eventId, throwable.getMessage(), throwable);
      fireRequestFailed(eventId, projectId, repositoryCoordinates, throwable);
      // TODO: Clean up here - remove temporary directories, etc.
    }
  }

  private Path getLocalWorkingDirectory(UserId userId, ProjectId projectId) {
    var tempDir = Paths.get(System.getProperty("java.io.tmpdir"));
    return tempDir.resolve(
        "github-repos" + File.separator + userId.value() + File.separator + projectId.value());
  }

  private GitHubRepository getGitHubRepository(
      RepositoryCoordinates repositoryCoordinates, Path workingDirectory)
      throws GitHubNavigatorException {
    var repository =
        GitHubRepositoryBuilderFactory.create(repositoryCoordinates)
            .localWorkingDirectory(workingDirectory)
            .build();
    repository.initialize();
    return repository;
  }

  private void fireCloneFailed(
      EventId eventId,
      ProjectId projectId,
      RepositoryCoordinates repositoryCoordinates,
      GitHubNavigatorException e) {
    eventDispatcher.dispatchEvent(
        new GitHubCloneRepositoryFailedEvent(
            eventId, projectId, repositoryCoordinates, e.getMessage()));
  }

  private void fireCloneSucceeded(
      EventId eventId,
      ProjectId projectId,
      RepositoryCoordinates repositoryCoordinates,
      GitHubRepository repository) {
    eventDispatcher.dispatchEvent(
        new GitHubCloneRepositorySucceededEvent(
            eventId, projectId, repositoryCoordinates, repository));
  }

  private void fireImportFailed(
      EventId eventId,
      ProjectId projectId,
      RepositoryCoordinates repositoryCoordinates,
      OntologyComparisonException e) {
    eventDispatcher.dispatchEvent(
        new GitHubProjectHistoryImportFailedEvent(
            eventId, projectId, repositoryCoordinates, e.getMessage()));
  }

  private void fireImportSucceeded(
      EventId eventId, ProjectId projectId, RepositoryCoordinates repositoryCoordinates) {
    eventDispatcher.dispatchEvent(
        new GitHubProjectHistoryImportSucceededEvent(eventId, projectId, repositoryCoordinates));
  }

  private void fireStoreFailed(
      EventId eventId,
      ProjectId projectId,
      RepositoryCoordinates repositoryCoordinates,
      Exception e) {
    eventDispatcher.dispatchEvent(
        new GitHubProjectHistoryStoreFailedEvent(
            eventId, projectId, repositoryCoordinates, e.getMessage()));
  }

  private void fireStoreSucceeded(
      EventId eventId,
      ProjectId projectId,
      RepositoryCoordinates repositoryCoordinates,
      BlobLocation projectHistoryLocation) {
    eventDispatcher.dispatchEvent(
        new GitHubProjectHistoryStoreSucceededEvent(
            eventId, projectId, repositoryCoordinates, projectHistoryLocation));
  }

  private void fireRequestFailed(
      EventId eventId,
      ProjectId projectId,
      RepositoryCoordinates repositoryCoordinates,
      Throwable t) {
    eventDispatcher.dispatchEvent(
        new GitHubCloneRequestFailedEvent(
            eventId, projectId, repositoryCoordinates, t.getMessage()));
  }
}
