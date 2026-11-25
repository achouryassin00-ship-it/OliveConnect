package com.oliveconnect.config;

import com.oliveconnect.entity.Parcelle;
import com.oliveconnect.entity.Recolte;
import com.oliveconnect.entity.User;
import com.oliveconnect.entity.Variete;
import com.oliveconnect.entity.Verger;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexType;
import org.springframework.data.mongodb.core.index.GeospatialIndex;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.PartialIndexFilter;
import org.springframework.data.mongodb.core.query.Criteria;

@Configuration
public class MongoIndexConfig {

    private static final Logger log = LoggerFactory.getLogger(MongoIndexConfig.class);

    private final MongoTemplate mongoTemplate;

    public MongoIndexConfig(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initIndexes() {
        log.info("Initialisation explicite et idempotente des index MongoDB...");

        createUsersIndexes();
        createVergersIndexes();
        createParcellesIndexes();
        createVarietesIndexes();
        createRecoltesIndexes();

        log.info("Tous les index MongoDB ont été initialisés avec succès.");
    }

    public void createUsersIndexes() {
        // CPT-01 : index unique sur email normalisé
        Index emailIndex = new Index().on("email", Sort.Direction.ASC).unique().named("idx_users_email_unique");
        mongoTemplate.indexOps(User.class).ensureIndex(emailIndex);
        log.info("Index créé ou vérifié : users [idx_users_email_unique]");
    }

    public void createVergersIndexes() {
        // VER-05 : index géospatial 2dsphere sur localisation
        GeospatialIndex geoIndex = new GeospatialIndex("localisation")
                .typed(GeoSpatialIndexType.GEO_2DSPHERE)
                .named("idx_vergers_localisation_2dsphere");
        try {
            mongoTemplate.indexOps(Verger.class).ensureIndex(geoIndex);
        } catch (Exception e) {
            log.warn("Recréation de l'index 2dsphere suite à conflit d'options : {}", e.getMessage());
            try {
                mongoTemplate.indexOps(Verger.class).dropIndex("idx_vergers_localisation_2dsphere");
                mongoTemplate.indexOps(Verger.class).ensureIndex(geoIndex);
            } catch (Exception ex) {
                log.error("Impossible de recréer l'index 2dsphere : {}", ex.getMessage());
            }
        }
        log.info("Index créé ou vérifié : vergers [idx_vergers_localisation_2dsphere] (2dsphere)");

        // Code unique
        Index codeIndex = new Index().on("code", Sort.Direction.ASC).unique().named("idx_vergers_code_unique");
        mongoTemplate.indexOps(Verger.class).ensureIndex(codeIndex);
        log.info("Index créé ou vérifié : vergers [idx_vergers_code_unique]");

        // Compound index proprietaireId + actif
        Index ownerActifIndex = new Index()
                .on("proprietaireId", Sort.Direction.ASC)
                .on("actif", Sort.Direction.ASC)
                .named("idx_vergers_owner_actif");
        mongoTemplate.indexOps(Verger.class).ensureIndex(ownerActifIndex);
        log.info("Index créé ou vérifié : vergers [idx_vergers_owner_actif]");
    }

    public void createParcellesIndexes() {
        // Compound index vergerId + actif
        Index vergerActifIndex = new Index()
                .on("vergerId", Sort.Direction.ASC)
                .on("actif", Sort.Direction.ASC)
                .named("idx_parcelles_verger_actif");
        mongoTemplate.indexOps(Parcelle.class).ensureIndex(vergerActifIndex);
        log.info("Index créé ou vérifié : parcelles [idx_parcelles_verger_actif]");

        // Index varieteId
        Index varieteIndex = new Index()
                .on("varieteId", Sort.Direction.ASC)
                .named("idx_parcelles_variete");
        mongoTemplate.indexOps(Parcelle.class).ensureIndex(varieteIndex);
        log.info("Index créé ou vérifié : parcelles [idx_parcelles_variete]");
    }

    public void createVarietesIndexes() {
        // Code unique
        Index codeIndex = new Index().on("code", Sort.Direction.ASC).unique().named("idx_varietes_code_unique");
        mongoTemplate.indexOps(Variete.class).ensureIndex(codeIndex);
        log.info("Index créé ou vérifié : varietes [idx_varietes_code_unique]");
    }

    public void createRecoltesIndexes() {
        Index referenceIndex = new Index().on("reference", Sort.Direction.ASC).unique().named("idx_recoltes_reference_unique");
        mongoTemplate.indexOps(Recolte.class).ensureIndex(referenceIndex);
        log.info("Index créé ou vérifié : recoltes [idx_recoltes_reference_unique]");

        Index ownerStatutDateIndex = new Index()
                .on("proprietaireId", Sort.Direction.ASC)
                .on("statut", Sort.Direction.ASC)
                .on("dateSouhaitee", Sort.Direction.ASC)
                .named("idx_recoltes_owner_statut_date");
        mongoTemplate.indexOps(Recolte.class).ensureIndex(ownerStatutDateIndex);
        log.info("Index créé ou vérifié : recoltes [idx_recoltes_owner_statut_date]");

        Index vergerStatutIndex = new Index()
                .on("vergerId", Sort.Direction.ASC)
                .on("statut", Sort.Direction.ASC)
                .named("idx_recoltes_verger_statut");
        mongoTemplate.indexOps(Recolte.class).ensureIndex(vergerStatutIndex);
        log.info("Index créé ou vérifié : recoltes [idx_recoltes_verger_statut]");

        Index activeDuplicateIndex = new Index()
                .on("vergerId", Sort.Direction.ASC)
                .on("dateSouhaitee", Sort.Direction.ASC)
                .unique()
                .partial(PartialIndexFilter.of(
                        Criteria.where("statut").in("DEMANDEE", "VALIDEE", "PLANIFIEE", "EN_COURS")
                ))
                .named("idx_recoltes_verger_date_active_unique");
        mongoTemplate.indexOps(Recolte.class).ensureIndex(activeDuplicateIndex);
        log.info("Index créé ou vérifié : recoltes [idx_recoltes_verger_date_active_unique]");

        Index idempotencyIndex = new Index()
                .on("proprietaireId", Sort.Direction.ASC)
                .on("idempotencyKey", Sort.Direction.ASC)
                .unique()
                .partial(PartialIndexFilter.of(new Document("idempotencyKey", new Document("$type", "string"))))
                .named("idx_recoltes_owner_idempotency_unique");
        mongoTemplate.indexOps(Recolte.class).ensureIndex(idempotencyIndex);
        log.info("Index créé ou vérifié : recoltes [idx_recoltes_owner_idempotency_unique]");
    }
}
