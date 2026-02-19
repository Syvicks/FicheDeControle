package com.fichedecontrole.service;

import com.fichedecontrole.model.FicheDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Service de validation des données du formulaire
 */
public class ValidationService {

    private static final Logger logger = LoggerFactory.getLogger(ValidationService.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /**
     * Valide toutes les données d'une fiche
     * @param fiche La fiche à valider
     * @return Le résultat de validation
     */
    public ValidationResult validate(FicheDto fiche) {
        ValidationResult result = new ValidationResult();

        if (fiche == null) {
            result.addError("fiche", "Les données du formulaire sont nulles");
            return result;
        }

        // Contrat Juridique obligatoire
        if (isNullOrEmpty(fiche.getContratJuridique())) {
            result.addError("contratJuridique", "Le Contrat Juridique (CJ) est obligatoire");
        }

        // Numéro formulaire obligatoire
        if (isNullOrEmpty(fiche.getNumFormulaire())) {
            result.addError("numFormulaire", "Le Numéro Formulaire/Demande est obligatoire");
        }

        // Liste PC obligatoire et non vide
        if (fiche.getListePC() == null || fiche.getListePC().length == 0
            || isNullOrEmpty(fiche.getListePC()[0])) {
            result.addError("listePC", "Au moins un code PC est obligatoire");
        }

        // Validation du format de date
        if (!isNullOrEmpty(fiche.getDateEffet())) {
            try {
                LocalDate.parse(fiche.getDateEffet(), DATE_FORMATTER);
            } catch (Exception e) {
                result.addError("dateEffet", "Format de date incorrect. Utilisez jj/mm/aaaa");
            }
        } else {
            result.addError("dateEffet", "La date d'effet est obligatoire");
        }

        // Validation des longueurs de PC
        if (fiche.getListePC() != null) {
            for (int i = 0; i < fiche.getListePC().length; i++) {
                String pc = fiche.getListePC()[i];
                if (pc != null && !pc.trim().isEmpty() && pc.trim().length() < 5) {
                    result.addError("listePC[" + i + "]",
                        "Le code PC doit contenir au moins 5 caractères");
                }
            }
        }

        logger.debug("Validation {} : {}", result.isValid() ? "réussie" : "échouée", result);
        return result;
    }

    /**
     * Vérifie si une chaîne est nulle ou vide
     */
    private boolean isNullOrEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }
}
