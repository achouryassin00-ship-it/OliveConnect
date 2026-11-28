package com.oliveconnect.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class TourneeRequestDTO {

    @NotBlank(message = "Le code de la tournée est obligatoire.")
    @Size(min = 2, max = 50, message = "Le code doit comporter entre 2 et 50 caractères.")
    private String code;

    @NotNull(message = "La date et l'heure de début sont obligatoires.")
    private Instant debut;

    @NotNull(message = "La date et l'heure de fin sont obligatoires.")
    private Instant fin;

    private String chefEquipeId;

    private List<String> recolteIds = new ArrayList<>();

    private List<String> ouvrierIds = new ArrayList<>();

    private List<String> tracteurIds = new ArrayList<>();

    private List<String> benneIds = new ArrayList<>();

    private Long version;

    public TourneeRequestDTO() {
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Instant getDebut() {
        return debut;
    }

    public void setDebut(Instant debut) {
        this.debut = debut;
    }

    public Instant getFin() {
        return fin;
    }

    public void setFin(Instant fin) {
        this.fin = fin;
    }

    public String getChefEquipeId() {
        return chefEquipeId;
    }

    public void setChefEquipeId(String chefEquipeId) {
        this.chefEquipeId = chefEquipeId;
    }

    public List<String> getRecolteIds() {
        return recolteIds;
    }

    public void setRecolteIds(List<String> recolteIds) {
        this.recolteIds = recolteIds != null ? recolteIds : new ArrayList<>();
    }

    public List<String> getOuvrierIds() {
        return ouvrierIds;
    }

    public void setOuvrierIds(List<String> ouvrierIds) {
        this.ouvrierIds = ouvrierIds != null ? ouvrierIds : new ArrayList<>();
    }

    public List<String> getTracteurIds() {
        return tracteurIds;
    }

    public void setTracteurIds(List<String> tracteurIds) {
        this.tracteurIds = tracteurIds != null ? tracteurIds : new ArrayList<>();
    }

    public List<String> getBenneIds() {
        return benneIds;
    }

    public void setBenneIds(List<String> benneIds) {
        this.benneIds = benneIds != null ? benneIds : new ArrayList<>();
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
