package edu.stanford.protege.github.cloneservice.utils;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import edu.stanford.protege.commitnavigator.GitHubRepository;
import edu.stanford.protege.github.cloneservice.exception.OntologyComparisonException;
import edu.stanford.protege.github.cloneservice.model.AxiomChange;
import edu.stanford.protege.github.cloneservice.model.OntologyCommitChange;
import edu.stanford.protege.github.cloneservice.model.RelativeFilePath;
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
      var commitNavigator = gitHubRepository.getCommitNavigator();
      var cloneDirectory = gitHubRepository.getConfig().getLocalCloneDirectory();
      var ontologyFile = cloneDirectory.resolve(ontologyFilePath.asPath());

      while (true) {
        var commitMetadata = commitNavigator.getCurrentCommit();
        var currentOntologies = ontologyLoader.loadOntologyWithImports(ontologyFile);

        if (commitNavigator.hasPrevious()) {
          commitNavigator.previousAndCheckout();
          var previousOntologies = ontologyLoader.loadOntologyWithImports(ontologyFile);
          var axiomChanges =
              calculateAxiomChangesBetweenCommits(currentOntologies, previousOntologies);
          allCommitChanges.add(new OntologyCommitChange(axiomChanges, commitMetadata));
        } else {
          var axiomChanges = calculateInitialCommitChanges(currentOntologies);
          allCommitChanges.add(new OntologyCommitChange(axiomChanges, commitMetadata));
          break;
        }
      }
      return ImmutableList.copyOf(allCommitChanges);

    } catch (Exception e) {
      throw new OntologyComparisonException("Failed to analyze ontology commit history", e);
    }
  }

  /**
   * Calculates axiom changes between current and previous commit ontologies
   *
   * @param currentOntologies ontologies from current commit
   * @param previousOntologies ontologies from previous commit
   * @return list of axiom changes between commits
   */
  @Nonnull
  private List<AxiomChange> calculateAxiomChangesBetweenCommits(
      @Nonnull List<OWLOntology> currentOntologies, @Nonnull List<OWLOntology> previousOntologies) {

    var allAxiomChanges = Lists.<AxiomChange>newArrayList();

    // Process current ontologies one by one and find their previous versions
    var results =
        currentOntologies.stream()
            .map(current -> processMatchingOntology(current, previousOntologies))
            .toList();

    var processedOntologyIds = Lists.<OWLOntologyID>newArrayList();
    for (var result : results) {
      allAxiomChanges.addAll(result.axiomChanges());
      processedOntologyIds.add(result.ontologyID());
    }

    // Process removed ontologies (exist in previous but not in current)
    var emptyOntology = ontologyLoader.createEmptyOntology();
    var removedOntologyChanges =
        previousOntologies.stream()
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
   * @param currentOntologies ontologies from the initial commit
   * @return list of axiom changes for initial commit
   */
  @Nonnull
  private List<AxiomChange> calculateInitialCommitChanges(
      @Nonnull List<OWLOntology> currentOntologies) {

    var emptyOntology = ontologyLoader.createEmptyOntology();
    return currentOntologies.stream()
        .flatMap(
            ontology ->
                differenceCalculator
                    .calculateAxiomChanges(ontology, emptyOntology, ontology.getOntologyID())
                    .stream())
        .collect(ImmutableList.toImmutableList());
  }

  /**
   * Processes a current ontology by finding its matching previous version and calculating changes.
   * If no match is found, compares it to an empty ontology.
   *
   * @param currentOntology the ontology to process
   * @param previousOntologies list of previous ontologies to match against
   * @return processing result containing axiom changes and ontology ID
   */
  @Nonnull
  private OntologyProcessingResult processMatchingOntology(
      @Nonnull OWLOntology currentOntology, @Nonnull List<OWLOntology> previousOntologies) {

    var emptyOntology = ontologyLoader.createEmptyOntology();

    var ontologyId = currentOntology.getOntologyID();
    var matchingPrevious = findMatchingOntology(currentOntology, previousOntologies);

    var axiomChanges =
        matchingPrevious
            .map(
                previous ->
                    differenceCalculator.calculateAxiomChanges(
                        currentOntology, previous, ontologyId))
            .orElseGet(
                () ->
                    differenceCalculator.calculateAxiomChanges(
                        currentOntology, emptyOntology, ontologyId));

    return new OntologyProcessingResult(axiomChanges, ontologyId);
  }

  /**
   * Finds matching ontology in the previous ontologies list
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
