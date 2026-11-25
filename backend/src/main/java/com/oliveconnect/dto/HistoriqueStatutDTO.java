package com.oliveconnect.dto;

import com.oliveconnect.entity.HistoriqueStatutRecolte;
import com.oliveconnect.entity.StatutRecolte;

import java.time.Instant;

public class HistoriqueStatutDTO {

    private StatutRecolte ancienStatut;
    private StatutRecolte nouveauStatut;
    private String acteurId;
    private String acteurNom;
    private String acteurRole;
    private String motif;
    private Instant horodatage;

    public HistoriqueStatutDTO() {
    }

    public static HistoriqueStatutDTO fromEntity(HistoriqueStatutRecolte entry) {
        if (entry == null) return null;
        HistoriqueStatutDTO dto = new HistoriqueStatutDTO();
        dto.setAncienStatut(entry.getAncienStatut());
        dto.setNouveauStatut(entry.getNouveauStatut());
        dto.setActeurId(entry.getActeurId());
        dto.setActeurNom(entry.getActeurNom());
        dto.setActeurRole(entry.getActeurRole());
        dto.setMotif(entry.getMotif());
        dto.setHorodatage(entry.getHorodatage());
        return dto;
    }

    public StatutRecolte getAncienStatut() {
        return ancienStatut;
    }

    public void setAncienStatut(StatutRecolte ancienStatut) {
        this.ancienStatut = ancienStatut;
    }

    public StatutRecolte getNouveauStatut() {
        return nouveauStatut;
    }

    public void setNouveauStatut(StatutRecolte nouveauStatut) {
        this.nouveauStatut = nouveauStatut;
    }

    public String getActeurId() {
        return acteurId;
    }

    public void setActeurId(String acteurId) {
        this.acteurId = acteurId;
    }

    public String getActeurNom() {
        return acteurNom;
    }

    public void setActeurNom(String acteurNom) {
        this.acteurNom = acteurNom;
    }

    public String getActeurRole() {
        return acteurRole;
    }

    public void setActeurRole(String acteurRole) {
        this.acteurRole = acteurRole;
    }

    public String getMotif() {
        return motif;
    }

    public void setMotif(String motif) {
        this.motif = motif;
    }

    public Instant getHorodatage() {
        return horodatage;
    }

    public void setHorodatage(Instant horodatage) {
        this.horodatage = horodatage;
    }
}
