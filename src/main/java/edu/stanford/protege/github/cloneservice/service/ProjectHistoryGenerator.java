package edu.stanford.protege.github.cloneservice.service;

import edu.stanford.protege.commitnavigator.GitHubRepository;
import edu.stanford.protege.commitnavigator.GitHubRepositoryBuilderFactory;
import edu.stanford.protege.commitnavigator.exceptions.GitHubNavigatorException;
import edu.stanford.protege.commitnavigator.model.RepositoryCoordinate;
import edu.stanford.protege.github.cloneservice.utils.OntologyHistoryAnalyzer;
import edu.stanford.protege.webprotege.common.BlobLocation;
import edu.stanford.protege.webprotege.common.ProjectId;
import edu.stanford.protege.webprotege.common.UserId;
import javax.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ProjectHistoryGenerator {

  private final Logger logger = LoggerFactory.getLogger(ProjectHistoryGenerator.class);

  private final OntologyHistoryAnalyzer ontologyHistoryAnalyzer;
  private final ProjectHistoryStorer projectHistoryStorer;

  public ProjectHistoryGenerator(
      @Nonnull OntologyHistoryAnalyzer ontologyHistoryAnalyzer,
      @Nonnull ProjectHistoryStorer projectHistoryStorer) {
    this.ontologyHistoryAnalyzer = ontologyHistoryAnalyzer;
    this.projectHistoryStorer = projectHistoryStorer;
  }

  /**
   * Generates and stores the project history for a specified ontology file from a GitHub
   * repository.
   *
   * <p>This method retrieves the commit history for the target ontology file from the specified
   * GitHub repository, processes it into a project history document, and stores it in a blob
   * location.
   *
   * @param userId the unique identifier of the WebProtege user
   * @param projectId the unique identifier of the ontology project in WebProtege
   * @param repositoryCoordinate the GitHub repository specification
   * @param targetOntologyFile the ontology file in the repository to generate the history for
   * @return a {@link BlobLocation} indicating where the project history document has been stored
   * @throws Exception if an error occurs during repository access, history analysis, or storage
   *     operations. This may include network issues, authentication failures, file access problems,
   *     or serialization errors
   */
  public BlobLocation writeProjectHistoryFromGitHubRepo(
      UserId userId,
      ProjectId projectId,
      RepositoryCoordinate repositoryCoordinate,
      String targetOntologyFile)
      throws Exception {
    var repository = getGitHubRepository(repositoryCoordinate);
    var projectHistory = ontologyHistoryAnalyzer.getCommitHistory(targetOntologyFile, repository);
    var projectHistoryLocation = projectHistoryStorer.storeProjectHistory(projectHistory);
    logger.info("Stored project history document at: {}", projectHistoryLocation);
    return projectHistoryLocation;
  }

  private GitHubRepository getGitHubRepository(RepositoryCoordinate repositoryCoordinate)
      throws GitHubNavigatorException {
    var repository =
        GitHubRepositoryBuilderFactory.create(repositoryCoordinate)
            .fileFilters("*.owl", "*.rdf", "*.ttl")
            .build();
    repository.initialize();
    return repository;
  }
}
