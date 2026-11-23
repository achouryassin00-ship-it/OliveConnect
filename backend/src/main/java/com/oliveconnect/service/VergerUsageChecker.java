package com.oliveconnect.service;

/**
 * Point d'extension pour les règles de suppression et modification de vergers/parcelles.
 * Sera implémenté au Jour 5 et 6 lors de l'intégration des récoltes et des tournées.
 */
public interface VergerUsageChecker {

    /**
     * Vérifie si un verger est actuellement utilisé par des récoltes ou tournées en cours.
     * @param vergerId l'identifiant du verger
     * @return true si le verger est en cours d'utilisation, false sinon
     */
    boolean isInUse(String vergerId);
}
