package com.oliveconnect.service;

import com.oliveconnect.dto.*;
import com.oliveconnect.entity.*;
import com.oliveconnect.exception.BusinessConflictException;
import com.oliveconnect.exception.ResourceNotFoundException;
import com.oliveconnect.exception.ValidationException;
import com.oliveconnect.repository.RecolteRepository;
import com.oliveconnect.repository.VergerRepository;
import com.oliveconnect.security.CustomUserDetails;
import com.oliveconnect.security.SecurityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class RecolteService {

    public static final ZoneId TUNIS_ZONE = ZoneId.of("Africa/Tunis");

    public static final Set<StatutRecolte> ACTIVE_STATUSES = EnumSet.of(
            StatutRecolte.DEMANDEE,
            StatutRecolte.VALIDEE,
            StatutRecolte.PLANIFIEE,
            StatutRecolte.EN_COURS
    );

    public static final Set<StatutRecolte> CANCELLABLE_STATUSES = EnumSet.of(
            StatutRecolte.DEMANDEE,
            StatutRecolte.VALIDEE
    );

    private static final int MAX_MOTIF_LENGTH = 500;
    private static final int MAX_IDEMPOTENCY_KEY_LENGTH = 128;

    private final RecolteRepository recolteRepository;
    private final VergerRepository vergerRepository;
    private final SecurityService securityService;
    private final MongoTemplate mongoTemplate;
    private final Clock clock;
    private final double maxQuantiteKg;

    public record CreationResult(RecolteResponseDTO recolte, boolean idempotentReplay) {
    }

    @Autowired
    public RecolteService(
            RecolteRepository recolteRepository,
            VergerRepository vergerRepository,
            SecurityService securityService,
            MongoTemplate mongoTemplate,
            Clock clock,
            @Value("${oliveconnect.recolte.max-quantite-kg:500000.0}") double maxQuantiteKg) {
        this.recolteRepository = recolteRepository;
        this.vergerRepository = vergerRepository;
        this.securityService = securityService;
        this.mongoTemplate = mongoTemplate;
        this.clock = clock;
        this.maxQuantiteKg = maxQuantiteKg;
    }

    public CreationResult createRecolte(RecolteRequestDTO request, String rawIdempotencyKey) {
        String currentUserId = requireCurrentUserId();
        String idempotencyKey = normalizeIdempotencyKey(rawIdempotencyKey);

        String vergerId = request.getVergerId() != null ? request.getVergerId().trim() : "";
        Verger verger = vergerRepository.findById(vergerId)
                .orElseThrow(() -> new ResourceNotFoundException("Verger introuvable."));

        // Anti-IDOR : si le verger appartient à un tiers, retourner 404 sans divulguer son existence.
        if (!currentUserId.equals(verger.getProprietaireId())) {
            throw new ResourceNotFoundException("Verger introuvable.");
        }
        if (!verger.isActif()) {
            throw new ValidationException(
                    "Ce verger est inactif et ne peut pas recevoir de demande de collecte.",
                    Map.of("vergerId", "Sélectionnez un de vos vergers actifs.")
            );
        }

        validateBusinessFields(request);

        String normalizedMotif = request.isUrgent() ? request.getMotifUrgence().trim() : null;
        String requestHash = computeRequestHash(
                verger.getId(),
                request.getDateSouhaitee(),
                request.getQuantiteEstimeeKg(),
                request.isUrgent(),
                normalizedMotif
        );

        if (idempotencyKey != null) {
            Optional<Recolte> existing = recolteRepository.findByProprietaireIdAndIdempotencyKey(currentUserId, idempotencyKey);
            if (existing.isPresent()) {
                return handleIdempotentHit(existing.get(), requestHash, verger);
            }
        }

        if (recolteRepository.existsByVergerIdAndDateSouhaiteeAndStatutIn(
                verger.getId(), request.getDateSouhaitee(), ACTIVE_STATUSES)) {
            throw new BusinessConflictException("Une demande de collecte active existe déjà pour ce verger à cette date.");
        }

        ActorInfo actor = resolveActor(currentUserId);
        Instant now = Instant.now(clock);

        Recolte recolte = new Recolte();
        recolte.setReference(generateReference(request.getDateSouhaitee()));
        recolte.setProprietaireId(currentUserId);
        recolte.setVergerId(verger.getId());
        recolte.setDateSouhaitee(request.getDateSouhaitee());
        recolte.setQuantiteEstimeeKg(request.getQuantiteEstimeeKg());
        recolte.setUrgent(request.isUrgent());
        recolte.setMotifUrgence(normalizedMotif);
        recolte.setStatut(StatutRecolte.DEMANDEE);
        recolte.setIdempotencyKey(idempotencyKey);
        recolte.setRequestHash(requestHash);
        recolte.setHistorique(new ArrayList<>(List.of(
                new HistoriqueStatutRecolte(
                        null,
                        StatutRecolte.DEMANDEE,
                        actor.id(),
                        actor.name(),
                        actor.role(),
                        normalizedMotif,
                        now
                )
        )));

        try {
            Recolte saved = recolteRepository.insert(recolte);
            return new CreationResult(RecolteResponseDTO.fromEntity(saved, verger), false);
        } catch (DuplicateKeyException ex) {
            if (idempotencyKey != null) {
                Optional<Recolte> existing = recolteRepository.findByProprietaireIdAndIdempotencyKey(currentUserId, idempotencyKey);
                if (existing.isPresent()) {
                    return handleIdempotentHit(existing.get(), requestHash, verger);
                }
            }
            throw new BusinessConflictException("Une demande de collecte active existe déjà pour ce verger à cette date.");
        }
    }

    public PageResponse<RecolteResponseDTO> getRecoltes(
            int page,
            int size,
            String q,
            StatutRecolte statut,
            Boolean urgent,
            String vergerId,
            LocalDate dateDebut,
            LocalDate dateFin) {
        int boundedSize = Math.min(Math.max(size, 1), 100);
        int boundedPage = Math.max(page, 0);
        Pageable pageable = PageRequest.of(
                boundedPage,
                boundedSize,
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))
        );

        String currentUserId = requireCurrentUserId();
        boolean staff = isStaff();

        if (dateDebut != null && dateFin != null && dateDebut.isAfter(dateFin)) {
            throw new ValidationException(
                    "La date de début doit être antérieure ou égale à la date de fin.",
                    Map.of("dateDebut", "Intervalle de dates invalide.")
            );
        }

        List<Criteria> criteriaList = new ArrayList<>();
        if (!staff) {
            criteriaList.add(Criteria.where("proprietaireId").is(currentUserId));
        }
        if (statut != null) {
            criteriaList.add(Criteria.where("statut").is(statut));
        }
        if (urgent != null) {
            criteriaList.add(Criteria.where("urgent").is(urgent));
        }
        if (vergerId != null && !vergerId.isBlank()) {
            criteriaList.add(Criteria.where("vergerId").is(vergerId.trim()));
        }
        if (dateDebut != null && dateFin != null) {
            criteriaList.add(Criteria.where("dateSouhaitee").gte(dateDebut).lte(dateFin));
        } else if (dateDebut != null) {
            criteriaList.add(Criteria.where("dateSouhaitee").gte(dateDebut));
        } else if (dateFin != null) {
            criteriaList.add(Criteria.where("dateSouhaitee").lte(dateFin));
        }

        if (q != null && !q.isBlank()) {
            String trimmedQ = q.trim();
            String normalizedQ = normalizeSearchText(trimmedQ);
            List<Verger> candidateVergers = staff
                    ? vergerRepository.findAll()
                    : vergerRepository.findByProprietaireId(currentUserId);
            List<String> matchingVergerIds = candidateVergers.stream()
                    .filter(v -> normalizeSearchText(v.getNom()).contains(normalizedQ)
                            || normalizeSearchText(v.getCode()).contains(normalizedQ))
                    .map(Verger::getId)
                    .toList();

            String quoted = Pattern.quote(trimmedQ);
            List<Criteria> orCriteria = new ArrayList<>();
            orCriteria.add(Criteria.where("reference").regex(quoted, "i"));
            orCriteria.add(Criteria.where("_id").is(trimmedQ));
            if (!matchingVergerIds.isEmpty()) {
                orCriteria.add(Criteria.where("vergerId").in(matchingVergerIds));
            }
            criteriaList.add(new Criteria().orOperator(orCriteria));
        }

        Query query = new Query();
        if (!criteriaList.isEmpty()) {
            query.addCriteria(new Criteria().andOperator(criteriaList));
        }

        long totalElements = mongoTemplate.count(query, Recolte.class);
        query.with(pageable);
        List<Recolte> items = mongoTemplate.find(query, Recolte.class);

        Map<String, Verger> vergerMap = loadVergersMap(items);
        List<RecolteResponseDTO> dtos = items.stream()
                .map(r -> RecolteResponseDTO.fromEntity(r, vergerMap.get(r.getVergerId())))
                .toList();

        int totalPages = (int) Math.ceil((double) totalElements / boundedSize);
        return new PageResponse<>(dtos, boundedPage, boundedSize, totalElements, totalPages);
    }

    public RecolteResponseDTO getRecolteById(String id) {
        Recolte recolte = findRecolteAccessibleOrThrow(id);
        Verger verger = vergerRepository.findById(recolte.getVergerId()).orElse(null);
        return RecolteResponseDTO.fromEntity(recolte, verger);
    }

    public RecolteResponseDTO annulerRecolte(String id, RecolteTransitionRequestDTO request) {
        Long version = request != null ? request.getVersion() : null;
        String motif = request != null ? request.getMotif() : null;
        return applyTransition(id, StatutRecolte.ANNULEE, version, motif);
    }

    public RecolteResponseDTO transitionStatut(String id, RecolteTransitionRequestDTO request) {
        if (request == null || request.getStatutCible() == null) {
            throw new ValidationException(
                    "Le statut cible est obligatoire.",
                    Map.of("statutCible", "Indiquez le statut cible de la transition.")
            );
        }
        return applyTransition(id, request.getStatutCible(), request.getVersion(), request.getMotif());
    }

    private RecolteResponseDTO applyTransition(String id, StatutRecolte targetStatus, Long expectedVersion, String rawMotif) {
        String currentUserId = requireCurrentUserId();
        boolean staff = isStaff();
        Recolte recolte = findRecolteAccessibleOrThrow(id);

        if (expectedVersion != null && !expectedVersion.equals(recolte.getVersion())) {
            throw new BusinessConflictException("Conflit de version : la demande a été modifiée par un autre utilisateur.");
        }

        String normalizedMotif = rawMotif != null && !rawMotif.isBlank() ? rawMotif.trim() : null;
        if (normalizedMotif != null && normalizedMotif.length() > MAX_MOTIF_LENGTH) {
            throw new ValidationException(
                    "Le motif ne doit pas dépasser " + MAX_MOTIF_LENGTH + " caractères.",
                    Map.of("motif", "Limitez le motif à " + MAX_MOTIF_LENGTH + " caractères.")
            );
        }

        StatutRecolte currentStatus = recolte.getStatut();
        validateTransition(currentStatus, targetStatus, staff, normalizedMotif);

        ActorInfo actor = resolveActor(currentUserId);
        Instant now = Instant.now(clock);

        recolte.setStatut(targetStatus);
        List<HistoriqueStatutRecolte> history = new ArrayList<>(
                recolte.getHistorique() != null ? recolte.getHistorique() : List.of()
        );
        history.add(new HistoriqueStatutRecolte(
                currentStatus,
                targetStatus,
                actor.id(),
                actor.name(),
                actor.role(),
                normalizedMotif,
                now
        ));
        recolte.setHistorique(history);

        Recolte saved = recolteRepository.save(recolte);
        Verger verger = vergerRepository.findById(saved.getVergerId()).orElse(null);
        return RecolteResponseDTO.fromEntity(saved, verger);
    }

    private void validateTransition(StatutRecolte current, StatutRecolte target, boolean staff, String motif) {
        if (target == StatutRecolte.ANNULEE) {
            if (!CANCELLABLE_STATUSES.contains(current)) {
                throw new BusinessConflictException(
                        "Impossible d'annuler une demande au statut " + current + "."
                );
            }
            return;
        }

        if (current == StatutRecolte.DEMANDEE && (target == StatutRecolte.VALIDEE || target == StatutRecolte.REFUSEE)) {
            if (!staff) {
                throw new AccessDeniedException("Seule l'équipe de collecte peut valider ou refuser une demande.");
            }
            if (target == StatutRecolte.REFUSEE && motif == null) {
                throw new ValidationException(
                        "Un motif est obligatoire pour refuser une demande de collecte.",
                        Map.of("motif", "Précisez le motif du refus.")
                );
            }
            return;
        }

        throw new BusinessConflictException(
                "Transition interdite de " + current + " vers " + target + "."
        );
    }

    private Recolte findRecolteAccessibleOrThrow(String id) {
        Recolte recolte = recolteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Demande de collecte introuvable."));

        if (!isStaff()) {
            String currentUserId = requireCurrentUserId();
            if (!currentUserId.equals(recolte.getProprietaireId())) {
                // Masquage anti-énumération IDOR cohérent avec VergerService (AUT-01)
                throw new ResourceNotFoundException("Demande de collecte introuvable.");
            }
        }
        return recolte;
    }

    private CreationResult handleIdempotentHit(Recolte existing, String requestHash, Verger verger) {
        if (!Objects.equals(existing.getRequestHash(), requestHash)) {
            throw new BusinessConflictException(
                    "Cette clé d'idempotence a déjà été utilisée avec des paramètres différents."
            );
        }
        return new CreationResult(RecolteResponseDTO.fromEntity(existing, verger), true);
    }

    private void validateBusinessFields(RecolteRequestDTO request) {
        Map<String, String> fieldErrors = new HashMap<>();
        LocalDate todayTunis = LocalDate.now(clock.withZone(TUNIS_ZONE));

        if (request.getDateSouhaitee() == null || request.getDateSouhaitee().isBefore(todayTunis)) {
            fieldErrors.put("dateSouhaitee", "Choisissez une date valide, aujourd'hui ou plus tard (heure de Tunis).");
        }

        Double qty = request.getQuantiteEstimeeKg();
        if (qty != null) {
            if (!Double.isFinite(qty) || qty <= 0.0 || qty > maxQuantiteKg || hasMoreThanThreeDecimals(qty)) {
                fieldErrors.put(
                        "quantiteEstimeeKg",
                        "Indiquez un poids positif en kg (3 décimales maximum, plafond " + (long) maxQuantiteKg + " kg)."
                );
            }
        }

        String motif = request.getMotifUrgence();
        if (request.isUrgent() && (motif == null || motif.trim().isEmpty())) {
            fieldErrors.put("motifUrgence", "Précisez le motif de l'urgence.");
        } else if (motif != null && motif.trim().length() > MAX_MOTIF_LENGTH) {
            fieldErrors.put("motifUrgence", "Limitez le motif à " + MAX_MOTIF_LENGTH + " caractères.");
        }

        if (!fieldErrors.isEmpty()) {
            throw new ValidationException("Vérifiez les informations de la demande de collecte.", fieldErrors);
        }
    }

    private boolean hasMoreThanThreeDecimals(double value) {
        BigDecimal bd = BigDecimal.valueOf(value).stripTrailingZeros();
        return Math.max(0, bd.scale()) > 3;
    }

    private String normalizeIdempotencyKey(String rawKey) {
        if (rawKey == null) return null;
        String trimmed = rawKey.trim();
        if (trimmed.isEmpty() || trimmed.length() > MAX_IDEMPOTENCY_KEY_LENGTH) {
            throw new ValidationException(
                    "En-tête Idempotency-Key invalide.",
                    Map.of("Idempotency-Key", "La clé d'idempotence doit comporter entre 1 et 128 caractères.")
            );
        }
        return trimmed;
    }

    private String computeRequestHash(
            String vergerId,
            LocalDate dateSouhaitee,
            Double quantiteEstimeeKg,
            boolean urgent,
            String normalizedMotif) {
        String qtyPart = quantiteEstimeeKg == null
                ? "null"
                : BigDecimal.valueOf(quantiteEstimeeKg).stripTrailingZeros().toPlainString();
        String canonical = vergerId + "|" + dateSouhaitee + "|" + qtyPart + "|" + urgent + "|" + (normalizedMotif == null ? "" : normalizedMotif);
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(canonical.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponible", e);
        }
    }

    private String generateReference(LocalDate dateSouhaitee) {
        return "REC-" + dateSouhaitee.getYear() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
    }

    private Map<String, Verger> loadVergersMap(List<Recolte> recoltes) {
        Set<String> vergerIds = recoltes.stream()
                .map(Recolte::getVergerId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (vergerIds.isEmpty()) return Map.of();

        return vergerRepository.findAllById(vergerIds).stream()
                .collect(Collectors.toMap(Verger::getId, Function.identity(), (a, b) -> a));
    }

    private String requireCurrentUserId() {
        String id = securityService.getCurrentUserId();
        if (id == null || id.isBlank()) {
            throw new BadCredentialsException("Authentification requise.");
        }
        return id;
    }

    private boolean isStaff() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return false;
        return auth.getAuthorities().stream().anyMatch(a ->
                "ROLE_ADMINISTRATEUR".equals(a.getAuthority())
                        || "ROLE_RESPONSABLE_COLLECTE".equals(a.getAuthority())
        );
    }

    private ActorInfo resolveActor(String fallbackId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return new ActorInfo(fallbackId, fallbackId, "AGRICULTEUR");
        }
        String name = auth.getName();
        String id = fallbackId;
        if (auth.getPrincipal() instanceof CustomUserDetails details) {
            id = details.getId();
            name = details.getUser().getName();
        }
        String role = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(a -> a.startsWith("ROLE_") ? a.substring(5) : a)
                .sorted()
                .findFirst()
                .orElse("AGRICULTEUR");
        return new ActorInfo(id, name, role);
    }

    private String normalizeSearchText(String text) {
        if (text == null) return "";
        return Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
    }

    private record ActorInfo(String id, String name, String role) {
    }
}
