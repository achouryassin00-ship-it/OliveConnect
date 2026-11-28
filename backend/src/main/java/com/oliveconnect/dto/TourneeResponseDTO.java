package com.oliveconnect.dto;

import com.oliveconnect.entity.StatutTournee;
import com.oliveconnect.entity.Tournee;

import java.time.Instant;
import java.util.List;

public class TourneeResponseDTO {

    private String id;
    private String code;
    private Instant debut;
    private Instant fin;
    private StatutTournee statut;
    private String statutLibelle;
    private List<String> recolteIds;
    private String chefEquipeId;
    private List<String> ouvrierIds;
    private List<String> tracteurIds;
    private List<String> benneIds;
    private Double quantiteEstimeeKg;
    private Double capaciteBennesTotaleKg;
    private boolean actif;
    private Long version;
    private Instant createdAt;
    private Instant updatedAt;

    public TourneeResponseDTO() {
    }

    public static TourneeResponseDTO fromEntity(Tournee t, Double capaciteBennesTotaleKg) {
        if (t == null) return null;
        TourneeResponseDTO dto = new TourneeResponseDTO();
        dto.setId(t.getId());
        dto.setCode(t.getCode());
        dto.setDebut(t.getDebut());
        dto.setFin(t.getFin());
        dto.setStatut(t.getStatut());
        dto.setStatutLibelle(t.getStatut() != null ? t.getStatut().getLibelle() : null);
        dto.setRecolteIds(t.getRecolteIds());
        dto.setChefEquipeId(t.getChefEquipeId());
        dto.setOuvrierIds(t.getOuvrierIds());
        dto.setTracteurIds(t.getTracteurIds());
        dto.setBenneIds(t.getBenneIds());
        dto.setQuantiteEstimeeKg(t.getQuantiteEstimeeKg());
        dto.setCapaciteBennesTotaleKg(capaciteBennesTotaleKg);
        dto.setActif(t.isActif());
        dto.setVersion(t.getVersion());
        dto.setCreatedAt(t.getCreatedAt());
        dto.setUpdatedAt(t.getUpdatedAt());
        return dto;
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
        this.code = code;
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

    public String getStatutLibelle() {
        return statutLibelle;
    }

    public void setStatutLibelle(String statutLibelle) {
        this.statutLibelle = statutLibelle;
    }

    public List<String> getRecolteIds() {
        return recolteIds;
    }

    public void setRecolteIds(List<String> recolteIds) {
        this.recolteIds = recolteIds;
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
        this.ouvrierIds = ouvrierIds;
    }

    public List<String> getTracteurIds() {
        return tracteurIds;
    }

    public void setTracteurIds(List<String> tracteurIds) {
        this.tracteurIds = tracteurIds;
    }

    public List<String> getBenneIds() {
        return benneIds;
    }

    public void setBenneIds(List<String> benneIds) {
        this.benneIds = benneIds;
    }

    public Double getQuantiteEstimeeKg() {
        return quantiteEstimeeKg;
    }

    public void setQuantiteEstimeeKg(Double quantiteEstimeeKg) {
        this.quantiteEstimeeKg = quantiteEstimeeKg;
    }

    public Double getCapaciteBennesTotaleKg() {
        return capaciteBennesTotaleKg;
    }

    public void setCapaciteBennesTotaleKg(Double capaciteBennesTotaleKg) {
        this.capaciteBennesTotaleKg = capaciteBennesTotaleKg;
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
