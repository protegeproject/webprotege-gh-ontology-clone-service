package edu.stanford.protege.github.cloneservice.exception;

/** Exception thrown during ontology comparison operations */
public class OntologyComparisonException extends Exception {

    public OntologyComparisonException(String message) {
        super(message);
    }

    public OntologyComparisonException(String message, Throwable cause) {
        super(message, cause);
    }
}
