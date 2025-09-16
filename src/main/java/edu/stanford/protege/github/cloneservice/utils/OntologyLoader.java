package edu.stanford.protege.github.cloneservice.utils;

import com.google.common.collect.ImmutableList;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import org.protege.xmlcatalog.owlapi.XMLCatalogIRIMapper;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.functional.parser.OWLFunctionalSyntaxOWLParserFactory;
import org.semanticweb.owlapi.manchestersyntax.parser.ManchesterOWLSyntaxOntologyParserFactory;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.oboformat.OBOFormatOWLAPIParserFactory;
import org.semanticweb.owlapi.owlxml.parser.OWLXMLParserFactory;
import org.semanticweb.owlapi.rdf.rdfxml.parser.RDFXMLParserFactory;
import org.semanticweb.owlapi.rdf.turtle.parser.TurtleOntologyParserFactory;
import org.semanticweb.owlapi.rio.*;
import org.semanticweb.owlapi.util.AutoIRIMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import uk.ac.manchester.cs.owl.owlapi.OWLDataFactoryImpl;
import uk.ac.manchester.cs.owl.owlapi.OWLOntologyFactoryImpl;
import uk.ac.manchester.cs.owl.owlapi.OWLOntologyManagerImpl;
import uk.ac.manchester.cs.owl.owlapi.concurrent.NoOpReadWriteLock;
import uk.ac.manchester.cs.owl.owlapi.concurrent.NonConcurrentOWLOntologyBuilder;

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
  public List<OWLOntology> loadOntologyWithImports(@Nonnull Path filePath) throws IOException {
    Objects.requireNonNull(filePath, "filePath cannot be null");

    var ontologyFile = filePath.toFile();
    if (!ontologyFile.exists()) {
      var message = "Ontology file does not exist: " + filePath;
      logger.error(message);
      throw new FileNotFoundException(message);
    }

    var ontologyManager = getOntologyManager();

    // Add IRI mapper for local imports in the same directory
    ontologyManager.getIRIMappers().clear();
    var parentDir = filePath.getParent();
    if (parentDir != null) {
      var catalogFile = findCatalogFile(parentDir);
      if (catalogFile.isPresent()) {
        var newMapper = new XMLCatalogIRIMapper(catalogFile.get().toFile());
        ontologyManager.getIRIMappers().add(newMapper);
        logger.debug("Using XMLCatalogIRIMapper with catalog file: {}", catalogFile.get());
      } else {
        var newMapper = new AutoIRIMapper(parentDir.toFile(), true);
        ontologyManager.getIRIMappers().add(newMapper);
        logger.debug("Using AutoIRIMapper for directory: {}", parentDir);
      }
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

  /**
   * Finds a catalog file in the directory (catalog-v001.xml or catalog-*.xml)
   *
   * @param directory the directory to search
   * @return an Optional containing the path to the catalog file if found, empty otherwise
   */
  @Nonnull
  private Optional<Path> findCatalogFile(@Nonnull Path directory) {
    Objects.requireNonNull(directory, "directory cannot be null");

    try (Stream<Path> files = Files.list(directory)) {
      return files
          .filter(
              file -> {
                var fileName = file.getFileName().toString();
                return fileName.equals("catalog-v001.xml")
                    || (fileName.startsWith("catalog-") && fileName.endsWith(".xml"));
              })
          .findFirst();
    } catch (IOException e) {
      logger.warn("Failed to list files in directory: {}", directory, e);
      return Optional.empty();
    }
  }

  private OWLOntologyManager getOntologyManager() {
    var man =
        new OWLOntologyManagerImpl(new OWLDataFactoryImpl(), new NoOpReadWriteLock()) {
          @Override
          public void makeLoadImportRequest(
              OWLImportsDeclaration declaration, OWLOntologyLoaderConfiguration configuration) {
            var config = getOntologyLoaderConfiguration();
            super.makeLoadImportRequest(declaration, config);
          }
        };
    man.getOntologyFactories()
        .add(new OWLOntologyFactoryImpl(new NonConcurrentOWLOntologyBuilder()));

    // Add parsers that we care about
    var ontologyParsers = man.getOntologyParsers();
    ontologyParsers.add(new RioBinaryRdfParserFactory());
    ontologyParsers.add(new RioNQuadsParserFactory());
    ontologyParsers.add(new RioJsonLDParserFactory());
    ontologyParsers.add(new RioNTriplesParserFactory());
    ontologyParsers.add(new OBOFormatOWLAPIParserFactory());
    ontologyParsers.add(new OWLFunctionalSyntaxOWLParserFactory());
    ontologyParsers.add(new ManchesterOWLSyntaxOntologyParserFactory());
    ontologyParsers.add(new TurtleOntologyParserFactory());
    ontologyParsers.add(new OWLXMLParserFactory());
    ontologyParsers.add(new RDFXMLParserFactory());

    // Configure silent handling of missing/anonymous imports
    var config =
        new OWLOntologyLoaderConfiguration()
            .setMissingImportHandlingStrategy(MissingImportHandlingStrategy.SILENT);
    man.setOntologyLoaderConfiguration(config);

    return man;
  }
}
