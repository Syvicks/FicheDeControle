package com.fichedecontrole.model;

public enum Risque {
    FSS("FSS"),
    PREV("Prev");

    private final String displayName;
    
    Risque(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
}
