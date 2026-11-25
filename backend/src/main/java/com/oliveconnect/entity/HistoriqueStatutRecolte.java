package com.oliveconnect.entity;

import java.time.Instant;

/**
 * Entrée d'historique persistée lors de chaque création ou changement de statut d'une demande de collecte.
 */
public class HistoriqueStatutRecolte {

    private StatutRecolte ancienStatut;
    private StatutRecolte nouveauStatut;
    private String acteurId;
    private String acteurNom;
    private String acteurRole;
    private String motif;
    private Instant horodatage;

    public HistoriqueStatutRecolte() {
    }

    public HistoriqueStatutRecolte(
            StatutRecolte ancienStatut,
            StatutRecolte nouveauStatut,
            String acteurId,
            String acteurNom,
            String acteurRole,
            String motif,
            Instant horodatage) {
        this.ancienStatut = ancienStatut;
        this.nouveauStatut = nouveauStatut;
        this.acteurId = acteurId;
        this.acteurNom = acteurNom;
        this.acteurRole = acteurRole;
        this.motif = motif;
        this.horodatage = horodatage;
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
