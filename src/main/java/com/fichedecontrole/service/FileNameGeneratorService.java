package com.fichedecontrole.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service de génération de noms de fichiers
 */
public class FileNameGeneratorService {

    private static final Logger logger = LoggerFactory.getLogger(FileNameGeneratorService.class);
    private static final int MAX_PC_LENGTH = 8;

    /**
     * Génère un nom de fichier à partir des données du formulaire
     * Format: NumFormulaire - CJ - PC1 PC2 PC3
     *
     * @param numFormulaire Le numéro de formulaire
     * @param contratJuridique Le contrat juridique
     * @param listePC La liste des codes PC
     * @return Le nom de fichier généré (sans extension)
     */
    public String generate(String numFormulaire, String contratJuridique, String[] listePC) {
        StringBuilder nom = new StringBuilder();
        nom.append(numFormulaire).append(" - ");
        nom.append(contratJuridique).append(" - ");

        // Ajouter les PC (avec vérification de longueur)
        for (int i = 0; i < listePC.length; i++) {
            String pc = listePC[i].trim();
            if (!pc.isEmpty()) {
                // Limiter à MAX_PC_LENGTH caractères
                if (pc.length() > MAX_PC_LENGTH) {
                    pc = pc.substring(0, MAX_PC_LENGTH);
                }
                if (i > 0) nom.append(" ");
                nom.append(pc);
            }
        }

        // Nettoyer les caractères invalides pour un nom de fichier Windows/Linux
        String cleanedName = nom.toString().replaceAll("[\\\\/:*?\"<>|]", "_");
        logger.debug("Nom de fichier généré : {}", cleanedName);
        return cleanedName;
    }
}
