package com.oliveconnect.entity;

/**
 * Cycle de vie officiel d'une demande de collecte.
 */
public enum StatutRecolte {
    DEMANDEE,
    VALIDEE,
    PLANIFIEE,
    EN_COURS,
    LIVREE,
    PESEE,
    ARCHIVEE,
    REFUSEE,
    ANNULEE,
    INTERROMPUE
}
