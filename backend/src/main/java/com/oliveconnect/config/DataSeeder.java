package com.oliveconnect.config;

import com.oliveconnect.entity.*;
import com.oliveconnect.repository.ParcelleRepository;
import com.oliveconnect.repository.RecolteRepository;
import com.oliveconnect.repository.RessourceRepository;
import com.oliveconnect.repository.TourneeRepository;
import com.oliveconnect.repository.UserRepository;
import com.oliveconnect.repository.VarieteRepository;
import com.oliveconnect.repository.VergerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final VarieteRepository varieteRepository;
    private final UserRepository userRepository;
    private final VergerRepository vergerRepository;
    private final ParcelleRepository parcelleRepository;
    private final RecolteRepository recolteRepository;
    private final RessourceRepository ressourceRepository;
    private final TourneeRepository tourneeRepository;
    private final PasswordEncoder passwordEncoder;
    private final String defaultPassword;

    @Value("${oliveconnect.seed.enabled:true}")
    private boolean seedEnabled;

    public DataSeeder(VarieteRepository varieteRepository,
                      UserRepository userRepository,
                      VergerRepository vergerRepository,
                      ParcelleRepository parcelleRepository,
                      RecolteRepository recolteRepository,
                      RessourceRepository ressourceRepository,
                      TourneeRepository tourneeRepository,
                      PasswordEncoder passwordEncoder,
                      @Value("${oliveconnect.seed.default-password:Password123!}") String defaultPassword) {
        this.varieteRepository = varieteRepository;
        this.userRepository = userRepository;
        this.vergerRepository = vergerRepository;
        this.parcelleRepository = parcelleRepository;
        this.recolteRepository = recolteRepository;
        this.ressourceRepository = ressourceRepository;
        this.tourneeRepository = tourneeRepository;
        this.passwordEncoder = passwordEncoder;
        this.defaultPassword = defaultPassword;
    }

    @Override
    public void run(String... args) {
        if (seedEnabled) {
            seed();
        } else {
            log.info("DataSeeder désactivé au démarrage (oliveconnect.seed.enabled=false).");
        }
    }

    public synchronized void seed() {
        log.info("Démarrage du peuplement initial des données Sfax (DataSeeder idempotent)...");

        // 1. Variétés d'olives tunisiennes
        Variete chemlali = upsertVariete("CHEMLALI", "Chemlali de Sfax", 0.65, 2.2, true);
        Variete chetoui = upsertVariete("CHETOUI", "Chétoui", 0.70, 1.8, true);
        Variete zalmati = upsertVariete("ZALMATI", "Zalmati de Zarzis/Sfax", 0.60, 2.0, true);
        Variete oueslati = upsertVariete("OUESLATI", "Oueslati", 0.68, 1.9, true);

        // 2. Utilisateurs (1 Admin, 1 Responsable Collecte, 1 Logistique, 1 Chef d'équipe, 3 Agriculteurs)
        User admin = upsertUser("Mohamed Admin", "admin@oliveconnect.tn", Set.of(Role.ADMINISTRATEUR));
        User collecte = upsertUser("Sami Responsable", "collecte@oliveconnect.tn", Set.of(Role.RESPONSABLE_COLLECTE));
        upsertUser("Nizar Logistique", "logistique@oliveconnect.tn", Set.of(Role.RESPONSABLE_LOGISTIQUE));
        User chef = upsertUser("Tarek Chef Equipe", "chef@oliveconnect.tn", Set.of(Role.CHEF_EQUIPE));
        User ali = upsertUser("Ali Ben M'Barek", "ali.benmbarek@sfax.tn", Set.of(Role.AGRICULTEUR));
        User fatma = upsertUser("Fatma Triki", "fatma.triki@sfax.tn", Set.of(Role.AGRICULTEUR));
        User habib = upsertUser("Habib Kammoun", "habib.kammoun@sfax.tn", Set.of(Role.AGRICULTEUR));

        // 3. Vergers & Parcelles
        // Verger 1 (Ali) : El Hencha (~35.13, 10.74), multi-variétés VER-09
        Verger vrg1 = upsertVerger("VRG-SFX-001", "Verger El Hencha Nord", ali.getId(), "El Hencha",
                new GeoJsonPoint(10.74, 35.13), 4.5, SourcePosition.AUTO, 15.5, 250, 2005, true);
        upsertParcelle(vrg1.getId(), "Parcelle Nord A", chemlali.getId(), 8.0, true);
        upsertParcelle(vrg1.getId(), "Parcelle Nord B", zalmati.getId(), 7.5, true);

        // Verger 2 (Ali) : Mahrès (~34.53, 10.50), position manuelle et précision nulle (VER-04), surface décimale
        Verger vrg2 = upsertVerger("VRG-SFX-002", "Verger Mahrès Sud", ali.getId(), "Mahrès",
                new GeoJsonPoint(10.50, 34.53), null, SourcePosition.MANUEL, 8.25, 120, 2012, true);
        upsertParcelle(vrg2.getId(), "Parcelle Mahrès Centre", chemlali.getId(), 8.25, true);

        // Verger 3 (Fatma) : El Amra (~34.97, 10.85), multi-parcelles
        Verger vrg3 = upsertVerger("VRG-SFX-003", "Verger El Amra", fatma.getId(), "El Amra",
                new GeoJsonPoint(10.85, 34.97), 5.0, SourcePosition.AUTO, 20.0, 320, 1998, true);
        upsertParcelle(vrg3.getId(), "Parcelle Amra Est", chetoui.getId(), 12.0, true);
        upsertParcelle(vrg3.getId(), "Parcelle Amra Ouest", oueslati.getId(), 8.0, true);

        // Verger 4 (Fatma) : Sakiet Ezzit (~34.80, 10.76), inactif (INT-03)
        Verger vrg4 = upsertVerger("VRG-SFX-004", "Verger Sakiet Ezzit Ancien", fatma.getId(), "Sakiet Ezzit",
                new GeoJsonPoint(10.76, 34.80), 10.0, SourcePosition.AUTO, 5.0, 80, 1985, false);
        upsertParcelle(vrg4.getId(), "Parcelle Sakiet Sud", chemlali.getId(), 5.0, false);

        // Verger 5 (Habib) : Sakiet Ezzit (~34.80, 10.76), même coordonnées que VRG-SFX-004 (VER-11)
        Verger vrg5 = upsertVerger("VRG-SFX-005", "Verger Sakiet Ezzit Nouveau", habib.getId(), "Sakiet Ezzit",
                new GeoJsonPoint(10.76, 34.80), 3.2, SourcePosition.AUTO, 6.5, 110, 2018, true);
        upsertParcelle(vrg5.getId(), "Parcelle Sakiet Oliviers Jeunes", chemlali.getId(), 6.5, true);

        // 4. Demandes de collecte initiales (idempotentes par reference)
        LocalDate todayTunis = LocalDate.now(ZoneId.of("Africa/Tunis"));
        Instant now = Instant.now();

        upsertRecolte(
                "REC-SFX-001",
                ali.getId(),
                vrg1.getId(),
                todayTunis.plusDays(3),
                2400.0,
                false,
                null,
                StatutRecolte.DEMANDEE,
                List.of(new HistoriqueStatutRecolte(
                        null, StatutRecolte.DEMANDEE,
                        ali.getId(), ali.getName(), "AGRICULTEUR",
                        null, now.minus(2, ChronoUnit.DAYS)
                ))
        );

        Recolte rec2 = upsertRecolte(
                "REC-SFX-002",
                ali.getId(),
                vrg2.getId(),
                todayTunis.plusDays(7),
                1800.0,
                false,
                null,
                StatutRecolte.PLANIFIEE,
                List.of(
                        new HistoriqueStatutRecolte(
                                null, StatutRecolte.DEMANDEE,
                                ali.getId(), ali.getName(), "AGRICULTEUR",
                                null, now.minus(3, ChronoUnit.DAYS)
                        ),
                        new HistoriqueStatutRecolte(
                                StatutRecolte.DEMANDEE, StatutRecolte.VALIDEE,
                                collecte.getId(), collecte.getName(), "RESPONSABLE_COLLECTE",
                                "Capacité validée", now.minus(2, ChronoUnit.DAYS)
                        ),
                        new HistoriqueStatutRecolte(
                                StatutRecolte.VALIDEE, StatutRecolte.PLANIFIEE,
                                collecte.getId(), collecte.getName(), "RESPONSABLE_COLLECTE",
                                "Intégrée au planning Sfax Sud", now.minus(1, ChronoUnit.DAYS)
                        )
                )
        );

        upsertRecolte(
                "REC-SFX-003",
                fatma.getId(),
                vrg3.getId(),
                todayTunis.plusDays(4),
                3100.0,
                true,
                "Maturité avancée sur parcelle Amra Est",
                StatutRecolte.DEMANDEE,
                List.of(new HistoriqueStatutRecolte(
                        null, StatutRecolte.DEMANDEE,
                        fatma.getId(), fatma.getName(), "AGRICULTEUR",
                        "Maturité avancée sur parcelle Amra Est", now.minus(1, ChronoUnit.DAYS)
                ))
        );

        // 5. Ressources (10 ouvriers, 4 tracteurs dont TR-04 New Holland et 1 HORS_SERVICE, 6 bennes 2000 kg)
        Ressource ouv1 = upsertRessource("OUV-001", TypeRessource.OUVRIER, "Kamel Gharbi", StatutRessource.DISPONIBLE, null);
        Ressource ouv2 = upsertRessource("OUV-002", TypeRessource.OUVRIER, "Nabil Bouaziz", StatutRessource.DISPONIBLE, null);
        Ressource ouv3 = upsertRessource("OUV-003", TypeRessource.OUVRIER, "Walid Trabelsi", StatutRessource.DISPONIBLE, null);
        Ressource ouv4 = upsertRessource("OUV-004", TypeRessource.OUVRIER, "Bilel Jarray", StatutRessource.DISPONIBLE, null);
        Ressource ouv5 = upsertRessource("OUV-005", TypeRessource.OUVRIER, "Houssem Ayadi", StatutRessource.DISPONIBLE, null);
        upsertRessource("OUV-006", TypeRessource.OUVRIER, "Zied Ellouze", StatutRessource.DISPONIBLE, null);
        upsertRessource("OUV-007", TypeRessource.OUVRIER, "Mehdi Rekik", StatutRessource.DISPONIBLE, null);
        upsertRessource("OUV-008", TypeRessource.OUVRIER, "Anis Sellami", StatutRessource.DISPONIBLE, null);
        upsertRessource("OUV-009", TypeRessource.OUVRIER, "Youssef Frikha", StatutRessource.DISPONIBLE, null);
        upsertRessource("OUV-010", TypeRessource.OUVRIER, "Karim Masmoudi", StatutRessource.DISPONIBLE, null);

        Ressource tr1 = upsertRessource("TR-01", TypeRessource.TRACTEUR, "Massey Ferguson 240", StatutRessource.DISPONIBLE, null);
        upsertRessource("TR-02", TypeRessource.TRACTEUR, "John Deere 5055E", StatutRessource.DISPONIBLE, null);
        upsertRessource("TR-03", TypeRessource.TRACTEUR, "Landini Rex (Panne moteur)", StatutRessource.HORS_SERVICE, null);
        Ressource tr4 = upsertRessource("TR-04", TypeRessource.TRACTEUR, "New Holland T5", StatutRessource.DISPONIBLE, null);

        Ressource bn1 = upsertRessource("BN-01", TypeRessource.BENNE, "Benne basculante 2T #1", StatutRessource.DISPONIBLE, 2000.0);
        Ressource bn2 = upsertRessource("BN-02", TypeRessource.BENNE, "Benne basculante 2T #2", StatutRessource.DISPONIBLE, 2000.0);
        Ressource bn3 = upsertRessource("BN-03", TypeRessource.BENNE, "Benne basculante 2T #3", StatutRessource.DISPONIBLE, 2000.0);
        upsertRessource("BN-04", TypeRessource.BENNE, "Benne basculante 2T #4", StatutRessource.DISPONIBLE, 2000.0);
        upsertRessource("BN-05", TypeRessource.BENNE, "Benne basculante 2T #5", StatutRessource.DISPONIBLE, 2000.0);
        upsertRessource("BN-06", TypeRessource.BENNE, "Benne basculante 2T #6", StatutRessource.DISPONIBLE, 2000.0);

        // 6. Tournées de démonstration (permettant la démonstration d'un conflit TOU-01 sur TR-04)
        Instant t1Debut = todayTunis.plusDays(7).atTime(7, 0).atZone(ZoneId.of("Africa/Tunis")).toInstant();
        Instant t1Fin = todayTunis.plusDays(7).atTime(12, 0).atZone(ZoneId.of("Africa/Tunis")).toInstant();
        upsertTournee("T-001", t1Debut, t1Fin, StatutTournee.PLANIFIEE, chef.getId(),
                List.of(rec2.getId()), List.of(ouv1.getId(), ouv2.getId(), ouv3.getId()),
                List.of(tr4.getId()), List.of(bn1.getId()), 1800.0);

        Instant t2Debut = todayTunis.plusDays(8).atTime(7, 0).atZone(ZoneId.of("Africa/Tunis")).toInstant();
        Instant t2Fin = todayTunis.plusDays(8).atTime(11, 0).atZone(ZoneId.of("Africa/Tunis")).toInstant();
        upsertTournee("T-002", t2Debut, t2Fin, StatutTournee.BROUILLON, chef.getId(),
                List.of(), List.of(ouv4.getId(), ouv5.getId()),
                List.of(tr1.getId()), List.of(bn2.getId(), bn3.getId()), 0.0);

        log.info("Peuplement des données terminé : Variétés={}, Utilisateurs={}, Vergers={}, Parcelles={}, Récoltes={}, Ressources={}, Tournées={}",
                varieteRepository.count(), userRepository.count(), vergerRepository.count(),
                parcelleRepository.count(), recolteRepository.count(), ressourceRepository.count(), tourneeRepository.count());
    }

    private Variete upsertVariete(String code, String nom, Double seuilMaturite, Double rendementMoyenTHa, boolean actif) {
        String cleanCode = code.trim().toUpperCase(Locale.ROOT);
        return varieteRepository.findByCode(cleanCode).orElseGet(() -> {
            Variete v = new Variete(null, cleanCode, nom, seuilMaturite, rendementMoyenTHa, actif);
            return varieteRepository.save(v);
        });
    }

    private User upsertUser(String name, String email, Set<Role> roles) {
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        return userRepository.findByEmail(normalizedEmail).orElseGet(() -> {
            User user = new User();
            user.setName(name);
            user.setEmail(normalizedEmail);
            user.setPassword(passwordEncoder.encode(defaultPassword));
            user.setRoles(roles);
            user.setActive(true);
            return userRepository.save(user);
        });
    }

    private Verger upsertVerger(String code, String nom, String proprietaireId, String commune,
                                GeoJsonPoint localisation, Double precisionGpsMetres, SourcePosition sourcePosition,
                                Double surfaceHa, Integer nombreArbres, Integer anneePlantation, boolean actif) {
        String cleanCode = code.trim().toUpperCase(Locale.ROOT);
        return vergerRepository.findByCode(cleanCode).orElseGet(() -> {
            Verger v = new Verger(null, cleanCode, nom, proprietaireId, commune, localisation,
                    precisionGpsMetres, sourcePosition, surfaceHa, nombreArbres, anneePlantation, actif);
            return vergerRepository.save(v);
        });
    }

    private Parcelle upsertParcelle(String vergerId, String nom, String varieteId, Double surfaceHa, boolean actif) {
        return parcelleRepository.findByVergerIdAndNom(vergerId, nom).orElseGet(() -> {
            Parcelle p = new Parcelle(null, vergerId, nom, varieteId, surfaceHa, actif);
            return parcelleRepository.save(p);
        });
    }

    private Recolte upsertRecolte(String reference, String proprietaireId, String vergerId,
                                  LocalDate dateSouhaitee, Double quantiteEstimeeKg, boolean urgent,
                                  String motifUrgence, StatutRecolte statut,
                                  List<HistoriqueStatutRecolte> historique) {
        return recolteRepository.findByReference(reference).orElseGet(() -> {
            Recolte recolte = new Recolte();
            recolte.setReference(reference);
            recolte.setProprietaireId(proprietaireId);
            recolte.setVergerId(vergerId);
            recolte.setDateSouhaitee(dateSouhaitee);
            recolte.setQuantiteEstimeeKg(quantiteEstimeeKg);
            recolte.setUrgent(urgent);
            recolte.setMotifUrgence(motifUrgence);
            recolte.setStatut(statut);
            recolte.setHistorique(historique);
            return recolteRepository.save(recolte);
        });
    }

    private Ressource upsertRessource(String code, TypeRessource type, String libelle, StatutRessource statut, Double capaciteKg) {
        String cleanCode = code.trim().toUpperCase(Locale.ROOT);
        return ressourceRepository.findByCode(cleanCode).orElseGet(() ->
                ressourceRepository.save(new Ressource(null, cleanCode, type, libelle, statut, capaciteKg, true))
        );
    }

    private Tournee upsertTournee(String code, Instant debut, Instant fin, StatutTournee statut, String chefEquipeId,
                                  List<String> recolteIds, List<String> ouvrierIds, List<String> tracteurIds,
                                  List<String> benneIds, Double quantiteEstimeeKg) {
        String cleanCode = code.trim().toUpperCase(Locale.ROOT);
        return tourneeRepository.findByCode(cleanCode).orElseGet(() -> {
            Tournee t = new Tournee();
            t.setCode(cleanCode);
            t.setDebut(debut);
            t.setFin(fin);
            t.setStatut(statut);
            t.setChefEquipeId(chefEquipeId);
            t.setRecolteIds(recolteIds);
            t.setOuvrierIds(ouvrierIds);
            t.setTracteurIds(tracteurIds);
            t.setBenneIds(benneIds);
            t.setQuantiteEstimeeKg(quantiteEstimeeKg);
            t.setActif(true);
            return tourneeRepository.save(t);
        });
    }
}
