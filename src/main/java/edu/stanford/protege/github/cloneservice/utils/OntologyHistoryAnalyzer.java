package edu.stanford.protege.github.cloneservice.utils;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import edu.stanford.protege.commitnavigator.GitHubRepository;
import edu.stanford.protege.commitnavigator.config.CommitNavigatorConfig;
import edu.stanford.protege.commitnavigator.model.CommitMetadata;
import edu.stanford.protege.github.cloneservice.exception.OntologyComparisonException;
import edu.stanford.protege.github.cloneservice.model.AxiomChange;
import edu.stanford.protege.github.cloneservice.model.OntologyCommitChange;
import edu.stanford.protege.github.cloneservice.model.RelativeFilePath;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nonnull;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Main service for analyzing ontology history across Git commits */
@Component
public class OntologyHistoryAnalyzer {

  private static final Logger logger = LoggerFactory.getLogger(OntologyHistoryAnalyzer.class);

  private final OntologyLoader ontologyLoader;
  private final OntologyDifferenceCalculator differenceCalculator;

  public OntologyHistoryAnalyzer(
      OntologyLoader ontologyLoader, OntologyDifferenceCalculator differenceCalculator) {
    this.ontologyLoader = Objects.requireNonNull(ontologyLoader, "OntologyLoader cannot be null");
    this.differenceCalculator =
        Objects.requireNonNull(differenceCalculator, "OntologyDifferenceCalculator cannot be null");
  }

  /**
   * Analyzes ontology history across all consecutive commits from HEAD backwards
   *
   * @param ontologyFilePath The name of the ontology file to analyze
   * @param gitHubRepository The GitHub repository where all commits are stored
   * @return List of all ontology changes across commit history
   * @throws OntologyComparisonException if analysis fails
   */
  @Nonnull
  public List<OntologyCommitChange> getCommitHistory(
      @Nonnull RelativeFilePath ontologyFilePath, @Nonnull GitHubRepository gitHubRepository)
      throws OntologyComparisonException {

    Objects.requireNonNull(ontologyFilePath, "ontologyFilePath cannot be null");
    Objects.requireNonNull(gitHubRepository, "gitHubRepository cannot be null");

    logger.info(
        "Starting ontology commit history analysis for ontology file: {}", ontologyFilePath);

    var allCommitChanges = Lists.<OntologyCommitChange>newArrayList();

    try {
      // Configure commit navigator to focus on the target ontology file
      var targetOntologyFile = ontologyFilePath.asString();
      var commitNavigatorConfig =
          CommitNavigatorConfig.builder().fileFilters(targetOntologyFile).build();
      var commitNavigator = gitHubRepository.getCommitNavigator(commitNavigatorConfig);

      // Resolve the absolute path to the ontology file in the local clone
      var ontologyFile = commitNavigator.resolveFilePath(targetOntologyFile);

      // Get the current commit metadata
      var childCommitMetadata = commitNavigator.getCurrentCommit();
      var childCommitOntologies =
          loadOntologiesWithErrorHandling(ontologyFile, childCommitMetadata);

      while (commitNavigator.hasPrevious()) {
        // Trace back to previous commit
        commitNavigator.previousAndCheckout();

        // Get the previous commit metadata
        var parentCommitMetadata = commitNavigator.getCurrentCommit();

        // Load ontologies at the previous commit
        var parentCommitOntologies =
            loadOntologiesWithErrorHandling(ontologyFile, parentCommitMetadata);

        if (childCommitOntologies != null && parentCommitOntologies != null) {
          var axiomChanges =
              calculateAxiomChangesBetweenOntologies(childCommitOntologies, parentCommitOntologies);
          allCommitChanges.add(new OntologyCommitChange(axiomChanges, childCommitMetadata));

          // Swap the metadata and ontologies from parent commit to be the child commit
          childCommitOntologies = parentCommitOntologies;
          childCommitMetadata = parentCommitMetadata;
        }
      }

      // Handle the initial commit
      if (childCommitOntologies != null) {
        var axiomChanges = calculateInitialOntologyChanges(childCommitOntologies);
        allCommitChanges.add(new OntologyCommitChange(axiomChanges, childCommitMetadata));
      }

      return ImmutableList.copyOf(allCommitChanges);
    } catch (Exception e) {
      throw new OntologyComparisonException("Failed to analyze ontology commit history", e);
    }
  }

  /**
   * Loads ontologies with centralized error handling and logging
   *
   * @param ontologyFile the ontology file to load
   * @param commitMetadata metadata of the current commit for logging
   * @return loaded ontologies or null if loading failed
   */
  private List<OWLOntology> loadOntologiesWithErrorHandling(
      @Nonnull Path ontologyFile, @Nonnull CommitMetadata commitMetadata) {
    try {
      return ontologyLoader.loadOntologyWithImports(ontologyFile);
    } catch (Exception e) {
      logger.info(
          "Skipping commit {} due to ontology load error: {}",
          commitMetadata.commitHash(),
          e.getMessage());
      return null;
    }
  }

