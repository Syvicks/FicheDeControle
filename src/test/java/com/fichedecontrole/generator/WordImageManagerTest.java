package com.fichedecontrole.generator;

import com.fichedecontrole.model.CaptureCategory;
import com.fichedecontrole.model.ScreenCapture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests pour WordImageManager.replaceCaptureTags()
 */
class WordImageManagerTest {

    private WordImageManager imageManager;

    @BeforeEach
    void setUp() {
        imageManager = new WordImageManager();
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /** Crée une image de test minimale (10x5 px) */
    private static BufferedImage createTestImage() {
        return new BufferedImage(10, 5, BufferedImage.TYPE_INT_RGB);
    }

    /** Construit un XML minimal contenant le tag dans un <w:p> */
    private static String xmlAvecTag(String wordTag) {
        return "<w:p w14:paraId=\"ABC\"><w:pPr/><w:r><w:t>{{" + wordTag + "}}</w:t></w:r></w:p>";
    }

    // -----------------------------------------------------------------------
    // Cas : aucune capture, catégories affichant N/A
    // -----------------------------------------------------------------------

    @Test
    void testSansCapture_testAdhesion_devraitAfficherNA() {
        String xml = xmlAvecTag(CaptureCategory.TEST_ADHESION.getWordTag());

        String result = imageManager.replaceCaptureTags(xml, null);

        assertThat(result).contains("<w:t>N/A</w:t>");
        assertThat(result).doesNotContain("{{" + CaptureCategory.TEST_ADHESION.getWordTag() + "}}");
    }

    @Test
    void testSansCapture_txChgtFormulaire_devraitAfficherNA() {
        String xml = xmlAvecTag(CaptureCategory.TX_CHGT_FORMULAIRE.getWordTag());

        String result = imageManager.replaceCaptureTags(xml, null);

        assertThat(result).contains("<w:t>N/A</w:t>");
        assertThat(result).doesNotContain("{{" + CaptureCategory.TX_CHGT_FORMULAIRE.getWordTag() + "}}");
    }

    @Test
    void testSansCapture_cotisationsFormulaire_devraitAfficherNA() {
        String xml = xmlAvecTag(CaptureCategory.COTISATIONS_FORMULAIRE.getWordTag());

        String result = imageManager.replaceCaptureTags(xml, null);

        assertThat(result).contains("<w:t>N/A</w:t>");
        assertThat(result).doesNotContain("{{" + CaptureCategory.COTISATIONS_FORMULAIRE.getWordTag() + "}}");
    }

    // -----------------------------------------------------------------------
    // Cas : aucune capture, catégories supprimant le paragraphe
    // -----------------------------------------------------------------------

    @Test
    void testSansCapture_autresFormulaires_devraitSupprimerParagraphe() {
        String xml = "<body>" + xmlAvecTag(CaptureCategory.AUTRES_FORMULAIRES.getWordTag()) + "</body>";

        String result = imageManager.replaceCaptureTags(xml, null);

        assertThat(result).isEqualTo("<body></body>");
        assertThat(result).doesNotContain("N/A");
    }

    @Test
    void testSansCapture_autresInformations_devraitSupprimerParagraphe() {
        String xml = "<body>" + xmlAvecTag(CaptureCategory.AUTRES_INFORMATIONS.getWordTag()) + "</body>";

        String result = imageManager.replaceCaptureTags(xml, null);

        assertThat(result).isEqualTo("<body></body>");
        assertThat(result).doesNotContain("N/A");
    }

    // -----------------------------------------------------------------------
    // Cas : une ou plusieurs captures
    // -----------------------------------------------------------------------

    @Test
    void testAvecUneCapture_devraitGenererDrawingMLDansUnParagraphe() {
        ScreenCapture capture = new ScreenCapture(CaptureCategory.COTISATIONS_FORMULAIRE, createTestImage(), 0);
        imageManager.prepareImages(List.of(capture));
        String xml = xmlAvecTag(CaptureCategory.COTISATIONS_FORMULAIRE.getWordTag());

        String result = imageManager.replaceCaptureTags(xml, null);

        assertThat(result).contains("<w:p><w:r><w:drawing>");
        assertThat(result).contains("</w:drawing></w:r></w:p>");
        assertThat(result).doesNotContain("{{" + CaptureCategory.COTISATIONS_FORMULAIRE.getWordTag() + "}}");
    }

    @Test
    void testAvecDeuxCaptures_devraitAvoirSeparateurEntreElles() {
        ScreenCapture c1 = new ScreenCapture(CaptureCategory.COTISATIONS_FORMULAIRE, createTestImage(), 0);
        ScreenCapture c2 = new ScreenCapture(CaptureCategory.COTISATIONS_FORMULAIRE, createTestImage(), 1);
        imageManager.prepareImages(List.of(c1, c2));
        String xml = xmlAvecTag(CaptureCategory.COTISATIONS_FORMULAIRE.getWordTag());

        String result = imageManager.replaceCaptureTags(xml, null);

        assertThat(result).contains("<w:p/>");
        // Deux drawings générés
        assertThat(result.split("<w:drawing>", -1).length - 1).isEqualTo(2);
    }

    @Test
    void testAvecUneCapture_pasDeDoubleSeparateur() {
        ScreenCapture capture = new ScreenCapture(CaptureCategory.COTISATIONS_FORMULAIRE, createTestImage(), 0);
        imageManager.prepareImages(List.of(capture));
        String xml = xmlAvecTag(CaptureCategory.COTISATIONS_FORMULAIRE.getWordTag());

        String result = imageManager.replaceCaptureTags(xml, null);

        // Pas de <w:p/> quand il n'y a qu'une seule capture
        assertThat(result).doesNotContain("<w:p/>");
    }

    // -----------------------------------------------------------------------
    // Cas TEST_ADHESION avec Produits Ciblés
    // -----------------------------------------------------------------------

    @Test
    void testTestAdhesion_avecPC_devraitAfficherNomPCAvantImage() {
        ScreenCapture capture = new ScreenCapture(CaptureCategory.TEST_ADHESION, createTestImage(), 0);
        imageManager.prepareImages(List.of(capture));
        String xml = xmlAvecTag(CaptureCategory.TEST_ADHESION.getWordTag());

        String result = imageManager.replaceCaptureTags(xml, new String[]{"TPSS01"});

        assertThat(result).contains("TPSS01");
        assertThat(result).contains("<w:drawing>");
        // Le nom PC doit apparaître AVANT le drawing
        assertThat(result.indexOf("TPSS01")).isLessThan(result.indexOf("<w:drawing>"));
    }

    @Test
    void testTestAdhesion_sansPC_devraitGenererImageSansNomPC() {
        ScreenCapture capture = new ScreenCapture(CaptureCategory.TEST_ADHESION, createTestImage(), 0);
        imageManager.prepareImages(List.of(capture));
        String xml = xmlAvecTag(CaptureCategory.TEST_ADHESION.getWordTag());

        String result = imageManager.replaceCaptureTags(xml, null);

        assertThat(result).contains("<w:drawing>");
        // Pas de paragraphe de texte parasite
        assertThat(result).doesNotContain("<w:t>TPSS");
    }

    // -----------------------------------------------------------------------
    // Cas : tag absent du XML
    // -----------------------------------------------------------------------

    @Test
    void testTagAbsent_devraitLaisserXMLInchange() {
        String xml = "<body><w:p><w:r><w:t>Contenu sans tag</w:t></w:r></w:p></body>";

        String result = imageManager.replaceCaptureTags(xml, null);

        assertThat(result).isEqualTo(xml);
    }
}
