package edu.stanford.protege.github.cloneservice.integration;

import static org.junit.jupiter.api.Assertions.*;

import edu.stanford.protege.commitnavigator.model.RepositoryCoordinates;
import edu.stanford.protege.github.cloneservice.model.RelativeFilePath;
import edu.stanford.protege.github.cloneservice.service.ProjectHistoryGenerator;
import edu.stanford.protege.webprotege.common.BlobLocation;
import edu.stanford.protege.webprotege.common.ProjectId;
import edu.stanford.protege.webprotege.common.UserId;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.StatObjectArgs;
import io.minio.errors.ErrorResponseException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Integration test that tests the complete workflow from GitHub repository analysis to MinIO
 * storage.
 *
 * <p>This test uses Testcontainers to automatically start and stop a MinIO instance for integration
 * testing. The MinIO container is managed automatically by the test lifecycle.
 *
 * <p>The test verifies the complete integration flow: - GitHub repository coordination and analysis
 * - Ontology history processing and revision generation - MinIO storage of revision documents -
 * File existence verification in MinIO
 *
 * <p>External dependencies: - Network access to GitHub (grocery-ontology repository) - Docker for
 * running the MinIO container - File system operations for temporary cloning
 */
@SpringBootTest
@Testcontainers
@DisplayName("Grocery Ontology MinIO Integration Tests")
class GroceryOntologyMinioIntegrationTest {

