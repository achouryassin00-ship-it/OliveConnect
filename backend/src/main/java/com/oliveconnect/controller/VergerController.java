package com.oliveconnect.controller;

import com.oliveconnect.dto.*;
import com.oliveconnect.service.VergerService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/vergers")
public class VergerController {

    private final VergerService vergerService;

    public VergerController(VergerService vergerService) {
        this.vergerService = vergerService;
    }

    /**
     * Liste paginée des vergers.
     * Agriculteur : voit uniquement ses vergers actifs.
     * Responsable collecte / Admin : voient tous les vergers actifs.
     */
    @GetMapping
    public ResponseEntity<PageResponse<VergerResponseDTO>> getVergers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(vergerService.getVergers(page, size));
    }

    /**
     * Consultation d'un verger par son identifiant.
     * AUT-01 : Contrôle IDOR (404 si non possédé).
     */
    @GetMapping("/{id}")
    public ResponseEntity<VergerResponseDTO> getVergerById(@PathVariable String id) {
        return ResponseEntity.ok(vergerService.getVergerById(id));
    }

    /**
     * Création d'un nouveau verger.
     * AUT-02 : Le propriétaire est extrait de la session courante.
     */
    @PostMapping
    public ResponseEntity<VergerResponseDTO> createVerger(@Valid @RequestBody VergerRequestDTO request) {
        VergerResponseDTO created = vergerService.createVerger(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.getId())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    /**
     * Mise à jour d'un verger existant.
     * INT-07 : Code immuable.
     * Optimistic locking via version.
     */
    @PutMapping("/{id}")
    public ResponseEntity<VergerResponseDTO> updateVerger(
            @PathVariable String id,
            @Valid @RequestBody VergerRequestDTO request) {
        return ResponseEntity.ok(vergerService.updateVerger(id, request));
    }

    /**
     * Suppression logique d'un verger (actif = false) et de ses parcelles.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteVerger(@PathVariable String id) {
        vergerService.deleteVerger(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Liste des parcelles actives associées à un verger.
     */
    @GetMapping("/{id}/parcelles")
    public ResponseEntity<List<ParcelleResponseDTO>> getParcellesByVergerId(@PathVariable String id) {
        return ResponseEntity.ok(vergerService.getParcellesByVergerId(id));
    }

    /**
     * Création d'une nouvelle parcelle rattachée au verger.
     * VER-07 : Somme des surfaces des parcelles <= surface du verger.
     * VER-15 : Référence vers une variété valide et active.
     */
    @PostMapping("/{id}/parcelles")
    public ResponseEntity<ParcelleResponseDTO> createParcelle(
            @PathVariable String id,
            @Valid @RequestBody ParcelleRequestDTO request) {
        ParcelleResponseDTO created = vergerService.createParcelle(id, request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{parcelleId}")
                .buildAndExpand(created.getId())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    /**
     * Mise à jour d'une parcelle existante.
     */
    @PutMapping("/{id}/parcelles/{parcelleId}")
    public ResponseEntity<ParcelleResponseDTO> updateParcelle(
            @PathVariable String id,
            @PathVariable String parcelleId,
            @Valid @RequestBody ParcelleRequestDTO request) {
        return ResponseEntity.ok(vergerService.updateParcelle(id, parcelleId, request));
    }

    /**
     * Suppression logique d'une parcelle (actif = false).
     */
    @DeleteMapping("/{id}/parcelles/{parcelleId}")
    public ResponseEntity<Void> deleteParcelle(
            @PathVariable String id,
            @PathVariable String parcelleId) {
        vergerService.deleteParcelle(id, parcelleId);
        return ResponseEntity.noContent().build();
    }
}
