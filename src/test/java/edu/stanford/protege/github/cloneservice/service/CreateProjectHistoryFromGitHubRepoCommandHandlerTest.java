package edu.stanford.protege.github.cloneservice.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import edu.stanford.protege.commitnavigator.model.RepositoryCoordinates;
import edu.stanford.protege.github.cloneservice.model.RelativeFilePath;
import edu.stanford.protege.webprotege.common.BlobLocation;
import edu.stanford.protege.webprotege.common.ProjectId;
import edu.stanford.protege.webprotege.common.UserId;
import edu.stanford.protege.webprotege.ipc.ExecutionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

/** Unit tests for {@link CreateProjectHistoryFromGitHubRepoCommandHandler} */
@ExtendWith(MockitoExtension.class)
@DisplayName("CreateProjectHistoryFromGitHubRepoCommandHandler Tests")
class CreateProjectHistoryFromGitHubRepoCommandHandlerTest {

  private CreateProjectHistoryFromGitHubRepoCommandHandler commandHandler;

  @Mock private ProjectHistoryGenerator projectHistoryGenerator;

  @Mock private ExecutionContext executionContext;

  @Mock private RepositoryCoordinates repositoryCoordinates;

  private ProjectId testProjectId;
  private UserId testUserId;
  private BlobLocation testBlobLocation;
  private RelativeFilePath testTargetOntologyFile;
  private CreateProjectHistoryFromGitHubRepoRequest testRequest;

  @BeforeEach
  void setUp() {
    commandHandler = new CreateProjectHistoryFromGitHubRepoCommandHandler(projectHistoryGenerator);
    testProjectId = ProjectId.valueOf("12345678-1234-1234-1234-123456789012");
    testUserId = UserId.valueOf("test-user");
    testBlobLocation = new BlobLocation("test-bucket", "test/path/document.json");
    testTargetOntologyFile = new RelativeFilePath("ontology.owl");

    testRequest =
        new CreateProjectHistoryFromGitHubRepoRequest(
            testProjectId, repositoryCoordinates, testTargetOntologyFile);
  }

  @Test
  @DisplayName("handleRequest should process request successfully and return response")
  void handleRequestShouldProcessRequestSuccessfullyAndReturnResponse() throws Exception {
    // Arrange
    when(executionContext.userId()).thenReturn(testUserId);
    when(projectHistoryGenerator.writeProjectHistoryFromGitHubRepo(
            testUserId, testProjectId, repositoryCoordinates, testTargetOntologyFile))
        .thenReturn(testBlobLocation);

    // Act
    Mono<CreateProjectHistoryFromGitHubRepoResponse> result =
        commandHandler.handleRequest(testRequest, executionContext);

    // Assert
    CreateProjectHistoryFromGitHubRepoResponse response = result.block();
    assertNotNull(response);
    assertEquals(testProjectId, response.projectId());
    assertEquals(repositoryCoordinates, response.repositoryCoordinates());
    assertEquals(testBlobLocation, response.projectHistoryLocation());

    // Verify interactions with mocked dependencies
    verify(projectHistoryGenerator)
        .writeProjectHistoryFromGitHubRepo(
            testUserId, testProjectId, repositoryCoordinates, testTargetOntologyFile);
    verify(executionContext).userId();
  }

  @Test
  @DisplayName("handleRequest should pass all request parameters correctly")
  void handleRequestShouldPassAllRequestParametersCorrectly() throws Exception {
    // Arrange
    var specificProjectId = ProjectId.valueOf("87654321-4321-4321-4321-210987654321");
    var specificUserId = UserId.valueOf("specific-user");
    var specificTargetFile = new RelativeFilePath("specific-ontology.ttl");
    var specificRequest =
        new CreateProjectHistoryFromGitHubRepoRequest(
            specificProjectId, repositoryCoordinates, specificTargetFile);

    when(executionContext.userId()).thenReturn(specificUserId);
    when(projectHistoryGenerator.writeProjectHistoryFromGitHubRepo(
            specificUserId, specificProjectId, repositoryCoordinates, specificTargetFile))
        .thenReturn(testBlobLocation);

    // Act
    commandHandler.handleRequest(specificRequest, executionContext).block();

    // Assert - verify exact parameter passing
    verify(projectHistoryGenerator)
        .writeProjectHistoryFromGitHubRepo(
            eq(specificUserId),
            eq(specificProjectId),
            eq(repositoryCoordinates),
            eq(specificTargetFile));
    verify(executionContext).userId();
  }

  @Test
  @DisplayName("handleRequest should preserve repository coordinates in response")
  void handleRequestShouldPreserveRepositoryCoordinatesInResponse() throws Exception {
    // Arrange
    when(executionContext.userId()).thenReturn(testUserId);
    when(projectHistoryGenerator.writeProjectHistoryFromGitHubRepo(any(), any(), any(), any()))
        .thenReturn(testBlobLocation);

    // Act
    Mono<CreateProjectHistoryFromGitHubRepoResponse> result =
        commandHandler.handleRequest(testRequest, executionContext);

    // Assert
    CreateProjectHistoryFromGitHubRepoResponse response = result.block();
    assertNotNull(response);
    assertSame(repositoryCoordinates, response.repositoryCoordinates());
    assertSame(testProjectId, response.projectId());
  }
}
