package com.fichedecontrole.model;

public enum TypeDemande {
    O2("O2"),
    E_CONTRACTU("E-Contractu");
    
    private final String displayName;
    
    TypeDemande(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
}
