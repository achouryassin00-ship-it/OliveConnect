package com.oliveconnect.service;

import com.oliveconnect.repository.RecolteRepository;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

/**
 * Vérification réelle d'utilisation d'un verger par des demandes de collecte actives (Jour 5).
 */
@Service
@Primary
public class RecolteVergerUsageChecker implements VergerUsageChecker {

    private final RecolteRepository recolteRepository;

    public RecolteVergerUsageChecker(RecolteRepository recolteRepository) {
        this.recolteRepository = recolteRepository;
    }

    @Override
    public boolean isInUse(String vergerId) {
        if (vergerId == null || vergerId.isBlank()) {
            return false;
        }
        return recolteRepository.existsByVergerIdAndStatutIn(vergerId, RecolteService.ACTIVE_STATUSES);
    }
}
