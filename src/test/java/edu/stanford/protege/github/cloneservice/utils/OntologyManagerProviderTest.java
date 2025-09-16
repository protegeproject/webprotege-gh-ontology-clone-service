package edu.stanford.protege.github.cloneservice.utils;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.semanticweb.owlapi.functional.parser.OWLFunctionalSyntaxOWLParserFactory;
import org.semanticweb.owlapi.manchestersyntax.parser.ManchesterOWLSyntaxOntologyParserFactory;
import org.semanticweb.owlapi.model.MissingImportHandlingStrategy;
import org.semanticweb.owlapi.oboformat.OBOFormatOWLAPIParserFactory;
import org.semanticweb.owlapi.owlxml.parser.OWLXMLParserFactory;
import org.semanticweb.owlapi.rdf.rdfxml.parser.RDFXMLParserFactory;
import org.semanticweb.owlapi.rdf.turtle.parser.TurtleOntologyParserFactory;
import org.semanticweb.owlapi.rio.RioBinaryRdfParserFactory;
import org.semanticweb.owlapi.rio.RioJsonLDParserFactory;
import org.semanticweb.owlapi.rio.RioNQuadsParserFactory;
import org.semanticweb.owlapi.rio.RioNTriplesParserFactory;
import uk.ac.manchester.cs.owl.owlapi.OWLOntologyFactoryImpl;

/** Unit tests for {@link OntologyManagerProvider} */
@DisplayName("OntologyManagerProvider Tests")
class OntologyManagerProviderTest {

  private OntologyManagerProvider provider;

  @BeforeEach
  void setUp() {
    provider = new OntologyManagerProvider();
  }

  @Test
  @DisplayName("Should create empty ontology manager successfully")
  void createEmptyOntologyManagerSuccessfully() {
    var manager = provider.getEmptyOntologyManager();

    assertNotNull(manager, "Empty ontology manager should not be null");
    assertNotNull(manager.getOWLDataFactory(), "Data factory should not be null");
    assertTrue(manager.getOntologies().isEmpty(), "Empty manager should have no ontologies");
  }

  @Test
  @DisplayName("Should create different instances of empty ontology manager")
  void createDifferentInstancesOfEmptyOntologyManager() {
    var manager1 = provider.getEmptyOntologyManager();
    var manager2 = provider.getEmptyOntologyManager();

    assertNotNull(manager1, "First manager should not be null");
    assertNotNull(manager2, "Second manager should not be null");
    assertNotSame(manager1, manager2, "Should create different instances");
  }

  @Test
  @DisplayName("Should create configured ontology manager successfully")
  void createConfiguredOntologyManagerSuccessfully() {
    var manager = provider.getOntologyManagerWithLoadImports();

    assertNotNull(manager, "Configured ontology manager should not be null");
    assertNotNull(manager.getOWLDataFactory(), "Data factory should not be null");
    assertTrue(manager.getOntologies().isEmpty(), "New manager should have no ontologies");
  }

  @Test
  @DisplayName("Should create different instances of configured ontology manager")
  void createDifferentInstancesOfConfiguredOntologyManager() {
    var manager1 = provider.getOntologyManagerWithLoadImports();
    var manager2 = provider.getOntologyManagerWithLoadImports();

    assertNotNull(manager1, "First manager should not be null");
    assertNotNull(manager2, "Second manager should not be null");
    assertNotSame(manager1, manager2, "Should create different instances");
  }

  @Test
  @DisplayName("Should configure ontology manager with expected parsers")
  void configureOntologyManagerWithExpectedParsers() {
    var manager = provider.getOntologyManagerWithLoadImports();
    var parsers = manager.getOntologyParsers();

    assertNotNull(parsers, "Parsers collection should not be null");
    assertFalse(parsers.isEmpty(), "Should have parsers configured");

    // Verify specific parsers are present by iterating through the collection
    var hasRioBinaryRdf = false;
    var hasRioNQuads = false;
    var hasRioJsonLD = false;
    var hasRioNTriples = false;
    var hasOBOFormat = false;
    var hasOWLFunctional = false;
    var hasManchester = false;
    var hasTurtle = false;
    var hasOWLXML = false;
    var hasRDFXML = false;

    for (var parser : parsers) {
      if (parser instanceof RioBinaryRdfParserFactory) hasRioBinaryRdf = true;
      if (parser instanceof RioNQuadsParserFactory) hasRioNQuads = true;
      if (parser instanceof RioJsonLDParserFactory) hasRioJsonLD = true;
      if (parser instanceof RioNTriplesParserFactory) hasRioNTriples = true;
      if (parser instanceof OBOFormatOWLAPIParserFactory) hasOBOFormat = true;
      if (parser instanceof OWLFunctionalSyntaxOWLParserFactory) hasOWLFunctional = true;
      if (parser instanceof ManchesterOWLSyntaxOntologyParserFactory) hasManchester = true;
      if (parser instanceof TurtleOntologyParserFactory) hasTurtle = true;
      if (parser instanceof OWLXMLParserFactory) hasOWLXML = true;
      if (parser instanceof RDFXMLParserFactory) hasRDFXML = true;
    }

    assertTrue(hasRioBinaryRdf, "Should contain RioBinaryRdfParserFactory");
    assertTrue(hasRioNQuads, "Should contain RioNQuadsParserFactory");
    assertTrue(hasRioJsonLD, "Should contain RioJsonLDParserFactory");
    assertTrue(hasRioNTriples, "Should contain RioNTriplesParserFactory");
    assertTrue(hasOBOFormat, "Should contain OBOFormatOWLAPIParserFactory");
    assertTrue(hasOWLFunctional, "Should contain OWLFunctionalSyntaxOWLParserFactory");
    assertTrue(hasManchester, "Should contain ManchesterOWLSyntaxOntologyParserFactory");
    assertTrue(hasTurtle, "Should contain TurtleOntologyParserFactory");
    assertTrue(hasOWLXML, "Should contain OWLXMLParserFactory");
    assertTrue(hasRDFXML, "Should contain RDFXMLParserFactory");
  }

