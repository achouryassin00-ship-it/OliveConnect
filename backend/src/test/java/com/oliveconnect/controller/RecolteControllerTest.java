package com.oliveconnect.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oliveconnect.dto.HistoriqueStatutDTO;
import com.oliveconnect.dto.PageResponse;
import com.oliveconnect.dto.RecolteRequestDTO;
import com.oliveconnect.dto.RecolteResponseDTO;
import com.oliveconnect.dto.RecolteTransitionRequestDTO;
import com.oliveconnect.entity.StatutRecolte;
import com.oliveconnect.exception.BusinessConflictException;
import com.oliveconnect.security.CustomUserDetailsService;
import com.oliveconnect.security.JwtService;
import com.oliveconnect.security.SecurityConfig;
import com.oliveconnect.security.SecurityService;
import com.oliveconnect.service.RecolteService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RecolteController.class)
@Import({SecurityConfig.class, SecurityService.class})
class RecolteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RecolteService recolteService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    private RecolteResponseDTO sampleResponse() {
        RecolteResponseDTO dto = new RecolteResponseDTO();
        dto.setId("rec-1");
        dto.setReference("REC-2026-1234ABCD");
        dto.setProprietaireId("farmer-1");
        dto.setVergerId("vrg-1");
        dto.setVergerNom("Verger El Hencha");
        dto.setVergerCode("VRG-SFX-001");
        dto.setDateSouhaitee(LocalDate.of(2026, 11, 28));
        dto.setQuantiteEstimeeKg(2400.0);
        dto.setUrgent(false);
        dto.setStatut(StatutRecolte.DEMANDEE);
        dto.setAnnulable(true);
        dto.setVersion(0L);
        dto.setCreatedAt(Instant.parse("2026-11-25T10:00:00Z"));
        HistoriqueStatutDTO h = new HistoriqueStatutDTO();
        h.setNouveauStatut(StatutRecolte.DEMANDEE);
        h.setActeurId("farmer-1");
        h.setHorodatage(Instant.parse("2026-11-25T10:00:00Z"));
        dto.setHistorique(List.of(h));
        return dto;
    }

    @Test
    @DisplayName("GET /api/recoltes sans jeton : retourne 401 UNAUTHORIZED")
    void testGetRecoltesWithoutAuth() throws Exception {
        mockMvc.perform(get("/api/recoltes"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    @WithMockUser(username = "collecte-1", roles = {"RESPONSABLE_COLLECTE"})
    @DisplayName("POST /api/recoltes sans rôle AGRICULTEUR : retourne 403 ACCESS_DENIED")
    void testCreateRecolteForbiddenForStaff() throws Exception {
        RecolteRequestDTO req = new RecolteRequestDTO();
        req.setVergerId("vrg-1");
        req.setDateSouhaitee(LocalDate.of(2026, 11, 28));

        mockMvc.perform(post("/api/recoltes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    @WithMockUser(username = "farmer-1", roles = {"AGRICULTEUR"})
    @DisplayName("POST /api/recoltes nominal et rejeu idempotent : 201 puis 200 avec X-Idempotent-Replay")
    void testCreateAndIdempotentReplay() throws Exception {
        RecolteResponseDTO response = sampleResponse();
        when(recolteService.createRecolte(any(RecolteRequestDTO.class), eq("idem-1")))
                .thenReturn(new RecolteService.CreationResult(response, false))
                .thenReturn(new RecolteService.CreationResult(response, true));

        String payloadWithForgedOwner = """
                {
                  "vergerId": "vrg-1",
                  "proprietaireId": "forged-other-farmer",
                  "dateSouhaitee": "2026-11-28",
                  "quantiteEstimeeKg": 2400.0,
                  "urgent": false
                }
                """;

        mockMvc.perform(post("/api/recoltes")
                        .header("Idempotency-Key", "idem-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payloadWithForgedOwner))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/recoltes/rec-1"))
                .andExpect(jsonPath("$.proprietaireId").value("farmer-1"))
                .andExpect(jsonPath("$.reference").value("REC-2026-1234ABCD"));

        mockMvc.perform(post("/api/recoltes")
                        .header("Idempotency-Key", "idem-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payloadWithForgedOwner))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Idempotent-Replay", "true"))
                .andExpect(jsonPath("$.id").value("rec-1"));
    }

    @Test
    @WithMockUser(username = "farmer-1", roles = {"AGRICULTEUR"})
    @DisplayName("POST /api/recoltes date JSON invalide : retourne 400 VALIDATION_ERROR")
    void testCreateRecolteInvalidDateJson() throws Exception {
        String badJson = """
                {
                  "vergerId": "vrg-1",
                  "dateSouhaitee": "2026-02-30"
                }
                """;

        mockMvc.perform(post("/api/recoltes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(badJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors.dateSouhaitee").exists());
    }

    @Test
    @WithMockUser(username = "farmer-1", roles = {"AGRICULTEUR"})
    @DisplayName("GET /api/recoltes avec filtres : retourne 200 PageResponse")
    void testListRecoltesWithFilters() throws Exception {
        PageResponse<RecolteResponseDTO> page = new PageResponse<>(List.of(sampleResponse()), 0, 20, 1, 1);
        when(recolteService.getRecoltes(0, 20, "Hencha", StatutRecolte.DEMANDEE, false, "vrg-1", null, null))
                .thenReturn(page);

        mockMvc.perform(get("/api/recoltes")
                        .param("page", "0")
                        .param("size", "20")
                        .param("q", "Hencha")
                        .param("statut", "DEMANDEE")
                        .param("urgent", "false")
                        .param("vergerId", "vrg-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].vergerNom").value("Verger El Hencha"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @WithMockUser(username = "farmer-1", roles = {"AGRICULTEUR"})
    @DisplayName("POST /api/recoltes/{id}/annuler conflit : retourne 409 RESOURCE_CONFLICT")
    void testCancelConflict() throws Exception {
        when(recolteService.annulerRecolte(eq("rec-1"), any()))
                .thenThrow(new BusinessConflictException("Impossible d'annuler une demande au statut PLANIFIEE."));

        RecolteTransitionRequestDTO req = new RecolteTransitionRequestDTO();
        req.setMotif("Changement");

        mockMvc.perform(post("/api/recoltes/rec-1/annuler")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RESOURCE_CONFLICT"));
    }
}
