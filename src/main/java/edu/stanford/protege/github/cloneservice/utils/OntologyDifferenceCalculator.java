package edu.stanford.protege.github.cloneservice.utils;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import edu.stanford.protege.github.cloneservice.model.AxiomChange;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.*;

/**
 * Calculates differences between ontology versions
 */
public class OntologyDifferenceCalculator {

    private static final Logger logger = LoggerFactory.getLogger(OntologyDifferenceCalculator.class);

    /**
     * Calculates differences between current and previous ontology versions
     *
     * @param currentOntology The current version of the ontology
     * @param previousOntology The previous version of the ontology
     *
     * @return OntologyDifference containing all changes for this commit
     */
    @Nonnull
    public List<AxiomChange> calculateAxiomChanges(@Nonnull OWLOntology currentOntology,
                                                   @Nonnull OWLOntology previousOntology,
                                                   @Nonnull OWLOntologyID ontologyId) {

        Objects.requireNonNull(currentOntology, "currentOntology cannot be null");
        Objects.requireNonNull(previousOntology, "previousOntology cannot be null");

        var axiomChanges = Lists.<AxiomChange>newArrayList();

        var currentAxioms = Sets.newHashSet(currentOntology.getAxioms());
        var previousAxioms = Sets.newHashSet(previousOntology.getAxioms());

        // Find added axioms (present in current but not in previous)
        var addedAxioms = findAddedAxioms(currentAxioms, previousAxioms);
        addedAxioms.forEach(axiom -> axiomChanges.add(AxiomChange.addAxiom(axiom, ontologyId)));

        // Find removed axioms (present in previous but not in current)
        var removedAxioms = findRemovedAxioms(currentAxioms, previousAxioms);
        removedAxioms.forEach(axiom -> axiomChanges.add(AxiomChange.removeAxiom(axiom, ontologyId)));

        logger.info("Found {} added axioms and {} removed axioms for ontology {}",
                addedAxioms.size(), removedAxioms.size(), ontologyId);

        return ImmutableList.copyOf(axiomChanges);
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