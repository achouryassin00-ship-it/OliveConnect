package com.oliveconnect.entity;

/**
 * Type de ressource matérielle ou humaine affectable à une tournée.
 */
public enum TypeRessource {
    OUVRIER("Ouvrier saisonnier"),
    TRACTEUR("Tracteur"),
    BENNE("Benne");

    private final String libelle;

    TypeRessource(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }
}
