package com.oliveconnect;

import com.oliveconnect.config.DataSeeder;
import com.oliveconnect.config.MongoIndexConfig;
import com.oliveconnect.dto.RegisterRequest;
import com.oliveconnect.entity.Parcelle;
import com.oliveconnect.entity.Role;
import com.oliveconnect.entity.User;
import com.oliveconnect.entity.Variete;
import com.oliveconnect.entity.Verger;
import com.oliveconnect.exception.DuplicateEmailException;
import com.oliveconnect.repository.ParcelleRepository;
import com.oliveconnect.repository.UserRepository;
import com.oliveconnect.repository.VarieteRepository;
import com.oliveconnect.repository.VergerRepository;
import com.oliveconnect.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.core.index.IndexInfo;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class MongoIndexAndGeoIntegrationTest {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private MongoIndexConfig mongoIndexConfig;

    @Autowired
    private VergerRepository vergerRepository;

    @Autowired
    private ParcelleRepository parcelleRepository;

    @Autowired
    private VarieteRepository varieteRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuthService authService;

    @Autowired
    private DataSeeder dataSeeder;

    @BeforeEach
    void setUp() {
        try {
            mongoTemplate.indexOps(Verger.class).dropIndex("idx_vergers_localisation_2dsphere");
        } catch (Exception ignored) {
        }
        mongoIndexConfig.initIndexes();
    }

    @Test
    @DisplayName("IDX_indexesExist : Vérifie la présence de tous les index requis (unique, 2dsphere, composés)")
    void testIDX_indexesExist() {
        // Users index
        List<IndexInfo> userIndexes = mongoTemplate.indexOps(User.class).getIndexInfo();
        boolean hasUniqueEmail = userIndexes.stream()
                .anyMatch(idx -> idx.isUnique() && idx.getIndexFields().stream()
                        .anyMatch(f -> "email".equals(f.getKey())));
        assertTrue(hasUniqueEmail, "L'index unique sur users.email doit exister");

        // Vergers indexes
        List<IndexInfo> vergerIndexes = mongoTemplate.indexOps(Verger.class).getIndexInfo();
        boolean has2dSphere = vergerIndexes.stream()
                .anyMatch(idx -> idx.getIndexFields().stream()
                        .anyMatch(f -> "localisation".equals(f.getKey())));
        assertTrue(has2dSphere, "L'index 2dsphere sur vergers.localisation doit exister");

        boolean hasUniqueVergerCode = vergerIndexes.stream()
                .anyMatch(idx -> idx.isUnique() && idx.getIndexFields().stream()
                        .anyMatch(f -> "code".equals(f.getKey())));
        assertTrue(hasUniqueVergerCode, "L'index unique sur vergers.code doit exister");

        boolean hasOwnerActifCompound = vergerIndexes.stream()
                .anyMatch(idx -> idx.getIndexFields().size() >= 2 &&
                        idx.getIndexFields().get(0).getKey().equals("proprietaireId") &&
                        idx.getIndexFields().get(1).getKey().equals("actif"));
        assertTrue(hasOwnerActifCompound, "L'index composé {proprietaireId: 1, actif: 1} sur vergers doit exister");

        // Parcelles indexes
        List<IndexInfo> parcelleIndexes = mongoTemplate.indexOps(Parcelle.class).getIndexInfo();
        boolean hasVergerActifCompound = parcelleIndexes.stream()
                .anyMatch(idx -> idx.getIndexFields().size() >= 2 &&
                        idx.getIndexFields().get(0).getKey().equals("vergerId") &&
                        idx.getIndexFields().get(1).getKey().equals("actif"));
        assertTrue(hasVergerActifCompound, "L'index composé {vergerId: 1, actif: 1} sur parcelles doit exister");

        boolean hasVarieteIndex = parcelleIndexes.stream()
                .anyMatch(idx -> idx.getIndexFields().stream()
                        .anyMatch(f -> "varieteId".equals(f.getKey())));
        assertTrue(hasVarieteIndex, "L'index {varieteId: 1} sur parcelles doit exister");

        // Varietes indexes
        List<IndexInfo> varieteIndexes = mongoTemplate.indexOps(Variete.class).getIndexInfo();
        boolean hasUniqueVarieteCode = varieteIndexes.stream()
                .anyMatch(idx -> idx.isUnique() && idx.getIndexFields().stream()
                        .anyMatch(f -> "code".equals(f.getKey())));
        assertTrue(hasUniqueVarieteCode, "L'index unique sur varietes.code doit exister");
    }

    @Test
    @DisplayName("VER05_nearQuery_returnsExpectedVerger : Requête géospatiale réelle $near avec index 2dsphere")
    void testVER05_nearQuery_returnsExpectedVerger() {
        String testCode = "VRG-GEO-TEST-" + System.currentTimeMillis();
        // Sakiet Ezzit coordinates : lng = 10.76, lat = 34.80
        GeoJsonPoint location = new GeoJsonPoint(10.76, 34.80);
        Verger verger = new Verger(null, testCode, "Verger Geo Test", "owner-1", "Sakiet Ezzit",
                location, 5.0, null, 10.0, 150, 2010, true);
        vergerRepository.save(verger);

        // Query near Sakiet Ezzit center within 2 kilometers
        Point searchPoint = new Point(10.76, 34.80);
        Distance searchRadius = new Distance(2.0, Metrics.KILOMETERS);

        List<Verger> foundVergers = vergerRepository.findByLocalisationNear(searchPoint, searchRadius);
        assertNotNull(foundVergers);
        assertFalse(foundVergers.isEmpty(), "Le verger doit être trouvé par la requête 2dsphere near");
        assertTrue(foundVergers.stream().anyMatch(v -> testCode.equals(v.getCode())));

        // Nettoyage
        vergerRepository.delete(verger);
    }

    @Test
    @DisplayName("CPT01_duplicateEmailDifferentCase_rejected : Rejet de la duplication d'e-mail avec casse différente")
    void testCPT01_duplicateEmailDifferentCase_rejected() {
        String baseEmail = "agri.test." + System.currentTimeMillis() + "@sfax.tn";

        RegisterRequest req1 = new RegisterRequest();
        req1.setName("Agriculteur Test 1");
        req1.setEmail(baseEmail.toLowerCase());
        req1.setPassword("Secret123!");
        req1.setRoles(Set.of(Role.AGRICULTEUR));
        authService.register(req1);

        // Tentative d'enregistrement avec casse différente
        RegisterRequest req2 = new RegisterRequest();
        req2.setName("Agriculteur Test 2");
        req2.setEmail(baseEmail.toUpperCase());
        req2.setPassword("Secret123!");
        req2.setRoles(Set.of(Role.AGRICULTEUR));

        assertThrows(DuplicateEmailException.class, () -> authService.register(req2),
                "L'inscription avec la même adresse e-mail dans une casse différente doit être rejetée (CPT-01)");
    }

    @Test
    @DisplayName("INT03_softDeletedVergerHiddenFromList : Un verger supprimé logiquement est masqué des listes")
    void testINT03_softDeletedVergerHiddenFromList() {
        String code = "VRG-SOFT-DEL-" + System.currentTimeMillis();
        Verger verger = new Verger(null, code, "Verger Inactif Test", "owner-soft", "El Hencha",
                new GeoJsonPoint(10.74, 35.13), null, null, 5.0, 50, 2015, false);
        vergerRepository.save(verger);

        assertTrue(vergerRepository.findById(verger.getId()).isPresent(), "Le document existe physiquement");
        assertFalse(vergerRepository.findByIdAndActifTrue(verger.getId()).isPresent(),
                "Le document inactif ne doit pas être trouvé par findByIdAndActifTrue");

        vergerRepository.delete(verger);
    }

    @Test
    @DisplayName("SEED_runTwice_sameCounts : Le seeder est idempotent et produit des décomptes identiques")
    void testSEED_runTwice_sameCounts() {
        // Premier passage du seeder
        dataSeeder.seed();
        long countVarietes1 = varieteRepository.count();
        long countUsers1 = userRepository.count();
        long countVergers1 = vergerRepository.count();
        long countParcelles1 = parcelleRepository.count();

        // Deuxième passage du seeder
        dataSeeder.seed();
        long countVarietes2 = varieteRepository.count();
        long countUsers2 = userRepository.count();
        long countVergers2 = vergerRepository.count();
        long countParcelles2 = parcelleRepository.count();

        assertEquals(countVarietes1, countVarietes2, "Le nombre de variétés doit rester strictement identique après un second run");
        assertEquals(countUsers1, countUsers2, "Le nombre d'utilisateurs doit rester strictement identique après un second run");
        assertEquals(countVergers1, countVergers2, "Le nombre de vergers doit rester strictement identique après un second run");
        assertEquals(countParcelles1, countParcelles2, "Le nombre de parcelles doit rester strictement identique après un second run");
    }
}
