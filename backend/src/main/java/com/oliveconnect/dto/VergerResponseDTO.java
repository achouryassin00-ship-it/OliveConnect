package com.oliveconnect.dto;

import com.oliveconnect.entity.SourcePosition;
import com.oliveconnect.entity.Verger;

import java.time.Instant;

public class VergerResponseDTO {

    private String id;
    private String code;
    private String nom;
    private String proprietaireId;
    private String commune;
    private Double longitude;
    private Double latitude;
    private Double precisionGpsMetres;
    private SourcePosition sourcePosition;
    private Double surfaceHa;
    private Integer nombreArbres;
    private Integer anneePlantation;
    private boolean actif;
    private Long version;
    private Instant createdAt;
    private Instant updatedAt;
    private Double surfaceParcellesTotale;

    public VergerResponseDTO() {
    }

    public static VergerResponseDTO fromEntity(Verger v, Double surfaceParcellesTotale) {
        if (v == null) return null;
        VergerResponseDTO dto = new VergerResponseDTO();
        dto.setId(v.getId());
        dto.setCode(v.getCode());
        dto.setNom(v.getNom());
        dto.setProprietaireId(v.getProprietaireId());
        dto.setCommune(v.getCommune());
        if (v.getLocalisation() != null) {
            dto.setLongitude(v.getLocalisation().getX());
            dto.setLatitude(v.getLocalisation().getY());
        }
        dto.setPrecisionGpsMetres(v.getPrecisionGpsMetres());
        dto.setSourcePosition(v.getSourcePosition());
        dto.setSurfaceHa(v.getSurfaceHa());
        dto.setNombreArbres(v.getNombreArbres());
        dto.setAnneePlantation(v.getAnneePlantation());
        dto.setActif(v.isActif());
        dto.setVersion(v.getVersion());
        dto.setCreatedAt(v.getCreatedAt());
        dto.setUpdatedAt(v.getUpdatedAt());
        dto.setSurfaceParcellesTotale(surfaceParcellesTotale != null ? surfaceParcellesTotale : 0.0);
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

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getProprietaireId() {
        return proprietaireId;
    }

    public void setProprietaireId(String proprietaireId) {
        this.proprietaireId = proprietaireId;
    }

    public String getCommune() {
        return commune;
    }

    public void setCommune(String commune) {
        this.commune = commune;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getPrecisionGpsMetres() {
        return precisionGpsMetres;
    }

    public void setPrecisionGpsMetres(Double precisionGpsMetres) {
        this.precisionGpsMetres = precisionGpsMetres;
    }

    public SourcePosition getSourcePosition() {
        return sourcePosition;
    }

    public void setSourcePosition(SourcePosition sourcePosition) {
        this.sourcePosition = sourcePosition;
    }

    public Double getSurfaceHa() {
        return surfaceHa;
    }

    public void setSurfaceHa(Double surfaceHa) {
        this.surfaceHa = surfaceHa;
    }

    public Integer getNombreArbres() {
        return nombreArbres;
    }

    public void setNombreArbres(Integer nombreArbres) {
        this.nombreArbres = nombreArbres;
    }

    public Integer getAnneePlantation() {
        return anneePlantation;
    }

    public void setAnneePlantation(Integer anneePlantation) {
        this.anneePlantation = anneePlantation;
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

    public Double getSurfaceParcellesTotale() {
        return surfaceParcellesTotale;
    }

    public void setSurfaceParcellesTotale(Double surfaceParcellesTotale) {
        this.surfaceParcellesTotale = surfaceParcellesTotale;
    }
}
