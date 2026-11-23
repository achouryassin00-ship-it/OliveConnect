package com.oliveconnect.entity;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "varietes")
public class Variete {

    @Id
    private String id;

    private String code;

    private String nom;

    private Double seuilMaturite;

    private Double rendementMoyenTHa;

    private boolean actif = true;

    @Version
    private Long version;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    public Variete() {
    }

    public Variete(String id, String code, String nom, Double seuilMaturite, Double rendementMoyenTHa, boolean actif) {
        this.id = id;
        this.code = code != null ? code.trim().toUpperCase() : null;
        this.nom = nom;
        this.seuilMaturite = seuilMaturite;
        this.rendementMoyenTHa = rendementMoyenTHa;
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

    public Double getSeuilMaturite() {
        return seuilMaturite;
    }

    public void setSeuilMaturite(Double seuilMaturite) {
        this.seuilMaturite = seuilMaturite;
    }

    public Double getRendementMoyenTHa() {
        return rendementMoyenTHa;
    }

    public void setRendementMoyenTHa(Double rendementMoyenTHa) {
        this.rendementMoyenTHa = rendementMoyenTHa;
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
