package edu.stanford.protege.github.cloneservice.utils;

import com.google.common.collect.ImmutableList;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nonnull;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.MissingImportHandlingStrategy;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyLoaderConfiguration;
import org.semanticweb.owlapi.util.AutoIRIMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Handles loading of ontology files using OWL-API */
@Component
public class OntologyLoader {

  private static final Logger logger = LoggerFactory.getLogger(OntologyLoader.class);

  /**
   * Creates a new empty OWL ontology
   *
   * @return A new empty OWL ontology
   */
  @Nonnull
  public OWLOntology createEmptyOntology() {
    var ontologyManager = OWLManager.createOWLOntologyManager();
    try {
      return ontologyManager.createOntology();
    } catch (OWLOntologyCreationException e) {
      throw new RuntimeException("Failed to create empty ontology", e);
    }
  }

  /**
   * Loads an OWL ontology from the specified file path along with all its imported ontologies.
   *
   * <p>The returned list contains the main ontology as the first element, followed by all imported
   * ontologies. If the ontology has no imports, the list will contain only the main ontology.
   *
   * <p><strong>Import Resolution:</strong>
   *
   * <ul>
   *   <li>Missing or anonymous imports are handled silently (no exceptions thrown)
   *   <li>Local imports in the same directory as the main ontology are automatically mapped
   *   <li>All successfully resolved imports are included in the result
   * </ul>
   *
   * @param filePath the path to the ontology file to load.
   * @return a list containing the main ontology and all its imported ontologies. The main ontology
   *     is always the first element. Returns an empty list if the file does not exist.
   * @throws NullPointerException if {@code filePath} is {@code null}
   */
  @Nonnull
  public List<OWLOntology> loadOntologyWithImports(@Nonnull Path filePath) {
    Objects.requireNonNull(filePath, "filePath cannot be null");

    var ontologyFile = filePath.toFile();
    if (!ontologyFile.exists()) {
      logger.warn("Ontology file does not exist: {}", filePath);
      return ImmutableList.of();
    }

    var ontologyManager = OWLManager.createOWLOntologyManager();

    // Configure silent handling of missing/anonymous imports
    var config =
        new OWLOntologyLoaderConfiguration()
            .setMissingImportHandlingStrategy(MissingImportHandlingStrategy.SILENT);
    ontologyManager.setOntologyLoaderConfiguration(config);

    // Add IRI mapper for local imports in the same directory
    var parentDir = filePath.getParent();
    if (parentDir != null) {
      ontologyManager.getIRIMappers().add(new AutoIRIMapper(parentDir.toFile(), true));
    }
    try {
      logger.info("Loading ontology from: {}", filePath);
      var ontology = ontologyManager.loadOntologyFromOntologyDocument(ontologyFile);

      // Log information about imports
      var importedOntologies = ontologyManager.getImports(ontology);
      logger.info("Successfully loaded ontology with {} imports", importedOntologies.size());

      // Get all ontologies including imports
      return ImmutableList.<OWLOntology>builder().add(ontology).addAll(importedOntologies).build();
    } catch (OWLOntologyCreationException e) {
      throw new RuntimeException("Failed to load ontology from: " + filePath, e);
    }
  }
}
