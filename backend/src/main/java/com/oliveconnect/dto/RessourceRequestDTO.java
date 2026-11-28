package com.oliveconnect.dto;

import com.oliveconnect.entity.StatutRessource;
import com.oliveconnect.entity.TypeRessource;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class RessourceRequestDTO {

    @NotBlank(message = "Le code de la ressource est obligatoire.")
    @Size(min = 2, max = 50, message = "Le code doit comporter entre 2 et 50 caractères.")
    private String code;

    @NotNull(message = "Le type de ressource est obligatoire.")
    private TypeRessource type;

    @NotBlank(message = "Le libellé de la ressource est obligatoire.")
    @Size(min = 2, max = 150, message = "Le libellé doit comporter entre 2 et 150 caractères.")
    private String libelle;

    private StatutRessource statut;

    private Double capaciteKg;

    private Long version;

    public RessourceRequestDTO() {
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

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
