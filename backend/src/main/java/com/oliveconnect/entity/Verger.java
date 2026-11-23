package com.oliveconnect.entity;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "vergers")
public class Verger {

    @Id
    private String id;

    private String code;

    private String nom;

    private String proprietaireId;

    private String commune;

    private GeoJsonPoint localisation;

    private Double precisionGpsMetres;

    private SourcePosition sourcePosition = SourcePosition.AUTO;

    private Double surfaceHa;

    private Integer nombreArbres;

    private Integer anneePlantation;

    private boolean actif = true;

    @Version
    private Long version;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    public Verger() {
    }

    public Verger(String id, String code, String nom, String proprietaireId, String commune,
                  GeoJsonPoint localisation, Double precisionGpsMetres, SourcePosition sourcePosition,
                  Double surfaceHa, Integer nombreArbres, Integer anneePlantation, boolean actif) {
        this.id = id;
        this.code = code != null ? code.trim().toUpperCase() : null;
        this.nom = nom;
        this.proprietaireId = proprietaireId;
        this.commune = commune;
        this.localisation = localisation;
        this.precisionGpsMetres = precisionGpsMetres;
        this.sourcePosition = sourcePosition != null ? sourcePosition : SourcePosition.AUTO;
        this.surfaceHa = surfaceHa;
        this.nombreArbres = nombreArbres;
        this.anneePlantation = anneePlantation;
        this.actif = actif;
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
        this.code = code != null ? code.trim().toUpperCase() : null;
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

    public GeoJsonPoint getLocalisation() {
        return localisation;
    }

    public void setLocalisation(GeoJsonPoint localisation) {
        this.localisation = localisation;
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
}
