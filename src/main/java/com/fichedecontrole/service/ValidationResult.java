package com.fichedecontrole.service;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Résultat de validation d'un formulaire
 */
public class ValidationResult {

    private final Map<String, String> errors = new HashMap<>();

    /**
     * Ajoute une erreur de validation
     * @param field Le champ en erreur
     * @param message Le message d'erreur
     */
    public void addError(String field, String message) {
        errors.put(field, message);
    }

    /**
     * Vérifie si la validation est réussie (aucune erreur)
     * @return true si aucune erreur
     */
    public boolean isValid() {
        return errors.isEmpty();
    }

    /**
     * Récupère toutes les erreurs
     * @return Map immuable des erreurs
     */
    public Map<String, String> getErrors() {
        return Collections.unmodifiableMap(errors);
    }

    /**
     * Récupère la première erreur rencontrée
     * @return Le message de la première erreur ou une chaîne vide
     */
    public String getFirstError() {
        return errors.values().stream().findFirst().orElse("");
    }

    @Override
    public String toString() {
        return "ValidationResult{errors=" + errors + "}";
    }
}
