package com.oliveconnect.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public class RecolteRequestDTO {

    @NotBlank(message = "L'identifiant du verger est obligatoire.")
    private String vergerId;

    @NotNull(message = "La date souhaitée est obligatoire.")
    private LocalDate dateSouhaitee;

    private Double quantiteEstimeeKg;

    private boolean urgent;

    @Size(max = 500, message = "Le motif ne doit pas dépasser 500 caractères.")
    private String motifUrgence;

    public RecolteRequestDTO() {
    }

    public String getVergerId() {
        return vergerId;
    }

    public void setVergerId(String vergerId) {
        this.vergerId = vergerId;
    }

    public LocalDate getDateSouhaitee() {
        return dateSouhaitee;
    }

    public void setDateSouhaitee(LocalDate dateSouhaitee) {
        this.dateSouhaitee = dateSouhaitee;
    }

    public Double getQuantiteEstimeeKg() {
        return quantiteEstimeeKg;
    }

    public void setQuantiteEstimeeKg(Double quantiteEstimeeKg) {
        this.quantiteEstimeeKg = quantiteEstimeeKg;
    }

    public boolean isUrgent() {
        return urgent;
    }

    public void setUrgent(boolean urgent) {
        this.urgent = urgent;
    }

    public String getMotifUrgence() {
        return motifUrgence;
    }

    public void setMotifUrgence(String motifUrgence) {
        this.motifUrgence = motifUrgence;
    }
}
