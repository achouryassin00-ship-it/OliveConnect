package com.oliveconnect.entity;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Tournée de collecte regroupant des demandes de récolte et des ressources affectées.
 */
@Document(collection = "tournees")
public class Tournee {

    @Id
    private String id;

    private String code;

    private Instant debut;

    private Instant fin;

    private StatutTournee statut = StatutTournee.BROUILLON;

    private List<String> recolteIds = new ArrayList<>();

    private String chefEquipeId;

    private List<String> ouvrierIds = new ArrayList<>();

    private List<String> tracteurIds = new ArrayList<>();

    private List<String> benneIds = new ArrayList<>();

    private Double quantiteEstimeeKg = 0.0;

    private boolean actif = true;

    @Version
    private Long version;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    public Tournee() {
    }

    private static String normalizeCode(String code) {
        return code != null ? code.trim().toUpperCase(Locale.ROOT) : null;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = normalizeCode(code);
    }

    public Instant getDebut() {
        return debut;
    }

    public void setDebut(Instant debut) {
        this.debut = debut;
    }

    public Instant getFin() {
        return fin;
    }

    public void setFin(Instant fin) {
        this.fin = fin;
    }

    public StatutTournee getStatut() {
        return statut;
    }

    public void setStatut(StatutTournee statut) {
        this.statut = statut;
    }

    public List<String> getRecolteIds() {
        return recolteIds;
    }

    public void setRecolteIds(List<String> recolteIds) {
        this.recolteIds = recolteIds != null ? new ArrayList<>(recolteIds) : new ArrayList<>();
    }

    public String getChefEquipeId() {
        return chefEquipeId;
    }

    public void setChefEquipeId(String chefEquipeId) {
        this.chefEquipeId = chefEquipeId;
    }

    public List<String> getOuvrierIds() {
        return ouvrierIds;
    }

    public void setOuvrierIds(List<String> ouvrierIds) {
        this.ouvrierIds = ouvrierIds != null ? new ArrayList<>(ouvrierIds) : new ArrayList<>();
    }

    public List<String> getTracteurIds() {
        return tracteurIds;
    }

    public void setTracteurIds(List<String> tracteurIds) {
        this.tracteurIds = tracteurIds != null ? new ArrayList<>(tracteurIds) : new ArrayList<>();
    }

    public List<String> getBenneIds() {
        return benneIds;
    }

    public void setBenneIds(List<String> benneIds) {
        this.benneIds = benneIds != null ? new ArrayList<>(benneIds) : new ArrayList<>();
    }

    public Double getQuantiteEstimeeKg() {
        return quantiteEstimeeKg;
    }

    public void setQuantiteEstimeeKg(Double quantiteEstimeeKg) {
        this.quantiteEstimeeKg = quantiteEstimeeKg;
    }

    public boolean isActif() {
        return actif;
    }

    public void setActif(boolean actif) {
        this.actif = actif;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
