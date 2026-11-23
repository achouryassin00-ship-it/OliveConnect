package com.oliveconnect.dto;

import com.oliveconnect.entity.Variete;

public class VarieteResponseDTO {

    private String id;
    private String code;
    private String nom;
    private Double seuilMaturite;
    private Double rendementMoyenTHa;
    private boolean actif;

    public VarieteResponseDTO() {
    }

    public VarieteResponseDTO(String id, String code, String nom, Double seuilMaturite, Double rendementMoyenTHa, boolean actif) {
        this.id = id;
        this.code = code;
        this.nom = nom;
        this.seuilMaturite = seuilMaturite;
        this.rendementMoyenTHa = rendementMoyenTHa;
        this.actif = actif;
    }

    public static VarieteResponseDTO fromEntity(Variete v) {
        if (v == null) return null;
        return new VarieteResponseDTO(
                v.getId(),
                v.getCode(),
                v.getNom(),
                v.getSeuilMaturite(),
                v.getRendementMoyenTHa(),
                v.isActif()
        );
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
}
