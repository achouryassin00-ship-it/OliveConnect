package com.oliveconnect.dto;

import com.oliveconnect.entity.SourcePosition;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public class VergerRequestDTO {

    @NotBlank(message = "Le code du verger est obligatoire.")
    @Size(min = 2, max = 50, message = "Le code doit comporter entre 2 et 50 caractères.")
    private String code;

    @NotBlank(message = "Le nom du verger est obligatoire.")
    @Size(min = 2, max = 150, message = "Le nom doit comporter entre 2 et 150 caractères.")
    private String nom;

    private String commune;

    @NotNull(message = "La longitude est obligatoire.")
    private Double longitude;

    @NotNull(message = "La latitude est obligatoire.")
    private Double latitude;

    private Double precisionGpsMetres;

    private SourcePosition sourcePosition;

    @NotNull(message = "La surface en hectares est obligatoire.")
    @Positive(message = "La surface doit être strictement positive.")
    private Double surfaceHa;

    private Integer nombreArbres;

    private Integer anneePlantation;

    private Long version;

    public VergerRequestDTO() {
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

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
