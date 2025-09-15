package edu.stanford.protege.github.cloneservice.service;

import edu.stanford.protege.github.cloneservice.model.OntologyCommitChange;
import edu.stanford.protege.webprotege.common.BlobLocation;
import edu.stanford.protege.webprotege.revision.Revision;
import edu.stanford.protege.webprotege.revision.RevisionSerializationTask;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nonnull;
import org.springframework.stereotype.Component;

@Component
public class ProjectHistoryStorer {

  private final ChangeCommitToRevisionConverter changeCommitToRevisionConverter;
  private final MinioProjectHistoryDocumentStorer minioProjectHistoryDocumentStorer;

  public ProjectHistoryStorer(
      @Nonnull ChangeCommitToRevisionConverter changeCommitToRevisionConverter,
      @Nonnull MinioProjectHistoryDocumentStorer minioProjectHistoryDocumentStorer) {
    this.changeCommitToRevisionConverter = changeCommitToRevisionConverter;
    this.minioProjectHistoryDocumentStorer = minioProjectHistoryDocumentStorer;
  }

  /**
   * Stores a project's commit history as a serialized document in blob storage.
   *
   * <p>This method converts ontology commit changes to WebProtege revisions, serializes them to a
   * temporary binary file, uploads the file to MinIO blob storage, and cleans up the temporary
   * file. The entire project history is stored as a single document that can be retrieved later to
   * be used by the WebProtege platform.
   *
   * @param projectHistory a list of ontology commit changes representing the project's history
   * @return a {@link BlobLocation} indicating where the serialized project history document has
   *     been stored in blob storage
   * @throws UncheckedIOException if an I/O error occurs during file creation, serialization,
   *     storage, or cleanup operations
   */
  public BlobLocation storeProjectHistory(List<OntologyCommitChange> projectHistory) {
    try {
      var tempFile = Files.createTempFile("webprotege-", "-clone-project-history.bin");
      // Reverse the order to have the oldest changes as the first revision
      var reversedHistory = new ArrayList<>(projectHistory);
      Collections.reverse(reversedHistory);
      reversedHistory.stream()
          .map(changeCommitToRevisionConverter::convert)
          .forEach(revision -> serialize(revision, tempFile));
      var location = minioProjectHistoryDocumentStorer.storeDocument(tempFile);
      Files.delete(tempFile);
      return location;
    } catch (IOException e) {
      throw new UncheckedIOException("Problem storing project history", e);
    }
  }

  private void serialize(Revision revision, Path tempFile) {
    try {
      var revisionSerializationTask = new RevisionSerializationTask(tempFile.toFile(), revision);
      revisionSerializationTask.call();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
