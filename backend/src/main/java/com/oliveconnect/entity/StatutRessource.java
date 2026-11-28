package com.oliveconnect.entity;

/**
 * Statut opérationnel d'une ressource.
 */
public enum StatutRessource {
    DISPONIBLE("Disponible"),
    MAINTENANCE("En maintenance"),
    HORS_SERVICE("Hors service"),
    INDISPONIBLE("Indisponible");

    private final String libelle;

    StatutRessource(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }
}
