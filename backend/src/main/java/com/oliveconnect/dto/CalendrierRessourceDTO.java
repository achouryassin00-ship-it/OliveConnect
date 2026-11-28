package com.oliveconnect.dto;

import com.oliveconnect.entity.StatutRessource;
import com.oliveconnect.entity.StatutTournee;
import com.oliveconnect.entity.TypeRessource;

import java.time.Instant;
import java.util.List;

public class CalendrierRessourceDTO {

    private String ressourceId;
    private String ressourceCode;
    private String ressourceLibelle;
    private TypeRessource type;
    private StatutRessource statut;
    private List<CreneauTourneeDTO> creneaux;

    public CalendrierRessourceDTO() {
    }

    public CalendrierRessourceDTO(String ressourceId, String ressourceCode, String ressourceLibelle,
                                  TypeRessource type, StatutRessource statut, List<CreneauTourneeDTO> creneaux) {
        this.ressourceId = ressourceId;
        this.ressourceCode = ressourceCode;
        this.ressourceLibelle = ressourceLibelle;
        this.type = type;
        this.statut = statut;
        this.creneaux = creneaux;
    }

    public record CreneauTourneeDTO(
            String tourneeId,
            String tourneeCode,
            Instant debut,
            Instant fin,
            StatutTournee statut
    ) {
    }

    public String getRessourceId() {
        return ressourceId;
    }

    public void setRessourceId(String ressourceId) {
        this.ressourceId = ressourceId;
    }

    public String getRessourceCode() {
        return ressourceCode;
    }

    public void setRessourceCode(String ressourceCode) {
        this.ressourceCode = ressourceCode;
    }

    public String getRessourceLibelle() {
        return ressourceLibelle;
    }

    public void setRessourceLibelle(String ressourceLibelle) {
        this.ressourceLibelle = ressourceLibelle;
    }

    public TypeRessource getType() {
        return type;
    }

    public void setType(TypeRessource type) {
        this.type = type;
    }

    public StatutRessource getStatut() {
        return statut;
    }

    public void setStatut(StatutRessource statut) {
        this.statut = statut;
    }

    public List<CreneauTourneeDTO> getCreneaux() {
        return creneaux;
    }

    public void setCreneaux(List<CreneauTourneeDTO> creneaux) {
        this.creneaux = creneaux;
    }
}
