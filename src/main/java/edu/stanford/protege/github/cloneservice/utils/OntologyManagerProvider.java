package edu.stanford.protege.github.cloneservice.utils;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.functional.parser.OWLFunctionalSyntaxOWLParserFactory;
import org.semanticweb.owlapi.manchestersyntax.parser.ManchesterOWLSyntaxOntologyParserFactory;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.oboformat.OBOFormatOWLAPIParserFactory;
import org.semanticweb.owlapi.owlxml.parser.OWLXMLParserFactory;
import org.semanticweb.owlapi.rdf.rdfxml.parser.RDFXMLParserFactory;
import org.semanticweb.owlapi.rdf.turtle.parser.TurtleOntologyParserFactory;
import org.semanticweb.owlapi.rio.RioBinaryRdfParserFactory;
import org.semanticweb.owlapi.rio.RioJsonLDParserFactory;
import org.semanticweb.owlapi.rio.RioNQuadsParserFactory;
import org.semanticweb.owlapi.rio.RioNTriplesParserFactory;
import org.springframework.stereotype.Component;
import uk.ac.manchester.cs.owl.owlapi.OWLDataFactoryImpl;
import uk.ac.manchester.cs.owl.owlapi.OWLOntologyFactoryImpl;
import uk.ac.manchester.cs.owl.owlapi.OWLOntologyManagerImpl;
import uk.ac.manchester.cs.owl.owlapi.concurrent.NoOpReadWriteLock;
import uk.ac.manchester.cs.owl.owlapi.concurrent.NonConcurrentOWLOntologyBuilder;

@Component
public class OntologyManagerProvider {

  public OWLOntologyManager getEmptyOntologyManager() {
    return OWLManager.createOWLOntologyManager();
  }

  public OWLOntologyManager getOntologyManagerWithLoadImports() {
    var man = getCustomOntologyManager();

    // Configure silent handling of missing/anonymous imports
    var config =
        new OWLOntologyLoaderConfiguration()
            .setMissingImportHandlingStrategy(MissingImportHandlingStrategy.SILENT);
    man.setOntologyLoaderConfiguration(config);

    return man;
  }

  public OWLOntologyManager getOntologyManagerWithIgnoredImports() {
    var man = getCustomOntologyManager();

    // Configure silent handling of missing/anonymous imports
    var config =
        new OWLOntologyLoaderConfiguration() {
          @Override
          public boolean isIgnoredImport(IRI iri) {
            return true;
          }
        };
    man.setOntologyLoaderConfiguration(config);

    return man;
  }

  private OWLOntologyManager getCustomOntologyManager() {
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

    return man;
  }
}
