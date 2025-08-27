package edu.stanford.protege.github.cloneservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Main Spring Boot application class for the WebProtégé GitHub Ontology Clone Service.
 *
 * <p>This microservice analyzes GitHub repositories containing ontology files, extracts their
 * change history, and transforms it into WebProtégé-compatible revision documents stored in cloud
 * storage.
 */
@SpringBootApplication
@EnableConfigurationProperties
public class GitHubCloneServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(GitHubCloneServiceApplication.class, args);
  }
}
