package edu.stanford.protege.github.cloneservice.utils;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Unit tests for {@link OntologyLoader} */
@DisplayName("OntologyLoader Tests")
class OntologyLoaderTest {

  private OntologyLoader ontologyLoader;

  @TempDir Path tempDir;

  @BeforeEach
  void setUp() {
    ontologyLoader = new OntologyLoader();
  }

  @Test
  @DisplayName("Should create empty ontology successfully")
  void createEmptyOntologySuccessfully() {
    var ontology = ontologyLoader.createEmptyOntology();

    assertNotNull(ontology);
    assertTrue(ontology.getAxioms().isEmpty());
    assertNotNull(ontology.getOntologyID());
  }

  @Test
  @DisplayName("Should return empty list when file does not exist")
  void returnEmptyListWhenFileDoesNotExist() {
    var nonExistentFile = tempDir.resolve("non-existent.owl");

    var result = ontologyLoader.loadOntologyWithImports(nonExistentFile);

    assertNotNull(result);
    assertTrue(result.isEmpty());
  }

  @Test
  @DisplayName("Should throw NullPointerException when file path is null")
  void throwExceptionWhenFilePathNull() {
    var exception =
        assertThrows(
            NullPointerException.class, () -> ontologyLoader.loadOntologyWithImports(null));

    assertEquals("filePath cannot be null", exception.getMessage());
  }

  @Test
  @DisplayName("Should load simple OWL ontology successfully")
  void loadSimpleOwlOntologySuccessfully() throws IOException {
    var owlContent =
        """
            <?xml version="1.0"?>
            <rdf:RDF xmlns="http://example.org/test#"
                     xml:base="http://example.org/test"
                     xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
                     xmlns:owl="http://www.w3.org/2002/07/owl#"
                     xmlns:rdfs="http://www.w3.org/2000/01/rdf-schema#">
                <owl:Ontology rdf:about="http://example.org/test"/>
                <owl:Class rdf:about="http://example.org/test#TestClass"/>
            </rdf:RDF>
            """;

    var owlFile = tempDir.resolve("test.owl");
    Files.writeString(owlFile, owlContent);

    var result = ontologyLoader.loadOntologyWithImports(owlFile);

    assertNotNull(result);
    assertFalse(result.isEmpty());
    assertEquals(1, result.size());

    var ontology = result.get(0);
    assertNotNull(ontology);
    assertFalse(ontology.getAxioms().isEmpty());
  }

  @Test
  @DisplayName("Should handle invalid OWL file gracefully")
  void handleInvalidOwlFileGracefully() throws IOException {
    var invalidContent = "This is not a valid OWL file";
    var invalidFile = tempDir.resolve("invalid.owl");
    Files.writeString(invalidFile, invalidContent);

    var exception =
        assertThrows(
            RuntimeException.class, () -> ontologyLoader.loadOntologyWithImports(invalidFile));

    assertNotNull(exception.getMessage());
    assertTrue(exception.getMessage().contains("Failed to load ontology from"));
  }

  @Test
  @DisplayName("Should load ontology with missing imports silently")
  void loadOntologyWithMissingImportsSilently() throws IOException {
    var owlContentWithMissingImport =
        """
            <?xml version="1.0"?>
            <rdf:RDF xmlns="http://example.org/test#"
                     xml:base="http://example.org/test"
                     xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
                     xmlns:owl="http://www.w3.org/2002/07/owl#">
                <owl:Ontology rdf:about="http://example.org/test">
                    <owl:imports rdf:resource="http://example.org/non-existent-import"/>
                </owl:Ontology>
                <owl:Class rdf:about="http://example.org/test#TestClass"/>
            </rdf:RDF>
            """;

    var owlFile = tempDir.resolve("test-with-missing-import.owl");
    Files.writeString(owlFile, owlContentWithMissingImport);

    var result = ontologyLoader.loadOntologyWithImports(owlFile);

    assertNotNull(result);
    assertFalse(result.isEmpty());
    assertEquals(1, result.size());
  }

  @Test
  @DisplayName("Should load ontology with local import successfully")
  void loadOntologyWithLocalImportSuccessfully() throws IOException {
    var importedOntologyContent =
        """
            <?xml version="1.0"?>
            <rdf:RDF xmlns="http://example.org/imported#"
                     xml:base="http://example.org/imported"
                     xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
                     xmlns:owl="http://www.w3.org/2002/07/owl#">
                <owl:Ontology rdf:about="http://example.org/imported"/>
                <owl:Class rdf:about="http://example.org/imported#ImportedClass"/>
            </rdf:RDF>
            """;

    var mainOntologyContent =
        """
            <?xml version="1.0"?>
            <rdf:RDF xmlns="http://example.org/main#"
                     xml:base="http://example.org/main"
                     xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
                     xmlns:owl="http://www.w3.org/2002/07/owl#">
                <owl:Ontology rdf:about="http://example.org/main">
                    <owl:imports rdf:resource="http://example.org/imported"/>
                </owl:Ontology>
                <owl:Class rdf:about="http://example.org/main#MainClass"/>
            </rdf:RDF>
            """;

    var importedFile = tempDir.resolve("imported.owl");
    var mainFile = tempDir.resolve("main.owl");
    Files.writeString(importedFile, importedOntologyContent);
    Files.writeString(mainFile, mainOntologyContent);

    var result = ontologyLoader.loadOntologyWithImports(mainFile);

    assertNotNull(result);
    assertFalse(result.isEmpty());
    assertEquals(2, result.size());

    var mainOntology = result.get(0);
    assertNotNull(mainOntology);
    assertTrue(mainOntology.getOntologyID().getOntologyIRI().isPresent());
  }

  @Test
  @DisplayName("Should create multiple empty ontologies independently")
  void createMultipleEmptyOntologiesIndependently() {
    var ontology1 = ontologyLoader.createEmptyOntology();
    var ontology2 = ontologyLoader.createEmptyOntology();

    assertNotNull(ontology1);
    assertNotNull(ontology2);
    assertNotSame(ontology1, ontology2);
    assertNotEquals(ontology1.getOntologyID(), ontology2.getOntologyID());
  }
}
