package com.oliveconnect.dto;

import com.oliveconnect.entity.StatutRecolte;
import jakarta.validation.constraints.Size;

public class RecolteTransitionRequestDTO {

    private StatutRecolte statutCible;

    private Long version;

    @Size(max = 500, message = "Le motif ne doit pas dépasser 500 caractères.")
    private String motif;

    public RecolteTransitionRequestDTO() {
    }

    public StatutRecolte getStatutCible() {
        return statutCible;
    }

    public void setStatutCible(StatutRecolte statutCible) {
        this.statutCible = statutCible;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public String getMotif() {
        return motif;
    }

    public void setMotif(String motif) {
        this.motif = motif;
    }
}
