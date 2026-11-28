package com.oliveconnect.service;

import com.oliveconnect.dto.TourneeAffectationDTO;
import com.oliveconnect.dto.TourneeRequestDTO;
import com.oliveconnect.dto.TourneeResponseDTO;
import com.oliveconnect.entity.*;
import com.oliveconnect.exception.BusinessConflictException;
import com.oliveconnect.exception.ValidationException;
import com.oliveconnect.repository.RecolteRepository;
import com.oliveconnect.repository.RessourceRepository;
import com.oliveconnect.repository.TourneeRepository;
import com.oliveconnect.repository.UserRepository;
import com.oliveconnect.security.SecurityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TourneeServiceTest {

    @Mock
    private TourneeRepository tourneeRepository;
    @Mock
    private RessourceRepository ressourceRepository;
    @Mock
    private RecolteRepository recolteRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private RecolteService recolteService;
    @Mock
    private SecurityService securityService;

    private TourneeService tourneeService;

    private Ressource ouvrier1;
    private Ressource tracteur1;
    private Ressource benne1;
    private Recolte recolte1;

    @BeforeEach
    void setUp() {
        tourneeService = new TourneeService(
                tourneeRepository,
                ressourceRepository,
                recolteRepository,
                userRepository,
                recolteService,
                securityService,
                12L,
                0L
        );

        ouvrier1 = new Ressource("ouv-1", "OUV-001", TypeRessource.OUVRIER, "Ouvrier 1", StatutRessource.DISPONIBLE, null, true);
        tracteur1 = new Ressource("tr-04", "TR-04", TypeRessource.TRACTEUR, "New Holland", StatutRessource.DISPONIBLE, null, true);
        benne1 = new Ressource("bn-01", "BN-01", TypeRessource.BENNE, "Benne 2T", StatutRessource.DISPONIBLE, 2000.0, true);

        recolte1 = new Recolte();
        recolte1.setId("rec-1");
        recolte1.setReference("REC-2026-001");
        recolte1.setStatut(StatutRecolte.VALIDEE);
        recolte1.setQuantiteEstimeeKg(1500.0);
    }

    @Test
    @DisplayName("nominal_tourneeValide_estCreeeEtPlanifiee")
    void nominal_tourneeValide_estCreeeEtPlanifiee() {
        Instant debut = Instant.parse("2026-12-01T08:00:00Z");
        Instant fin = Instant.parse("2026-12-01T12:00:00Z");

        TourneeRequestDTO req = new TourneeRequestDTO();
        req.setCode("T-001");
        req.setDebut(debut);
        req.setFin(fin);
        req.setRecolteIds(List.of("rec-1"));
        req.setOuvrierIds(List.of("ouv-1"));
        req.setTracteurIds(List.of("tr-04"));
        req.setBenneIds(List.of("bn-01"));

        when(tourneeRepository.existsByCode("T-001")).thenReturn(false);
        when(recolteRepository.findById("rec-1")).thenReturn(Optional.of(recolte1));
        when(ressourceRepository.findAllByIdInAndActifTrue(List.of("ouv-1"))).thenReturn(List.of(ouvrier1));
        when(ressourceRepository.findAllByIdInAndActifTrue(List.of("tr-04"))).thenReturn(List.of(tracteur1));
        when(ressourceRepository.findAllByIdInAndActifTrue(List.of("bn-01"))).thenReturn(List.of(benne1));
        when(tourneeRepository.save(any(Tournee.class))).thenAnswer(inv -> {
            Tournee t = inv.getArgument(0);
            t.setId("tou-1");
            t.setVersion(0L);
            return t;
        });

        TourneeResponseDTO created = tourneeService.createTournee(req);
        assertEquals(StatutTournee.BROUILLON, created.getStatut());
        assertEquals(1500.0, created.getQuantiteEstimeeKg());
        assertEquals(2000.0, created.getCapaciteBennesTotaleKg());

        // Planification
        Tournee stored = new Tournee();
        stored.setId("tou-1");
        stored.setCode("T-001");
        stored.setDebut(debut);
        stored.setFin(fin);
        stored.setStatut(StatutTournee.BROUILLON);
        stored.setRecolteIds(List.of("rec-1"));
        stored.setOuvrierIds(List.of("ouv-1"));
        stored.setTracteurIds(List.of("tr-04"));
        stored.setBenneIds(List.of("bn-01"));
        stored.setVersion(0L);

        when(tourneeRepository.findByIdAndActifTrue("tou-1")).thenReturn(Optional.of(stored));
        when(tourneeRepository.findById("tou-1")).thenReturn(Optional.of(stored));

        TourneeResponseDTO planned = tourneeService.planifierTournee("tou-1", 0L);
        assertEquals(StatutTournee.PLANIFIEE, planned.getStatut());
        verify(recolteService).planifierDepuisTournee("rec-1", "T-001");
    }

    @Test
    @DisplayName("TOU01_tracteurDejaAffecte_retourne409")
    void TOU01_tracteurDejaAffecte_retourne409() {
        Instant debutA = Instant.parse("2026-12-01T08:00:00Z");
        Instant finA = Instant.parse("2026-12-01T12:00:00Z");

        Tournee existing = new Tournee();
        existing.setId("tou-existing");
        existing.setCode("T-001");
        existing.setDebut(Instant.parse("2026-12-01T10:00:00Z"));
        existing.setFin(Instant.parse("2026-12-01T14:00:00Z"));
        existing.setStatut(StatutTournee.PLANIFIEE);
        existing.setTracteurIds(List.of("tr-04"));

        TourneeRequestDTO req = new TourneeRequestDTO();
        req.setCode("T-002");
        req.setDebut(debutA);
        req.setFin(finA);
        req.setTracteurIds(List.of("tr-04"));

        when(tourneeRepository.existsByCode("T-002")).thenReturn(false);
        when(ressourceRepository.findAllByIdInAndActifTrue(List.of("tr-04"))).thenReturn(List.of(tracteur1));
        when(tourneeRepository.findByActifTrueAndStatutInAndTracteurIdsIn(
                StatutTournee.statutsActifsChevauchement(), List.of("tr-04")))
                .thenReturn(List.of(existing));

        BusinessConflictException ex = assertThrows(BusinessConflictException.class,
                () -> tourneeService.createTournee(req));
        assertTrue(ex.getMessage().contains("T-001"));
        assertTrue(ex.getMessage().contains("TR-04"));
    }

    @Test
    @DisplayName("TOU02_tourneesAdjacentes_pasDeConflit")
    void TOU02_tourneesAdjacentes_pasDeConflit() {
        Instant debutB = Instant.parse("2026-12-01T12:00:00Z");
        Instant finB = Instant.parse("2026-12-01T16:00:00Z");

        Tournee existing = new Tournee();
        existing.setId("tou-existing");
        existing.setCode("T-001");
        existing.setDebut(Instant.parse("2026-12-01T08:00:00Z"));
        existing.setFin(Instant.parse("2026-12-01T12:00:00Z"));
        existing.setStatut(StatutTournee.PLANIFIEE);
        existing.setTracteurIds(List.of("tr-04"));

        TourneeRequestDTO req = new TourneeRequestDTO();
        req.setCode("T-002");
        req.setDebut(debutB);
        req.setFin(finB);
        req.setTracteurIds(List.of("tr-04"));

        when(tourneeRepository.existsByCode("T-002")).thenReturn(false);
        when(ressourceRepository.findAllByIdInAndActifTrue(List.of("tr-04"))).thenReturn(List.of(tracteur1));
        when(tourneeRepository.findByActifTrueAndStatutInAndTracteurIdsIn(
                StatutTournee.statutsActifsChevauchement(), List.of("tr-04")))
                .thenReturn(List.of(existing));
        when(tourneeRepository.save(any(Tournee.class))).thenAnswer(inv -> {
            Tournee t = inv.getArgument(0);
            t.setId("tou-2");
            return t;
        });

        TourneeResponseDTO res = tourneeService.createTournee(req);
        assertEquals("T-002", res.getCode());
    }

    @Test
    @DisplayName("TOU03_modificationConcurrente_retourne409")
    void TOU03_modificationConcurrente_retourne409() {
        Tournee stored = new Tournee();
        stored.setId("tou-1");
        stored.setCode("T-001");
        stored.setStatut(StatutTournee.BROUILLON);
        stored.setVersion(2L);

        TourneeAffectationDTO dto = new TourneeAffectationDTO();
        dto.setVersion(1L); // version obsolète

        when(tourneeRepository.findByIdAndActifTrue("tou-1")).thenReturn(Optional.of(stored));

        BusinessConflictException ex = assertThrows(BusinessConflictException.class,
                () -> tourneeService.updateAffectations("tou-1", dto));
        assertTrue(ex.getMessage().contains("Conflit de version"));
    }

    @Test
    @DisplayName("TOU04_finAvantDebut_retourne400")
    void TOU04_finAvantDebut_retourne400() {
        TourneeRequestDTO req = new TourneeRequestDTO();
        req.setCode("T-ERR");
        req.setDebut(Instant.parse("2026-12-01T14:00:00Z"));
        req.setFin(Instant.parse("2026-12-01T10:00:00Z"));

        when(tourneeRepository.existsByCode("T-ERR")).thenReturn(false);
        assertThrows(ValidationException.class, () -> tourneeService.createTournee(req));
    }

    @Test
    @DisplayName("TOU04_dureeNulle_retourne400")
    void TOU04_dureeNulle_retourne400() {
        Instant same = Instant.parse("2026-12-01T10:00:00Z");
        TourneeRequestDTO req = new TourneeRequestDTO();
        req.setCode("T-ERR2");
        req.setDebut(same);
        req.setFin(same);

        when(tourneeRepository.existsByCode("T-ERR2")).thenReturn(false);
        assertThrows(ValidationException.class, () -> tourneeService.createTournee(req));
    }

    @Test
    @DisplayName("TOU06_tourneeSansRessource_planificationRefusee")
    void TOU06_tourneeSansRessource_planificationRefusee() {
        Tournee stored = new Tournee();
        stored.setId("tou-1");
        stored.setCode("T-001");
        stored.setDebut(Instant.parse("2026-12-01T08:00:00Z"));
        stored.setFin(Instant.parse("2026-12-01T12:00:00Z"));
        stored.setStatut(StatutTournee.BROUILLON);
        stored.setRecolteIds(List.of("rec-1"));
        stored.setVersion(0L);
        // Manque ouvriers, tracteurs, bennes

        when(tourneeRepository.findByIdAndActifTrue("tou-1")).thenReturn(Optional.of(stored));
        when(tourneeRepository.findById("tou-1")).thenReturn(Optional.of(stored));
        when(recolteRepository.findById("rec-1")).thenReturn(Optional.of(recolte1));

        BusinessConflictException ex = assertThrows(BusinessConflictException.class,
                () -> tourneeService.planifierTournee("tou-1", 0L));
        assertTrue(ex.getMessage().contains("ouvriers"));
        assertTrue(ex.getMessage().contains("tracteurs"));
        assertTrue(ex.getMessage().contains("bennes"));
    }

    @Test
    @DisplayName("TOU08_capaciteBennesInsuffisante_refusee")
    void TOU08_capaciteBennesInsuffisante_refusee() {
        recolte1.setQuantiteEstimeeKg(3500.0); // > 2000 kg benne

        Tournee stored = new Tournee();
        stored.setId("tou-1");
        stored.setCode("T-001");
        stored.setDebut(Instant.parse("2026-12-01T08:00:00Z"));
        stored.setFin(Instant.parse("2026-12-01T12:00:00Z"));
        stored.setStatut(StatutTournee.BROUILLON);
        stored.setRecolteIds(List.of("rec-1"));
        stored.setOuvrierIds(List.of("ouv-1"));
        stored.setTracteurIds(List.of("tr-04"));
        stored.setBenneIds(List.of("bn-01"));
        stored.setVersion(0L);

        when(tourneeRepository.findByIdAndActifTrue("tou-1")).thenReturn(Optional.of(stored));
        when(tourneeRepository.findById("tou-1")).thenReturn(Optional.of(stored));
        when(recolteRepository.findById("rec-1")).thenReturn(Optional.of(recolte1));
        when(ressourceRepository.findAllByIdInAndActifTrue(List.of("ouv-1"))).thenReturn(List.of(ouvrier1));
        when(ressourceRepository.findAllByIdInAndActifTrue(List.of("tr-04"))).thenReturn(List.of(tracteur1));
        when(ressourceRepository.findAllByIdInAndActifTrue(List.of("bn-01"))).thenReturn(List.of(benne1));

        BusinessConflictException ex = assertThrows(BusinessConflictException.class,
                () -> tourneeService.planifierTournee("tou-1", 0L));
        assertTrue(ex.getMessage().contains("Capacité des bennes insuffisante"));
        assertTrue(ex.getMessage().contains("1500.0 kg"));
    }

    @Test
    @DisplayName("TOU14_recolteDejaDansTourneeActive_refusee")
    void TOU14_recolteDejaDansTourneeActive_refusee() {
        Tournee autre = new Tournee();
        autre.setId("tou-other");
        autre.setCode("T-001");
        autre.setStatut(StatutTournee.BROUILLON);
        autre.setRecolteIds(List.of("rec-1"));

        TourneeRequestDTO req = new TourneeRequestDTO();
        req.setCode("T-002");
        req.setDebut(Instant.parse("2026-12-02T08:00:00Z"));
        req.setFin(Instant.parse("2026-12-02T12:00:00Z"));
        req.setRecolteIds(List.of("rec-1"));

        when(tourneeRepository.existsByCode("T-002")).thenReturn(false);
        when(recolteRepository.findById("rec-1")).thenReturn(Optional.of(recolte1));
        when(tourneeRepository.findByActifTrueAndStatutInAndRecolteIdsIn(
                StatutTournee.statutsActifsChevauchement(), List.of("rec-1")))
                .thenReturn(List.of(autre));

        BusinessConflictException ex = assertThrows(BusinessConflictException.class,
                () -> tourneeService.createTournee(req));
        assertTrue(ex.getMessage().contains("T-001"));
    }
}
