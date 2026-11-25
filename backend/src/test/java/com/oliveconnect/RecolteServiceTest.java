package com.oliveconnect;

import com.oliveconnect.dto.RecolteRequestDTO;
import com.oliveconnect.dto.RecolteResponseDTO;
import com.oliveconnect.dto.RecolteTransitionRequestDTO;
import com.oliveconnect.entity.Recolte;
import com.oliveconnect.entity.SourcePosition;
import com.oliveconnect.entity.StatutRecolte;
import com.oliveconnect.entity.Verger;
import com.oliveconnect.exception.BusinessConflictException;
import com.oliveconnect.exception.ResourceNotFoundException;
import com.oliveconnect.exception.ValidationException;
import com.oliveconnect.repository.RecolteRepository;
import com.oliveconnect.repository.VergerRepository;
import com.oliveconnect.security.SecurityService;
import com.oliveconnect.service.RecolteService;
import com.oliveconnect.service.RecolteVergerUsageChecker;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecolteServiceTest {

    @Mock
    private RecolteRepository recolteRepository;

    @Mock
    private VergerRepository vergerRepository;

    @Mock
    private SecurityService securityService;

    @Mock
    private MongoTemplate mongoTemplate;

    private RecolteService recolteService;
    private RecolteVergerUsageChecker usageChecker;

    // 2026-11-24T23:30:00Z = 2026-11-25T00:30:00+01:00 (Africa/Tunis)
    private static final Instant FIXED_INSTANT = Instant.parse("2026-11-24T23:30:00Z");

    @BeforeEach
    void setUp() {
        Clock fixedClock = Clock.fixed(FIXED_INSTANT, java.time.ZoneOffset.UTC);
        recolteService = new RecolteService(
                recolteRepository,
                vergerRepository,
                securityService,
                mongoTemplate,
                fixedClock,
                500000.0
        );
        usageChecker = new RecolteVergerUsageChecker(recolteRepository);
        setAuth("farmer-1", "ROLE_AGRICULTEUR");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void setAuth(String userId, String role) {
        var auth = new UsernamePasswordAuthenticationToken(
                userId,
                null,
                List.of(new SimpleGrantedAuthority(role))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
        lenient().when(securityService.getCurrentUserId()).thenReturn(userId);
    }

    private Verger activeOwnOrchard() {
        return new Verger("vrg-1", "VRG-SFX-001", "Verger El Hencha", "farmer-1", "El Hencha",
                new GeoJsonPoint(10.74, 35.13), 4.0, SourcePosition.AUTO, 12.0, 180, 2010, true);
    }

    private RecolteRequestDTO validRequest() {
        RecolteRequestDTO req = new RecolteRequestDTO();
        req.setVergerId("vrg-1");
        req.setDateSouhaitee(LocalDate.of(2026, 11, 25)); // Aujourd'hui à Tunis
        req.setQuantiteEstimeeKg(1250.5);
        req.setUrgent(false);
        return req;
    }

    @Test
    @DisplayName("Création nominale : déduit proprietaireId du principal, génère référence et historique")
    void testCreateRecolteNominal() {
        Verger orchard = activeOwnOrchard();
        when(vergerRepository.findById("vrg-1")).thenReturn(Optional.of(orchard));
        when(recolteRepository.existsByVergerIdAndDateSouhaiteeAndStatutIn(any(), any(), any())).thenReturn(false);
        when(recolteRepository.insert(any(Recolte.class))).thenAnswer(inv -> {
            Recolte r = inv.getArgument(0);
            r.setId("rec-1");
            return r;
        });

        RecolteService.CreationResult result = recolteService.createRecolte(validRequest(), null);

        assertThat(result.idempotentReplay()).isFalse();
        RecolteResponseDTO dto = result.recolte();
        assertThat(dto.getId()).isEqualTo("rec-1");
        assertThat(dto.getProprietaireId()).isEqualTo("farmer-1");
        assertThat(dto.getVergerNom()).isEqualTo("Verger El Hencha");
        assertThat(dto.getStatut()).isEqualTo(StatutRecolte.DEMANDEE);
        assertThat(dto.isAnnulable()).isTrue();
        assertThat(dto.getHistorique()).hasSize(1);
        assertThat(dto.getHistorique().get(0).getNouveauStatut()).isEqualTo(StatutRecolte.DEMANDEE);
        assertThat(dto.getHistorique().get(0).getHorodatage()).isEqualTo(FIXED_INSTANT);
    }

    @Test
    @DisplayName("Sécurité IDOR : verger d'un autre agriculteur refusé en 404")
    void testCreateRecolteForeignOrchardReturns404() {
        Verger foreign = activeOwnOrchard();
        foreign.setProprietaireId("farmer-2");
        when(vergerRepository.findById("vrg-1")).thenReturn(Optional.of(foreign));

        assertThatThrownBy(() -> recolteService.createRecolte(validRequest(), null))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(recolteRepository, never()).insert(any(Recolte.class));
    }

    @Test
    @DisplayName("Verger inexistant : refusé en 404")
    void testCreateRecolteMissingOrchardReturns404() {
        when(vergerRepository.findById("vrg-missing")).thenReturn(Optional.empty());
        RecolteRequestDTO req = validRequest();
        req.setVergerId("vrg-missing");

        assertThatThrownBy(() -> recolteService.createRecolte(req, null))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Verger inactif appartenant à l'agriculteur : refusé en 400")
    void testCreateRecolteInactiveOrchardReturns400() {
        Verger inactive = activeOwnOrchard();
        inactive.setActif(false);
        when(vergerRepository.findById("vrg-1")).thenReturn(Optional.of(inactive));

        assertThatThrownBy(() -> recolteService.createRecolte(validRequest(), null))
                .isInstanceOf(ValidationException.class)
                .satisfies(ex -> assertThat(((ValidationException) ex).getFieldErrors()).containsKey("vergerId"));
    }

    @Test
    @DisplayName("Validation date Tunis : 2026-11-24 refusée alors qu'il est 23:30 UTC (minuit passé à Tunis)")
    void testCreateRecoltePastDateTunisRejected() {
        when(vergerRepository.findById("vrg-1")).thenReturn(Optional.of(activeOwnOrchard()));
        RecolteRequestDTO req = validRequest();
        req.setDateSouhaitee(LocalDate.of(2026, 11, 24));

        assertThatThrownBy(() -> recolteService.createRecolte(req, null))
                .isInstanceOf(ValidationException.class)
                .satisfies(ex -> assertThat(((ValidationException) ex).getFieldErrors()).containsKey("dateSouhaitee"));
    }

    @Test
    @DisplayName("Validation quantité et urgence : valeurs invalides rejetées en 400")
    void testCreateRecolteInvalidQuantityAndUrgency() {
        when(vergerRepository.findById("vrg-1")).thenReturn(Optional.of(activeOwnOrchard()));

        RecolteRequestDTO badQty = validRequest();
        badQty.setQuantiteEstimeeKg(0.0);
        assertThatThrownBy(() -> recolteService.createRecolte(badQty, null))
                .isInstanceOf(ValidationException.class)
                .satisfies(ex -> assertThat(((ValidationException) ex).getFieldErrors()).containsKey("quantiteEstimeeKg"));

        RecolteRequestDTO tooManyDecimals = validRequest();
        tooManyDecimals.setQuantiteEstimeeKg(10.1234);
        assertThatThrownBy(() -> recolteService.createRecolte(tooManyDecimals, null))
                .isInstanceOf(ValidationException.class)
                .satisfies(ex -> assertThat(((ValidationException) ex).getFieldErrors()).containsKey("quantiteEstimeeKg"));

        RecolteRequestDTO urgentNoReason = validRequest();
        urgentNoReason.setUrgent(true);
        urgentNoReason.setMotifUrgence("   ");
        assertThatThrownBy(() -> recolteService.createRecolte(urgentNoReason, null))
                .isInstanceOf(ValidationException.class)
                .satisfies(ex -> assertThat(((ValidationException) ex).getFieldErrors()).containsKey("motifUrgence"));
    }

    @Test
    @DisplayName("Doublon actif (même verger + même date) : refusé en 409")
    void testCreateRecolteActiveDuplicateReturns409() {
        when(vergerRepository.findById("vrg-1")).thenReturn(Optional.of(activeOwnOrchard()));
        when(recolteRepository.existsByVergerIdAndDateSouhaiteeAndStatutIn(any(), any(), any())).thenReturn(true);

        assertThatThrownBy(() -> recolteService.createRecolte(validRequest(), null))
                .isInstanceOf(BusinessConflictException.class);
    }

    @Test
    @DisplayName("Idempotence : même clé + même payload renvoie l'enregistrement existant ; payload différent renvoie 409")
    void testCreateRecolteIdempotencySemantics() {
        Verger orchard = activeOwnOrchard();
        when(vergerRepository.findById("vrg-1")).thenReturn(Optional.of(orchard));
        when(recolteRepository.existsByVergerIdAndDateSouhaiteeAndStatutIn(any(), any(), any())).thenReturn(false);

        final Recolte[] stored = new Recolte[1];
        when(recolteRepository.findByProprietaireIdAndIdempotencyKey("farmer-1", "key-abc"))
                .thenAnswer(inv -> Optional.ofNullable(stored[0]));
        when(recolteRepository.insert(any(Recolte.class))).thenAnswer(inv -> {
            Recolte r = inv.getArgument(0);
            r.setId("rec-idem-1");
            stored[0] = r;
            return r;
        });

        RecolteService.CreationResult first = recolteService.createRecolte(validRequest(), "key-abc");
        assertThat(first.idempotentReplay()).isFalse();

        RecolteService.CreationResult replay = recolteService.createRecolte(validRequest(), "key-abc");
        assertThat(replay.idempotentReplay()).isTrue();
        assertThat(replay.recolte().getId()).isEqualTo("rec-idem-1");
        verify(recolteRepository, times(1)).insert(any(Recolte.class));

        RecolteRequestDTO differentPayload = validRequest();
        differentPayload.setQuantiteEstimeeKg(999.0);
        assertThatThrownBy(() -> recolteService.createRecolte(differentPayload, "key-abc"))
                .isInstanceOf(BusinessConflictException.class);
    }

    @Test
    @DisplayName("Annulation autorisée depuis DEMANDEE et refusée après PLANIFIEE ou EN_COURS")
    void testCancelRulesAndForbiddenTransitions() {
        Recolte pending = new Recolte();
        pending.setId("rec-1");
        pending.setProprietaireId("farmer-1");
        pending.setVergerId("vrg-1");
        pending.setStatut(StatutRecolte.DEMANDEE);
        pending.setVersion(0L);

        when(recolteRepository.findById("rec-1")).thenReturn(Optional.of(pending));
        when(recolteRepository.save(any(Recolte.class))).thenAnswer(inv -> inv.getArgument(0));
        when(vergerRepository.findById("vrg-1")).thenReturn(Optional.of(activeOwnOrchard()));

        RecolteTransitionRequestDTO cancelBody = new RecolteTransitionRequestDTO();
        cancelBody.setMotif("Pluie prévue");
        cancelBody.setVersion(0L);

        RecolteResponseDTO cancelled = recolteService.annulerRecolte("rec-1", cancelBody);
        assertThat(cancelled.getStatut()).isEqualTo(StatutRecolte.ANNULEE);
        assertThat(cancelled.isAnnulable()).isFalse();
        assertThat(cancelled.getHistorique()).hasSize(1);
        assertThat(cancelled.getHistorique().get(0).getAncienStatut()).isEqualTo(StatutRecolte.DEMANDEE);
        assertThat(cancelled.getHistorique().get(0).getNouveauStatut()).isEqualTo(StatutRecolte.ANNULEE);

        // Annulation d'une demande déjà planifiée interdite (409)
        Recolte planned = new Recolte();
        planned.setId("rec-2");
        planned.setProprietaireId("farmer-1");
        planned.setStatut(StatutRecolte.PLANIFIEE);
        when(recolteRepository.findById("rec-2")).thenReturn(Optional.of(planned));

        assertThatThrownBy(() -> recolteService.annulerRecolte("rec-2", null))
                .isInstanceOf(BusinessConflictException.class);

        // Un agriculteur ne peut pas valider sa propre demande (403)
        pending.setStatut(StatutRecolte.DEMANDEE);
        RecolteTransitionRequestDTO validateReq = new RecolteTransitionRequestDTO();
        validateReq.setStatutCible(StatutRecolte.VALIDEE);
        assertThatThrownBy(() -> recolteService.transitionStatut("rec-1", validateReq))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("Isolation : un agriculteur ne peut pas lire ou annuler la demande d'un autre (404)")
    void testFarmerIsolationOnGetAndCancel() {
        Recolte other = new Recolte();
        other.setId("rec-other");
        other.setProprietaireId("farmer-2");
        other.setStatut(StatutRecolte.DEMANDEE);
        when(recolteRepository.findById("rec-other")).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> recolteService.getRecolteById("rec-other"))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> recolteService.annulerRecolte("rec-other", null))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("RecolteVergerUsageChecker délègue au repository sur les statuts actifs")
    void testVergerUsageChecker() {
        when(recolteRepository.existsByVergerIdAndStatutIn("vrg-1", RecolteService.ACTIVE_STATUSES)).thenReturn(true);
        when(recolteRepository.existsByVergerIdAndStatutIn("vrg-2", RecolteService.ACTIVE_STATUSES)).thenReturn(false);

        assertThat(usageChecker.isInUse("vrg-1")).isTrue();
        assertThat(usageChecker.isInUse("vrg-2")).isFalse();
        assertThat(usageChecker.isInUse(null)).isFalse();
    }
}
