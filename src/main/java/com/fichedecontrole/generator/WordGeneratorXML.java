package com.fichedecontrole.generator;

import com.fichedecontrole.config.ConfigManager;
import com.fichedecontrole.model.CaptureCategory;
import com.fichedecontrole.model.FicheDto;
import com.fichedecontrole.model.NatureDemande;
import com.fichedecontrole.model.TypeDemande;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Générateur de documents Word basé sur un modèle .docx
 * Version utilisant la manipulation XML directe
 */
public class WordGeneratorXML {

    // Clés pour les commentaires
    private static final String CONFIG_COMMENT_NO_PARAMS = "commentaire.sans.parametrage";

    // Clés pour les opérations (contrat/avenant)
    private static final String CONFIG_OPERATION_CONTRAT = "commentaire.operation.contrat";
    private static final String CONFIG_OPERATION_AVENANT = "commentaire.operation.avenant";

    // Clés pour les équipes prestation
    private static final String CONFIG_PRESTATION_SANTE = "commentaire.prestation.sante";
    private static final String CONFIG_PRESTATION_PREV = "commentaire.prestation.prev";

    private static final Logger logger = LoggerFactory.getLogger(WordGeneratorXML.class);
    private static final String TEMPLATE_PATH = "/templates/modele.docx";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    
    /**
     * Génère une fiche de contrôle Word
     */
    public void genererFicheDeControle(FicheDto fiche, File outputFile) throws Exception {
        logger.info("Generation du document pour le formulaire : {}", fiche.getNumFormulaire());

        // Charger le modèle (externe ou depuis resources)
        InputStream templateStream = getTemplateStream();
        if (templateStream == null) {
            logger.error("Le modele Word n'a pas ete trouve");
            throw new FileNotFoundException("Le modele Word n'a pas ete trouve");
        }

        // Date du jour
        String dateJour = LocalDate.now().format(DATE_FORMATTER);

        // Préparer les images si des captures sont présentes
        WordImageManager imageManager = new WordImageManager();
        if (fiche.getCaptures() != null && !fiche.getCaptures().isEmpty()) {
            imageManager.prepareImages(fiche.getCaptures());
        }

        // Traiter le document en manipulant le ZIP directement
        try (ZipInputStream zis = new ZipInputStream(templateStream);
             ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(outputFile))) {

            ZipEntry entry;
            byte[] buffer = new byte[8192];

            while ((entry = zis.getNextEntry()) != null) {

                // Créer une nouvelle entrée (ne pas réutiliser l'ancienne)
                ZipEntry newEntry = new ZipEntry(entry.getName());
                zos.putNextEntry(newEntry);

                if (entry.getName().equals("word/document.xml")) {
                    // === Traitement du document.xml ===
                    logger.debug("Traitement du fichier word/document.xml");
                    String xml = readEntryAsString(zis, buffer);

                    // Remplacer les valeurs depuis le DTO
                    xml = remplacerBalise(xml, "NUM_FORMULAIRE", fiche.getNumFormulaire());
                    xml = remplacerBalise(xml, "CONTRAT_JURIDIQUE", fiche.getContratJuridique());
                    // NATURE_DEMANDE = displayName + éléments cochés
                    // Création  → séparateur "+"   (ex : "Création PG+PC+RG")
                    // Modification → séparateur " / " (ex : "Modification Cotis / Tx Chgt / CJ")
                    String natureDemande = fiche.getNatureDemande().getDisplayName();
                    List<String> elements = fiche.getElements();
                    if (elements != null && !elements.isEmpty()) {
                        String sep = (fiche.getNatureDemande() == NatureDemande.CREATION) ? "+" : " / ";
                        natureDemande += " " + String.join(sep, elements);
                    }
                    xml = remplacerBalise(xml, "NATURE_DEMANDE", natureDemande);
                    xml = remplacerBalise(xml, "DATE_DU_JOUR", dateJour);
                    xml = remplacerBalise(xml, "DATE_EFFET", fiche.getDateEffet());
                    xml = remplacerBalise(xml, "DISPOSITIF", fiche.getDispositif());
                    xml = remplacerBalise(xml, "RAISON_SOCIAL", fiche.getRaisonSocial());
                    xml = remplacerBalise(xml, "PARAMETREUR", fiche.getParametreur());
                    xml = remplacerBalise(xml, "PRODUIT_GESTION", fiche.getProduitGestion());
                    xml = remplacerBalise(xml, "FORMULE", fiche.getFormule());
                    xml = remplacerBalise(xml, "TAUX_CHARGEMENT", fiche.getTauxChargement());
                    xml = remplacerBalise(xml, "STRUCTURE2", fiche.getStructure2());
                    xml = remplacerBalise(xml, "STRUCTURE", fiche.getStructure());
                    xml = remplacerBalise(xml, "TYPE_DEMANDE", fiche.getTypeDemande().getDisplayName());
                    xml = remplacerBalise(xml, "RISQUE", fiche.getRisque().getDisplayName());
                    xml = remplacerBalise(xml, "ACTION", fiche.getNatureDemande().getLibelle());
                    
                    NatureDemande nature = fiche.getNatureDemande();
                    boolean isEligible = nature == NatureDemande.CREATION 
                                    || nature == NatureDemande.MODIFICATION;

                    String operation = isEligible
                            ? getConfigValueSafely(getOperationConfigKey(nature))
                            : "";

                    String prestation = (nature == NatureDemande.CREATION 
                                && fiche.getTypeDemande() == TypeDemande.E_CONTRACTU)
                            ? getConfigValueSafely(
                                getPrestationConfigKey(fiche.getRisque().getDisplayName())
                            )
                            : "";

                    xml = remplacerBalise(xml, "OPERATION", operation);
                    xml = remplacerBalise(xml, "EQUIPE_PRESTATION", prestation);

                    
                    // TODO : Faire Fermeture RG
                    xml = remplacerBalise(xml, "FERMETURE_RG", "");

                    String aucunParametrage = (nature == NatureDemande.AUCUN)
                        ? ConfigManager.getValue(CONFIG_COMMENT_NO_PARAMS)
                        : "";
                    xml = remplacerBalise(xml, "AUCUN_PARAMETRAGE", aucunParametrage);
                    
                    // Traiter les PC
                    String[] listePC = fiche.getListePC();
                    String pc1 = listePC.length > 0 && !listePC[0].trim().isEmpty() ? listePC[0].trim() : "";
                    String pc2 = listePC.length > 1 && !listePC[1].trim().isEmpty() ? listePC[1].trim() : "";
                    String pc3 = listePC.length > 2 && !listePC[2].trim().isEmpty() ? listePC[2].trim() : "";
                    xml = remplacerBalise(xml, "PC1", pc1);
                    xml = remplacerBalise(xml, "PC2", pc2);
                    xml = remplacerBalise(xml, "PC3", pc3);
                    
                    // Insérer les captures d'écran ou supprimer les tags {{CAPTURES_XXX}}
                    xml = imageManager.replaceCaptureTags(xml, fiche.getListePC());

                    // Nettoyer les tags de capture fragmentés par Word
                    // (replaceCaptureTags gère les tags non-fragmentés,
                    //  remplacerBalise gère ceux éclatés entre plusieurs <w:t>)
                    for (CaptureCategory category : CaptureCategory.values()) {
                        xml = remplacerBalise(xml, category.getWordTag(), "");
                    }

                    // Écrire le XML modifié
                    zos.write(xml.getBytes(StandardCharsets.UTF_8));

                } else if (entry.getName().equals("word/_rels/document.xml.rels") && imageManager.hasImages()) {
                    // === Ajouter les relations d'images ===
                    logger.debug("Ajout des relations d'images dans document.xml.rels");
                    String relsXml = readEntryAsString(zis, buffer);
                    relsXml = imageManager.addImageRelationships(relsXml);
                    zos.write(relsXml.getBytes(StandardCharsets.UTF_8));

                } else if (entry.getName().equals("[Content_Types].xml") && imageManager.hasImages()) {
                    // === Ajouter le type de contenu PNG ===
                    logger.debug("Ajout du type de contenu PNG dans [Content_Types].xml");
                    String contentTypesXml = readEntryAsString(zis, buffer);
                    contentTypesXml = imageManager.addPngContentType(contentTypesXml);
                    zos.write(contentTypesXml.getBytes(StandardCharsets.UTF_8));

                } else {
                    // Copier tel quel les autres fichiers
                    int len;
                    while ((len = zis.read(buffer)) > 0) {
                        zos.write(buffer, 0, len);
                    }
                }

                zos.closeEntry();
            }

            // Ajouter les fichiers images dans word/media/
            if (imageManager.hasImages()) {
                for (WordImageManager.ImageEntry imgEntry : imageManager.getImageEntries()) {
                    ZipEntry imgZipEntry = new ZipEntry("word/media/" + imgEntry.fileName);
                    zos.putNextEntry(imgZipEntry);
                    zos.write(imgEntry.pngData);
                    zos.closeEntry();
                    logger.debug("Image ajoutee au ZIP : word/media/{}", imgEntry.fileName);
                }
            }

            zos.finish();
        }

