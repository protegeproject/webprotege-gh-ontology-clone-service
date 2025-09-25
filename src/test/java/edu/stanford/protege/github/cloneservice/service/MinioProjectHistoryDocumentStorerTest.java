package edu.stanford.protege.github.cloneservice.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import edu.stanford.protege.github.cloneservice.exception.StorageException;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.UploadObjectArgs;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit tests for {@link MinioProjectHistoryDocumentStorer} */
@ExtendWith(MockitoExtension.class)
@DisplayName("MinioProjectHistoryDocumentStorer Tests")
class MinioProjectHistoryDocumentStorerTest {

  private MinioProjectHistoryDocumentStorer documentStorer;

  @Mock private MinioClient minioClient;

  @Mock private MinioProperties minioProperties;

  @TempDir Path tempDir;

  @BeforeEach
  void setUp() {
    documentStorer = new MinioProjectHistoryDocumentStorer(minioClient, minioProperties);
  }

  @Test
  @DisplayName("Store document successfully when bucket exists")
  void storeDocumentSuccessfullyWhenBucketExists() throws Exception {
    // Arrange
    var bucketName = "test-bucket";
    var documentContent = "test document content";
    var documentFile = tempDir.resolve("test-document.bin");
    Files.writeString(documentFile, documentContent);

    when(minioProperties.getProjectHistoryDocumentsBucketName()).thenReturn(bucketName);
    when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);

    // Act
    var result = documentStorer.storeDocument(documentFile);

    // Assert
    assertNotNull(result);
    assertEquals(bucketName, result.bucket());
    assertTrue(result.name().startsWith("project-history-"));
    assertTrue(result.name().endsWith(".bin"));

    verify(minioClient).bucketExists(any(BucketExistsArgs.class));
    verify(minioClient, never()).makeBucket(any(MakeBucketArgs.class));

    var uploadArgsCaptor = ArgumentCaptor.forClass(UploadObjectArgs.class);
    verify(minioClient).uploadObject(uploadArgsCaptor.capture());

