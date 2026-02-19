package com.fichedecontrole.model;

/**
 * Catégories possibles pour les captures d'écran.
 * Le tag correspond au placeholder dans le template Word : {{TAG}}
 */
public enum CaptureCategory {

    // Largeurs en EMU = (largeur colonne dxa - marges cellule 216 dxa) * 635
    // Colonne SOURCE/Formulaire : 5168 dxa → (5168-216)*635 = 3 144 520 EMU
    // Colonne Plei@de          : 5742 dxa → (5742-216)*635 = 3 509 010 EMU
    COTISATIONS_FORMULAIRE("Cotisations Formulaire", "CAPTURES_COTISATIONS_FORMULAIRE", true, 3144520L),
    TX_CHGT_FORMULAIRE("Taux chargement Formulaire", "CAPTURES_TX_CHGT_FORMULAIRE", false, 3144520L),
    AUTRES_FORMULAIRES("Autres formulaires", "CAPTURES_AUTRES_FORMULAIRES", true, 3144520L),
    TX_CHGT_PLEIADE("Taux chargement Pléiade", "CAPTURES_TX_CHGT_PLEIADE", true, 3509010L),
    COTISATIONS_PLEIADE("Cotisations Pléiade", "CAPTURES_COTISATIONS_PLEIADE", true, 3509010L),
    TEST_ADHESION("Test d'adhésion", "CAPTURES_TEST_ADHESION", true, 3509010L),
    AUTRES_INFORMATIONS("Autres informations", "CAPTURES_AUTRES_INFORMATIONS", true, 3509010L);

    private final String displayName;
    private final String wordTag;
    private final boolean multiple; // true si on peut avoir plusieurs captures de ce type
    private final long targetWidthEmu; // largeur cible de l'image dans la colonne Word

    CaptureCategory(String displayName, String wordTag, boolean multiple, long targetWidthEmu) {
        this.displayName = displayName;
        this.wordTag = wordTag;
        this.multiple = multiple;
        this.targetWidthEmu = targetWidthEmu;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getWordTag() {
        return wordTag;
    }

    public boolean isMultiple() {
        return multiple;
    }

    public long getTargetWidthEmu() {
        return targetWidthEmu;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