        templateStream.close();
        logger.info("Document genere avec succes : {}", outputFile.getAbsolutePath());
    }

    /**
     * Retourne la clé de configuration pour le type d'opération (contrat/avenant)
     *
     * @param natureDemande la nature de demande
     * @return la clé de config ou null
     */
    private String getOperationConfigKey(NatureDemande natureDemande) {
        if (natureDemande == null) {
            return null;
        }
        if (natureDemande == NatureDemande.CREATION) {
            return CONFIG_OPERATION_CONTRAT;
        }
        if (natureDemande == NatureDemande.MODIFICATION) {
            return CONFIG_OPERATION_AVENANT;
        }
        return null;
    }

    /**
     * Retourne la clé de configuration pour l'équipe prestation selon le risque
     *
     * @param risque le risque (FSS ou Prev)
     * @return la clé de config ou null
     */
    private String getPrestationConfigKey(String risque) {
        if (risque == null) {
            return null;
        }

        if ("FSS".equalsIgnoreCase(risque)) {
            return CONFIG_PRESTATION_SANTE;
        }

        if ("Prev".equalsIgnoreCase(risque)) {
            return CONFIG_PRESTATION_PREV;
        }

        return null;
    }

    /**
     * Récupère une valeur de configuration de manière sécurisée
     * 
     * @param configKey la clé de configuration
     * @return la valeur ou null si absent/vide
     */
    private String getConfigValueSafely(String configKey) {
        try {
            String value = ConfigManager.getValue(configKey);
            
            // Vérifier que la valeur n'est pas vide
            if (value != null && !value.trim().isEmpty()) {
                return value;
            }
            
            return null;
        } catch (Exception e) {
            logger.error("Erreur lors de la lecture de la configuration '{}'", configKey, e);
            return null;
        }
    }

    /**
     * Lit le contenu d'une entrée ZIP en tant que String UTF-8
     */
    private String readEntryAsString(ZipInputStream zis, byte[] buffer) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int len;
        while ((len = zis.read(buffer)) > 0) {
            baos.write(buffer, 0, len);
        }
        return baos.toString(StandardCharsets.UTF_8.name());
    }

    /**
     * Charge le template depuis le filesystem externe ou depuis le classpath
     * @return InputStream du template
     * @throws FileNotFoundException si le template n'est pas trouvé
     */
    private InputStream getTemplateStream() throws FileNotFoundException {
        // Tenter de charger depuis ./templates/ d'abord (configuration externe)
        File externalTemplate = new File("templates/modele.docx");
        if (externalTemplate.exists()) {
            logger.info("Template charge depuis : {}", externalTemplate.getAbsolutePath());
            return new FileInputStream(externalTemplate);
        }

        // Sinon charger depuis le classpath (embarqué dans le JAR)
        InputStream stream = getClass().getResourceAsStream(TEMPLATE_PATH);
        if (stream != null) {
            logger.info("Template charge depuis le classpath : {}", TEMPLATE_PATH);
            return stream;
        }

        throw new FileNotFoundException("Le modele Word n'a pas ete trouve");
    }

    /**
     * Remplace une balise dans le XML au format {{TAG}}.
     * Si la valeur est vide, supprime l'intégralité du paragraphe Word (<w:p>) contenant
     * le tag (s'il en est l'unique contenu textuel), évitant ainsi les sauts de ligne parasites.
     */
    private String remplacerBalise(String xml, String tag, String valeur) {
        if (valeur == null) {
            valeur = "";
        }

        String valeurEchappee = escapeXml(valeur);
        String pattern = "{{" + tag + "}}";

        if (valeurEchappee.isEmpty()) {
            xml = supprimerParagrapheContenant(xml, pattern);
        } else {
            xml = remplacerPatternSimple(xml, pattern, valeurEchappee);
        }

        return xml;
    }

    /**
     * Quand la valeur est vide, supprime le paragraphe Word (<w:p>…</w:p>) entier
     * si le pattern en est le seul contenu textuel.
     * Sinon, se contente d'effacer le pattern dans le paragraphe.
     */
    private String supprimerParagrapheContenant(String xml, String pattern) {
        int pos = 0;
        while (true) {
            int patPos = xml.indexOf(pattern, pos);
            if (patPos == -1) break;

            // Remonter pour trouver le début du <w:p> enclosant (pas <w:pPr> ni <w:pStyle>)
            int pStart = -1;
            int searchBack = patPos;
            while (searchBack > 0) {
                int candidate = xml.lastIndexOf("<w:p", searchBack - 1);
                if (candidate == -1) break;
                char next = (candidate + 4 < xml.length()) ? xml.charAt(candidate + 4) : '\0';
                if (next == '>' || next == ' ') { pStart = candidate; break; }
                searchBack = candidate;
            }

            int pEnd = (pStart != -1) ? xml.indexOf("</w:p>", patPos) : -1;
            if (pEnd != -1) pEnd += "</w:p>".length();

            if (pStart != -1 && pEnd != -1) {
                String para = xml.substring(pStart, pEnd);
                // Texte brut du paragraphe une fois le pattern retiré
                String resteTexte = extraireTexteBrut(para).replace(pattern, "").trim();
                if (resteTexte.isEmpty()) {
                    // Supprimer tout le paragraphe
                    xml = xml.substring(0, pStart) + xml.substring(pEnd);
                    pos = pStart;
                } else {
                    // Ne supprimer que le pattern
                    xml = xml.substring(0, patPos) + xml.substring(patPos + pattern.length());
                    pos = patPos;
                }
            } else {
                xml = xml.substring(0, patPos) + xml.substring(patPos + pattern.length());
                pos = patPos;
            }
        }
        // Gérer les éventuels résidus fragmentés
        xml = remplacerPatternFragmente(xml, pattern, "");
        return xml;
    }
    
    /**
     * Remplace un pattern simple dans le XML
     * Gère le cas où le pattern peut être fragmenté entre plusieurs balises <w:t>
     */
    private String remplacerPatternSimple(String xml, String pattern, String valeur) {
        // Méthode 1 : Remplacement direct si le pattern est dans une seule balise
        if (xml.contains(pattern)) {
            xml = xml.replace(pattern, valeur);
        }
        
        // Méthode 2 : Chercher le pattern même s'il est fragmenté
        // Exemple: <w:t>{{</w:t><w:t>TAG</w:t><w:t>}}</w:t>
        xml = remplacerPatternFragmente(xml, pattern, valeur);
        
        return xml;
    }
    
    /**
     * Remplace un pattern même s'il est fragmenté entre plusieurs balises <w:t>
     */
    private String remplacerPatternFragmente(String xml, String pattern, String valeur) {
        // Chercher les occurrences de {{ dans le XML
        int pos = 0;
        while ((pos = xml.indexOf("{{", pos)) != -1) {
            // Extraire le texte entre {{ et }} en ignorant les balises XML
            int startPos = pos;
            int endPos = trouverFinPattern(xml, pos);
            
            if (endPos != -1) {
                // Extraire le segment XML complet
                String segment = xml.substring(startPos, endPos);
                
                // Extraire le texte brut (sans balises XML)
                String texteBrut = extraireTexteBrut(segment);
                
                // Vérifier si c'est notre pattern
                if (texteBrut.equals(pattern)) {
                    // Remplacer tout le segment par la valeur
                    String avant = xml.substring(0, startPos);
                    String apres = xml.substring(endPos);
                    
                    // Reconstruire avec une balise <w:t> propre
                    xml = avant + "<w:t>" + valeur + "</w:t>" + apres;
                    
                    // Continuer la recherche après le remplacement
                    pos = avant.length() + valeur.length() + 10;
                } else {
                    pos++;
                }
            } else {
                pos++;
            }
        }
        
        return xml;
    }
    
    /**
     * Trouve la fin d'un pattern {{ ... }}
     */
    private int trouverFinPattern(String xml, int startPos) {
        int depth = 0;
        int pos = startPos;
        boolean inTag = false;
        
        while (pos < xml.length()) {
            char c = xml.charAt(pos);
            
            if (c == '<') {
                inTag = true;
            } else if (c == '>') {
                inTag = false;
            } else if (!inTag) {
                if (c == '{' && pos + 1 < xml.length() && xml.charAt(pos + 1) == '{') {
                    depth++;
                    pos++; // Skip the second {
                } else if (c == '}' && pos + 1 < xml.length() && xml.charAt(pos + 1) == '}') {
                    depth--;
                    if (depth == 0) {
                        return pos + 2; // Inclure les deux }}
                    }
                    pos++; // Skip the second }
                }
            }
            pos++;
        }
        
        return -1;
    }
    
    /**
     * Extrait le texte brut d'un segment XML (sans les balises)
     */
    private String extraireTexteBrut(String segment) {
        StringBuilder texte = new StringBuilder();
        boolean inTag = false;
        
        for (int i = 0; i < segment.length(); i++) {
            char c = segment.charAt(i);
            
            if (c == '<') {
                inTag = true;
            } else if (c == '>') {
                inTag = false;
            } else if (!inTag) {
                texte.append(c);
            }
        }
        
        return texte.toString();
    }
    
    /**
     * Échappe les caractères spéciaux XML
     */
    private String escapeXml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&apos;");
    }
}
