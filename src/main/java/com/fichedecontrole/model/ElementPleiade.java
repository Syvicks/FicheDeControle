package com.fichedecontrole.model;

public enum ElementPleiade {
    // Groupe Création : séparateur "+"
    PG("PG", "Produit de Gestion", true),
    PC("PC", "Produit Ciblé", true),
    RG("RG", "Regroupement de Garantie", true),
    // Groupe Modification : séparateur " / "
    FORMULE("Formule","Formule", false),
    COTIS("Cotis", "Cotisation", false),
    TX_CHGT("Tx Chgt", "Taux de Chargement", false),
    DISPO("Dispo", "Dispositif", false),
    CJ("CJ", "Contrat Juridique", false),
    AUTRE("Autre", "Autre", false);

    private final String displayName;
    private final String libelle;
    /** true = élément applicable à la Création (PG/PC/RG), false = Modification */
    private final boolean pourCreation;

    ElementPleiade(String displayName, String libelle, boolean pourCreation) {
        this.displayName = displayName;
        this.libelle = libelle;
        this.pourCreation = pourCreation;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getLibelle() {
        return libelle;
    }

    public boolean isPourCreation() {
        return pourCreation;
    }

    public boolean isPourModification() {
        return !pourCreation;
    }
}
