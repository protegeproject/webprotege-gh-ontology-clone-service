package edu.stanford.protege.github.cloneservice.utils;

import com.google.common.collect.ImmutableList;
import edu.stanford.protege.commitnavigator.model.CommitMetadata;
import edu.stanford.protege.commitnavigator.services.CommitNavigator;
import edu.stanford.protege.github.cloneservice.exception.OntologyComparisonException;
import edu.stanford.protege.github.cloneservice.model.OntologyCommitChange;
import org.semanticweb.owlapi.model.OWLOntology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Objects;

/**
 * Main service for analyzing ontology history across Git commits
 */
public class OntologyHistoryAnalyzer {

    private static final Logger logger = LoggerFactory.getLogger(OntologyHistoryAnalyzer.class);

    private final OntologyLoader ontologyLoader;
    private final DifferenceCalculator differenceCalculator;

    public OntologyHistoryAnalyzer(OntologyLoader ontologyLoader,
                                   DifferenceCalculator differenceCalculator) {
        this.ontologyLoader = Objects.requireNonNull(ontologyLoader, "OntologyLoader cannot be null");
        this.differenceCalculator = Objects.requireNonNull( differenceCalculator, "DifferenceCalculator cannot be null" );
    }

    /**
     * Analyzes ontology history across all consecutive commits from HEAD backwards
     *
     * @param ontologyFilePath The name of the ontology file to analyze
     * @param commitNavigator The commit navigator to traverse commit history
     * @return List of all ontology changes across commit history
     *
     * @throws OntologyComparisonException if analysis fails
     */
    @Nonnull
    public ImmutableList<OntologyCommitChange> getCommitHistory(
            @Nonnull String ontologyFilePath,
            @Nonnull CommitNavigator commitNavigator) throws OntologyComparisonException {

        Objects.requireNonNull(ontologyFilePath, "ontologyFilePath cannot be null");
        Objects.requireNonNull(commitNavigator, "commitNavigator cannot be null");

        logger.info("Starting ontology commit history analysis for ontology file: {}", ontologyFilePath);

        var emptyOntology = ontologyLoader.createEmptyOntology();
        var allCommitChanges = new ArrayList<OntologyCommitChange>();
        try {
            var ontologyFile = commitNavigator.resolveFilePath(ontologyFilePath);
            while (true) {
                var commitMetadata = commitNavigator.getCurrentCommit();
                var currentOntology = ontologyLoader.loadOntology(ontologyFile).orElse(emptyOntology);
                if ((commitNavigator.hasPrevious())) {
                    commitNavigator.previousAndCheckout();
                    var previousOntology = ontologyLoader.loadOntology(ontologyFile).orElse(emptyOntology);
                    calculateAxiomChangesBetweenVersions(currentOntology, previousOntology, commitMetadata, allCommitChanges);
                } else {
                    // No previous commit, treat the first commit as a diff between the current one and an empty ontology
                    calculateAxiomChangesBetweenVersions(currentOntology, emptyOntology, commitMetadata, allCommitChanges);
                    break; // No previous commit, stop analysis
                }
            }
            return ImmutableList.copyOf(allCommitChanges);

        } catch (Exception e) {
            throw new OntologyComparisonException("Failed to analyze ontology commit history", e);
        }
    }

    private void calculateAxiomChangesBetweenVersions(OWLOntology currentOntology,
                                                      OWLOntology previousOntology,
                                                      CommitMetadata commitMetadata,
                                                      ArrayList<OntologyCommitChange> allCommitChanges) {
        var axiomChanges = differenceCalculator.calculateAxiomChanges(currentOntology, previousOntology);
        var ontologyCommitChange = new OntologyCommitChange(axiomChanges, commitMetadata);
        allCommitChanges.add(ontologyCommitChange);
    }
}