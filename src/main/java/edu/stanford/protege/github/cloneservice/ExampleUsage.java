package edu.stanford.protege.github.cloneservice;

import edu.stanford.protege.commitnavigator.GitHubRepositoryBuilder;
import edu.stanford.protege.commitnavigator.exceptions.GitHubNavigatorException;
import edu.stanford.protege.commitnavigator.services.CommitNavigator;
import edu.stanford.protege.github.cloneservice.exception.OntologyComparisonException;
import edu.stanford.protege.github.cloneservice.utils.DifferenceCalculator;
import edu.stanford.protege.github.cloneservice.utils.OntologyHistoryAnalyzer;
import edu.stanford.protege.github.cloneservice.utils.OntologyLoader;

public class ExampleUsage {

    public static void main(String[] args) {
        try {
            var navigator = getCommitNavigator();
            var ontologyLoader = new OntologyLoader();
            var differenceCalculator = new DifferenceCalculator();
            var ontologyHistoryAnalyzer = new OntologyHistoryAnalyzer(ontologyLoader, differenceCalculator);
            var results = ontologyHistoryAnalyzer.getCommitHistory("ro-full.owl", navigator);
            System.out.println("Ontology History Analysis Results:");
            results.forEach((commit) -> {
                var commitMetadata = commit.commitMetadata();
                System.out.println("Commit: " + commitMetadata.commitHash() + " by " + commitMetadata.committerUsername() + " on " + commitMetadata.commitDate());
                System.out.println("Axiom changes:");
                commit.axiomChanges().forEach(
                        (axiomChange) -> System.out.println("- " + axiomChange.operationType() + " " + axiomChange.axiom()));
            });
            System.out.println("\n");
        } catch (GitHubNavigatorException | OntologyComparisonException e) {
            throw new RuntimeException(e);
        }
    }

    private static CommitNavigator getCommitNavigator() throws GitHubNavigatorException {
        var repository = GitHubRepositoryBuilder
                .forRepository("https://github.com/oborel/obo-relations")
                .branch("master")
                .fileFilters("*.owl", "*.rdf", "*.ttl")
                .build();
        repository.initialize();
        return repository.getCommitNavigator();
    }
}
