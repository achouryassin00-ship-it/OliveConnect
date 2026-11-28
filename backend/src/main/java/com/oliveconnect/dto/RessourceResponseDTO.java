package com.oliveconnect.dto;

import com.oliveconnect.entity.Ressource;
import com.oliveconnect.entity.StatutRessource;
import com.oliveconnect.entity.TypeRessource;

import java.time.Instant;

public class RessourceResponseDTO {

    private String id;
    private String code;
    private TypeRessource type;
    private String typeLibelle;
    private String libelle;
    private StatutRessource statut;
    private String statutLibelle;
    private Double capaciteKg;
    private boolean actif;
    private Long version;
    private Instant createdAt;
    private Instant updatedAt;

    public RessourceResponseDTO() {
    }

    public static RessourceResponseDTO fromEntity(Ressource r) {
        if (r == null) return null;
        RessourceResponseDTO dto = new RessourceResponseDTO();
        dto.setId(r.getId());
        dto.setCode(r.getCode());
        dto.setType(r.getType());
        dto.setTypeLibelle(r.getType() != null ? r.getType().getLibelle() : null);
        dto.setLibelle(r.getLibelle());
        dto.setStatut(r.getStatut());
        dto.setStatutLibelle(r.getStatut() != null ? r.getStatut().getLibelle() : null);
        dto.setCapaciteKg(r.getCapaciteKg());
        dto.setActif(r.isActif());
        dto.setVersion(r.getVersion());
        dto.setCreatedAt(r.getCreatedAt());
        dto.setUpdatedAt(r.getUpdatedAt());
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

    public TypeRessource getType() {
        return type;
    }

    public void setType(TypeRessource type) {
        this.type = type;
    }

    public String getTypeLibelle() {
        return typeLibelle;
    }

    public void setTypeLibelle(String typeLibelle) {
        this.typeLibelle = typeLibelle;
    }

    public String getLibelle() {
        return libelle;
    }

    public void setLibelle(String libelle) {
        this.libelle = libelle;
    }

    public StatutRessource getStatut() {
        return statut;
    }

    public void setStatut(StatutRessource statut) {
        this.statut = statut;
    }

    public String getStatutLibelle() {
        return statutLibelle;
    }

    public void setStatutLibelle(String statutLibelle) {
        this.statutLibelle = statutLibelle;
    }

    public Double getCapaciteKg() {
        return capaciteKg;
    }

    public void setCapaciteKg(Double capaciteKg) {
        this.capaciteKg = capaciteKg;
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
