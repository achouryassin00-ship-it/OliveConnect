package com.oliveconnect.dto;

import com.oliveconnect.entity.Recolte;
import com.oliveconnect.entity.StatutRecolte;
import com.oliveconnect.entity.Verger;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public class RecolteResponseDTO {

    private String id;
    private String reference;
    private String proprietaireId;
    private String vergerId;
    private String vergerNom;
    private String vergerCode;
    private LocalDate dateSouhaitee;
    private Double quantiteEstimeeKg;
    private boolean urgent;
    private String motifUrgence;
    private StatutRecolte statut;
    private boolean annulable;
    private List<HistoriqueStatutDTO> historique;
    private Long version;
    private Instant createdAt;
    private Instant updatedAt;

    public RecolteResponseDTO() {
    }

    public static RecolteResponseDTO fromEntity(Recolte recolte, Verger verger) {
        if (recolte == null) return null;
        RecolteResponseDTO dto = new RecolteResponseDTO();
        dto.setId(recolte.getId());
        dto.setReference(recolte.getReference());
        dto.setProprietaireId(recolte.getProprietaireId());
        dto.setVergerId(recolte.getVergerId());
        if (verger != null) {
            dto.setVergerNom(verger.getNom());
            dto.setVergerCode(verger.getCode());
        }
        dto.setDateSouhaitee(recolte.getDateSouhaitee());
        dto.setQuantiteEstimeeKg(recolte.getQuantiteEstimeeKg());
        dto.setUrgent(recolte.isUrgent());
        dto.setMotifUrgence(recolte.getMotifUrgence());
        dto.setStatut(recolte.getStatut());
        dto.setAnnulable(recolte.getStatut() == StatutRecolte.DEMANDEE
                || recolte.getStatut() == StatutRecolte.VALIDEE);
        dto.setHistorique(recolte.getHistorique() == null
                ? List.of()
                : recolte.getHistorique().stream().map(HistoriqueStatutDTO::fromEntity).toList());
        dto.setVersion(recolte.getVersion());
        dto.setCreatedAt(recolte.getCreatedAt());
        dto.setUpdatedAt(recolte.getUpdatedAt());
        return dto;
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

    public String getVergerNom() {
        return vergerNom;
    }

    public void setVergerNom(String vergerNom) {
        this.vergerNom = vergerNom;
    }

    public String getVergerCode() {
        return vergerCode;
    }

    public void setVergerCode(String vergerCode) {
        this.vergerCode = vergerCode;
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

    public boolean isAnnulable() {
        return annulable;
    }

    public void setAnnulable(boolean annulable) {
        this.annulable = annulable;
    }

    public List<HistoriqueStatutDTO> getHistorique() {
        return historique;
    }

    public void setHistorique(List<HistoriqueStatutDTO> historique) {
        this.historique = historique;
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
