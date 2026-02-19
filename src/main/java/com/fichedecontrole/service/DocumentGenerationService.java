package com.fichedecontrole.service;

import com.fichedecontrole.model.FicheDto;
import com.fichedecontrole.generator.WordGeneratorXML;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * Service principal de génération de documents
 * Coordonne la validation et la génération
 */
public class DocumentGenerationService {

    private static final Logger logger = LoggerFactory.getLogger(DocumentGenerationService.class);
    private final WordGeneratorXML wordGenerator;
    private final ValidationService validationService;
    private final FileNameGeneratorService fileNameGenerator;

    public DocumentGenerationService() {
        this.wordGenerator = new WordGeneratorXML();
        this.validationService = new ValidationService();
        this.fileNameGenerator = new FileNameGeneratorService();
    }

    /**
     * Génère un document Word à partir des données du formulaire
     *
     * @param fiche La fiche de contrôle à générer
     * @param outputFile Le fichier de sortie
     * @throws DocumentGenerationException En cas d'erreur de validation ou de génération
     */
    public void generateDocument(FicheDto fiche, File outputFile) throws DocumentGenerationException {
        logger.info("Debut de generation du document pour : {}", fiche.getNumFormulaire());

        // Valider les données
        ValidationResult validation = validationService.validate(fiche);
        if (!validation.isValid()) {
            logger.warn("Validation echouee : {}", validation.getErrors());
            throw new DocumentGenerationException("Validation echouee", validation.getErrors());
        }

        // Générer le document
        try {
            wordGenerator.genererFicheDeControle(fiche, outputFile);
            logger.info("Document genere avec succes : {}", outputFile.getName());
        } catch (Exception e) {
            logger.error("Erreur lors de la generation du document", e);
            throw new DocumentGenerationException("Erreur de generation : " + e.getMessage(), e);
        }
    }

    /**
     * Génère le nom de fichier suggéré
     *
     * @param numFormulaire Le numéro de formulaire
     * @param contratJuridique Le contrat juridique
     * @param listePC La liste des codes PC
     * @return Le nom de fichier (sans extension)
     */
    public String generateFileName(String numFormulaire, String contratJuridique, String[] listePC) {
        return fileNameGenerator.generate(numFormulaire, contratJuridique, listePC);
    }
}