    var uploadArgs = uploadArgsCaptor.getValue();
    assertEquals(bucketName, uploadArgs.bucket());
    assertEquals(documentFile.toString(), uploadArgs.filename());
    assertEquals("application/octet-stream", uploadArgs.contentType());
  }

  @Test
  @DisplayName("Store document successfully when bucket does not exist")
  void storeDocumentSuccessfullyWhenBucketDoesNotExist() throws Exception {
    // Arrange
    var bucketName = "test-bucket";
    var documentContent = "test document content";
    var documentFile = tempDir.resolve("test-document.bin");
    Files.writeString(documentFile, documentContent);

    when(minioProperties.getProjectHistoryDocumentsBucketName()).thenReturn(bucketName);
    when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(false);

    // Act
    var result = documentStorer.storeDocument(documentFile);

    // Assert
    assertNotNull(result);
    assertEquals(bucketName, result.bucket());

    verify(minioClient).bucketExists(any(BucketExistsArgs.class));
    verify(minioClient).makeBucket(any(MakeBucketArgs.class));
    verify(minioClient).uploadObject(any(UploadObjectArgs.class));
  }

  @Test
  @DisplayName("Throw StorageException when MinIO client throws exception")
  void throwStorageExceptionWhenMinioClientThrowsException() throws Exception {
    // Arrange
    var bucketName = "test-bucket";
    var documentFile = tempDir.resolve("test-document.bin");
    Files.writeString(documentFile, "test content");

    when(minioProperties.getProjectHistoryDocumentsBucketName()).thenReturn(bucketName);
    when(minioClient.bucketExists(any(BucketExistsArgs.class)))
        .thenThrow(new RuntimeException("MinIO error"));

    // Act & Assert
    var exception =
        assertThrows(RuntimeException.class, () -> documentStorer.storeDocument(documentFile));

    assertEquals("MinIO error", exception.getMessage());
  }

  @Test
  @DisplayName("Throw StorageException when upload fails")
  void throwStorageExceptionWhenUploadFails() throws Exception {
    // Arrange
    var bucketName = "test-bucket";
    var documentFile = tempDir.resolve("test-document.bin");
    Files.writeString(documentFile, "test content");

    when(minioProperties.getProjectHistoryDocumentsBucketName()).thenReturn(bucketName);
    when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);
    doThrow(new IOException("Upload failed"))
        .when(minioClient)
        .uploadObject(any(UploadObjectArgs.class));

    // Act & Assert
    var exception =
        assertThrows(StorageException.class, () -> documentStorer.storeDocument(documentFile));

    assertTrue(
        exception.getMessage().startsWith("Problem writing revision history document to storage"));
    assertInstanceOf(IOException.class, exception.getCause());
  }

  @Test
  @DisplayName("Generate unique object names for different uploads")
  void generateUniqueObjectNamesForDifferentUploads() throws Exception {
    // Arrange
    var bucketName = "test-bucket";
    var documentFile1 = tempDir.resolve("doc1.bin");
    var documentFile2 = tempDir.resolve("doc2.bin");
    Files.writeString(documentFile1, "content 1");
    Files.writeString(documentFile2, "content 2");

    when(minioProperties.getProjectHistoryDocumentsBucketName()).thenReturn(bucketName);
    when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);

    // Act
    var result1 = documentStorer.storeDocument(documentFile1);
    var result2 = documentStorer.storeDocument(documentFile2);

    // Assert
    assertNotEquals(result1.name(), result2.name());
    assertTrue(result1.name().startsWith("project-history-"));
    assertTrue(result2.name().startsWith("project-history-"));
    assertTrue(result1.name().endsWith(".bin"));
    assertTrue(result2.name().endsWith(".bin"));
  }

  @Test
  @DisplayName("Use correct bucket name from properties")
  void useCorrectBucketNameFromProperties() throws Exception {
    // Arrange
    var bucketName = "webprotege-project-history-documents";
    var documentFile = tempDir.resolve("test-document.bin");
    Files.writeString(documentFile, "test content");

    when(minioProperties.getProjectHistoryDocumentsBucketName()).thenReturn(bucketName);
    when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);

    // Act
    var result = documentStorer.storeDocument(documentFile);

    // Assert
    assertEquals(bucketName, result.bucket());

    var bucketExistsCaptor = ArgumentCaptor.forClass(BucketExistsArgs.class);
    verify(minioClient).bucketExists(bucketExistsCaptor.capture());
    assertEquals(bucketName, bucketExistsCaptor.getValue().bucket());
  }

  @Test
  @DisplayName("Create bucket with correct name when it does not exist")
  void createBucketWithCorrectNameWhenItDoesNotExist() throws Exception {
    // Arrange
    var bucketName = "test-bucket";
    var documentFile = tempDir.resolve("test-document.bin");
    Files.writeString(documentFile, "test content");

    when(minioProperties.getProjectHistoryDocumentsBucketName()).thenReturn(bucketName);
    when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(false);

    // Act
    documentStorer.storeDocument(documentFile);

    // Assert
    var makeBucketCaptor = ArgumentCaptor.forClass(MakeBucketArgs.class);
    verify(minioClient).makeBucket(makeBucketCaptor.capture());
    assertEquals(bucketName, makeBucketCaptor.getValue().bucket());
  }

  @Test
  @DisplayName("Upload file with correct content type")
  void uploadFileWithCorrectContentType() throws Exception {
    // Arrange
    var bucketName = "test-bucket";
    var documentFile = tempDir.resolve("test-document.bin");
    Files.writeString(documentFile, "binary content");

    when(minioProperties.getProjectHistoryDocumentsBucketName()).thenReturn(bucketName);
    when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);

    // Act
    documentStorer.storeDocument(documentFile);

    // Assert
    var uploadArgsCaptor = ArgumentCaptor.forClass(UploadObjectArgs.class);
    verify(minioClient).uploadObject(uploadArgsCaptor.capture());
    assertEquals("application/octet-stream", uploadArgsCaptor.getValue().contentType());
  }
}
