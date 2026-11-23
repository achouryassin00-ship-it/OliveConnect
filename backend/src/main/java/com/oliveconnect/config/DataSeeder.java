package com.oliveconnect.config;

import com.oliveconnect.entity.*;
import com.oliveconnect.repository.ParcelleRepository;
import com.oliveconnect.repository.UserRepository;
import com.oliveconnect.repository.VarieteRepository;
import com.oliveconnect.repository.VergerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Set;

@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final VarieteRepository varieteRepository;
    private final UserRepository userRepository;
    private final VergerRepository vergerRepository;
    private final ParcelleRepository parcelleRepository;
    private final PasswordEncoder passwordEncoder;
    private final String defaultPassword;

    @Value("${oliveconnect.seed.enabled:true}")
    private boolean seedEnabled;

    public DataSeeder(VarieteRepository varieteRepository,
                      UserRepository userRepository,
                      VergerRepository vergerRepository,
                      ParcelleRepository parcelleRepository,
                      PasswordEncoder passwordEncoder,
                      @Value("${oliveconnect.seed.default-password:Password123!}") String defaultPassword) {
        this.varieteRepository = varieteRepository;
        this.userRepository = userRepository;
        this.vergerRepository = vergerRepository;
        this.parcelleRepository = parcelleRepository;
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

        // 2. Utilisateurs (1 Admin, 1 Responsable Collecte, 3 Agriculteurs avec patronymes arabes/apostrophes CPT-06)
        User admin = upsertUser("Mohamed Admin", "admin@oliveconnect.tn", Set.of(Role.ADMINISTRATEUR));
        User collecte = upsertUser("Sami Responsable", "collecte@oliveconnect.tn", Set.of(Role.RESPONSABLE_COLLECTE));
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

        log.info("Peuplement des données terminé : Variétés={}, Utilisateurs={}, Vergers={}, Parcelles={}",
                varieteRepository.count(), userRepository.count(), vergerRepository.count(), parcelleRepository.count());
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
}
