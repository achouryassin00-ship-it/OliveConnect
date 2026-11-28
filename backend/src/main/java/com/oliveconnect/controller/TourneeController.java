package com.oliveconnect.controller;

import com.oliveconnect.dto.PageResponse;
import com.oliveconnect.dto.TourneeActionDTO;
import com.oliveconnect.dto.TourneeAffectationDTO;
import com.oliveconnect.dto.TourneeRequestDTO;
import com.oliveconnect.dto.TourneeResponseDTO;
import com.oliveconnect.service.TourneeService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.Instant;

@RestController
@RequestMapping("/api/tournees")
public class TourneeController {

    private final TourneeService tourneeService;

    public TourneeController(TourneeService tourneeService) {
        this.tourneeService = tourneeService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('RESPONSABLE_COLLECTE', 'RESPONSABLE_LOGISTIQUE', 'ADMINISTRATEUR')")
    public ResponseEntity<TourneeResponseDTO> createTournee(@Valid @RequestBody TourneeRequestDTO request) {
        TourneeResponseDTO created = tourneeService.createTournee(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.getId())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('RESPONSABLE_COLLECTE', 'RESPONSABLE_LOGISTIQUE', 'CHEF_EQUIPE', 'ADMINISTRATEUR')")
    public ResponseEntity<PageResponse<TourneeResponseDTO>> getTournees(
            @RequestParam(required = false) Instant debut,
            @RequestParam(required = false) Instant fin,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(tourneeService.getTournees(debut, fin, page, size));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('RESPONSABLE_COLLECTE', 'RESPONSABLE_LOGISTIQUE', 'CHEF_EQUIPE', 'ADMINISTRATEUR')")
    public ResponseEntity<TourneeResponseDTO> getTourneeById(@PathVariable String id) {
        return ResponseEntity.ok(tourneeService.getTourneeById(id));
    }

    @PutMapping("/{id}/affectations")
    @PreAuthorize("hasAnyRole('RESPONSABLE_COLLECTE', 'RESPONSABLE_LOGISTIQUE', 'ADMINISTRATEUR')")
    public ResponseEntity<TourneeResponseDTO> updateAffectations(
            @PathVariable String id,
            @Valid @RequestBody TourneeAffectationDTO request) {
        return ResponseEntity.ok(tourneeService.updateAffectations(id, request));
    }

    @PostMapping("/{id}/planifier")
    @PreAuthorize("hasAnyRole('RESPONSABLE_COLLECTE', 'RESPONSABLE_LOGISTIQUE', 'ADMINISTRATEUR')")
    public ResponseEntity<TourneeResponseDTO> planifierTournee(
            @PathVariable String id,
            @RequestBody(required = false) TourneeActionDTO request) {
        Long version = request != null ? request.getVersion() : null;
        return ResponseEntity.ok(tourneeService.planifierTournee(id, version));
    }

    @PostMapping("/{id}/annuler")
    @PreAuthorize("hasAnyRole('RESPONSABLE_COLLECTE', 'RESPONSABLE_LOGISTIQUE', 'ADMINISTRATEUR')")
    public ResponseEntity<TourneeResponseDTO> annulerTournee(
            @PathVariable String id,
            @RequestBody(required = false) TourneeActionDTO request) {
        Long version = request != null ? request.getVersion() : null;
        return ResponseEntity.ok(tourneeService.annulerTournee(id, version));
    }
}
