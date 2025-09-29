package edu.stanford.protege.github.cloneservice.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import edu.stanford.protege.commitnavigator.model.RepositoryCoordinates;
import edu.stanford.protege.github.cloneservice.model.RelativeFilePath;
import edu.stanford.protege.github.cloneservice.utils.OntologyHistoryAnalyzer;
import edu.stanford.protege.webprotege.common.BlobLocation;
import edu.stanford.protege.webprotege.common.ProjectId;
import edu.stanford.protege.webprotege.common.UserId;
import edu.stanford.protege.webprotege.ipc.EventDispatcher;
import edu.stanford.protege.webprotege.ipc.ExecutionContext;
import java.util.concurrent.Executor;
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

    @Mock
    private OntologyHistoryAnalyzer ontologyHistoryAnalyzer;

    @Mock
    private ProjectHistoryStorer projectHistoryStorer;

    @Mock
    private EventDispatcher eventDispatcher;

    @Mock
    private Executor executor;

    @Mock
    private ExecutionContext executionContext;

    @Mock
    private RepositoryCoordinates repositoryCoordinates;

    private ProjectId testProjectId;
    private UserId testUserId;
    private BlobLocation testBlobLocation;
    private RelativeFilePath testTargetOntologyFile;
    private CreateProjectHistoryFromGitHubRepoRequest testRequest;

    @BeforeEach
    void setUp() {
        commandHandler = new CreateProjectHistoryFromGitHubRepoCommandHandler(
                ontologyHistoryAnalyzer, projectHistoryStorer, eventDispatcher, executor);
        testProjectId = ProjectId.valueOf("12345678-1234-1234-1234-123456789012");
        testUserId = UserId.valueOf("test-user");
        testBlobLocation = new BlobLocation("test-bucket", "test/path/document.json");
        testTargetOntologyFile = new RelativeFilePath("ontology.owl");

        testRequest = new CreateProjectHistoryFromGitHubRepoRequest(
                testProjectId, repositoryCoordinates, testTargetOntologyFile);
    }

    @Test
    @DisplayName("handleRequest should process request successfully and return response")
    void handleRequestShouldProcessRequestSuccessfullyAndReturnResponse() {
        // Arrange
        when(executionContext.userId()).thenReturn(testUserId);

        // Act
        Mono<CreateProjectHistoryFromGitHubRepoResponse> result =
                commandHandler.handleRequest(testRequest, executionContext);

        // Assert
        CreateProjectHistoryFromGitHubRepoResponse response = result.block();
        assertNotNull(response);
        assertEquals(testProjectId, response.projectId());
        assertEquals(repositoryCoordinates, response.repositoryCoordinates());
        assertNotNull(response.operationId(), "Event ID should be generated");

        // Verify the execution context was accessed
        verify(executionContext).userId();
    }

    @Test
    @DisplayName("handleRequest should return response with correct request data")
    void handleRequestShouldReturnResponseWithCorrectRequestData() {
        // Arrange
        var specificProjectId = ProjectId.valueOf("87654321-4321-4321-4321-210987654321");
        var specificUserId = UserId.valueOf("specific-user");
        var specificTargetFile = new RelativeFilePath("specific-ontology.ttl");
        var specificRequest = new CreateProjectHistoryFromGitHubRepoRequest(
                specificProjectId, repositoryCoordinates, specificTargetFile);

        when(executionContext.userId()).thenReturn(specificUserId);

        // Act
        CreateProjectHistoryFromGitHubRepoResponse response =
                commandHandler.handleRequest(specificRequest, executionContext).block();

        // Assert - verify response contains the correct data from the request
        assertNotNull(response);
        assertEquals(specificProjectId, response.projectId());
        assertEquals(repositoryCoordinates, response.repositoryCoordinates());
        assertNotNull(response.operationId());
        verify(executionContext).userId();
    }

    @Test
    @DisplayName("handleRequest should generate unique event IDs for different requests")
    void handleRequestShouldGenerateUniqueEventIdsForDifferentRequests() {
        // Arrange
        when(executionContext.userId()).thenReturn(testUserId);

        // Act - make two separate requests
        CreateProjectHistoryFromGitHubRepoResponse response1 =
                commandHandler.handleRequest(testRequest, executionContext).block();
        CreateProjectHistoryFromGitHubRepoResponse response2 =
                commandHandler.handleRequest(testRequest, executionContext).block();

        // Assert - event IDs should be different
        assertNotNull(response1);
        assertNotNull(response2);
        assertNotEquals(
                response1.operationId(), response2.operationId(), "Each request should generate a unique event ID");

        // But project and repository data should be the same
        assertEquals(response1.projectId(), response2.projectId());
        assertEquals(response1.repositoryCoordinates(), response2.repositoryCoordinates());
    }
}
