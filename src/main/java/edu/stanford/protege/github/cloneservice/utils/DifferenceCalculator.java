package edu.stanford.protege.github.cloneservice.utils;

import edu.stanford.protege.github.cloneservice.model.AxiomChange;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.parameters.Imports;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Calculates differences between ontology versions
 */
public class DifferenceCalculator {

    private static final Logger logger = LoggerFactory.getLogger(DifferenceCalculator.class);

    /**
     * Calculates differences between current and previous ontology versions
     *
     * @param currentOntology The current version of the ontology
     * @param previousOntology The previous version of the ontology
     *
     * @return OntologyDifference containing all changes for this commit
     */
    @Nonnull
    public Set<AxiomChange> calculateAxiomChanges(OWLOntology currentOntology, OWLOntology previousOntology) {

        Objects.requireNonNull(currentOntology, "currentOntology cannot be null");
        Objects.requireNonNull(previousOntology, "previousOntology cannot be null");

        var axiomChanges = new HashSet<AxiomChange>();

        var currentAxioms = new HashSet<>(currentOntology.getAxioms(Imports.INCLUDED));
        var previousAxioms = new HashSet<>(previousOntology.getAxioms(Imports.INCLUDED));

        // Find added axioms (present in current but not in previous)
        var addedAxioms = findAddedAxioms(currentAxioms, previousAxioms);
        addedAxioms.forEach(axiom ->
                axiomChanges.add(AxiomChange.addAxiom(axiom))
        );

        // Find removed axioms (present in previous but not in current)
        var removedAxioms = findRemovedAxioms(currentAxioms, previousAxioms);
        removedAxioms.forEach(axiom ->/**/
                axiomChanges.add(AxiomChange.removeAxiom(axiom))
        );

        logger.info("Found {} added axioms and {} removed axioms", addedAxioms.size(), removedAxioms.size());

        return axiomChanges;
    }

    /**
     * Finds axioms that were added (present in current but not in previous)
     */
    private Set<OWLAxiom> findAddedAxioms(Set<OWLAxiom> currentAxioms, Set<OWLAxiom> previousAxioms) {
        var addedAxioms = new HashSet<>(currentAxioms);
        addedAxioms.removeAll(previousAxioms);
        return addedAxioms;
    }

    /**
     * Finds axioms that were removed (present in previous but not in current)
     */
    private Set<OWLAxiom> findRemovedAxioms(Set<OWLAxiom> currentAxioms, Set<OWLAxiom> previousAxioms) {
        var removedAxioms = new HashSet<>(previousAxioms);
        removedAxioms.removeAll(currentAxioms);
        return removedAxioms;
    }
}