  @Container
  static final MinIOContainer minioContainer =
      new MinIOContainer("minio/minio:RELEASE.2024-01-16T16-07-38Z")
          .withUserName("webprotege")
          .withPassword("webprotege");

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("webprotege.minio.end-point", minioContainer::getS3URL);
    registry.add("webprotege.minio.access-key", minioContainer::getUserName);
    registry.add("webprotege.minio.secret-key", minioContainer::getPassword);
  }

  private static final Logger logger =
      LoggerFactory.getLogger(GroceryOntologyMinioIntegrationTest.class);

  private static final String GROCERY_ONTOLOGY_URL =
      "https://github.com/protegeteam/grocery-ontology";
  private static final String MASTER_BRANCH = "master";
  private static final RelativeFilePath ONTOLOGY_FILE_PATH = new RelativeFilePath("grocery.owl");

  @Autowired private ProjectHistoryGenerator projectHistoryGenerator;

  @Autowired private MinioClient minioClient;

  @Test
  @DisplayName(
      "Should generate project history from grocery ontology and store in MinIO with file existence verification")
  void generateProjectHistoryAndStoreInMinioWithFileVerification() throws Exception {
    // Arrange
    logger.info("Starting MinIO integration test for grocery ontology");
    var userId = UserId.valueOf("integration-test-user");
    var projectId = ProjectId.generate();
    var repositoryCoordinates =
        RepositoryCoordinates.createFromUrl(GROCERY_ONTOLOGY_URL, MASTER_BRANCH);

    logger.info(
        "Test configuration - User: {}, Project: {}, Repository: {}, Branch: {}, Target file: {}",
        userId.id(),
        projectId.id(),
        GROCERY_ONTOLOGY_URL,
        MASTER_BRANCH,
        ONTOLOGY_FILE_PATH.value());

    // Act
    logger.info("Generating and storing project history in MinIO");
    var blobLocation =
        projectHistoryGenerator.writeProjectHistoryFromGitHubRepo(
            userId, projectId, repositoryCoordinates, ONTOLOGY_FILE_PATH);

    // Assert - Verify BlobLocation structure
    assertNotNull(blobLocation, "BlobLocation should not be null");
    assertNotNull(blobLocation.bucket(), "Bucket name should not be null");
    assertNotNull(blobLocation.name(), "Object name should not be null");
    assertEquals(
        "webprotege-project-history-documents",
        blobLocation.bucket(),
        "Should use correct bucket name");
    assertTrue(
        blobLocation.name().startsWith("project-history-"),
        "Object name should start with 'project-history-'");
    assertTrue(blobLocation.name().endsWith(".bin"), "Object name should end with '.bin'");

    logger.info(
        "Successfully stored project history at bucket: {}, object: {}",
        blobLocation.bucket(),
        blobLocation.name());

    // Assert - Verify file exists in MinIO
    logger.info("Verifying file existence in MinIO");
    verifyFileExistsInMinio(blobLocation);

    // Assert - Verify file properties
    logger.info("Verifying file properties in MinIO");
    verifyFilePropertiesInMinio(blobLocation);

    // Assert - Verify file is readable
    logger.info("Verifying file is readable from MinIO");
    verifyFileIsReadableFromMinio(blobLocation);

    logger.info("Successfully completed MinIO integration test");
  }

  @Test
  @DisplayName("Should handle multiple project history generations with unique file names in MinIO")
  void handleMultipleProjectHistoryGenerationsWithUniqueFileNames() throws Exception {
    // Arrange
    logger.info("Testing multiple project history generations for unique file naming");
    var userId = UserId.valueOf("multi-test-user");
    var projectId1 = ProjectId.generate();
    var projectId2 = ProjectId.generate();
    var repositoryCoordinates =
        RepositoryCoordinates.createFromUrl(GROCERY_ONTOLOGY_URL, MASTER_BRANCH);

    // Act
    var blobLocation1 =
        projectHistoryGenerator.writeProjectHistoryFromGitHubRepo(
            userId, projectId1, repositoryCoordinates, ONTOLOGY_FILE_PATH);
    var blobLocation2 =
        projectHistoryGenerator.writeProjectHistoryFromGitHubRepo(
            userId, projectId2, repositoryCoordinates, ONTOLOGY_FILE_PATH);

    // Assert
    assertNotEquals(
        blobLocation1.name(),
        blobLocation2.name(),
        "Different generations should produce unique file names");
    assertEquals(
        blobLocation1.bucket(),
        blobLocation2.bucket(),
        "Should use same bucket for all project histories");

    // Verify both files exist
    verifyFileExistsInMinio(blobLocation1);
    verifyFileExistsInMinio(blobLocation2);

    logger.info(
        "Successfully verified unique file names: {} and {}",
        blobLocation1.name(),
        blobLocation2.name());
  }

  @Test
  @DisplayName("Should verify MinIO bucket auto-creation functionality")
  void verifyMinioBucketAutoCreationFunctionality() throws Exception {
    // This test verifies that the bucket is created automatically if it doesn't exist
    // Since we're using the same bucket name, it should already exist, but we can verify
    // the storage functionality works regardless

    // Arrange
    var userId = UserId.valueOf("bucket-test-user");
    var projectId = ProjectId.generate();
    var repositoryCoordinates =
        RepositoryCoordinates.createFromUrl(GROCERY_ONTOLOGY_URL, MASTER_BRANCH);

    // Act & Assert
    assertDoesNotThrow(
        () ->
            projectHistoryGenerator.writeProjectHistoryFromGitHubRepo(
                userId, projectId, repositoryCoordinates, ONTOLOGY_FILE_PATH),
        "Should successfully store project history even if bucket needs to be created");
  }

  /**
   * Verifies that a file exists in MinIO using the StatObject operation.
   *
   * @param blobLocation the location of the file to verify
   * @throws Exception if verification fails
   */
  private void verifyFileExistsInMinio(BlobLocation blobLocation) throws Exception {
    try {
      var statObjectResponse =
          minioClient.statObject(
              StatObjectArgs.builder()
                  .bucket(blobLocation.bucket())
                  .object(blobLocation.name())
                  .build());

      assertNotNull(statObjectResponse, "StatObject response should not be null");
      assertTrue(statObjectResponse.size() > 0, "File should have content (size > 0)");
      assertEquals(
          "application/octet-stream",
          statObjectResponse.contentType(),
          "File should have correct content type");

      logger.info(
          "File verification successful - Size: {} bytes, Content-Type: {}, ETag: {}",
          statObjectResponse.size(),
          statObjectResponse.contentType(),
          statObjectResponse.etag());

    } catch (ErrorResponseException e) {
      if (e.errorResponse().code().equals("NoSuchKey")) {
        fail("File does not exist in MinIO: " + blobLocation.name());
      } else {
        throw new Exception("Failed to verify file existence in MinIO", e);
      }
    }
  }

  /**
   * Verifies additional properties of the stored file in MinIO.
   *
   * @param blobLocation the location of the file to verify
   * @throws Exception if verification fails
   */
  private void verifyFilePropertiesInMinio(BlobLocation blobLocation) throws Exception {
    var statObjectResponse =
        minioClient.statObject(
            StatObjectArgs.builder()
                .bucket(blobLocation.bucket())
                .object(blobLocation.name())
                .build());

    // Verify the file has reasonable size (project history should not be empty)
    assertTrue(
        statObjectResponse.size() > 100,
        "Project history file should have substantial content (> 100 bytes)");

    // Verify content type is set correctly for binary files
    assertEquals(
        "application/octet-stream",
        statObjectResponse.contentType(),
        "Binary revision files should have octet-stream content type");

    // Verify the file has metadata indicating it was recently created
    assertNotNull(statObjectResponse.lastModified(), "File should have last modified timestamp");
  }

  /**
   * Verifies that the stored file can be read from MinIO.
   *
   * @param blobLocation the location of the file to read
   * @throws Exception if reading fails
   */
  private void verifyFileIsReadableFromMinio(BlobLocation blobLocation) throws Exception {
    try (var inputStream =
        minioClient.getObject(
            GetObjectArgs.builder()
                .bucket(blobLocation.bucket())
                .object(blobLocation.name())
                .build())) {

      assertNotNull(inputStream, "Should be able to get input stream for stored file");

      // Read first few bytes to verify file is accessible and has content
      var firstBytes = inputStream.readNBytes(10);
      assertTrue(firstBytes.length > 0, "File should have readable content");

      logger.info("Successfully read {} bytes from stored file", firstBytes.length);
    }
  }
}
