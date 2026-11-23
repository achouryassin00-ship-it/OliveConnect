package com.oliveconnect.service;

import com.oliveconnect.dto.*;
import com.oliveconnect.entity.Parcelle;
import com.oliveconnect.entity.SourcePosition;
import com.oliveconnect.entity.Variete;
import com.oliveconnect.entity.Verger;
import com.oliveconnect.exception.BusinessConflictException;
import com.oliveconnect.exception.ResourceNotFoundException;
import com.oliveconnect.exception.ValidationException;
import com.oliveconnect.repository.ParcelleRepository;
import com.oliveconnect.repository.VarieteRepository;
import com.oliveconnect.repository.VergerRepository;
import com.oliveconnect.security.SecurityService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class VergerService {

    private final VergerRepository vergerRepository;
    private final ParcelleRepository parcelleRepository;
    private final VarieteRepository varieteRepository;
    private final SecurityService securityService;
    private final VergerUsageChecker vergerUsageChecker;

    private final double minLat;
    private final double maxLat;
    private final double minLng;
    private final double maxLng;
    private final double maxSurfaceHa;

    public VergerService(VergerRepository vergerRepository,
                         ParcelleRepository parcelleRepository,
                         VarieteRepository varieteRepository,
                         SecurityService securityService,
                         VergerUsageChecker vergerUsageChecker,
                         @Value("${oliveconnect.geo.sfax.min-lat:34.30}") double minLat,
                         @Value("${oliveconnect.geo.sfax.max-lat:35.40}") double maxLat,
                         @Value("${oliveconnect.geo.sfax.min-lng:9.80}") double minLng,
                         @Value("${oliveconnect.geo.sfax.max-lng:11.20}") double maxLng,
                         @Value("${oliveconnect.verger.max-surface-ha:1000.0}") double maxSurfaceHa) {
        this.vergerRepository = vergerRepository;
        this.parcelleRepository = parcelleRepository;
        this.varieteRepository = varieteRepository;
        this.securityService = securityService;
        this.vergerUsageChecker = vergerUsageChecker;
        this.minLat = minLat;
        this.maxLat = maxLat;
        this.minLng = minLng;
        this.maxLng = maxLng;
        this.maxSurfaceHa = maxSurfaceHa;
    }

    public PageResponse<VergerResponseDTO> getVergers(int page, int size) {
        int boundedSize = Math.min(Math.max(size, 1), 100);
        int boundedPage = Math.max(page, 0);
        Pageable pageable = PageRequest.of(boundedPage, boundedSize, Sort.by("createdAt").descending());

        Page<Verger> vergersPage;
        if (isStaff()) {
            vergersPage = vergerRepository.findByActifTrue(pageable);
        } else {
            String currentUserId = securityService.getCurrentUserId();
            vergersPage = vergerRepository.findByProprietaireIdAndActifTrue(currentUserId, pageable);
        }

        List<VergerResponseDTO> dtos = vergersPage.getContent().stream()
                .map(v -> {
                    double totalParcelles = computeTotalParcellesSurface(v.getId());
                    return VergerResponseDTO.fromEntity(v, totalParcelles);
                })
                .toList();

        return new PageResponse<>(dtos, vergersPage.getNumber(), vergersPage.getSize(),
                vergersPage.getTotalElements(), vergersPage.getTotalPages());
    }

    public VergerResponseDTO getVergerById(String id) {
        Verger verger = findVergerOrThrow(id);
        checkOwnershipOrStaff(verger);

        double totalParcelles = computeTotalParcellesSurface(verger.getId());
        return VergerResponseDTO.fromEntity(verger, totalParcelles);
    }

    public VergerResponseDTO createVerger(VergerRequestDTO request) {
        String cleanCode = request.getCode() != null ? request.getCode().trim().toUpperCase(Locale.ROOT) : "";
        if (vergerRepository.existsByCode(cleanCode)) {
            throw new BusinessConflictException("Un verger avec le code " + cleanCode + " existe déjà.");
        }

        validateSurface(request.getSurfaceHa());
        validateCoordinates(request.getLatitude(), request.getLongitude());

        // AUT-02 : le propriétaire est TOUJOURS résolu via l'utilisateur authentifié
        String currentUserId = securityService.getCurrentUserId();

        SourcePosition sourcePos = request.getSourcePosition() != null ? request.getSourcePosition() : SourcePosition.AUTO;
        GeoJsonPoint geoPoint = new GeoJsonPoint(request.getLongitude(), request.getLatitude());

        Verger verger = new Verger(
                null,
                cleanCode,
                request.getNom().trim(),
                currentUserId,
                request.getCommune() != null ? request.getCommune().trim() : null,
                geoPoint,
                request.getPrecisionGpsMetres(),
                sourcePos,
                request.getSurfaceHa(),
                request.getNombreArbres(),
                request.getAnneePlantation(),
                true
        );

        Verger saved = vergerRepository.save(verger);
        return VergerResponseDTO.fromEntity(saved, 0.0);
    }

    public VergerResponseDTO updateVerger(String id, VergerRequestDTO request) {
        Verger verger = findVergerOrThrow(id);
        checkOwnershipOrStaff(verger);

        // Optimistic locking
        if (request.getVersion() != null && !request.getVersion().equals(verger.getVersion())) {
            throw new BusinessConflictException("Conflit de version : le verger a été modifié par un autre utilisateur.");
        }

        validateSurface(request.getSurfaceHa());
        validateCoordinates(request.getLatitude(), request.getLongitude());

        // VER-07 : la surface du verger ne peut pas être inférieure à la somme de ses parcelles actives
        double totalParcelles = computeTotalParcellesSurface(id);
        if (request.getSurfaceHa() < totalParcelles) {
            throw new BusinessConflictException("La surface du verger (" + request.getSurfaceHa()
                    + " ha) ne peut pas être inférieure à la somme des surfaces de ses parcelles actives ("
                    + totalParcelles + " ha).");
        }

        // INT-07 : le code est immuable
        verger.setNom(request.getNom().trim());
        verger.setCommune(request.getCommune() != null ? request.getCommune().trim() : null);
        verger.setLocalisation(new GeoJsonPoint(request.getLongitude(), request.getLatitude()));
        verger.setPrecisionGpsMetres(request.getPrecisionGpsMetres());
        if (request.getSourcePosition() != null) {
            verger.setSourcePosition(request.getSourcePosition());
        }
        verger.setSurfaceHa(request.getSurfaceHa());
        verger.setNombreArbres(request.getNombreArbres());
        verger.setAnneePlantation(request.getAnneePlantation());

        Verger updated = vergerRepository.save(verger);
        return VergerResponseDTO.fromEntity(updated, totalParcelles);
    }

    public void deleteVerger(String id) {
        Verger verger = findVergerOrThrow(id);
        checkOwnershipOrStaff(verger);

        if (vergerUsageChecker.isInUse(id)) {
            throw new BusinessConflictException("Impossible de supprimer le verger : des récoltes ou tournées y sont actuellement rattachées.");
        }

        // Suppression logique (INT-03)
        verger.setActif(false);
        vergerRepository.save(verger);

        // Suppression logique en cascade des parcelles rattachées
        List<Parcelle> parcelles = parcelleRepository.findByVergerIdAndActifTrue(id);
        for (Parcelle p : parcelles) {
            p.setActif(false);
            parcelleRepository.save(p);
        }
    }

    public List<ParcelleResponseDTO> getParcellesByVergerId(String vergerId) {
        Verger verger = findVergerOrThrow(vergerId);
        checkOwnershipOrStaff(verger);

        return parcelleRepository.findByVergerIdAndActifTrue(vergerId).stream()
                .map(ParcelleResponseDTO::fromEntity)
                .toList();
    }

    public ParcelleResponseDTO createParcelle(String vergerId, ParcelleRequestDTO request) {
        Verger verger = findVergerOrThrow(vergerId);
        checkOwnershipOrStaff(verger);

        // VER-15 : validation que varieteId référence une variété existante et active
        Variete variete = varieteRepository.findById(request.getVarieteId())
                .filter(Variete::isActif)
                .orElseThrow(() -> new ValidationException("La variété spécifiée n'existe pas ou est inactive."));

        if (request.getSurfaceHa() == null || request.getSurfaceHa() <= 0) {
            throw new ValidationException("La surface de la parcelle doit être strictement positive.");
        }

        // VER-07 : somme des surfaces actives <= surface du verger
        double currentSum = computeTotalParcellesSurface(vergerId);
        if (currentSum + request.getSurfaceHa() > verger.getSurfaceHa()) {
            throw new BusinessConflictException("La somme des surfaces des parcelles ("
                    + (currentSum + request.getSurfaceHa()) + " ha) dépasse la surface totale du verger ("
                    + verger.getSurfaceHa() + " ha).");
        }

        Parcelle parcelle = new Parcelle(
                null,
                vergerId,
                request.getNom().trim(),
                variete.getId(),
                request.getSurfaceHa(),
                true
        );

        Parcelle saved = parcelleRepository.save(parcelle);
        return ParcelleResponseDTO.fromEntity(saved);
    }

    public ParcelleResponseDTO updateParcelle(String vergerId, String parcelleId, ParcelleRequestDTO request) {
        Verger verger = findVergerOrThrow(vergerId);
        checkOwnershipOrStaff(verger);

        Parcelle parcelle = parcelleRepository.findByIdAndVergerIdAndActifTrue(parcelleId, vergerId)
                .orElseThrow(() -> new ResourceNotFoundException("Parcelle introuvable."));

        // Optimistic locking
        if (request.getVersion() != null && !request.getVersion().equals(parcelle.getVersion())) {
            throw new BusinessConflictException("Conflit de version : la parcelle a été modifiée par un autre utilisateur.");
        }

        // VER-15 : Variété valide et active
        Variete variete = varieteRepository.findById(request.getVarieteId())
                .filter(Variete::isActif)
                .orElseThrow(() -> new ValidationException("La variété spécifiée n'existe pas ou est inactive."));

        if (request.getSurfaceHa() == null || request.getSurfaceHa() <= 0) {
            throw new ValidationException("La surface de la parcelle doit être strictement positive.");
        }

        // VER-07 : somme des surfaces des autres parcelles actives + nouvelle surface <= surface du verger
        double otherParcellesSum = parcelleRepository.findByVergerIdAndActifTrue(vergerId).stream()
                .filter(p -> !p.getId().equals(parcelleId))
                .mapToDouble(Parcelle::getSurfaceHa)
                .sum();

        if (otherParcellesSum + request.getSurfaceHa() > verger.getSurfaceHa()) {
            throw new BusinessConflictException("La somme des surfaces des parcelles ("
                    + (otherParcellesSum + request.getSurfaceHa()) + " ha) dépasse la surface totale du verger ("
                    + verger.getSurfaceHa() + " ha).");
        }

        parcelle.setNom(request.getNom().trim());
        parcelle.setVarieteId(variete.getId());
        parcelle.setSurfaceHa(request.getSurfaceHa());

        Parcelle updated = parcelleRepository.save(parcelle);
        return ParcelleResponseDTO.fromEntity(updated);
    }

    public void deleteParcelle(String vergerId, String parcelleId) {
        Verger verger = findVergerOrThrow(vergerId);
        checkOwnershipOrStaff(verger);

        Parcelle parcelle = parcelleRepository.findByIdAndVergerIdAndActifTrue(parcelleId, vergerId)
                .orElseThrow(() -> new ResourceNotFoundException("Parcelle introuvable."));

        // Suppression logique (INT-03)
        parcelle.setActif(false);
        parcelleRepository.save(parcelle);
    }

    private Verger findVergerOrThrow(String id) {
        return vergerRepository.findByIdAndActifTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Verger introuvable."));
    }

    /**
     * Contrôle d'accès IDOR (AUT-01) :
     * Si l'utilisateur est AGRICULTEUR et ne possède pas le verger, retourne 404 RESOURCE_NOT_FOUND
     * pour ne pas révéler l'existence de la ressource. Les rôles staff (ADMINISTRATEUR, RESPONSABLE_COLLECTE)
     * ont accès en lecture/gestion.
     */
    private void checkOwnershipOrStaff(Verger verger) {
        if (isStaff()) {
            return;
        }
        String currentUserId = securityService.getCurrentUserId();
        if (currentUserId == null || !currentUserId.equals(verger.getProprietaireId())) {
            throw new ResourceNotFoundException("Verger introuvable.");
        }
    }

    private boolean isStaff() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return false;
        return auth.getAuthorities().stream().anyMatch(a ->
                "ROLE_ADMINISTRATEUR".equals(a.getAuthority()) ||
                "ROLE_RESPONSABLE_COLLECTE".equals(a.getAuthority())
        );
    }

    private double computeTotalParcellesSurface(String vergerId) {
        return parcelleRepository.findByVergerIdAndActifTrue(vergerId).stream()
                .mapToDouble(Parcelle::getSurfaceHa)
                .sum();
    }

    private void validateSurface(Double surfaceHa) {
        if (surfaceHa == null || surfaceHa <= 0) {
            Map<String, String> errors = new HashMap<>();
            errors.put("surfaceHa", "La surface doit être strictement positive.");
            throw new ValidationException("La surface doit être strictement positive.", errors);
        }
        if (surfaceHa > maxSurfaceHa) {
            Map<String, String> errors = new HashMap<>();
            errors.put("surfaceHa", "La surface dépasse le plafond autorisé de " + maxSurfaceHa + " hectares.");
            throw new ValidationException("La surface dépasse le plafond autorisé de " + maxSurfaceHa + " hectares.", errors);
        }
    }

    private void validateCoordinates(Double latitude, Double longitude) {
        Map<String, String> fieldErrors = new HashMap<>();
        boolean hasFieldError = false;

        if (latitude == null || latitude < -90.0 || latitude > 90.0) {
            fieldErrors.put("latitude", "La latitude doit être comprise entre -90 et 90.");
            hasFieldError = true;
        }
        if (longitude == null || longitude < -180.0 || longitude > 180.0) {
            fieldErrors.put("longitude", "La longitude doit être comprise entre -180 et 180.");
            hasFieldError = true;
        }
        if (hasFieldError) {
            // VER-01 : 400 avec fieldErrors
            throw new ValidationException("Coordonnées GPS hors limites valides.", fieldErrors);
        }

        // VER-02 & VER-03 : Bounding box gouvernorat de Sfax
        boolean inSfax = (latitude >= minLat && latitude <= maxLat && longitude >= minLng && longitude <= maxLng);
        if (!inSfax) {
            // Test d'inversion des coordonnées
            boolean swappedInSfax = (longitude >= minLat && longitude <= maxLat && latitude >= minLng && latitude <= maxLng);
            if (swappedInSfax) {
                // VER-03 : Suggestion explicite de l'inversion
                throw new ValidationException("Coordonnées hors du gouvernorat de Sfax. Attention : avez-vous inversé la latitude et la longitude ?");
            } else {
                // VER-02 : Message clair
                throw new ValidationException("Coordonnées en dehors du gouvernorat de Sfax (bornes autorisées : lat ["
                        + minLat + ", " + maxLat + "], lng [" + minLng + ", " + maxLng + "]).");
            }
        }
    }
}