  /**
   * Calculates axiom changes between current and previous commit ontologies
   *
   * @param childCommitOntologies ontologies from the child commit
   * @param parentCommitOntologies ontologies from the parent commit
   * @return list of axiom changes between commits
   */
  @Nonnull
  private List<AxiomChange> calculateAxiomChangesBetweenOntologies(
      @Nonnull List<OWLOntology> childCommitOntologies,
      @Nonnull List<OWLOntology> parentCommitOntologies) {

    var allAxiomChanges = Lists.<AxiomChange>newArrayList();

    // Process current ontologies one by one and find their previous versions
    var results =
        childCommitOntologies.stream()
            .map(current -> processMatchingOntology(current, parentCommitOntologies))
            .toList();

    var processedOntologyIds = Lists.<OWLOntologyID>newArrayList();
    for (var result : results) {
      allAxiomChanges.addAll(result.axiomChanges());
      processedOntologyIds.add(result.ontologyID());
    }

    // Process removed ontologies (exist in the parent commit but not in the child commit)
    var emptyOntology = ontologyLoader.createEmptyOntology();
    var removedOntologyChanges =
        parentCommitOntologies.stream()
            .filter(ontology -> !processedOntologyIds.contains(ontology.getOntologyID()))
            .flatMap(
                ontology ->
                    differenceCalculator
                        .calculateAxiomChanges(emptyOntology, ontology, ontology.getOntologyID())
                        .stream())
            .toList();

    allAxiomChanges.addAll(removedOntologyChanges);
    return ImmutableList.copyOf(allAxiomChanges);
  }

  /**
   * Calculates axiom changes for the initial commit (compared to empty ontology)
   *
   * @param ontologies ontologies from the initial commit
   * @return list of axiom changes for initial commit
   */
  @Nonnull
  private List<AxiomChange> calculateInitialOntologyChanges(@Nonnull List<OWLOntology> ontologies) {

    var emptyOntology = ontologyLoader.createEmptyOntology();
    return ontologies.stream()
        .flatMap(
            ontology ->
                differenceCalculator
                    .calculateAxiomChanges(ontology, emptyOntology, ontology.getOntologyID())
                    .stream())
        .collect(ImmutableList.toImmutableList());
  }

  /**
   * Processes an ontology from a child commit by finding first its match from the parent commit and
   * then calculating changes. If no match is found, compares it to an empty ontology.
   *
   * @param childCommitOntology the ontology to process from a child commit.
   * @param parentCommitOntologies list of ontologies to match against, coming from the parent
   *     commit.
   * @return processing result containing axiom changes and ontology ID
   */
  @Nonnull
  private OntologyProcessingResult processMatchingOntology(
      @Nonnull OWLOntology childCommitOntology, @Nonnull List<OWLOntology> parentCommitOntologies) {

    var emptyOntology = ontologyLoader.createEmptyOntology();

    var ontologyId = childCommitOntology.getOntologyID();
    var matchedOntology = findMatchingOntology(childCommitOntology, parentCommitOntologies);

    var axiomChanges =
        matchedOntology
            .map(
                parentCommitOntology ->
                    differenceCalculator.calculateAxiomChanges(
                        childCommitOntology, parentCommitOntology, ontologyId))
            .orElseGet(
                () ->
                    differenceCalculator.calculateAxiomChanges(
                        childCommitOntology, emptyOntology, ontologyId));

    return new OntologyProcessingResult(axiomChanges, ontologyId);
  }

  /**
   * Finds matching ontology in the given ontologies list
   *
   * @param targetOntology the ontology to find a match for
   * @param ontologiesToSearch list of ontologies to search in
   * @return Optional containing the matching ontology, or empty if not found
   */
  @Nonnull
  private Optional<OWLOntology> findMatchingOntology(
      @Nonnull OWLOntology targetOntology, @Nonnull List<OWLOntology> ontologiesToSearch) {

    return ontologiesToSearch.stream()
        .filter(ontology -> ontology.getOntologyID().equals(targetOntology.getOntologyID()))
        .findFirst();
  }

  /** Internal record for holding ontology processing results */
  private record OntologyProcessingResult(
      @Nonnull List<AxiomChange> axiomChanges, @Nonnull OWLOntologyID ontologyID) {
    private OntologyProcessingResult {
      Objects.requireNonNull(axiomChanges, "axiomChanges cannot be null");
      Objects.requireNonNull(ontologyID, "ontologyID cannot be null");
    }
  }
}
