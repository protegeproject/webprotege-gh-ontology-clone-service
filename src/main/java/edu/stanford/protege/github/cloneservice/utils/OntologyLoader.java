package edu.stanford.protege.github.cloneservice.utils;

import edu.stanford.protege.github.cloneservice.exception.OntologyComparisonException;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.UnloadableImportException;
import org.semanticweb.owlapi.util.AutoIRIMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

/**
 * Handles loading of ontology files using OWL-API
 */
public class OntologyLoader {

    private static final Logger logger = LoggerFactory.getLogger(OntologyLoader.class);

    /**
     * Creates a new empty OWL ontology
     *
     * @return A new empty OWL ontology
     * @throws OntologyComparisonException if ontology creation fails
     */
    @Nonnull
    public OWLOntology createEmptyOntology() throws OntologyComparisonException {
        var ontologyManager = OWLManager.createOWLOntologyManager();
        try {
            return ontologyManager.createOntology();
        } catch (OWLOntologyCreationException e) {
            throw new OntologyComparisonException("Failed to create empty ontology", e);
        }
    }

    /**
     * Loads an ontology from the specified file path
     *
     * @param filePath The path to the ontology file
     * @return Optional containing the loaded ontology, empty if file doesn't exist
     * @throws OntologyComparisonException if loading fails
     */
    @Nonnull
    public Optional<OWLOntology> loadOntology(@Nonnull Path filePath) throws OntologyComparisonException {
        Objects.requireNonNull(filePath, "filePath cannot be null");

        var ontologyFile = filePath.toFile();
        if (!ontologyFile.exists()) {
            logger.warn("Ontology file does not exist: {}", filePath);
            return Optional.empty();
        }

        var ontologyManager = OWLManager.createOWLOntologyManager();
        ontologyManager.getIRIMappers().add(new AutoIRIMapper(filePath.getParent().toFile(), true));
        try {
            logger.info("Loading ontology from: {}", filePath);
            var ontology = ontologyManager.loadOntologyFromOntologyDocument(ontologyFile);
            // look at the ontologyManager to get all ontology imports and remove any annonymous imports.
            return Optional.of(ontology);
        } catch (UnloadableImportException e) {
            logger.warn("Ontology contains unloadable imports: {}", e.getImportsDeclaration());
            return Optional.empty();
        } catch (OWLOntologyCreationException e) {
            throw new OntologyComparisonException("Failed to load ontology from: " + filePath, e);
        }
    }
}