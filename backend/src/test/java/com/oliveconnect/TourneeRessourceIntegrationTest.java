package com.oliveconnect;

import com.oliveconnect.config.MongoIndexConfig;
import com.oliveconnect.dto.TourneeRequestDTO;
import com.oliveconnect.dto.TourneeResponseDTO;
import com.oliveconnect.entity.*;
import com.oliveconnect.exception.BusinessConflictException;
import com.oliveconnect.repository.*;
import com.oliveconnect.service.RessourceService;
import com.oliveconnect.service.TourneeService;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.core.index.IndexInfo;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class TourneeRessourceIntegrationTest {

    @Autowired
    private MongoTemplate mongoTemplate;
    @Autowired
    private MongoIndexConfig mongoIndexConfig;
    @Autowired
    private RessourceRepository ressourceRepository;
    @Autowired
    private TourneeRepository tourneeRepository;
    @Autowired
    private RecolteRepository recolteRepository;
    @Autowired
    private VergerRepository vergerRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RessourceService ressourceService;
    @Autowired
    private TourneeService tourneeService;

    private Ressource ouvrier;
    private Ressource tracteur;
    private Ressource benne;
    private Recolte recolteValidee;

    @BeforeEach
    void setUp() {
        tourneeRepository.deleteAll();
        ressourceRepository.deleteAll();
        recolteRepository.deleteAll();
        mongoIndexConfig.initIndexes();

        User logUser = userRepository.findByEmail("logistique.test@sfax.tn").orElseGet(() -> {
            User u = new User(null, "Logistique Test", "logistique.test@sfax.tn", "hash", Set.of(Role.RESPONSABLE_LOGISTIQUE), true);
            return userRepository.save(u);
        });

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        logUser.getId(),
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_RESPONSABLE_LOGISTIQUE"))
                )
        );

        ouvrier = ressourceRepository.save(new Ressource(null, "OUV-INT-01", TypeRessource.OUVRIER, "Ouvrier Int", StatutRessource.DISPONIBLE, null, true));
        tracteur = ressourceRepository.save(new Ressource(null, "TR-INT-04", TypeRessource.TRACTEUR, "New Holland T5", StatutRessource.DISPONIBLE, null, true));
        benne = ressourceRepository.save(new Ressource(null, "BN-INT-01", TypeRessource.BENNE, "Benne 2T", StatutRessource.DISPONIBLE, 2000.0, true));

        Verger verger = vergerRepository.findByCode("VRG-INT-J06").orElseGet(() ->
                vergerRepository.save(new Verger(null, "VRG-INT-J06", "Verger J06", logUser.getId(), "El Amra",
                        new GeoJsonPoint(10.85, 34.97), 4.0, SourcePosition.AUTO, 10.0, 150, 2010, true)));

        Recolte r = new Recolte();
        r.setReference("REC-INT-J06-1");
        r.setProprietaireId(logUser.getId());
        r.setVergerId(verger.getId());
        r.setDateSouhaitee(LocalDate.now().plusDays(2));
        r.setQuantiteEstimeeKg(1800.0);
        r.setStatut(StatutRecolte.VALIDEE);
        recolteValidee = recolteRepository.save(r);
    }

    @Test
    @DisplayName("MongoDB : Index ressources/tournees créés et requête chevauchement utilise IXSCAN (pas COLLSCAN)")
    void testIndexesAndExplainUsesIxScan() {
        List<IndexInfo> resIdx = mongoTemplate.indexOps(Ressource.class).getIndexInfo();
        assertTrue(resIdx.stream().anyMatch(i -> "idx_ressources_code_unique".equals(i.getName()) && i.isUnique()));
        assertTrue(resIdx.stream().anyMatch(i -> "idx_ressources_type_actif_statut".equals(i.getName())));

        List<IndexInfo> touIdx = mongoTemplate.indexOps(Tournee.class).getIndexInfo();
        assertTrue(touIdx.stream().anyMatch(i -> "idx_tournees_code_unique".equals(i.getName()) && i.isUnique()));
        assertTrue(touIdx.stream().anyMatch(i -> "idx_tournees_tracteur_debut_fin".equals(i.getName())));
        assertTrue(touIdx.stream().anyMatch(i -> "idx_tournees_benne_debut_fin".equals(i.getName())));
        assertTrue(touIdx.stream().anyMatch(i -> "idx_tournees_ouvrier_debut_fin".equals(i.getName())));
        assertTrue(touIdx.stream().anyMatch(i -> "idx_tournees_statut_debut".equals(i.getName())));

        // Explain sur requête de chevauchement tracteurIds + debut + fin
        Document explainCmd = new Document("explain", new Document("find", "tournees")
                .append("filter", new Document("tracteurIds", tracteur.getId())
                        .append("debut", new Document("$lt", Instant.parse("2026-12-01T14:00:00Z")))
                        .append("fin", new Document("$gt", Instant.parse("2026-12-01T08:00:00Z")))))
                .append("verbosity", "queryPlanner");

        Document explainResult = mongoTemplate.getDb().runCommand(explainCmd);
        String explainJson = explainResult.toJson();
        assertTrue(explainJson.contains("IXSCAN"), "La requête de chevauchement doit utiliser un IXSCAN : " + explainJson);
    }

    @Test
    @DisplayName("Intégration : Création tournée -> Planification -> Récolte passe en PLANIFIEE -> Blocage suppression ressource (INT-01)")
    void testNominalPlanificationAndInt01BlockInMongo() {
        TourneeRequestDTO req = new TourneeRequestDTO();
        req.setCode("T-INT-001");
        req.setDebut(Instant.parse("2026-12-01T08:00:00Z"));
        req.setFin(Instant.parse("2026-12-01T12:00:00Z"));
        req.setRecolteIds(List.of(recolteValidee.getId()));
        req.setOuvrierIds(List.of(ouvrier.getId()));
        req.setTracteurIds(List.of(tracteur.getId()));
        req.setBenneIds(List.of(benne.getId()));

        TourneeResponseDTO created = tourneeService.createTournee(req);
        assertEquals(StatutTournee.BROUILLON, created.getStatut());

        TourneeResponseDTO planned = tourneeService.planifierTournee(created.getId(), created.getVersion());
        assertEquals(StatutTournee.PLANIFIEE, planned.getStatut());

        // La récolte est passée en PLANIFIEE avec historique via RecolteService
        Recolte updatedRecolte = recolteRepository.findById(recolteValidee.getId()).orElseThrow();
        assertEquals(StatutRecolte.PLANIFIEE, updatedRecolte.getStatut());
        assertFalse(updatedRecolte.getHistorique().isEmpty());

        // INT-01 : impossible de supprimer le tracteur tant que T-INT-001 est PLANIFIEE
        BusinessConflictException ex = assertThrows(BusinessConflictException.class,
                () -> ressourceService.deleteRessource(tracteur.getId()));
        assertTrue(ex.getMessage().contains("T-INT-001"));
    }
}
