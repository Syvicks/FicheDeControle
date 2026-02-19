package com.fichedecontrole.model;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO (Data Transfer Object) pour les données de la fiche
 */
public class FicheDto {

    private String contratJuridique;
    private String numFormulaire;
    private TypeDemande typeDemande;        // O2 ou E-Contractu
    private Risque risque;             // FSS ou Prev
    private NatureDemande natureDemande;
    private List<String> elements;     // PG, PC, RG, Formule, Cotis, Tx Chgt, Dispo, CJ
    private String dateEffet;
    private String dispositif;
    private String raisonSocial;
    private String parametreur;
    private List<String> formules;
    private String tauxChargement;
    private String structure;
    private String structure2;
    private String[] listePC;
    private String produitGestion;
    private List<ScreenCapture> captures; // Captures d'écran à insérer dans le Word
    
    public FicheDto() {
        this.elements = new ArrayList<>();
        this.formules = new ArrayList<>();
        this.captures = new ArrayList<>();
    }

    public FicheDto(String contratJuridique, String numFormulaire, TypeDemande typeDemande,
                    Risque risque, NatureDemande natureDemande, List<String> elements,
                    String dateEffet, String dispositif, String raisonSocial, String parametreur,
                    List<String> formules, String tauxChargement,
                    String structure, String structure2, String[] listePC) {
        this.contratJuridique = contratJuridique;
        this.numFormulaire = numFormulaire;
        this.typeDemande = typeDemande;
        this.risque = risque;
        this.natureDemande = natureDemande;
        this.elements = elements != null ? elements : new ArrayList<>();
        this.dateEffet = dateEffet;
        this.dispositif = dispositif;
        this.raisonSocial = raisonSocial;
        this.parametreur = parametreur;
        this.formules = formules != null ? formules : new ArrayList<>();
        this.tauxChargement = tauxChargement;
        this.structure = structure;
        this.structure2 = structure2;
        this.listePC = listePC;

        // Extraction du produit de gestion avec vérification de longueur
        if (listePC != null && listePC.length > 0 && listePC[0] != null) {
            String pc = listePC[0].trim();
            this.produitGestion = pc.length() >= 5 ? pc.substring(0, 5) : pc;
        } else {
            this.produitGestion = "";
        }
    }
    
    // Getters et Setters
    
    public String getContratJuridique() {
        return contratJuridique;
    }
    
    public void setContratJuridique(String contratJuridique) {
        this.contratJuridique = contratJuridique;
    }
    
    public String getNumFormulaire() {
        return numFormulaire;
    }
    
    public void setNumFormulaire(String numFormulaire) {
        this.numFormulaire = numFormulaire;
    }
    
    public NatureDemande getNatureDemande() {
        return natureDemande;
    }

    public void setNatureDemande(NatureDemande natureDemande) {
        this.natureDemande = natureDemande;
    }
    
    public String getDateEffet() {
        return dateEffet;
    }
    
    public void setDateEffet(String dateEffet) {
        this.dateEffet = dateEffet;
    }
    
    public String getDispositif() {
        return dispositif;
    }

    public void setDispositif(String dispositif) {
        this.dispositif = dispositif;
    }

    public String getRaisonSocial() {
        return raisonSocial;
    }

    public void setRaisonSocial(String raisonSocial) {
        this.raisonSocial = raisonSocial;
    }

    public String getParametreur() {
        return parametreur;
    }
    
    public void setParametreur(String parametreur) {
        this.parametreur = parametreur;
    }
    
    /**
     * Retourne les formules jointes par " + " pour le tag Word {{FORMULE}}
     */
    public String getFormule() {
        if (formules == null || formules.isEmpty()) {
            return "";
        }
        return String.join(" + ", formules);
    }

    public List<String> getFormules() {
        return formules;
    }

    public void setFormules(List<String> formules) {
        this.formules = formules != null ? formules : new ArrayList<>();
    }
    
    public String getTauxChargement() {
        return tauxChargement;
    }
    
    public void setTauxChargement(String tauxChargement) {
        this.tauxChargement = tauxChargement;
    }
    
    public String getStructure() {
        return structure;
    }
    
    public void setStructure(String structure) {
        this.structure = structure;
    }

    public String getStructure2() {
        return structure2;
    }

    public void setStructure2(String structure2) {
        this.structure2 = structure2;
    }
    
    public String[] getListePC() {
        return listePC;
    }
    
    public void setListePC(String[] listePC) {
        this.listePC = listePC;
    }

    public String getProduitGestion() {
        return produitGestion;
    }

    public void setProduitGestion(String produitGestion) {
        this.produitGestion = produitGestion;
    }

    public TypeDemande getTypeDemande() {
        return typeDemande;
    }

    public void setTypeDemande(TypeDemande typeDemande) {
        this.typeDemande = typeDemande;
    }

    public Risque getRisque() {
        return risque;
    }

    public void setRisque(Risque risque) {
        this.risque = risque;
    }

    public List<String> getElements() {
        return elements;
    }

    public void setElements(List<String> elements) {
        this.elements = elements;
    }

    public List<ScreenCapture> getCaptures() {
        return captures;
    }

    public void setCaptures(List<ScreenCapture> captures) {
        this.captures = captures != null ? captures : new ArrayList<>();
    }
}
