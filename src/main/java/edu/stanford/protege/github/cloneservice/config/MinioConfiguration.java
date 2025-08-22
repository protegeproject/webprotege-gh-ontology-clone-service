package edu.stanford.protege.github.cloneservice.config;

import edu.stanford.protege.github.cloneservice.service.MinioProperties;
import io.minio.MinioClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for MinIO client setup.
 */
@Configuration
public class MinioConfiguration {

  private final MinioProperties minioProperties;

  public MinioConfiguration(MinioProperties minioProperties) {
    this.minioProperties = minioProperties;
  }

  /**
   * Creates and configures a MinIO client bean.
   *
   * @return configured MinioClient instance
   */
  @Bean
  public MinioClient minioClient() {
    return MinioClient.builder()
        .endpoint(minioProperties.getEndPoint())
        .credentials(minioProperties.getAccessKey(), minioProperties.getSecretKey())
        .build();
  }
}