package com.oliveconnect.entity;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Demande de collecte d'olives déposée par un agriculteur pour l'un de ses vergers actifs.
 */
@Document(collection = "recoltes")
public class Recolte {

    @Id
    private String id;

    private String reference;

    private String proprietaireId;

    private String vergerId;

    private LocalDate dateSouhaitee;

    private Double quantiteEstimeeKg;

    private boolean urgent;

    private String motifUrgence;

    private StatutRecolte statut = StatutRecolte.DEMANDEE;

    private String idempotencyKey;

    private String requestHash;

    private List<HistoriqueStatutRecolte> historique = new ArrayList<>();

    @Version
    private Long version;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    public Recolte() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public String getProprietaireId() {
        return proprietaireId;
    }

    public void setProprietaireId(String proprietaireId) {
        this.proprietaireId = proprietaireId;
    }

    public String getVergerId() {
        return vergerId;
    }

    public void setVergerId(String vergerId) {
        this.vergerId = vergerId;
    }

    public LocalDate getDateSouhaitee() {
        return dateSouhaitee;
    }

    public void setDateSouhaitee(LocalDate dateSouhaitee) {
        this.dateSouhaitee = dateSouhaitee;
    }

    public Double getQuantiteEstimeeKg() {
        return quantiteEstimeeKg;
    }

    public void setQuantiteEstimeeKg(Double quantiteEstimeeKg) {
        this.quantiteEstimeeKg = quantiteEstimeeKg;
    }

    public boolean isUrgent() {
        return urgent;
    }

    public void setUrgent(boolean urgent) {
        this.urgent = urgent;
    }

    public String getMotifUrgence() {
        return motifUrgence;
    }

    public void setMotifUrgence(String motifUrgence) {
        this.motifUrgence = motifUrgence;
    }

    public StatutRecolte getStatut() {
        return statut;
    }

    public void setStatut(StatutRecolte statut) {
        this.statut = statut;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public String getRequestHash() {
        return requestHash;
    }

    public void setRequestHash(String requestHash) {
        this.requestHash = requestHash;
    }

    public List<HistoriqueStatutRecolte> getHistorique() {
        return historique;
    }

    public void setHistorique(List<HistoriqueStatutRecolte> historique) {
        this.historique = historique != null ? historique : new ArrayList<>();
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
