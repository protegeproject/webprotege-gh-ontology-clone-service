package edu.stanford.protege.github.cloneservice;

import edu.stanford.protege.commitnavigator.GitHubRepository;
import edu.stanford.protege.commitnavigator.GitHubRepositoryBuilder;
import edu.stanford.protege.commitnavigator.exceptions.GitHubNavigatorException;
import edu.stanford.protege.github.cloneservice.exception.OntologyComparisonException;
import edu.stanford.protege.github.cloneservice.utils.OntologyDifferenceCalculator;
import edu.stanford.protege.github.cloneservice.utils.OntologyHistoryAnalyzer;
import edu.stanford.protege.github.cloneservice.utils.OntologyLoader;

public class ExampleUsage {

    public static void main(String[] args) {
        try {
            var repository = getGitHubRepository("https://github.com/protegeteam/grocery-ontology");
            var ontologyLoader = new OntologyLoader();
            var ontologyDifferenceCalculator = new OntologyDifferenceCalculator();
            var ontologyHistoryAnalyzer = new OntologyHistoryAnalyzer(ontologyLoader, ontologyDifferenceCalculator);
            var results = ontologyHistoryAnalyzer.getCommitHistory("grocery.owl", repository);
            System.out.println("Ontology History Analysis Results:");
            results.forEach((commit) -> {
                var commitMetadata = commit.commitMetadata();
                System.out.println("Commit: " + commitMetadata.commitHash() + " by " + commitMetadata.committerUsername() + " on " + commitMetadata.commitDate());
                System.out.println("Message: " + commitMetadata.commitMessage().trim());
                System.out.println("Axiom changes:");
                commit.axiomChanges().forEach(
                        (axiomChange) -> System.out.println("- " + axiomChange.operationType() + " " + axiomChange.axiom()));
                System.out.println();
            });
        } catch (GitHubNavigatorException | OntologyComparisonException e) {
            throw new RuntimeException(e);
        }
    }

    private static GitHubRepository getGitHubRepository(String repositoryUrl) throws GitHubNavigatorException {
        var repository = GitHubRepositoryBuilder
                .forRepository(repositoryUrl)
                .branch("master")
                .fileFilters("*.owl", "*.rdf", "*.ttl")
                .build();
        repository.initialize();
        return repository;
    }
}
