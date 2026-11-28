package com.oliveconnect.controller;

import com.oliveconnect.dto.CalendrierRessourceDTO;
import com.oliveconnect.dto.PageResponse;
import com.oliveconnect.dto.RessourceRequestDTO;
import com.oliveconnect.dto.RessourceResponseDTO;
import com.oliveconnect.entity.StatutRessource;
import com.oliveconnect.entity.TypeRessource;
import com.oliveconnect.service.RessourceService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.Instant;

@RestController
@RequestMapping("/api/ressources")
public class RessourceController {

    private final RessourceService ressourceService;

    public RessourceController(RessourceService ressourceService) {
        this.ressourceService = ressourceService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('RESPONSABLE_COLLECTE', 'RESPONSABLE_LOGISTIQUE', 'CHEF_EQUIPE', 'ADMINISTRATEUR')")
    public ResponseEntity<PageResponse<RessourceResponseDTO>> getRessources(
            @RequestParam(required = false) TypeRessource type,
            @RequestParam(required = false) StatutRessource statut,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ressourceService.getRessources(type, statut, page, size));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('RESPONSABLE_LOGISTIQUE', 'ADMINISTRATEUR')")
    public ResponseEntity<RessourceResponseDTO> createRessource(@Valid @RequestBody RessourceRequestDTO request) {
        RessourceResponseDTO created = ressourceService.createRessource(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.getId())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('RESPONSABLE_COLLECTE', 'RESPONSABLE_LOGISTIQUE', 'CHEF_EQUIPE', 'ADMINISTRATEUR')")
    public ResponseEntity<RessourceResponseDTO> getRessourceById(@PathVariable String id) {
        return ResponseEntity.ok(ressourceService.getRessourceById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('RESPONSABLE_LOGISTIQUE', 'ADMINISTRATEUR')")
    public ResponseEntity<RessourceResponseDTO> updateRessource(
            @PathVariable String id,
            @Valid @RequestBody RessourceRequestDTO request) {
        return ResponseEntity.ok(ressourceService.updateRessource(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('RESPONSABLE_LOGISTIQUE', 'ADMINISTRATEUR')")
    public ResponseEntity<Void> deleteRessource(@PathVariable String id) {
        ressourceService.deleteRessource(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/calendrier")
    @PreAuthorize("hasAnyRole('RESPONSABLE_COLLECTE', 'RESPONSABLE_LOGISTIQUE', 'CHEF_EQUIPE', 'ADMINISTRATEUR')")
    public ResponseEntity<CalendrierRessourceDTO> getCalendrierRessource(
            @PathVariable String id,
            @RequestParam(required = false) Instant debut,
            @RequestParam(required = false) Instant fin) {
        return ResponseEntity.ok(ressourceService.getCalendrierRessource(id, debut, fin));
    }
}
