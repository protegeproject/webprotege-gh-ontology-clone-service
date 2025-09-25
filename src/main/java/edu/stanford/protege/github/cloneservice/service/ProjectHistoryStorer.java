package edu.stanford.protege.github.cloneservice.service;

import edu.stanford.protege.github.cloneservice.exception.StorageException;
import edu.stanford.protege.github.cloneservice.model.OntologyCommitChange;
import edu.stanford.protege.webprotege.common.BlobLocation;
import edu.stanford.protege.webprotege.common.ProjectId;
import edu.stanford.protege.webprotege.revision.Revision;
import edu.stanford.protege.webprotege.revision.RevisionSerializationTask;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ProjectHistoryStorer {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final ProjectHistoryConverter projectHistoryConverter;
    private final MinioProjectHistoryDocumentStorer minioProjectHistoryDocumentStorer;

    public ProjectHistoryStorer(
            @Nonnull ProjectHistoryConverter projectHistoryConverter,
            @Nonnull MinioProjectHistoryDocumentStorer minioProjectHistoryDocumentStorer) {
        this.projectHistoryConverter =
                Objects.requireNonNull(projectHistoryConverter, "projectHistoryConverter cannot be null");
        this.minioProjectHistoryDocumentStorer = Objects.requireNonNull(
                minioProjectHistoryDocumentStorer, "minioProjectHistoryDocumentStorer cannot be null");
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
    public BlobLocation storeProjectHistory(ProjectId projectId, List<OntologyCommitChange> projectHistory) {
        try {
            var revisions = projectHistoryConverter.convertProjectHistoryToRevisions(projectHistory);
            return serializeAndStoreRevisions(projectId, revisions);
        } catch (IOException e) {
            logger.error("{} Problem storing project history", projectId, e);
            throw new UncheckedIOException("Problem storing project history", e);
        }
    }

    private BlobLocation serializeAndStoreRevisions(ProjectId projectId, List<Revision> revisions) throws IOException {
        var tempFilePath = Files.createTempFile("webprotege-", "-clone-project-history.bin");
        try {
            revisions.forEach(revision -> serialize(projectId, revision, tempFilePath));
            return minioProjectHistoryDocumentStorer.storeDocument(tempFilePath);
        } catch (StorageException | UncheckedIOException e) {
            logger.error("{} Problem serializing project history", projectId, e);
            throw e;
        } finally {
            try {
                Files.delete(tempFilePath);
            } catch (IOException e) {
                logger.error("{} Error deleting temp file {}", projectId, tempFilePath, e);
            }
        }
    }

    private void serialize(ProjectId projectId, Revision revision, Path tempFile) {
        try {
            var revisionSerializationTask = new RevisionSerializationTask(tempFile.toFile(), revision);
            revisionSerializationTask.call();
        } catch (IOException e) {
            logger.error(
                    "{} Error serializing revision {} to {}", projectId, revision.getRevisionNumber(), tempFile, e);
            throw new UncheckedIOException("Problem during serializing the project revision", e);
        }
    }
}
