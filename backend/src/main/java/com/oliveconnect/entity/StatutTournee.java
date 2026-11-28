package com.oliveconnect.entity;

import java.util.EnumSet;
import java.util.Set;

/**
 * Cycle de vie officiel d'une tournée de collecte.
 */
public enum StatutTournee {
    BROUILLON("Brouillon"),
    PLANIFIEE("Planifiée"),
    EN_COURS("En cours"),
    TERMINEE("Terminée"),
    ANNULEE("Annulée"),
    INTERROMPUE("Interrompue");

    private final String libelle;

    StatutTournee(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }

    public boolean peutPasserA(StatutTournee nouveauStatut) {
        if (nouveauStatut == null) {
            return false;
        }
        return switch (this) {
            case BROUILLON -> EnumSet.of(PLANIFIEE, ANNULEE).contains(nouveauStatut);
            case PLANIFIEE -> EnumSet.of(EN_COURS, ANNULEE).contains(nouveauStatut);
            case EN_COURS -> EnumSet.of(TERMINEE, INTERROMPUE).contains(nouveauStatut);
            case TERMINEE, ANNULEE, INTERROMPUE -> false;
        };
    }

    public boolean estActivePourChevauchement() {
        return this == BROUILLON || this == PLANIFIEE || this == EN_COURS;
    }

    public boolean estEngageePourBlocageRessource() {
        return this == PLANIFIEE || this == EN_COURS;
    }

    public static Set<StatutTournee> statutsActifsChevauchement() {
        return EnumSet.of(BROUILLON, PLANIFIEE, EN_COURS);
    }

    public static Set<StatutTournee> statutsBlocageRessource() {
        return EnumSet.of(PLANIFIEE, EN_COURS);
    }
}
