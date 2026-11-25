package com.oliveconnect.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implémentation de repli hors contexte Spring (tests unitaires isolés J03).
 * Dans le contexte applicatif, {@link RecolteVergerUsageChecker} vérifie réellement les récoltes actives.
 */
public class DefaultVergerUsageChecker implements VergerUsageChecker {

    private static final Logger log = LoggerFactory.getLogger(DefaultVergerUsageChecker.class);

    @Override
    public boolean isInUse(String vergerId) {
        log.debug("Vérification d'utilisation pour le verger {} (J03 : pas de récoltes actives connectées -> false)", vergerId);
        return false;
    }
}
