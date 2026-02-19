package com.fichedecontrole.service;

import com.fichedecontrole.model.FicheDto;
import com.fichedecontrole.model.NatureDemande;
import com.fichedecontrole.model.Risque;
import com.fichedecontrole.model.TypeDemande;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.Arrays;
import static org.assertj.core.api.Assertions.*;

/**
 * Tests pour ValidationService
 */
class ValidationServiceTest {

    private ValidationService validationService;

    @BeforeEach
    void setUp() {
        validationService = new ValidationService();
    }

    @Test
    void testValidate_withValidData_shouldPass() {
        FicheDto fiche = createValidFiche();
        ValidationResult result = validationService.validate(fiche);

        assertThat(result.isValid()).isTrue();
        assertThat(result.getErrors()).isEmpty();
    }

    @Test
    void testValidate_withMissingCJ_shouldFail() {
        FicheDto fiche = createValidFiche();
        fiche.setContratJuridique(null);

        ValidationResult result = validationService.validate(fiche);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).containsKey("contratJuridique");
    }

    @Test
    void testValidate_withInvalidDate_shouldFail() {
        FicheDto fiche = createValidFiche();
        fiche.setDateEffet("invalid-date");

        ValidationResult result = validationService.validate(fiche);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).containsKey("dateEffet");
    }

    @Test
    void testValidate_withShortPC_shouldFail() {
        FicheDto fiche = createValidFiche();
        fiche.setListePC(new String[]{"123"});  // Trop court

        ValidationResult result = validationService.validate(fiche);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).containsKey("listePC[0]");
    }

    private FicheDto createValidFiche() {
        return new FicheDto(
            "CJ123",                        // contratJuridique
            "FORM456",                      // numFormulaire
            TypeDemande.O2,                 // typeDemande
            Risque.FSS,                     // risque
            NatureDemande.CREATION,                     // natureDemande
            Arrays.asList("PG", "PC"),      // elements
            "01/01/2024",                   // dateEffet
            "Disp1",                        // dispositif
            "Raison Sociale",               // raisonSocial
            "Paramètre",                    // parametreur
            Arrays.asList("TPSS"),          // formules
            "10%",                          // tauxChargement
            "Structure1",                   // structure
            "",                             // structure2
            new String[]{"12345678"}        // listePC
        );
    }
}
