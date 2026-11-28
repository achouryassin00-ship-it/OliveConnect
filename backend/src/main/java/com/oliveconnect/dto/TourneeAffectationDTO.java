package com.oliveconnect.dto;

import java.time.Instant;
import java.util.List;

public class TourneeAffectationDTO {

    private Instant debut;
    private Instant fin;
    private String chefEquipeId;
    private List<String> recolteIds;
    private List<String> ouvrierIds;
    private List<String> tracteurIds;
    private List<String> benneIds;
    private Long version;

    public TourneeAffectationDTO() {
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
        this.recolteIds = recolteIds;
    }

    public List<String> getOuvrierIds() {
        return ouvrierIds;
    }

    public void setOuvrierIds(List<String> ouvrierIds) {
        this.ouvrierIds = ouvrierIds;
    }

    public List<String> getTracteurIds() {
        return tracteurIds;
    }

    public void setTracteurIds(List<String> tracteurIds) {
        this.tracteurIds = tracteurIds;
    }

    public List<String> getBenneIds() {
        return benneIds;
    }

    public void setBenneIds(List<String> benneIds) {
        this.benneIds = benneIds;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
