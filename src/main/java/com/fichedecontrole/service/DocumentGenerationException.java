package com.fichedecontrole.service;

import java.util.Collections;
import java.util.Map;

/**
 * Exception levée lors de la génération d'un document
 */
public class DocumentGenerationException extends Exception {

    private final Map<String, String> validationErrors;

    /**
     * Constructeur pour une erreur technique
     * @param message Le message d'erreur
     * @param cause La cause de l'erreur
     */
    public DocumentGenerationException(String message, Throwable cause) {
        super(message, cause);
        this.validationErrors = Collections.emptyMap();
    }

    /**
     * Constructeur pour des erreurs de validation
     * @param message Le message d'erreur
     * @param validationErrors Les erreurs de validation
     */
    public DocumentGenerationException(String message, Map<String, String> validationErrors) {
        super(message);
        this.validationErrors = validationErrors;
    }

    /**
     * Récupère les erreurs de validation
     * @return Les erreurs de validation
     */
    public Map<String, String> getValidationErrors() {
        return validationErrors;
    }

    /**
     * Vérifie si l'exception contient des erreurs de validation
     * @return true si des erreurs de validation sont présentes
     */
    public boolean hasValidationErrors() {
        return !validationErrors.isEmpty();
    }
}