  @Test
  @DisplayName("Should configure ontology manager with custom ontology factory")
  void configureOntologyManagerWithCustomOntologyFactory() {
    var manager = provider.getOntologyManagerWithLoadImports();
    var factories = manager.getOntologyFactories();

    assertNotNull(factories, "Factories collection should not be null");
    assertFalse(factories.isEmpty(), "Should have factories configured");

    // Verify that OWLOntologyFactoryImpl is present by iterating through the collection
    var hasOWLOntologyFactory = false;
    for (var factory : factories) {
      if (factory instanceof OWLOntologyFactoryImpl) {
        hasOWLOntologyFactory = true;
        break;
      }
    }

    assertTrue(hasOWLOntologyFactory, "Should contain OWLOntologyFactoryImpl");
  }

  @Test
  @DisplayName("Should configure ontology manager with silent import handling")
  void configureOntologyManagerWithSilentImportHandling() {
    var manager = provider.getOntologyManagerWithLoadImports();
    var config = manager.getOntologyLoaderConfiguration();

    assertNotNull(config, "Loader configuration should not be null");
    assertEquals(
        MissingImportHandlingStrategy.SILENT,
        config.getMissingImportHandlingStrategy(),
        "Should use SILENT missing import handling strategy");
  }

  @Test
  @DisplayName("Should allow creation of ontologies with empty manager")
  void allowCreationOfOntologiesWithEmptyManager() throws Exception {
    var manager = provider.getEmptyOntologyManager();

    var ontology = manager.createOntology();

    assertNotNull(ontology, "Created ontology should not be null");
    assertEquals(1, manager.getOntologies().size(), "Manager should contain one ontology");
    assertTrue(manager.contains(ontology), "Manager should contain the created ontology");
  }

  @Test
  @DisplayName("Should allow creation of ontologies with configured manager")
  void allowCreationOfOntologiesWithConfiguredManager() throws Exception {
    var manager = provider.getOntologyManagerWithLoadImports();

    var ontology = manager.createOntology();

    assertNotNull(ontology, "Created ontology should not be null");
    assertEquals(1, manager.getOntologies().size(), "Manager should contain one ontology");
    assertTrue(manager.contains(ontology), "Manager should contain the created ontology");
  }

  @Test
  @DisplayName("Should maintain independent state between manager instances")
  void maintainIndependentStateBetweenManagerInstances() throws Exception {
    var manager1 = provider.getOntologyManagerWithLoadImports();
    var manager2 = provider.getOntologyManagerWithLoadImports();

    // Create ontology in first manager
    var ontology1 = manager1.createOntology();

    assertEquals(1, manager1.getOntologies().size(), "First manager should have one ontology");
    assertEquals(0, manager2.getOntologies().size(), "Second manager should have no ontologies");
    assertTrue(manager1.contains(ontology1), "First manager should contain its ontology");
    assertFalse(manager2.contains(ontology1), "Second manager should not contain first ontology");
  }

  @Test
  @DisplayName("Should have consistent parser count across manager instances")
  void haveConsistentParserCountAcrossManagerInstances() {
    var manager1 = provider.getOntologyManagerWithLoadImports();
    var manager2 = provider.getOntologyManagerWithLoadImports();

    var parsers1 = manager1.getOntologyParsers();
    var parsers2 = manager2.getOntologyParsers();

    assertEquals(
        parsers1.size(), parsers2.size(), "Both managers should have the same number of parsers");

    // Verify parsers are configured but instances are independent
    assertNotSame(parsers1, parsers2, "Parser collections should be different instances");
  }

  @Test
  @DisplayName("Should have consistent factory count across manager instances")
  void haveConsistentFactoryCountAcrossManagerInstances() {
    var manager1 = provider.getOntologyManagerWithLoadImports();
    var manager2 = provider.getOntologyManagerWithLoadImports();

    var factories1 = manager1.getOntologyFactories();
    var factories2 = manager2.getOntologyFactories();

    assertEquals(
        factories1.size(),
        factories2.size(),
        "Both managers should have the same number of factories");

    // Verify factories are configured but instances are independent
    assertNotSame(factories1, factories2, "Factory collections should be different instances");
  }

  @Test
  @DisplayName("Should have consistent configuration across manager instances")
  void haveConsistentConfigurationAcrossManagerInstances() {
    var manager1 = provider.getOntologyManagerWithLoadImports();
    var manager2 = provider.getOntologyManagerWithLoadImports();

    var config1 = manager1.getOntologyLoaderConfiguration();
    var config2 = manager2.getOntologyLoaderConfiguration();

    assertEquals(
        config1.getMissingImportHandlingStrategy(),
        config2.getMissingImportHandlingStrategy(),
        "Both managers should have the same import handling strategy");
  }
}
