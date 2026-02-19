package com.fichedecontrole.generator;

import com.fichedecontrole.model.CaptureCategory;
import com.fichedecontrole.model.ScreenCapture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Gère l'insertion d'images (captures d'écran) dans un document Word .docx
 * Responsable de :
 * - Générer le XML DrawingML pour chaque image
 * - Gérer les relations (rId) dans document.xml.rels
 * - Fournir les données PNG pour word/media/
 */
public class WordImageManager {

    private static final Logger logger = LoggerFactory.getLogger(WordImageManager.class);

    // La largeur cible est maintenant définie par catégorie dans CaptureCategory (en EMU)

    // Compteur pour les IDs de relation (commence haut pour éviter les conflits)
    private int nextRelId = 100;
    private int nextImageId = 1;

    // Stockage des images préparées
    private final List<ImageEntry> imageEntries = new ArrayList<>();

    /**
     * Représente une image prête à être insérée dans le document
     */
    public static class ImageEntry {
        public final String relationId;     // rId100, rId101, etc.
        public final String fileName;       // image1.png, image2.png, etc.
        public final byte[] pngData;        // Données PNG de l'image
        public final long widthEmu;         // Largeur en EMU
        public final long heightEmu;        // Hauteur en EMU
        public final int imageId;           // ID unique pour DrawingML
        public final CaptureCategory category; // Catégorie de la capture

        ImageEntry(String relationId, String fileName, byte[] pngData,
                   long widthEmu, long heightEmu, int imageId, CaptureCategory category) {
            this.relationId = relationId;
            this.fileName = fileName;
            this.pngData = pngData;
            this.widthEmu = widthEmu;
            this.heightEmu = heightEmu;
            this.imageId = imageId;
            this.category = category;
        }
    }

    /**
     * Prépare toutes les captures pour l'insertion dans le document.
     * Doit être appelé en premier, avant les autres méthodes.
     *
     * @param captures la liste des captures d'écran
     */
    public void prepareImages(List<ScreenCapture> captures) {
        if (captures == null || captures.isEmpty()) {
            return;
        }

        for (ScreenCapture capture : captures) {
            try {
                byte[] pngData = bufferedImageToPng(capture.getImage());
                String relId = "rId" + nextRelId++;
                String fileName = "image" + nextImageId + ".png";
                int imageId = nextImageId++;

                // Calculer les dimensions en EMU (proportionnel à la largeur de la colonne cible)
                long widthEmu = capture.getCategory().getTargetWidthEmu();
                double ratio = (double) capture.getImage().getHeight() / capture.getImage().getWidth();
                long heightEmu = (long) (widthEmu * ratio);

                ImageEntry entry = new ImageEntry(relId, fileName, pngData,
                    widthEmu, heightEmu, imageId, capture.getCategory());
                imageEntries.add(entry);

                logger.debug("Image preparee : {} ({}x{} px) → {} (rId={})",
                    capture.getDisplayName(),
                    capture.getImage().getWidth(), capture.getImage().getHeight(),
                    fileName, relId);

            } catch (IOException e) {
                logger.error("Erreur lors de la conversion de la capture en PNG : {}",
                    capture.getDisplayName(), e);
            }
        }

        logger.info("{} image(s) preparee(s) pour l'insertion", imageEntries.size());
    }

    /**
     * Remplace les tags {{CAPTURES_XXX}} dans le XML du document
     * par le DrawingML correspondant.
     * - Une ligne vide (<w:p/>) est insérée entre chaque capture d'une même catégorie.
     * - Pour les captures TEST_ADHESION, le Produit Ciblé correspondant est affiché
     *   dans un paragraphe avant l'image (un PC = une capture).
     *
     * @param xml              le contenu XML de word/document.xml
     * @param produitsCibles  les Produits Ciblés (PC1, PC2, PC3…) dans l'ordre de saisie
     * @return le XML modifié avec les images insérées
     */
    public String replaceCaptureTags(String xml, String[] produitsCibles) {
        // Regrouper les images par catégorie
        Map<CaptureCategory, List<ImageEntry>> imagesByCategory = imageEntries.stream()
            .collect(Collectors.groupingBy(e -> e.category, LinkedHashMap::new, Collectors.toList()));

        // Pour chaque catégorie, remplacer le tag par le DrawingML
        for (CaptureCategory category : CaptureCategory.values()) {
            String tag = "{{" + category.getWordTag() + "}}";
            List<ImageEntry> entries = imagesByCategory.getOrDefault(category, Collections.emptyList());

            int tagPos = xml.indexOf(tag);
            if (tagPos < 0) continue;

            // Trouver le <w:p> englobant et son </w:p> de fermeture
            // On cherche "<w:p>" ou "<w:p " pour éviter de matcher <w:pPr>, <w:pStyle>, etc.
            int pStart = Math.max(xml.lastIndexOf("<w:p>", tagPos), xml.lastIndexOf("<w:p ", tagPos));
            int pEnd = xml.indexOf("</w:p>", tagPos) + "</w:p>".length();

            if (entries.isEmpty()) {
                boolean afficherNA = category == CaptureCategory.TEST_ADHESION
                        || category == CaptureCategory.TX_CHGT_FORMULAIRE
                        || category == CaptureCategory.COTISATIONS_FORMULAIRE;
                String remplacement = afficherNA
                        ? "<w:p><w:r><w:t>N/A</w:t></w:r></w:p>"
                        : "";
                xml = xml.substring(0, pStart) + remplacement + xml.substring(pEnd);
            } else {
                StringBuilder drawingXml = new StringBuilder();
                for (int i = 0; i < entries.size(); i++) {
                    ImageEntry entry = entries.get(i);

                    // Ligne vide entre chaque capture (pas avant la première)
                    if (i > 0) {
                        drawingXml.append("<w:p/>");
                    }

                    // Pour Taux de chargement Pléiade
                    if ((category == CaptureCategory.TX_CHGT_PLEIADE 
                            || category == CaptureCategory.COTISATIONS_PLEIADE) 
                        && i == 0) {
                        drawingXml.append("<w:p><w:r><w:t>")
                                  .append(escapeXml("Avant :"))
                                  .append("</w:t></w:r></w:p>");
                    } else if (category == CaptureCategory.TX_CHGT_PLEIADE && i == 1) {
                        drawingXml.append("<w:p><w:r><w:t>")
                                  .append(escapeXml("Après :"))
                                  .append("</w:t></w:r></w:p>");
                    }

                    // Pour TEST_ADHESION : insérer le nom du Produit Ciblé avant l'image
                    if (category == CaptureCategory.TEST_ADHESION
                            && produitsCibles != null && i < produitsCibles.length) {
                        String pc = produitsCibles[i] != null ? produitsCibles[i].trim() : "";
                        if (!pc.isEmpty()) {
                            drawingXml.append("<w:p><w:r><w:t>")
                                      .append(escapeXml(pc))
                                      .append("</w:t></w:r></w:p>");
                        }
                    }

                    // Chaque image dans son propre paragraphe <w:p><w:r><w:drawing/></w:r></w:p>
                    drawingXml.append("<w:p><w:r>")
                              .append(generateDrawingML(entry))
                              .append("</w:r></w:p>");
                }
                // Remplacer le paragraphe entier contenant le tag
                xml = xml.substring(0, pStart) + drawingXml.toString() + xml.substring(pEnd);
            }
        }

        return xml;
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

    /**
     * Ajoute les relations d'images au contenu de document.xml.rels
     *
     * @param relsXml le contenu XML de word/_rels/document.xml.rels
     * @return le XML modifié avec les nouvelles relations
     */
    public String addImageRelationships(String relsXml) {
        if (imageEntries.isEmpty()) {
            return relsXml;
        }

        // Trouver la position de </Relationships> pour insérer avant
        int insertPos = relsXml.lastIndexOf("</Relationships>");
        if (insertPos == -1) {
            logger.error("Impossible de trouver </Relationships> dans document.xml.rels");
            return relsXml;
        }

        StringBuilder newRels = new StringBuilder();
        for (ImageEntry entry : imageEntries) {
            newRels.append("  <Relationship Id=\"").append(entry.relationId)
                   .append("\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/image\"")
                   .append(" Target=\"media/").append(entry.fileName).append("\"/>\n");
        }

        return relsXml.substring(0, insertPos) + newRels.toString() + relsXml.substring(insertPos);
    }

    /**
     * Ajoute le type de contenu PNG dans [Content_Types].xml si absent
     *
     * @param contentTypesXml le contenu de [Content_Types].xml
     * @return le XML modifié
     */
    public String addPngContentType(String contentTypesXml) {
        if (imageEntries.isEmpty()) {
            return contentTypesXml;
        }

        // Vérifier si le type PNG est déjà déclaré
        if (contentTypesXml.contains("Extension=\"png\"")) {
            return contentTypesXml;
        }

        // Ajouter avant </Types>
        int insertPos = contentTypesXml.lastIndexOf("</Types>");
        if (insertPos == -1) {
            return contentTypesXml;
        }

        String pngType = "  <Default Extension=\"png\" ContentType=\"image/png\"/>\n";
        return contentTypesXml.substring(0, insertPos) + pngType + contentTypesXml.substring(insertPos);
    }

    /**
     * Retourne la liste des images préparées (pour les ajouter au ZIP)
     */
    public List<ImageEntry> getImageEntries() {
        return Collections.unmodifiableList(imageEntries);
    }

    /**
     * Vérifie s'il y a des images à insérer
     */
    public boolean hasImages() {
        return !imageEntries.isEmpty();
    }

    /**
     * Génère le XML DrawingML pour une image inline
     */
    private String generateDrawingML(ImageEntry entry) {
        return "<w:drawing>" +
            "<wp:inline distT=\"0\" distB=\"0\" distL=\"0\" distR=\"0\">" +
            "<wp:extent cx=\"" + entry.widthEmu + "\" cy=\"" + entry.heightEmu + "\"/>" +
            "<wp:docPr id=\"" + entry.imageId + "\" name=\"" + entry.fileName + "\"/>" +
            "<a:graphic xmlns:a=\"http://schemas.openxmlformats.org/drawingml/2006/main\">" +
            "<a:graphicData uri=\"http://schemas.openxmlformats.org/drawingml/2006/picture\">" +
            "<pic:pic xmlns:pic=\"http://schemas.openxmlformats.org/drawingml/2006/picture\">" +
            "<pic:nvPicPr>" +
            "<pic:cNvPr id=\"" + entry.imageId + "\" name=\"" + entry.fileName + "\"/>" +
            "<pic:cNvPicPr/>" +
            "</pic:nvPicPr>" +
            "<pic:blipFill>" +
            "<a:blip r:embed=\"" + entry.relationId + "\"/>" +
            "<a:stretch><a:fillRect/></a:stretch>" +
            "</pic:blipFill>" +
            "<pic:spPr>" +
            "<a:xfrm>" +
            "<a:off x=\"0\" y=\"0\"/>" +
            "<a:ext cx=\"" + entry.widthEmu + "\" cy=\"" + entry.heightEmu + "\"/>" +
            "</a:xfrm>" +
            "<a:prstGeom prst=\"rect\"><a:avLst/></a:prstGeom>" +
            "</pic:spPr>" +
            "</pic:pic>" +
            "</a:graphicData>" +
            "</a:graphic>" +
            "</wp:inline>" +
            "</w:drawing>";
    }

    /**
     * Convertit un BufferedImage en données PNG
     */
    private byte[] bufferedImageToPng(BufferedImage image) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        return baos.toByteArray();
    }
}
