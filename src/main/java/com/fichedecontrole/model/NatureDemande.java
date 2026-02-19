package com.fichedecontrole.model;

public enum NatureDemande {
    CREATION("Création", "créé"),
    MODIFICATION("Modification", "modifié"),
    RESILIATION("Résiliation", "résilié"),
    REPRISE_PASSIF("Reprise de passif", "repris de passif"),
    AUCUN("Aucun Paramétrage", "inchangé");
    
    private final String displayName;
    private final String libelle;

    NatureDemande(String displayName, String libelle) {
        this.displayName = displayName;
        this.libelle = libelle;
    }

    public String getDisplayName() {
        return displayName;
    }

    /** Libellé inséré dans le document Word à la place du tag {{ACTION}} */
    public String getLibelle() {
        return libelle;
    }
    
}
