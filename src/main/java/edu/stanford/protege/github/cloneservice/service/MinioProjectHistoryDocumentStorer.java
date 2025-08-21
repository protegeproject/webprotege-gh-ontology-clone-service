package edu.stanford.protege.github.cloneservice.service;

import edu.stanford.protege.github.cloneservice.exception.StorageException;
import edu.stanford.protege.webprotege.common.BlobLocation;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.UploadObjectArgs;
import io.minio.errors.*;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.file.Path;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

@Component
public class MinioProjectHistoryDocumentStorer {

    private final MinioClient minioClient;

    private final MinioProperties minioProperties;

    public MinioProjectHistoryDocumentStorer(@Nonnull MinioClient minioClient,
                                             @Nonnull MinioProperties minioProperties) {
        this.minioClient = minioClient;
        this.minioProperties = minioProperties;
    }

    /**
     * Stores a document file in MinIO object storage and returns its blob location.
     * 
     * <p>This method uploads a local file to MinIO storage, automatically creating the bucket
     * if it doesn't exist. The document is stored as a binary object with the content type
     * "application/octet-stream". A unique blob location is generated for each upload to
     * ensure proper organization and retrieval of stored documents.</p>
     * 
     * @param documentPath the path to the local file to be uploaded to MinIO storage
     * @return a {@link BlobLocation} containing the bucket name and object key where the
     *         document has been stored
     * @throws StorageException if any error occurs during the upload process, including
     *                         network issues, authentication failures, or MinIO server errors
     */
    public BlobLocation storeDocument(Path documentPath) {
        try {
            var location = generateBlobLocation();
            // Create bucket if necessary
            createBucketIfNecessary(location);
            minioClient.uploadObject(UploadObjectArgs.builder()
                    .filename(documentPath.toString())
                    .bucket(location.bucket())
                    .object(location.name())
                    .contentType("application/octet-stream")
                    .build());
            return location;
        } catch (ErrorResponseException | XmlParserException | ServerException | NoSuchAlgorithmException |
                 IOException | InvalidResponseException | InvalidKeyException | InternalException |
                 InsufficientDataException e) {
            throw new StorageException("Problem writing revision history document to storage", e);
        }
    }

    private void createBucketIfNecessary(BlobLocation location) throws ErrorResponseException, InsufficientDataException, InternalException, InvalidKeyException, InvalidResponseException, IOException, NoSuchAlgorithmException, ServerException, XmlParserException {
        if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(location.bucket()).build())) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(location.bucket()).build());
        }
    }

    private BlobLocation generateBlobLocation() {
        return new BlobLocation(minioProperties.getProjectHistoryDocumentsBucketName(), generateObjectName());
    }

    private static String generateObjectName() {
        return "project-history-" + UUID.randomUUID() + ".bin";
    }
}
