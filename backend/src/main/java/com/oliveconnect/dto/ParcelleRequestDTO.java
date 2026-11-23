package com.oliveconnect.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public class ParcelleRequestDTO {

    @NotBlank(message = "Le nom de la parcelle est obligatoire.")
    @Size(min = 2, max = 150, message = "Le nom doit comporter entre 2 et 150 caractères.")
    private String nom;

    @NotBlank(message = "L'identifiant de la variété est obligatoire.")
    private String varieteId;

    @NotNull(message = "La surface en hectares est obligatoire.")
    @Positive(message = "La surface doit être strictement positive.")
    private Double surfaceHa;

    private Long version;

    public ParcelleRequestDTO() {
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

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
