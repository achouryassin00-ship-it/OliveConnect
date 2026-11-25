package com.oliveconnect.controller;

import com.oliveconnect.dto.PageResponse;
import com.oliveconnect.dto.RecolteRequestDTO;
import com.oliveconnect.dto.RecolteResponseDTO;
import com.oliveconnect.dto.RecolteTransitionRequestDTO;
import com.oliveconnect.entity.StatutRecolte;
import com.oliveconnect.service.RecolteService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/recoltes")
public class RecolteController {

    private final RecolteService recolteService;

    public RecolteController(RecolteService recolteService) {
        this.recolteService = recolteService;
    }

    /**
     * Création d'une demande de collecte par un agriculteur authentifié.
     * Supporte l'idempotence via l'en-tête Idempotency-Key.
     */
    @PostMapping
    @PreAuthorize("hasRole('AGRICULTEUR')")
    public ResponseEntity<RecolteResponseDTO> createRecolte(
            @Valid @RequestBody RecolteRequestDTO request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        RecolteService.CreationResult result = recolteService.createRecolte(request, idempotencyKey);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(result.recolte().getId())
                .toUri();
        if (result.idempotentReplay()) {
            return ResponseEntity.ok()
                    .location(location)
                    .header("X-Idempotent-Replay", "true")
                    .body(result.recolte());
        }
        return ResponseEntity.created(location).body(result.recolte());
    }

    /**
     * Liste paginée des demandes de collecte avec recherche et filtres métier.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('AGRICULTEUR', 'RESPONSABLE_COLLECTE', 'ADMINISTRATEUR')")
    public ResponseEntity<PageResponse<RecolteResponseDTO>> getRecoltes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) StatutRecolte statut,
            @RequestParam(required = false) Boolean urgent,
            @RequestParam(required = false) String vergerId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin) {
        return ResponseEntity.ok(
                recolteService.getRecoltes(page, size, q, statut, urgent, vergerId, dateDebut, dateFin)
        );
    }

    /**
     * Consultation détaillée d'une demande de collecte avec historique persisté.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('AGRICULTEUR', 'RESPONSABLE_COLLECTE', 'ADMINISTRATEUR')")
    public ResponseEntity<RecolteResponseDTO> getRecolteById(@PathVariable String id) {
        return ResponseEntity.ok(recolteService.getRecolteById(id));
    }

    /**
     * Annulation d'une demande de collecte tant qu'elle n'est ni planifiée, ni démarrée, ni clôturée.
     */
    @PostMapping("/{id}/annuler")
    @PreAuthorize("hasAnyRole('AGRICULTEUR', 'RESPONSABLE_COLLECTE', 'ADMINISTRATEUR')")
    public ResponseEntity<RecolteResponseDTO> annulerRecolte(
            @PathVariable String id,
            @Valid @RequestBody(required = false) RecolteTransitionRequestDTO request) {
        return ResponseEntity.ok(recolteService.annulerRecolte(id, request));
    }

    /**
     * Transition officielle de statut d'une demande de collecte.
     */
    @PostMapping("/{id}/transitions")
    @PreAuthorize("hasAnyRole('AGRICULTEUR', 'RESPONSABLE_COLLECTE', 'ADMINISTRATEUR')")
    public ResponseEntity<RecolteResponseDTO> transitionStatut(
            @PathVariable String id,
            @Valid @RequestBody RecolteTransitionRequestDTO request) {
        return ResponseEntity.ok(recolteService.transitionStatut(id, request));
    }
}
