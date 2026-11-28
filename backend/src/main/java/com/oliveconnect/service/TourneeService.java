package com.oliveconnect.service;

import com.oliveconnect.dto.PageResponse;
import com.oliveconnect.dto.TourneeAffectationDTO;
import com.oliveconnect.dto.TourneeRequestDTO;
import com.oliveconnect.dto.TourneeResponseDTO;
import com.oliveconnect.entity.Recolte;
import com.oliveconnect.entity.Ressource;
import com.oliveconnect.entity.StatutRecolte;
import com.oliveconnect.entity.StatutRessource;
import com.oliveconnect.entity.StatutTournee;
import com.oliveconnect.entity.Tournee;
import com.oliveconnect.entity.TypeRessource;
import com.oliveconnect.entity.User;
import com.oliveconnect.exception.BusinessConflictException;
import com.oliveconnect.exception.ResourceNotFoundException;
import com.oliveconnect.exception.ValidationException;
import com.oliveconnect.repository.RecolteRepository;
import com.oliveconnect.repository.RessourceRepository;
import com.oliveconnect.repository.TourneeRepository;
import com.oliveconnect.repository.UserRepository;
import com.oliveconnect.security.SecurityService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class TourneeService {

    private final TourneeRepository tourneeRepository;
    private final RessourceRepository ressourceRepository;
    private final RecolteRepository recolteRepository;
    private final UserRepository userRepository;
    private final RecolteService recolteService;
    private final SecurityService securityService;

    private final long maxDureeHeures;
    private final long margeTransfertMinutes;

    public TourneeService(TourneeRepository tourneeRepository,
                          RessourceRepository ressourceRepository,
                          RecolteRepository recolteRepository,
                          UserRepository userRepository,
                          RecolteService recolteService,
                          SecurityService securityService,
                          @Value("${oliveconnect.tournee.max-duree-heures:12}") long maxDureeHeures,
                          @Value("${oliveconnect.tournee.marge-transfert-minutes:0}") long margeTransfertMinutes) {
        this.tourneeRepository = tourneeRepository;
        this.ressourceRepository = ressourceRepository;
        this.recolteRepository = recolteRepository;
        this.userRepository = userRepository;
        this.recolteService = recolteService;
        this.securityService = securityService;
        this.maxDureeHeures = maxDureeHeures;
        this.margeTransfertMinutes = margeTransfertMinutes;
    }

    public PageResponse<TourneeResponseDTO> getTournees(Instant debut, Instant fin, int page, int size) {
        if (debut != null && fin != null && fin.isBefore(debut)) {
            throw new ValidationException(
                    "La fenêtre de dates est invalide : la fin doit être postérieure au début.",
                    Map.of("fin", "La date de fin doit être postérieure au début.")
            );
        }

        int boundedSize = Math.min(Math.max(size, 1), 100);
        int boundedPage = Math.max(page, 0);
        Pageable pageable = PageRequest.of(boundedPage, boundedSize, Sort.by("debut").ascending());

        Page<Tournee> result;
        boolean chefEquipeOnly = isChefEquipeOnly();
        String currentUserId = securityService.getCurrentUserId();

        if (debut != null && fin != null) {
            result = chefEquipeOnly
                    ? tourneeRepository.findByActifTrueAndChefEquipeIdAndDebutLessThanEqualAndFinGreaterThanEqual(
                    currentUserId, fin, debut, pageable)
                    : tourneeRepository.findByActifTrueAndDebutLessThanEqualAndFinGreaterThanEqual(
                    fin, debut, pageable);
        } else {
            result = chefEquipeOnly
                    ? tourneeRepository.findByActifTrueAndChefEquipeId(currentUserId, pageable)
                    : tourneeRepository.findByActifTrue(pageable);
        }

        List<TourneeResponseDTO> items = result.getContent().stream()
                .map(t -> TourneeResponseDTO.fromEntity(t, computeCapaciteBennes(t.getBenneIds())))
                .toList();

        return new PageResponse<>(items, result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages());
    }

    public TourneeResponseDTO getTourneeById(String id) {
        Tournee tournee = findActiveOrThrow(id);
        if (isChefEquipeOnly()) {
            String currentUserId = securityService.getCurrentUserId();
            if (currentUserId == null || !currentUserId.equals(tournee.getChefEquipeId())) {
                throw new AccessDeniedException("Vous ne pouvez consulter que vos propres tournées.");
            }
        }
        return TourneeResponseDTO.fromEntity(tournee, computeCapaciteBennes(tournee.getBenneIds()));
    }

    public TourneeResponseDTO createTournee(TourneeRequestDTO request) {
        String cleanCode = request.getCode().trim().toUpperCase(Locale.ROOT);
        if (tourneeRepository.existsByCode(cleanCode)) {
            throw new BusinessConflictException("Une tournée avec le code " + cleanCode + " existe déjà.");
        }

        validateDates(request.getDebut(), request.getFin());

        List<String> recolteIds = deduplicate(request.getRecolteIds());
        List<String> ouvrierIds = deduplicate(request.getOuvrierIds());
        List<String> tracteurIds = deduplicate(request.getTracteurIds());
        List<String> benneIds = deduplicate(request.getBenneIds());

        validateChefEquipe(request.getChefEquipeId());
        double quantiteEstimeeKg = validateAndSumRecoltes(recolteIds, null);
        validateRessourcesTypedAndAvailable(ouvrierIds, TypeRessource.OUVRIER);
        validateRessourcesTypedAndAvailable(tracteurIds, TypeRessource.TRACTEUR);
        double capaciteBennesKg = validateRessourcesTypedAndAvailable(benneIds, TypeRessource.BENNE);

        checkChevauchements(null, request.getDebut(), request.getFin(), tracteurIds, benneIds, ouvrierIds);

        Tournee tournee = new Tournee();
        tournee.setCode(cleanCode);
        tournee.setDebut(request.getDebut());
        tournee.setFin(request.getFin());
        tournee.setStatut(StatutTournee.BROUILLON);
        tournee.setChefEquipeId(normalizeNullableId(request.getChefEquipeId()));
        tournee.setRecolteIds(recolteIds);
        tournee.setOuvrierIds(ouvrierIds);
        tournee.setTracteurIds(tracteurIds);
        tournee.setBenneIds(benneIds);
        tournee.setQuantiteEstimeeKg(quantiteEstimeeKg);
        tournee.setActif(true);

        Tournee saved = tourneeRepository.save(tournee);
        return TourneeResponseDTO.fromEntity(saved, capaciteBennesKg);
    }

    public TourneeResponseDTO updateAffectations(String id, TourneeAffectationDTO request) {
        Tournee tournee = findActiveOrThrow(id);

        if (request.getVersion() != null && !request.getVersion().equals(tournee.getVersion())) {
            throw new BusinessConflictException("Conflit de version : la tournée a été modifiée par un autre utilisateur. Rechargez les données.");
        }

        if (tournee.getStatut() != StatutTournee.BROUILLON && tournee.getStatut() != StatutTournee.PLANIFIEE) {
            throw new BusinessConflictException(
                    "Impossible de modifier une tournée au statut " + tournee.getStatut() + " : ses données sont gelées."
            );
        }

        Instant debut = request.getDebut() != null ? request.getDebut() : tournee.getDebut();
        Instant fin = request.getFin() != null ? request.getFin() : tournee.getFin();
        validateDates(debut, fin);

        String chefEquipeId = request.getChefEquipeId() != null
                ? normalizeNullableId(request.getChefEquipeId())
                : tournee.getChefEquipeId();
        validateChefEquipe(chefEquipeId);

        List<String> recolteIds = request.getRecolteIds() != null ? deduplicate(request.getRecolteIds()) : tournee.getRecolteIds();
        List<String> ouvrierIds = request.getOuvrierIds() != null ? deduplicate(request.getOuvrierIds()) : tournee.getOuvrierIds();
        List<String> tracteurIds = request.getTracteurIds() != null ? deduplicate(request.getTracteurIds()) : tournee.getTracteurIds();
        List<String> benneIds = request.getBenneIds() != null ? deduplicate(request.getBenneIds()) : tournee.getBenneIds();

        double quantiteEstimeeKg = validateAndSumRecoltes(recolteIds, tournee.getId());
        validateRessourcesTypedAndAvailable(ouvrierIds, TypeRessource.OUVRIER);
        validateRessourcesTypedAndAvailable(tracteurIds, TypeRessource.TRACTEUR);
        double capaciteBennesKg = validateRessourcesTypedAndAvailable(benneIds, TypeRessource.BENNE);

        checkChevauchements(tournee.getId(), debut, fin, tracteurIds, benneIds, ouvrierIds);

        if (tournee.getStatut() == StatutTournee.PLANIFIEE) {
            validatePlanificationPrerequis(recolteIds, ouvrierIds, tracteurIds, benneIds, quantiteEstimeeKg, capaciteBennesKg);
        }

        tournee.setDebut(debut);
        tournee.setFin(fin);
        tournee.setChefEquipeId(chefEquipeId);
        tournee.setRecolteIds(recolteIds);
        tournee.setOuvrierIds(ouvrierIds);
        tournee.setTracteurIds(tracteurIds);
        tournee.setBenneIds(benneIds);
        tournee.setQuantiteEstimeeKg(quantiteEstimeeKg);

        Tournee saved = tourneeRepository.save(tournee);
        return TourneeResponseDTO.fromEntity(saved, capaciteBennesKg);
    }

    public TourneeResponseDTO planifierTournee(String id, Long expectedVersion) {
        Tournee tournee = findActiveOrThrow(id);

        if (expectedVersion != null && !expectedVersion.equals(tournee.getVersion())) {
            throw new BusinessConflictException("Conflit de version : la tournée a été modifiée par un autre utilisateur. Rechargez les données.");
        }

        if (!tournee.getStatut().peutPasserA(StatutTournee.PLANIFIEE)) {
            throw new BusinessConflictException(
                    "Transition interdite de " + tournee.getStatut() + " vers PLANIFIEE."
            );
        }

        validateDates(tournee.getDebut(), tournee.getFin());
        double quantiteEstimeeKg = validateAndSumRecoltes(tournee.getRecolteIds(), tournee.getId());
        validateRessourcesTypedAndAvailable(tournee.getOuvrierIds(), TypeRessource.OUVRIER);
        validateRessourcesTypedAndAvailable(tournee.getTracteurIds(), TypeRessource.TRACTEUR);
        double capaciteBennesKg = validateRessourcesTypedAndAvailable(tournee.getBenneIds(), TypeRessource.BENNE);

        validatePlanificationPrerequis(
                tournee.getRecolteIds(),
                tournee.getOuvrierIds(),
                tournee.getTracteurIds(),
                tournee.getBenneIds(),
                quantiteEstimeeKg,
                capaciteBennesKg
        );

        checkChevauchements(
                tournee.getId(),
                tournee.getDebut(),
                tournee.getFin(),
                tournee.getTracteurIds(),
                tournee.getBenneIds(),
                tournee.getOuvrierIds()
        );

        // Passage des récoltes VALIDEE -> PLANIFIEE via RecolteService (§7.1)
        for (String recolteId : tournee.getRecolteIds()) {
            recolteService.planifierDepuisTournee(recolteId, tournee.getCode());
        }

        tournee.setStatut(StatutTournee.PLANIFIEE);
        tournee.setQuantiteEstimeeKg(quantiteEstimeeKg);

        Tournee saved = tourneeRepository.save(tournee);
        return TourneeResponseDTO.fromEntity(saved, capaciteBennesKg);
    }

    public TourneeResponseDTO annulerTournee(String id, Long expectedVersion) {
        Tournee tournee = findActiveOrThrow(id);

        if (expectedVersion != null && !expectedVersion.equals(tournee.getVersion())) {
            throw new BusinessConflictException("Conflit de version : la tournée a été modifiée par un autre utilisateur. Rechargez les données.");
        }

        if (!tournee.getStatut().peutPasserA(StatutTournee.ANNULEE)) {
            throw new BusinessConflictException(
                    "Impossible d'annuler une tournée au statut " + tournee.getStatut() + "."
            );
        }

        tournee.setStatut(StatutTournee.ANNULEE);
        Tournee saved = tourneeRepository.save(tournee);
        return TourneeResponseDTO.fromEntity(saved, computeCapaciteBennes(saved.getBenneIds()));
    }

    private void validateDates(Instant debut, Instant fin) {
        if (debut == null || fin == null) {
            throw new ValidationException("Les dates de début et de fin sont obligatoires.");
        }
        if (!fin.isAfter(debut)) {
            throw new ValidationException(
                    "La date de fin doit être strictement postérieure à la date de début.",
                    Map.of("fin", "La fin doit être postérieure au début (durée > 0).")
            );
        }
        Duration duree = Duration.between(debut, fin);
        if (duree.toMinutes() > maxDureeHeures * 60) {
            throw new ValidationException(
                    "La durée d'une tournée ne peut pas dépasser " + maxDureeHeures + " heures.",
                    Map.of("fin", "Durée maximale autorisée : " + maxDureeHeures + " h.")
            );
        }
    }

    private void validateChefEquipe(String chefEquipeId) {
        String cleanId = normalizeNullableId(chefEquipeId);
        if (cleanId == null) return;

        User user = userRepository.findById(cleanId)
                .filter(User::isActive)
                .orElseThrow(() -> new ValidationException(
                        "Le chef d'équipe spécifié est introuvable ou inactif.",
                        Map.of("chefEquipeId", "Chef d'équipe invalide.")
                ));
        if (user.getId() == null) {
            throw new ValidationException("Chef d'équipe invalide.");
        }
    }

    private double validateAndSumRecoltes(List<String> recolteIds, String currentTourneeId) {
        if (recolteIds == null || recolteIds.isEmpty()) {
            return 0.0;
        }
        double totalKg = 0.0;
        for (String recolteId : recolteIds) {
            Recolte recolte = recolteRepository.findById(recolteId)
                    .orElseThrow(() -> new ValidationException(
                            "Récolte introuvable : " + recolteId,
                            Map.of("recolteIds", "Récolte introuvable : " + recolteId)
                    ));

            // Si la récolte n'est pas déjà dans cette tournée planifiée, elle doit être VALIDEE
            boolean alreadyInThisTournee = currentTourneeId != null
                    && tourneeRepository.findById(currentTourneeId)
                    .map(t -> t.getRecolteIds().contains(recolteId) && t.getStatut() == StatutTournee.PLANIFIEE)
                    .orElse(false);

            if (!alreadyInThisTournee && recolte.getStatut() != StatutRecolte.VALIDEE) {
                throw new BusinessConflictException(
                        "La récolte " + recolte.getReference() + " ne peut pas être ajoutée : statut "
                                + recolte.getStatut() + " (statut VALIDEE requis)."
                );
            }

            // TOU-14 : une récolte ne peut appartenir qu'à une seule tournée active
            List<Tournee> conflits = tourneeRepository.findByActifTrueAndStatutInAndRecolteIdsIn(
                    StatutTournee.statutsActifsChevauchement(),
                    List.of(recolteId)
            ).stream().filter(t -> !Objects.equals(t.getId(), currentTourneeId)).toList();

            if (!conflits.isEmpty()) {
                String tourCodes = conflits.stream().map(Tournee::getCode).collect(Collectors.joining(", "));
                throw new BusinessConflictException(
                        "La récolte " + recolte.getReference() + " appartient déjà à une tournée active (" + tourCodes + ")."
                );
            }

            if (recolte.getQuantiteEstimeeKg() != null) {
                totalKg += recolte.getQuantiteEstimeeKg();
            }
        }
        return totalKg;
    }

    private double validateRessourcesTypedAndAvailable(List<String> ids, TypeRessource expectedType) {
        if (ids == null || ids.isEmpty()) {
            return 0.0;
        }
        List<Ressource> ressources = ressourceRepository.findAllByIdInAndActifTrue(ids);
        Map<String, Ressource> byId = ressources.stream()
                .collect(Collectors.toMap(Ressource::getId, Function.identity()));

        double capaciteTotale = 0.0;
        for (String id : ids) {
            Ressource r = byId.get(id);
            if (r == null) {
                throw new ValidationException(
                        "Ressource introuvable ou inactive : " + id,
                        Map.of(expectedType.name().toLowerCase(Locale.ROOT) + "Ids", "Ressource introuvable ou inactive : " + id)
                );
            }
            if (r.getType() != expectedType) {
                throw new ValidationException(
                        "La ressource " + r.getCode() + " est de type " + r.getType() + " (attendu : " + expectedType + ")."
                );
            }
            if (r.getStatut() != StatutRessource.DISPONIBLE) {
                throw new BusinessConflictException(
                        "La ressource " + r.getCode() + " (" + r.getLibelle() + ") n'est pas disponible (statut : " + r.getStatut() + ")."
                );
            }
            if (expectedType == TypeRessource.BENNE && r.getCapaciteKg() != null) {
                capaciteTotale += r.getCapaciteKg();
            }
        }
        return capaciteTotale;
    }

    private void checkChevauchements(String currentTourneeId, Instant debutA, Instant finA,
                                     List<String> tracteurIds, List<String> benneIds, List<String> ouvrierIds) {
        Set<StatutTournee> statutsActifs = StatutTournee.statutsActifsChevauchement();

        if (!tracteurIds.isEmpty()) {
            List<Tournee> candidates = tourneeRepository.findByActifTrueAndStatutInAndTracteurIdsIn(statutsActifs, tracteurIds);
            verifyOverlapForResourceType(currentTourneeId, debutA, finA, candidates, tracteurIds, Tournee::getTracteurIds);
        }
        if (!benneIds.isEmpty()) {
            List<Tournee> candidates = tourneeRepository.findByActifTrueAndStatutInAndBenneIdsIn(statutsActifs, benneIds);
            verifyOverlapForResourceType(currentTourneeId, debutA, finA, candidates, benneIds, Tournee::getBenneIds);
        }
        if (!ouvrierIds.isEmpty()) {
            List<Tournee> candidates = tourneeRepository.findByActifTrueAndStatutInAndOuvrierIdsIn(statutsActifs, ouvrierIds);
            verifyOverlapForResourceType(currentTourneeId, debutA, finA, candidates, ouvrierIds, Tournee::getOuvrierIds);
        }
    }

    private void verifyOverlapForResourceType(String currentTourneeId, Instant debutA, Instant finA,
                                              List<Tournee> candidates, List<String> requestedIds,
                                              Function<Tournee, List<String>> extractor) {
        Duration marge = Duration.ofMinutes(margeTransfertMinutes);
        for (Tournee other : candidates) {
            if (Objects.equals(other.getId(), currentTourneeId)) {
                continue;
            }
            if (overlaps(debutA, finA, other.getDebut(), other.getFin(), marge)) {
                List<String> conflictingIds = extractor.apply(other).stream()
                        .filter(requestedIds::contains)
                        .toList();
                String resourceLabels = ressourceRepository.findAllByIdInAndActifTrue(conflictingIds).stream()
                        .map(r -> r.getCode() + " (" + r.getLibelle() + ")")
                        .collect(Collectors.joining(", "));
                if (resourceLabels.isBlank()) {
                    resourceLabels = String.join(", ", conflictingIds);
                }
                throw new BusinessConflictException(
                        "Conflit de créneau avec la tournée " + other.getCode()
                                + " pour la ressource : " + resourceLabels + "."
                );
            }
        }
    }

    /**
     * Règle TOU-01 & TOU-02 : debutA < finB + marge ET debutB < finA + marge.
     * Si marge == 0 et finA == debutB, pas de conflit.
     */
    private boolean overlaps(Instant debutA, Instant finA, Instant debutB, Instant finB, Duration marge) {
        if (debutA == null || finA == null || debutB == null || finB == null) {
            return false;
        }
        return debutA.isBefore(finB.plus(marge)) && debutB.isBefore(finA.plus(marge));
    }

    private void validatePlanificationPrerequis(List<String> recolteIds, List<String> ouvrierIds,
                                              List<String> tracteurIds, List<String> benneIds,
                                              double quantiteEstimeeKg, double capaciteBennesKg) {
        // TOU-06 : tournée sans récolte, ouvrier, tracteur ou benne refusée à la planification
        List<String> manquants = new ArrayList<>();
        if (recolteIds == null || recolteIds.isEmpty()) manquants.add("récoltes");
        if (ouvrierIds == null || ouvrierIds.isEmpty()) manquants.add("ouvriers");
        if (tracteurIds == null || tracteurIds.isEmpty()) manquants.add("tracteurs");
        if (benneIds == null || benneIds.isEmpty()) manquants.add("bennes");

        if (!manquants.isEmpty()) {
            throw new BusinessConflictException(
                    "Planification refusée : éléments obligatoires manquants (" + String.join(", ", manquants) + ")."
            );
        }

        // TOU-08 : capacité cumulée des bennes >= quantité estimée
        if (quantiteEstimeeKg > capaciteBennesKg) {
            double manquantKg = quantiteEstimeeKg - capaciteBennesKg;
            throw new BusinessConflictException(
                    "Capacité des bennes insuffisante : il manque " + manquantKg
                            + " kg (quantité estimée : " + quantiteEstimeeKg
                            + " kg, capacité totale : " + capaciteBennesKg
                            + " kg). Ajoutez une benne supplémentaire ou scindez la tournée."
            );
        }
    }

    private double computeCapaciteBennes(List<String> benneIds) {
        if (benneIds == null || benneIds.isEmpty()) {
            return 0.0;
        }
        return ressourceRepository.findAllByIdInAndActifTrue(benneIds).stream()
                .filter(r -> r.getCapaciteKg() != null)
                .mapToDouble(Ressource::getCapaciteKg)
                .sum();
    }

    private Tournee findActiveOrThrow(String id) {
        return tourneeRepository.findByIdAndActifTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tournée introuvable."));
    }

    private List<String> deduplicate(List<String> ids) {
        if (ids == null) return new ArrayList<>();
        return new ArrayList<>(new LinkedHashSet<>(ids.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList()));
    }

    private String normalizeNullableId(String id) {
        if (id == null || id.isBlank()) return null;
        return id.trim();
    }

    private boolean isChefEquipeOnly() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return false;
        boolean hasStaff = auth.getAuthorities().stream().anyMatch(a ->
                "ROLE_ADMINISTRATEUR".equals(a.getAuthority())
                        || "ROLE_RESPONSABLE_COLLECTE".equals(a.getAuthority())
                        || "ROLE_RESPONSABLE_LOGISTIQUE".equals(a.getAuthority())
        );
        boolean hasChef = auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_CHEF_EQUIPE".equals(a.getAuthority()));
        return hasChef && !hasStaff;
    }
}
