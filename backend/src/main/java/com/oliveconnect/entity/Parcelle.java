package com.oliveconnect.entity;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "parcelles")
public class Parcelle {

    @Id
    private String id;

    private String vergerId;

    private String nom;

    private String varieteId;

    private Double surfaceHa;

    private boolean actif = true;

    @Version
    private Long version;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    public Parcelle() {
    }

    public Parcelle(String id, String vergerId, String nom, String varieteId, Double surfaceHa, boolean actif) {
        this.id = id;
        this.vergerId = vergerId;
        this.nom = nom;
        this.varieteId = varieteId;
        this.surfaceHa = surfaceHa;
        this.actif = actif;
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
