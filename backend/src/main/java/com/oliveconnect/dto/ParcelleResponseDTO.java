package com.oliveconnect.dto;

import com.oliveconnect.entity.Parcelle;

import java.time.Instant;

public class ParcelleResponseDTO {

    private String id;
    private String vergerId;
    private String nom;
    private String varieteId;
    private Double surfaceHa;
    private boolean actif;
    private Long version;
    private Instant createdAt;
    private Instant updatedAt;

    public ParcelleResponseDTO() {
    }

    public static ParcelleResponseDTO fromEntity(Parcelle p) {
        if (p == null) return null;
        ParcelleResponseDTO dto = new ParcelleResponseDTO();
        dto.setId(p.getId());
        dto.setVergerId(p.getVergerId());
        dto.setNom(p.getNom());
        dto.setVarieteId(p.getVarieteId());
        dto.setSurfaceHa(p.getSurfaceHa());
        dto.setActif(p.isActif());
        dto.setVersion(p.getVersion());
        dto.setCreatedAt(p.getCreatedAt());
        dto.setUpdatedAt(p.getUpdatedAt());
        return dto;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getVergerId() {
        return vergerId;
    }

    public void setVergerId(String vergerId) {
        this.vergerId = vergerId;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getVarieteId() {
        return varieteId;
    }

    public void setVarieteId(String varieteId) {
        this.varieteId = varieteId;
    }

    public Double getSurfaceHa() {
        return surfaceHa;
    }

    public void setSurfaceHa(Double surfaceHa) {
        this.surfaceHa = surfaceHa;
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
