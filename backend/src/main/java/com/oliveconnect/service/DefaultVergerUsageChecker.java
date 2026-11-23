package com.oliveconnect.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Implémentation par défaut pour le Jour 3 (Ben Slima).
 * Retourne false (non utilisé) en attendant le développement des récoltes (J05) et tournées (J06).
 */
@Component
public class DefaultVergerUsageChecker implements VergerUsageChecker {

    private static final Logger log = LoggerFactory.getLogger(DefaultVergerUsageChecker.class);

    @Override
    public boolean isInUse(String vergerId) {
        log.debug("Vérification d'utilisation pour le verger {} (J03 : pas de récoltes actives connectées -> false)", vergerId);
        return false;
    }
}
