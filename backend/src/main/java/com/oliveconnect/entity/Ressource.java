package com.oliveconnect.entity;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Locale;

/**
 * Ressource humaine ou matérielle (ouvrier, tracteur, benne) mobilisable pour les tournées.
 */
@Document(collection = "ressources")
public class Ressource {

    @Id
    private String id;

    private String code;

    private TypeRessource type;

    private String libelle;

    private StatutRessource statut = StatutRessource.DISPONIBLE;

    private Double capaciteKg;

    private boolean actif = true;

    @Version
    private Long version;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    public Ressource() {
    }

    public Ressource(String id, String code, TypeRessource type, String libelle,
                     StatutRessource statut, Double capaciteKg, boolean actif) {
        this.id = id;
        this.code = normalizeCode(code);
        this.type = type;
        this.libelle = libelle;
        this.statut = statut != null ? statut : StatutRessource.DISPONIBLE;
        this.capaciteKg = capaciteKg;
        this.actif = actif;
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

    public TypeRessource getType() {
        return type;
    }

    public void setType(TypeRessource type) {
        this.type = type;
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
