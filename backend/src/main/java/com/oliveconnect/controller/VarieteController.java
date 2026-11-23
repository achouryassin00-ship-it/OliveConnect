package com.oliveconnect.controller;

import com.oliveconnect.dto.VarieteResponseDTO;
import com.oliveconnect.service.VarieteService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/varietes")
public class VarieteController {

    private final VarieteService varieteService;

    public VarieteController(VarieteService varieteService) {
        this.varieteService = varieteService;
    }

    /**
     * Liste des variétés d'olives actives de la coopérative.
     * Accessible à tout utilisateur authentifié.
     */
    @GetMapping
    public ResponseEntity<List<VarieteResponseDTO>> getActiveVarietes() {
        return ResponseEntity.ok(varieteService.getActiveVarietes());
    }
}
