package com.oliveconnect;

import com.oliveconnect.dto.PageResponse;
import com.oliveconnect.dto.RecolteRequestDTO;
import com.oliveconnect.dto.RecolteResponseDTO;
import com.oliveconnect.dto.RecolteTransitionRequestDTO;
import com.oliveconnect.entity.*;
import com.oliveconnect.exception.BusinessConflictException;
import com.oliveconnect.exception.ResourceNotFoundException;
import com.oliveconnect.repository.RecolteRepository;
import com.oliveconnect.repository.UserRepository;
import com.oliveconnect.repository.VergerRepository;
import com.oliveconnect.security.CustomUserDetails;
import com.oliveconnect.service.RecolteService;
import com.oliveconnect.service.VergerService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class RecolteMongoIntegrationTest {

    @Autowired
    private RecolteService recolteService;

    @Autowired
    private VergerService vergerService;

    @Autowired
    private RecolteRepository recolteRepository;

    @Autowired
    private VergerRepository vergerRepository;

    @Autowired
    private UserRepository userRepository;

    private User farmerA;
    private User farmerB;
    private User staff;
    private Verger vergerA;
    private Verger vergerB;

    @BeforeEach
    void setUp() {
        recolteRepository.deleteAll();
        vergerRepository.deleteAll();
        userRepository.deleteAll();

        farmerA = userRepository.save(new User(null, "Ali Sfax", "ali.int@sfax.tn", "hash", Set.of(Role.AGRICULTEUR), true));
        farmerB = userRepository.save(new User(null, "Fatma Sfax", "fatma.int@sfax.tn", "hash", Set.of(Role.AGRICULTEUR), true));
        staff = userRepository.save(new User(null, "Sami Collecte", "sami.int@sfax.tn", "hash", Set.of(Role.RESPONSABLE_COLLECTE), true));

        vergerA = vergerRepository.save(new Verger(
                null, "VRG-INT-A", "Verger Ali Int", farmerA.getId(), "El Hencha",
                new GeoJsonPoint(10.74, 35.13), 4.0, SourcePosition.AUTO, 10.0, 150, 2010, true
        ));
        vergerB = vergerRepository.save(new Verger(
                null, "VRG-INT-B", "Verger Fatma Int", farmerB.getId(), "El Amra",
                new GeoJsonPoint(10.85, 34.97), 4.0, SourcePosition.AUTO, 12.0, 200, 2012, true
        ));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        recolteRepository.deleteAll();
        vergerRepository.deleteAll();
        userRepository.deleteAll();
    }

    private void authenticateAs(User user) {
        CustomUserDetails details = new CustomUserDetails(user);
        var auth = new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    @DisplayName("MongoDB : isolation entre deux agriculteurs, historique persisté et usage checker réel")
    void testIsolationHistoryAndUsageChecker() {
        LocalDate targetDate = LocalDate.now(RecolteService.TUNIS_ZONE).plusDays(5);

        // 1. Farmer A crée une demande
        authenticateAs(farmerA);
        RecolteRequestDTO reqA = new RecolteRequestDTO();
        reqA.setVergerId(vergerA.getId());
        reqA.setDateSouhaitee(targetDate);
        reqA.setQuantiteEstimeeKg(2150.25);
        reqA.setUrgent(true);
        reqA.setMotifUrgence("Récolte mûre");

        RecolteResponseDTO created = recolteService.createRecolte(reqA, "idem-int-1").recolte();
        assertThat(created.getProprietaireId()).isEqualTo(farmerA.getId());
        assertThat(created.getStatut()).isEqualTo(StatutRecolte.DEMANDEE);

        // 2. Suppression logique du verger bloquée par RecolteVergerUsageChecker (409)
        assertThatThrownBy(() -> vergerService.deleteVerger(vergerA.getId()))
                .isInstanceOf(BusinessConflictException.class);

        // 3. Farmer B ne voit pas la demande de A et reçoit 404 en accès direct
        authenticateAs(farmerB);
        PageResponse<RecolteResponseDTO> listB = recolteService.getRecoltes(0, 20, null, null, null, null, null, null);
        assertThat(listB.getItems()).isEmpty();
        assertThatThrownBy(() -> recolteService.getRecolteById(created.getId()))
                .isInstanceOf(ResourceNotFoundException.class);

        // 4. Staff valide puis Farmer A annule -> historique réellement persisté en MongoDB
        authenticateAs(staff);
        RecolteTransitionRequestDTO validateDto = new RecolteTransitionRequestDTO();
        validateDto.setStatutCible(StatutRecolte.VALIDEE);
        validateDto.setMotif("Créneau confirmé");
        RecolteResponseDTO validated = recolteService.transitionStatut(created.getId(), validateDto);
        assertThat(validated.getStatut()).isEqualTo(StatutRecolte.VALIDEE);

        authenticateAs(farmerA);
        RecolteTransitionRequestDTO cancelDto = new RecolteTransitionRequestDTO();
        cancelDto.setMotif("Report météo");
        cancelDto.setVersion(validated.getVersion());
        recolteService.annulerRecolte(created.getId(), cancelDto);

        Recolte reloaded = recolteRepository.findById(created.getId()).orElseThrow();
        assertThat(reloaded.getStatut()).isEqualTo(StatutRecolte.ANNULEE);
        assertThat(reloaded.getHistorique()).hasSize(3);
        assertThat(reloaded.getHistorique())
                .extracting(HistoriqueStatutRecolte::getNouveauStatut)
                .containsExactly(StatutRecolte.DEMANDEE, StatutRecolte.VALIDEE, StatutRecolte.ANNULEE);

        // 5. Une nouvelle demande peut être créée sur le même verger et la même date après annulation
        RecolteResponseDTO recreated = recolteService.createRecolte(reqA, "idem-int-2").recolte();
        assertThat(recreated.getId()).isNotEqualTo(created.getId());
        recolteService.annulerRecolte(recreated.getId(), null);

        // 6. Après annulation de toutes les demandes actives, le verger peut être archivé (actif = false)
        vergerService.deleteVerger(vergerA.getId());
        assertThat(vergerRepository.findByIdAndActifTrue(vergerA.getId())).isEmpty();
    }

    @Test
    @DisplayName("MongoDB Concurrence : requêtes simultanées avec même Idempotency-Key produisent un seul document")
    void testConcurrentIdempotentRequests() throws Exception {
        LocalDate targetDate = LocalDate.now(RecolteService.TUNIS_ZONE).plusDays(6);
        int threads = 6;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch startGate = new CountDownLatch(1);
        List<Future<RecolteService.CreationResult>> futures = new ArrayList<>();

        for (int i = 0; i < threads; i++) {
            futures.add(pool.submit(() -> {
                authenticateAs(farmerA);
                try {
                    startGate.await(5, TimeUnit.SECONDS);
                    RecolteRequestDTO req = new RecolteRequestDTO();
                    req.setVergerId(vergerA.getId());
                    req.setDateSouhaitee(targetDate);
                    req.setQuantiteEstimeeKg(1500.0);
                    return recolteService.createRecolte(req, "concurrent-idem-key");
                } finally {
                    SecurityContextHolder.clearContext();
                }
            }));
        }

        startGate.countDown();
        List<String> ids = new ArrayList<>();
        for (Future<RecolteService.CreationResult> f : futures) {
            ids.add(f.get(10, TimeUnit.SECONDS).recolte().getId());
        }
        pool.shutdownNow();

        assertThat(ids).hasSize(threads);
        assertThat(ids.stream().distinct().count()).isEqualTo(1);
        assertThat(recolteRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("MongoDB Concurrence : doublons simultanés sans clé d'idempotence -> un seul succès, conflits 409")
    void testConcurrentDuplicateWithoutIdempotencyKey() throws Exception {
        LocalDate targetDate = LocalDate.now(RecolteService.TUNIS_ZONE).plusDays(9);
        int threads = 5;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch startGate = new CountDownLatch(1);
        List<Future<String>> futures = new ArrayList<>();

        for (int i = 0; i < threads; i++) {
            futures.add(pool.submit(() -> {
                authenticateAs(farmerB);
                try {
                    startGate.await(5, TimeUnit.SECONDS);
                    RecolteRequestDTO req = new RecolteRequestDTO();
                    req.setVergerId(vergerB.getId());
                    req.setDateSouhaitee(targetDate);
                    req.setQuantiteEstimeeKg(2000.0);
                    try {
                        return "OK:" + recolteService.createRecolte(req, null).recolte().getId();
                    } catch (BusinessConflictException conflict) {
                        return "CONFLICT";
                    }
                } finally {
                    SecurityContextHolder.clearContext();
                }
            }));
        }

        startGate.countDown();
        List<String> outcomes = new ArrayList<>();
        for (Future<String> f : futures) {
            outcomes.add(f.get(10, TimeUnit.SECONDS));
        }
        pool.shutdownNow();

        assertThat(outcomes.stream().filter(o -> o.startsWith("OK:")).count()).isEqualTo(1);
        assertThat(outcomes.stream().filter("CONFLICT"::equals).count()).isEqualTo(threads - 1);
        assertThat(recolteRepository.count()).isEqualTo(1);
    }
}
